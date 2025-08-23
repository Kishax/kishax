package net.kishax.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.utils.IoUtils;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MinecraftプラグインからAPI Gateway経由でDiscordメッセージ送信の統合テスト
 * 
 * テストフロー:
 * 1. MinecraftプラグインがAPI Gatewayに POST リクエスト送信
 * 2. API Gateway → Lambda → SQS にメッセージ転送
 * 3. SQSからメッセージ受信確認
 * 4. Discord Botが処理することを想定
 */
@Tag("integration")
public class McPluginApiGatewayTest {

  private static final Logger logger = LoggerFactory.getLogger(McPluginApiGatewayTest.class);
  private SdkHttpClient httpClient;
  private String apiGatewayUrl;
  private String sqsQueueUrl;

  @BeforeEach
  void setUp() {
    httpClient = ApacheHttpClient.builder().build();
    apiGatewayUrl = TestConfig.getApiGatewayUrl();
    sqsQueueUrl = TestConfig.SQS_QUEUE_URL;

    logger.info("Test setup - API Gateway URL: {}", apiGatewayUrl);
    logger.info("Test setup - SQS Queue URL: {}", sqsQueueUrl);
  }

  @Test
  void shouldSendDiscordMessageViaApiGateway() throws Exception {
    // テストメッセージ作成
    Map<String, Object> testMessage = TestUtils.createTestDiscordMessage();
    String messageBody = TestUtils.toJson(testMessage);

    logger.info("Sending test message: {}", messageBody);

    // SQS事前状態確認
    int initialMessageCount = TestUtils.getSQSMessageCount(sqsQueueUrl);
    logger.info("Initial SQS message count: {}", initialMessageCount);

    // API Gateway にリクエスト送信
    int statusCode = sendSignedRequest(messageBody);

    // ステータスコード確認
    assertThat(statusCode)
        .as("API Gateway should return 200 OK")
        .isEqualTo(200);

    logger.info("API Gateway request successful, status: {}", statusCode);

    // SQSメッセージ確認（少し待機してから）
    TestUtils.waitFor(Duration.ofSeconds(3));

    List<Message> messages = TestUtils.receiveSQSMessages(
        sqsQueueUrl,
        10,
        Duration.ofSeconds(5));

    // メッセージが受信されたことを確認
    assertThat(messages)
        .as("Should receive at least one message from SQS")
        .isNotEmpty();

    // メッセージ内容確認
    Message receivedMessage = messages.get(0);
    Map<String, Object> receivedMessageBody = TestUtils.parseJson(receivedMessage.body());

    logger.info("Received SQS message: {}", receivedMessage.body());

    // 送信したメッセージ内容が含まれているか確認
    assertThat(receivedMessageBody)
        .as("Received message should contain original test data")
        .containsEntry("type", testMessage.get("type"))
        .containsEntry("player", testMessage.get("player"));

    // テスト後クリーンアップ - メッセージ削除
    TestUtils.deleteSQSMessage(sqsQueueUrl, receivedMessage.receiptHandle());

    logger.info("Integration test completed successfully");
  }

  /**
   * IAM認証でAPI GatewayにPOSTリクエスト送信
   * 実際のMinecraftプラグインと同じ認証方式を使用
   */
  private int sendSignedRequest(String requestBody) throws Exception {
    // AWS認証情報（本来はMinecraftプラグインのIAMユーザー）
    // テスト用にプロファイル認証を使用
    AwsBasicCredentials credentials = AwsBasicCredentials.create(
        System.getenv("AWS_ACCESS_KEY_ID"),
        System.getenv("AWS_SECRET_ACCESS_KEY"));

    // HTTPリクエスト作成
    SdkHttpFullRequest request = SdkHttpFullRequest.builder()
        .method(SdkHttpMethod.POST)
        .uri(URI.create(apiGatewayUrl))
        .putHeader("Content-Type", "application/json")
        .contentStreamProvider(() -> new java.io.ByteArrayInputStream(requestBody.getBytes()))
        .build();

    // AWS Signature V4 で署名
    Aws4Signer signer = Aws4Signer.create();
    Aws4SignerParams signerParams = Aws4SignerParams.builder()
        .awsCredentials(credentials)
        .signingName("execute-api")
        .signingRegion(TestConfig.AWS_REGION)
        .build();

    SdkHttpFullRequest signedRequest = signer.sign(request, signerParams);

    // リクエスト実行
    HttpExecuteRequest executeRequest = HttpExecuteRequest.builder()
        .request(signedRequest)
        .contentStreamProvider(signedRequest.contentStreamProvider().orElse(null))
        .build();

    HttpExecuteResponse response = httpClient.prepareRequest(executeRequest).call();
    try {
      String responseBody = response.responseBody()
          .map(stream -> {
            try {
              return IoUtils.toUtf8String(stream);
            } catch (Exception e) {
              logger.error("Failed to read response body", e);
              return "";
            }
          })
          .orElse("");

      logger.info("API Gateway response: {} - {}", response.httpResponse().statusCode(), responseBody);

      return response.httpResponse().statusCode();
    } finally {
      // HttpExecuteResponseはcloseメソッドを持たないので削除
    }
  }
}
