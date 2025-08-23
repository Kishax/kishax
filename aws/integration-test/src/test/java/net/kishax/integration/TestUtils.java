package net.kishax.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * 統合テスト用ユーティリティクラス
 */
public class TestUtils {

  private static final Logger logger = LoggerFactory.getLogger(TestUtils.class);
  private static final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * SSMパラメータ値を取得
   */
  public static String getSSMParameter(String parameterName) {
    try (SsmClient ssmClient = TestConfig.createSsmClient()) {
      GetParameterRequest request = GetParameterRequest.builder()
          .name(parameterName)
          .withDecryption(true)
          .build();

      return ssmClient.getParameter(request).parameter().value();
    } catch (Exception e) {
      logger.error("Failed to get SSM parameter: {}", parameterName, e);
      throw new RuntimeException("SSM parameter retrieval failed", e);
    }
  }

  /**
   * SQSキューからメッセージを受信（テスト用）
   */
  public static List<Message> receiveSQSMessages(String queueUrl, int maxMessages, Duration timeout) {
    try (SqsClient sqsClient = TestConfig.createSqsClient()) {
      ReceiveMessageRequest request = ReceiveMessageRequest.builder()
          .queueUrl(queueUrl)
          .maxNumberOfMessages(maxMessages)
          .waitTimeSeconds((int) timeout.getSeconds())
          .build();

      ReceiveMessageResponse response = sqsClient.receiveMessage(request);
      logger.info("Received {} messages from SQS queue", response.messages().size());

      return response.messages();
    } catch (Exception e) {
      logger.error("Failed to receive SQS messages from: {}", queueUrl, e);
      throw new RuntimeException("SQS message receive failed", e);
    }
  }

  /**
   * SQSメッセージを削除
   */
  public static void deleteSQSMessage(String queueUrl, String receiptHandle) {
    try (SqsClient sqsClient = TestConfig.createSqsClient()) {
      DeleteMessageRequest request = DeleteMessageRequest.builder()
          .queueUrl(queueUrl)
          .receiptHandle(receiptHandle)
          .build();

      sqsClient.deleteMessage(request);
      logger.info("Deleted SQS message with receipt handle: {}", receiptHandle);
    } catch (Exception e) {
      logger.error("Failed to delete SQS message", e);
      throw new RuntimeException("SQS message deletion failed", e);
    }
  }

  /**
   * JSONオブジェクトをマップに変換
   */
  public static Map<String, Object> parseJson(String json) {
    try {
      return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
      });
    } catch (Exception e) {
      logger.error("Failed to parse JSON: {}", json, e);
      throw new RuntimeException("JSON parsing failed", e);
    }
  }

  /**
   * オブジェクトをJSONに変換
   */
  public static String toJson(Object object) {
    try {
      return objectMapper.writeValueAsString(object);
    } catch (Exception e) {
      logger.error("Failed to convert object to JSON", e);
      throw new RuntimeException("JSON conversion failed", e);
    }
  }

  /**
   * 指定時間待機
   */
  public static void waitFor(Duration duration) {
    try {
      TimeUnit.MILLISECONDS.sleep(duration.toMillis());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Wait interrupted", e);
    }
  }

  /**
   * テストメッセージ生成
   */
  public static Map<String, Object> createTestDiscordMessage() {
    return Map.of(
        "type", "player_join",
        "player", "TestPlayer",
        "message", "Integration test message from " + Instant.now(),
        "timestamp", Instant.now().toString(),
        "server", "test-server");
  }

  /**
   * SQSキューのメッセージ数を取得
   */
  public static int getSQSMessageCount(String queueUrl) {
    try (SqsClient sqsClient = TestConfig.createSqsClient()) {
      GetQueueAttributesRequest request = GetQueueAttributesRequest.builder()
          .queueUrl(queueUrl)
          .attributeNames(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES)
          .build();

      GetQueueAttributesResponse response = sqsClient.getQueueAttributes(request);
      String messageCount = response.attributes().get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES);

      return Integer.parseInt(messageCount);
    } catch (Exception e) {
      logger.error("Failed to get SQS message count for: {}", queueUrl, e);
      return 0;
    }
  }
}
