package net.kishax.mc.velocity.aws;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.inject.Inject;

import net.kishax.mc.common.socket.message.Message;
import net.kishax.mc.velocity.util.config.VelocityConfig;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.regions.Region;

public class AwsSqsService {
  private final Logger logger;
  private final VelocityConfig config;
  private final SqsClient sqsClient;
  private final Gson gson;

  @Inject
  public AwsSqsService(Logger logger, VelocityConfig config) {
    this.logger = logger;
    this.config = config;

    // AWS SQS クライアントを初期化 - config.ymlから認証情報を取得
    String region = config.getString("AWS.Region", "ap-northeast-1");
    String accessKeyId = config.getString("AWS.Credentials.AccessKey", "");
    String secretAccessKey = config.getString("AWS.Credentials.SecretKey", "");

    logger.info("DEBUG: AWS Region from config: {}", region);
    logger.info("DEBUG: AWS AccessKey configured: {}", !accessKeyId.isEmpty());
    logger.info("DEBUG: AWS SecretKey configured: {}", !secretAccessKey.isEmpty());

    if (accessKeyId.isEmpty() || secretAccessKey.isEmpty()) {
      logger.warn("AWS credentials not configured in config.yml. SQS functionality will be disabled.");
      this.sqsClient = null;
    } else {
      logger.info("DEBUG: Creating SQS client...");
      this.sqsClient = SqsClient.builder()
          .region(Region.of(region))
          .credentialsProvider(StaticCredentialsProvider.create(
              AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
          .build();
      logger.info("DEBUG: SQS client created successfully");
    }

    this.gson = new Gson();
  }

  /**
   * 認証トークン情報をWeb側(SQS)に送信
   */
  public void sendAuthTokenToWeb(Message.Web.AuthToken authToken) {
    logger.info("DEBUG: sendAuthTokenToWeb called for player: {} ({})", authToken.who.name, authToken.who.uuid);
    try {
      if (this.sqsClient == null) {
        logger.warn("SQS client not initialized. Skipping auth token send.");
        return;
      }
      logger.info("DEBUG: SQS client is initialized");

      String queueUrl = config.getString("AWS.SQS.McToWebQueueUrl", "");
      logger.info("DEBUG: Queue URL from config: {}", queueUrl);

      if (queueUrl.isEmpty()) {
        logger.warn("AWS.SQS.McToWebQueueUrl is not configured. Skipping auth token send.");
        return;
      }

      // SQS メッセージデータを構築
      Map<String, Object> messageData = new HashMap<>();
      messageData.put("type", "auth_token");
      messageData.put("mcid", authToken.who.name);
      messageData.put("uuid", authToken.who.uuid);
      messageData.put("authToken", authToken.token);
      messageData.put("expiresAt", authToken.expiresAt);
      messageData.put("action", authToken.action);

      String jsonMessage = gson.toJson(messageData);
      logger.info("DEBUG: Message JSON created: {}", jsonMessage);

      SendMessageRequest sendRequest = SendMessageRequest.builder()
          .queueUrl(queueUrl)
          .messageBody(jsonMessage)
          .messageGroupId("auth-token-group") // FIFO キューの場合
          .messageDeduplicationId(authToken.who.uuid + "_" + System.currentTimeMillis()) // FIFO キューの場合
          .build();

      logger.info("DEBUG: Sending message to SQS...");
      SendMessageResponse response = sqsClient.sendMessage(sendRequest);
      logger.info("DEBUG: SQS send response received");

      if (response.sdkHttpResponse().isSuccessful()) {
        logger.info("Successfully sent auth token to SQS for player: {} (MessageId: {})",
            authToken.who.name, response.messageId());
      } else {
        logger.warn("Failed to send auth token to SQS. HTTP Status: {}",
            response.sdkHttpResponse().statusCode());
      }

    } catch (Exception e) {
      logger.error("DEBUG: Exception occurred in sendAuthTokenToWeb");
      logger.error("Error sending auth token to SQS: {}", e.getMessage());
      for (StackTraceElement element : e.getStackTrace()) {
        logger.error(element.toString());
      }
    }
    logger.info("DEBUG: sendAuthTokenToWeb method completed");
  }
}
