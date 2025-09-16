package net.kishax.mc.velocity.aws;

import net.kishax.mc.velocity.util.config.VelocityConfig;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;

/**
 * AWS関連設定管理クラス
 */
@Singleton
public class AwsConfig {
  private final Logger logger;
  private final VelocityConfig config;

  @Inject
  public AwsConfig(Logger logger, VelocityConfig config) {
    this.logger = logger;
    this.config = config;
  }

  /**
   * AWS リージョンを取得
   */
  public String getAwsRegion() {
    return config.getString("AWS.Region", "ap-northeast-1");
  }

  /**
   * AWS Access Key を取得
   */
  public String getAwsAccessKey() {
    String accessKey = config.getString("AWS.Discord.Credentials.AccessKey", "");
    if (accessKey.isEmpty()) {
      logger.warn("AWS Discord Access Key が設定されていません");
    }
    logger.debug("AWS Discord Access Key loaded: {}", accessKey.isEmpty() ? "EMPTY" : "LOADED");
    return accessKey;
  }

  /**
   * AWS Secret Key を取得
   */
  public String getAwsSecretKey() {
    String secretKey = config.getString("AWS.Discord.Credentials.SecretKey", "");
    if (secretKey.isEmpty()) {
      logger.warn("AWS Discord Secret Key が設定されていません");
    }
    logger.debug("AWS Discord Secret Key loaded: {}", secretKey.isEmpty() ? "EMPTY" : "LOADED");
    return secretKey;
  }

  /**
   * Web→MC Queue URL を取得
   */
  public String getWebToMcQueueUrl() {
    String queueUrl = config.getString("AWS.SQS.WebToMcQueueUrl", "");
    if (queueUrl.isEmpty()) {
      logger.warn("Web→MC Queue URL が設定されていません");
    }
    return queueUrl;
  }

  /**
   * MC→Web Queue URL を取得
   */
  public String getMcToWebQueueUrl() {
    String queueUrl = config.getString("AWS.SQS.McToWebQueueUrl", "");
    if (queueUrl.isEmpty()) {
      logger.warn("MC→Web Queue URL が設定されていません");
    }
    return queueUrl;
  }

  /**
   * API Gateway URL を取得
   */
  public String getApiGatewayUrl() {
    String url = config.getString("AWS.ApiGateway.Endpoint", "");
    if (url.isEmpty()) {
      logger.warn("API Gateway URL が設定されていません");
    }
    return url;
  }

  /**
   * API Gateway サービス名を取得
   */
  public String getApiGatewayServiceName() {
    return config.getString("AWS.ApiGateway.ServiceName", "execute-api");
  }

  // Discord設定は AWS Discord Bot に移行済み

  /**
   * AWS設定が有効かチェック
   */
  public boolean isAwsConfigValid() {
    return !getAwsAccessKey().isEmpty() &&
        !getAwsSecretKey().isEmpty() &&
        !getMcToWebQueueUrl().isEmpty() &&
        !getWebToMcQueueUrl().isEmpty();
  }

  /**
   * SQS用 AWS Access Key を取得
   */
  public String getSqsAccessKey() {
    String accessKey = config.getString("AWS.SQS.Credentials.AccessKey", "");
    if (accessKey.isEmpty()) {
      logger.warn("AWS SQS Access Key が設定されていません");
    }
    logger.debug("AWS SQS Access Key loaded: {}", accessKey.isEmpty() ? "EMPTY" : "LOADED");
    return accessKey;
  }

  /**
   * SQS用 AWS Secret Key を取得
   */
  public String getSqsSecretKey() {
    String secretKey = config.getString("AWS.SQS.Credentials.SecretKey", "");
    if (secretKey.isEmpty()) {
      logger.warn("AWS SQS Secret Key が設定されていません");
    }
    logger.debug("AWS SQS Secret Key loaded: {}", secretKey.isEmpty() ? "EMPTY" : "LOADED");
    return secretKey;
  }

  /**
   * SQS設定が有効かチェック
   */
  public boolean isSqsConfigValid() {
    return !getMcToWebQueueUrl().isEmpty() && !getWebToMcQueueUrl().isEmpty() &&
        !getSqsAccessKey().isEmpty() && !getSqsSecretKey().isEmpty();
  }

  /**
   * 設定の検証を行い、ログ出力
   */
  public void validateConfig() {
    logger.info("AWS設定チェック...");

    if (isAwsConfigValid()) {
      logger.info("✅ AWS設定が正常です");
      logger.info("  - Region: {}", getAwsRegion());
      logger.info("  - MC→Web Queue URL: {}", getMcToWebQueueUrl());
      logger.info("  - Web→MC Queue URL: {}", getWebToMcQueueUrl());
      if (!getApiGatewayUrl().isEmpty()) {
        logger.info("  - API Gateway URL: {}", getApiGatewayUrl());
      }
    } else {
      logger.error("❌ AWS設定が不完全です");
      if (getAwsAccessKey().isEmpty())
        logger.error("  - AWS.Discord.Credentials.AccessKey が未設定");
      if (getAwsSecretKey().isEmpty())
        logger.error("  - AWS.Discord.Credentials.SecretKey が未設定");
      if (getMcToWebQueueUrl().isEmpty())
        logger.error("  - AWS.SQS.McToWebQueueUrl が未設定");
      if (getWebToMcQueueUrl().isEmpty())
        logger.error("  - AWS.SQS.WebToMcQueueUrl が未設定");
    }
  }
}
