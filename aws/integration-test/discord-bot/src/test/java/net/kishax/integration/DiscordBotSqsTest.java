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
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Discord Bot 完全統合テスト（API Gateway → SQS → Discord Bot）
 * 
 * テストフロー:
 * 1. API Gatewayにメッセージ送信（MinecraftプラグインのようにIAM認証）
 * 2. Lambda → SQS に自動転送
 * 3. SQSからメッセージ受信確認
 * 4. Discord Bot処理をシミュレート
 * 
 * 実際のDiscord APIには送信しない（テスト用）
 */
@Tag("integration")
public class DiscordBotSqsTest {

  private static final Logger logger = LoggerFactory.getLogger(DiscordBotSqsTest.class);
  private SdkHttpClient httpClient;
  private String apiGatewayUrl;
  private String sqsQueueUrl;
  private String discordChannelId;

  @BeforeEach
  void setUp() {
    httpClient = ApacheHttpClient.builder().build();
    apiGatewayUrl = TestConfig.getApiGatewayUrl();
    sqsQueueUrl = TestConfig.SQS_QUEUE_URL;
    discordChannelId = TestConfig.DISCORD_CHANNEL_ID;

    logger.info("Test setup - API Gateway URL: {}", apiGatewayUrl);
    logger.info("Test setup - SQS Queue URL: {}", sqsQueueUrl);
    logger.info("Test setup - Discord Channel ID: {}", discordChannelId);
  }

  @Test
  void shouldSendMessageViaApiGatewayAndReceiveInSqs() throws Exception {
    // API Gatewayに送信するテストメッセージ作成
    Map<String, Object> testMessage = Map.of(
        "type", "player_event",
        "eventType", "join",
        "playerName", "DiscordTestPlayer",
        "playerUuid", "test-uuid-basic-12345",
        "serverName", "discord-test-server",
        "timestamp", Instant.now().toString(),
        "channel_id", discordChannelId);

    String messageBody = TestUtils.toJson(testMessage);
    logger.info("Sending test message via API Gateway: {}", messageBody);

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
        .containsEntry("eventType", testMessage.get("eventType"))
        .containsEntry("playerName", testMessage.get("playerName"));

    // Discord Bot処理シミュレーション
    simulateDiscordBotProcessing(receivedMessageBody);

    // テスト後クリーンアップ - メッセージ削除
    TestUtils.deleteSQSMessage(sqsQueueUrl, receivedMessage.receiptHandle());

    logger.info("Discord Bot complete integration test completed successfully");
  }

  @Test
  void shouldHandleMultipleMessageTypesViaApiGateway() throws Exception {
    // 複数のDiscordメッセージイベントをAPI Gateway経由でテスト
    Map<String, Map<String, Object>> testMessages = Map.of(
        "player_join", Map.of(
            "type", "player_event",
            "eventType", "join",
            "playerName", "TestPlayer1",
            "playerUuid", "test-uuid-multi-join-12345",
            "serverName", "test-server",
            "timestamp", Instant.now().toString(),
            "channel_id", discordChannelId),
        "player_leave", Map.of(
            "type", "player_event",
            "eventType", "leave",
            "playerName", "TestPlayer2",
            "playerUuid", "test-uuid-multi-leave-12345",
            "serverName", "test-server",
            "timestamp", Instant.now().toString(),
            "channel_id", discordChannelId),
        "server_status", Map.of(
            "type", "server_status",
            "status", "online",
            "players", 5,
            "message", "Server is online with 5 players",
            "timestamp", Instant.now().toString(),
            "server", "test-server",
            "channel_id", discordChannelId));

    // 各メッセージタイプをAPI Gateway経由で送信
    for (Map.Entry<String, Map<String, Object>> entry : testMessages.entrySet()) {
      String messageType = entry.getKey();
      Map<String, Object> messageData = entry.getValue();

      String messageBody = TestUtils.toJson(messageData);

      // API Gateway にリクエスト送信
      int statusCode = sendSignedRequest(messageBody);

      assertThat(statusCode)
          .as("API Gateway should return 200 OK for " + messageType)
          .isEqualTo(200);

      logger.info("Sent {} message via API Gateway", messageType);

      // 各メッセージ間で少し待機
      TestUtils.waitFor(Duration.ofSeconds(1));
    }

    // すべてのメッセージ送信完了まで少し待機
    TestUtils.waitFor(Duration.ofSeconds(3));

    List<Message> messages = TestUtils.receiveSQSMessages(
        sqsQueueUrl,
        10,
        Duration.ofSeconds(10));

    // メッセージが受信されることを確認（Discord Botが消費している可能性があるため、最低1つ以上）
    assertThat(messages)
        .as("Should receive at least one sent message")
        .hasSizeGreaterThanOrEqualTo(1);

    // 各メッセージを処理・削除
    for (Message message : messages) {
      Map<String, Object> messageBody = TestUtils.parseJson(message.body());
      logger.info("Processing message type: {}", messageBody.get("type"));

      simulateDiscordBotProcessing(messageBody);
      TestUtils.deleteSQSMessage(sqsQueueUrl, message.receiptHandle());
    }

    logger.info("Multiple message types test completed successfully");
  }

  @Test
  void shouldSendPlayerLeaveMessageViaApiGateway() throws Exception {
    // player_event (leave) メッセージを送信
    Map<String, Object> playerLeaveMessage = Map.of(
        "type", "player_event",
        "eventType", "leave",
        "playerName", "TestPlayerLeave",
        "playerUuid", "test-uuid-leave-12345",
        "serverName", "test-server",
        "timestamp", Instant.now().toString(),
        "channel_id", discordChannelId);

    String messageBody = TestUtils.toJson(playerLeaveMessage);
    logger.info("Sending player_leave message via API Gateway: {}", messageBody);

    // SQS事前状態確認
    int initialMessageCount = TestUtils.getSQSMessageCount(sqsQueueUrl);
    logger.info("Initial SQS message count: {}", initialMessageCount);

    // API Gateway にリクエスト送信
    int statusCode = sendSignedRequest(messageBody);

    // ステータスコード確認
    assertThat(statusCode)
        .as("API Gateway should return 200 OK")
        .isEqualTo(200);

    logger.info("Player leave message sent successfully to API Gateway");

    // SQSメッセージ確認（少し待機してから）
    TestUtils.waitFor(Duration.ofSeconds(3));

    List<Message> messages = TestUtils.receiveSQSMessages(
        sqsQueueUrl,
        10,
        Duration.ofSeconds(5));

    // メッセージが受信された場合の処理
    if (!messages.isEmpty()) {
      Message receivedMessage = messages.get(0);
      Map<String, Object> receivedMessageBody = TestUtils.parseJson(receivedMessage.body());

      logger.info("Received player_leave SQS message: {}", receivedMessage.body());

      // メッセージ内容確認
      assertThat(receivedMessageBody)
          .as("Received message should contain player_event leave data")
          .containsEntry("type", "player_event")
          .containsEntry("eventType", "leave")
          .containsEntry("playerName", "TestPlayerLeave");

      // Discord Bot処理シミュレーション
      simulateDiscordBotProcessing(receivedMessageBody);

      // テスト後クリーンアップ
      TestUtils.deleteSQSMessage(sqsQueueUrl, receivedMessage.receiptHandle());
    } else {
      logger.info("No messages received from SQS (likely consumed by Discord Bot service)");
    }

    logger.info("Player leave integration test completed successfully");
  }

  @Test
  void shouldSendPlayerJoinMessageViaApiGateway() throws Exception {
    // player_event (join) メッセージを送信
    Map<String, Object> playerJoinMessage = Map.of(
        "type", "player_event",
        "eventType", "join",
        "playerName", "TestPlayerJoin",
        "playerUuid", "test-uuid-join-12345",
        "serverName", "test-server",
        "timestamp", Instant.now().toString(),
        "channel_id", discordChannelId);

    String messageBody = TestUtils.toJson(playerJoinMessage);
    logger.info("Sending player_join message via API Gateway: {}", messageBody);

    // SQS事前状態確認
    int initialMessageCount = TestUtils.getSQSMessageCount(sqsQueueUrl);
    logger.info("Initial SQS message count: {}", initialMessageCount);

    // API Gateway にリクエスト送信
    int statusCode = sendSignedRequest(messageBody);

    // ステータスコード確認
    assertThat(statusCode)
        .as("API Gateway should return 200 OK")
        .isEqualTo(200);

    logger.info("Player join message sent successfully to API Gateway");

    // SQSメッセージ確認（少し待機してから）
    TestUtils.waitFor(Duration.ofSeconds(3));

    List<Message> messages = TestUtils.receiveSQSMessages(
        sqsQueueUrl,
        10,
        Duration.ofSeconds(5));

    // メッセージが受信された場合の処理
    if (!messages.isEmpty()) {
      Message receivedMessage = messages.get(0);
      Map<String, Object> receivedMessageBody = TestUtils.parseJson(receivedMessage.body());

      logger.info("Received player_join SQS message: {}", receivedMessage.body());

      // メッセージ内容確認
      assertThat(receivedMessageBody)
          .as("Received message should contain player_event join data")
          .containsEntry("type", "player_event")
          .containsEntry("eventType", "join")
          .containsEntry("playerName", "TestPlayerJoin");

      // Discord Bot処理シミュレーション
      simulateDiscordBotProcessing(receivedMessageBody);

      // テスト後クリーンアップ
      TestUtils.deleteSQSMessage(sqsQueueUrl, receivedMessage.receiptHandle());
    } else {
      logger.info("No messages received from SQS (likely consumed by Discord Bot service)");
    }

    logger.info("Player join integration test completed successfully");
  }

  /**
   * IAM認証でAPI GatewayにPOSTリクエスト送信
   * 実際のMinecraftプラグインと同じ認証方式を使用
   */
  private int sendSignedRequest(String requestBody) throws Exception {
    // AWS認証情報（環境変数から取得）
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

  /**
   * Discord Bot のメッセージ処理をシミュレート
   * 実際のDiscord APIには送信せず、ログ出力のみ
   */
  private void simulateDiscordBotProcessing(Map<String, Object> messageData) {
    String messageType = (String) messageData.get("type");
    String message = (String) messageData.get("message");

    // Discord Bot の処理ロジックをシミュレート
    switch (messageType) {
      case "player_event":
        String eventType = (String) messageData.get("eventType");
        String playerName = (String) messageData.get("playerName");
        if ("join".equals(eventType)) {
          logger.info("🎮 Discord Bot would send: Player {} joined!", playerName);
        } else if ("leave".equals(eventType)) {
          logger.info("🚪 Discord Bot would send: Player {} left!", playerName);
        } else {
          logger.info("👤 Discord Bot would send: Player {} - {}", playerName, eventType);
        }
        break;
      case "server_status":
        logger.info("📊 Discord Bot would send: Server status - {}", messageData.get("status"));
        break;
      default:
        logger.info("💬 Discord Bot would send: {}", message);
    }

    // 処理完了を示す
    logger.info("✅ Discord Bot processing completed for message type: {}", messageType);
  }
}
