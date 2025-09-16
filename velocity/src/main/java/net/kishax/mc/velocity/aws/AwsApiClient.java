package net.kishax.mc.velocity.aws;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

/**
 * AWS API Gateway クライアント
 * AWS Signature Version 4 を使用してAPI Gateway にリクエストを送信
 */
public class AwsApiClient {
  private static final Logger logger = LoggerFactory.getLogger(AwsApiClient.class);

  private static final String AWS_ALGORITHM = "AWS4-HMAC-SHA256";
  private static final String AWS_REQUEST = "aws4_request";
  private static final DateTimeFormatter ISO_BASIC_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
  private static final DateTimeFormatter AMZ_DATE = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

  private final String region;
  private final String serviceName;
  private final String accessKey;
  private final String secretKey;
  private final String apiGatewayUrl;
  private final HttpClient httpClient;
  private ObjectMapper objectMapper;

  public AwsApiClient(String region, String serviceName, String accessKey, String secretKey, String apiGatewayUrl) {
    this.region = region;
    this.serviceName = serviceName;
    this.accessKey = accessKey;
    this.secretKey = secretKey;
    this.apiGatewayUrl = apiGatewayUrl;
    this.httpClient = HttpClient.newHttpClient();
  }

  private ObjectMapper getObjectMapper() {
    if (objectMapper == null) {
      objectMapper = new ObjectMapper();
    }
    return objectMapper;
  }

  /**
   * Discord Bot にメッセージ送信リクエストを送信
   */
  public CompletableFuture<Void> sendDiscordMessage(String type, Map<String, Object> payload) {
    Map<String, Object> requestBody = new TreeMap<>();
    requestBody.put("type", type);
    requestBody.putAll(payload);

    return sendPostRequest("/discord", requestBody);
  }

  /**
   * サーバーステータス更新リクエストを送信
   */
  public CompletableFuture<Void> sendServerStatus(String serverName, String status) {
    Map<String, Object> payload = new TreeMap<>();
    payload.put("type", "server_status");
    payload.put("serverName", serverName);
    payload.put("status", status);

    return sendPostRequest("/discord", payload);
  }

  /**
   * プレイヤーイベントリクエストを送信
   */
  public CompletableFuture<Void> sendPlayerEvent(String eventType, String playerName, String playerUuid,
      String serverName) {
    Map<String, Object> payload = new TreeMap<>();
    payload.put("type", "player_event");
    payload.put("eventType", eventType);
    payload.put("playerName", playerName);
    payload.put("playerUuid", playerUuid);
    payload.put("serverName", serverName);

    return sendPostRequest("/discord", payload);
  }

  /**
   * チャットメッセージリクエストを送信
   */
  public CompletableFuture<Void> sendChatMessage(String playerName, String playerUuid, String message) {
    Map<String, Object> payload = new TreeMap<>();
    payload.put("type", "player_event");
    payload.put("eventType", "chat");
    payload.put("playerName", playerName);
    payload.put("playerUuid", playerUuid);
    payload.put("message", message);

    return sendPostRequest("/discord", payload);
  }

  /**
   * Embedメッセージリクエストを送信
   */
  public CompletableFuture<Void> sendEmbedMessage(String content, int color, String channelId, String messageId,
      boolean edit) {
    Map<String, Object> payload = new TreeMap<>();
    payload.put("type", "embed");
    payload.put("content", content);
    payload.put("color", color);
    if (channelId != null)
      payload.put("channelId", channelId);
    if (messageId != null)
      payload.put("messageId", messageId);
    payload.put("edit", edit);

    return sendPostRequest("/discord", payload);
  }

  /**
   * POSTリクエストを送信
   */
  private CompletableFuture<Void> sendPostRequest(String path, Map<String, Object> payload) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        String jsonBody = getObjectMapper().writeValueAsString(payload);

        Instant now = Instant.now();
        String amzDate = now.atOffset(ZoneOffset.UTC).format(AMZ_DATE);
        String dateStamp = now.atOffset(ZoneOffset.UTC).format(ISO_BASIC_DATE);

        // HTTPリクエストを構築
        String baseUrl = apiGatewayUrl;
        if (apiGatewayUrl.endsWith("/")) {
          baseUrl = apiGatewayUrl.substring(0, apiGatewayUrl.length() - 1);
        }

        String requestPath = path;
        if (path.startsWith("/")) {
          requestPath = path.substring(1);
        }

        String finalUrl;
        URI baseUri = URI.create(baseUrl);
        String basePath = baseUri.getPath();

        // apiGatewayUrlのパス部分がリクエストパスで終わっているかチェック
        if (basePath != null && !basePath.isEmpty() && basePath.endsWith("/" + requestPath)) {
          finalUrl = baseUrl;
        } else {
          finalUrl = baseUrl + "/" + requestPath;
        }

        URI uri = URI.create(finalUrl);
        String canonicalPath = uri.getPath();
        if (canonicalPath == null || canonicalPath.isEmpty()) {
          canonicalPath = "/";
        }

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(uri)
            .header("Content-Type", "application/json")
            .header("X-Amz-Date", amzDate)
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8));

        // AWS Signature Version 4 の認証ヘッダーを生成
        String authorizationHeader = generateAuthorizationHeader(
            "POST", canonicalPath, "", jsonBody, amzDate, dateStamp);
        requestBuilder.header("Authorization", authorizationHeader);

        HttpRequest request = requestBuilder.build();

        // リクエスト送信
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
          logger.debug("API Gateway リクエスト成功: {} {}", path, response.statusCode());
        } else {
          logger.error("API Gateway リクエスト失敗: {} {} - {}", path, response.statusCode(), response.body());
          throw new RuntimeException("API Gateway リクエストが失敗しました: " + response.statusCode());
        }

        return null;
      } catch (Exception e) {
        logger.error("API Gateway リクエストでエラーが発生しました", e);
        throw new RuntimeException(e);
      }
    });
  }

  /**
   * AWS Signature Version 4 の認証ヘッダーを生成
   */
  private String generateAuthorizationHeader(String httpMethod, String path, String queryString,
      String payload, String amzDate, String dateStamp) throws Exception {

    // Step 1: タスク1 - 標準リクエストを作成
    String payloadHash = sha256Hex(payload);
    String canonicalHeaders = "content-type:application/json\n" +
        "host:" + URI.create(apiGatewayUrl).getHost() + "\n" +
        "x-amz-date:" + amzDate + "\n";
    String signedHeaders = "content-type;host;x-amz-date";

    String canonicalRequest = httpMethod + "\n" +
        path + "\n" +
        queryString + "\n" +
        canonicalHeaders + "\n" +
        signedHeaders + "\n" +
        payloadHash;

    // Step 2: タスク2 - 署名用文字列を作成
    String credentialScope = dateStamp + "/" + region + "/" + serviceName + "/" + AWS_REQUEST;
    String canonicalRequestHash = sha256Hex(canonicalRequest);
    String stringToSign = AWS_ALGORITHM + "\n" +
        amzDate + "\n" +
        credentialScope + "\n" +
        canonicalRequestHash;

    // Step 3: タスク3 - 署名を計算
    byte[] signingKey = getSignatureKey(secretKey, dateStamp, region, serviceName);
    String signature = bytesToHex(hmacSha256(stringToSign, signingKey));

    // Step 4: タスク4 - 認証ヘッダーを作成
    return AWS_ALGORITHM + " " +
        "Credential=" + accessKey + "/" + credentialScope + ", " +
        "SignedHeaders=" + signedHeaders + ", " +
        "Signature=" + signature;
  }

  /**
   * SHA256ハッシュを16進文字列として取得
   */
  private String sha256Hex(String data) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
    return bytesToHex(hash);
  }

  /**
   * HMAC-SHA256を計算
   */
  private byte[] hmacSha256(String data, byte[] key) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(key, "HmacSHA256"));
    return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * AWS Signature Version 4 の署名キーを生成
   */
  private byte[] getSignatureKey(String key, String dateStamp, String regionName, String serviceName) throws Exception {
    byte[] kDate = hmacSha256(dateStamp, ("AWS4" + key).getBytes(StandardCharsets.UTF_8));
    byte[] kRegion = hmacSha256(regionName, kDate);
    byte[] kService = hmacSha256(serviceName, kRegion);
    return hmacSha256(AWS_REQUEST, kService);
  }

  /**
   * バイト配列を16進文字列に変換
   */
  private String bytesToHex(byte[] bytes) {
    StringBuilder result = new StringBuilder();
    for (byte b : bytes) {
      result.append(String.format("%02x", b));
    }
    return result.toString();
  }
}
