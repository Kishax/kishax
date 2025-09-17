package net.kishax.mc.velocity.aws;

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
    String accessKeyId = config.getString("AWS.SQS.Credentials.AccessKey", "");
    String secretAccessKey = config.getString("AWS.SQS.Credentials.SecretKey", "");

    if (accessKeyId.isEmpty() || secretAccessKey.isEmpty()) {
      logger.warn("AWS credentials not configured in config.yml. SQS functionality will be disabled.");
      this.sqsClient = null;
    } else {
      logger.info("Initializing SQS client for region: {}", region);
      this.sqsClient = SqsClient.builder()
          .region(Region.of(region))
          .credentialsProvider(StaticCredentialsProvider.create(
              AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
          .build();
      logger.info("SQS client initialized successfully");
    }

    this.gson = new Gson();
  }

  /**
   * 認証トークン情報をWeb側(SQS)に送信
   */
  public void sendAuthTokenToWeb(Message.Web.AuthToken authToken) {
    logger.info("Sending auth token to SQS for player: {} ({})", authToken.who.name, authToken.who.uuid);
    try {
      if (this.sqsClient == null) {
        logger.warn("SQS client not initialized. Skipping auth token send.");
        return;
      }

      String queueUrl = config.getString("AWS.SQS.McToWebQueueUrl", "");

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

      SendMessageRequest sendRequest = SendMessageRequest.builder()
          .queueUrl(queueUrl)
          .messageBody(jsonMessage)
          .build();

      SendMessageResponse response = sqsClient.sendMessage(sendRequest);

      if (response.sdkHttpResponse().isSuccessful()) {
        logger.info("Successfully sent auth token to SQS for player: {} (MessageId: {})",
            authToken.who.name, response.messageId());
      } else {
        logger.warn("Failed to send auth token to SQS. HTTP Status: {}",
            response.sdkHttpResponse().statusCode());
      }

    } catch (Exception e) {
      logger.error("Error sending auth token to SQS: {}", e.getMessage());
      for (StackTraceElement element : e.getStackTrace()) {
        logger.error(element.toString());
      }
    }
  }
}
