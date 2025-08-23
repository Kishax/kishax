package net.kishax.integration;

import io.github.cdimascio.dotenv.Dotenv;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.apigateway.ApiGatewayClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.ssm.SsmClient;

/**
 * 統合テスト用の共通設定クラス
 * .env ファイルから環境変数を読み取り
 */
public class TestConfig {

  private static final Dotenv dotenv = Dotenv.configure()
      .directory(".")
      .ignoreIfMissing()
      .load();

  // AWS Settings
  public static final String AWS_PROFILE = getEnvValue("AWS_PROFILE");
  public static final Region AWS_REGION = Region.of(getEnvValue("AWS_REGION"));
  public static final String ACCOUNT_ID = getEnvValue("AWS_ACCOUNT_ID");

  // API Gateway
  public static final String API_GATEWAY_ID = getEnvValue("AWS_API_GATEWAY_ID");
  public static final String API_GATEWAY_STAGE = getEnvValue("AWS_API_GATEWAY_STAGE");
  public static final String API_GATEWAY_RESOURCE_PATH = getEnvValue("AWS_API_GATEWAY_RESOURCE_PATH");

  // SQS
  public static final String SQS_QUEUE_NAME = getEnvValue("AWS_SQS_QUEUE_NAME");
  public static final String SQS_DLQ_NAME = getEnvValue("AWS_SQS_DLQ_NAME");

  // 直接設定値
  public static final String SQS_QUEUE_URL = getEnvValue("AWS_SQS_QUEUE_URL");
  public static final String DISCORD_CHANNEL_ID = getEnvValue("DISCORD_CHANNEL_ID");

  // AWS Credentials for integration testing
  // Note: これらはDiscordBotSqsTestでSystem.getenvから直接取得されます

  /**
   * 環境変数または.envファイルから値を取得
   */
  private static String getEnvValue(String key) {
    // システム環境変数を優先、なければ.envファイルから取得
    String value = System.getenv(key);
    if (value == null) {
      value = dotenv.get(key);
    }
    if (value == null) {
      throw new IllegalStateException("Required environment variable not found: " + key);
    }
    return value;
  }

  /**
   * AWS API Gateway クライアント作成
   */
  public static ApiGatewayClient createApiGatewayClient() {
    return ApiGatewayClient.builder()
        .region(AWS_REGION)
        .credentialsProvider(ProfileCredentialsProvider.create(AWS_PROFILE))
        .build();
  }

  /**
   * AWS SQS クライアント作成
   */
  public static SqsClient createSqsClient() {
    return SqsClient.builder()
        .region(AWS_REGION)
        .credentialsProvider(ProfileCredentialsProvider.create(AWS_PROFILE))
        .build();
  }

  /**
   * AWS SSM クライアント作成
   */
  public static SsmClient createSsmClient() {
    return SsmClient.builder()
        .region(AWS_REGION)
        .credentialsProvider(ProfileCredentialsProvider.create(AWS_PROFILE))
        .build();
  }

  /**
   * API Gateway エンドポイントURL生成
   */
  public static String getApiGatewayUrl() {
    return String.format("https://%s.execute-api.%s.amazonaws.com/%s%s",
        API_GATEWAY_ID, AWS_REGION.id(), API_GATEWAY_STAGE, API_GATEWAY_RESOURCE_PATH);
  }

  /**
   * SQS Queue URL生成
   */
  public static String getSqsQueueUrl() {
    return String.format("https://sqs.%s.amazonaws.com/%s/%s",
        AWS_REGION.id(), ACCOUNT_ID, SQS_QUEUE_NAME);
  }
}
