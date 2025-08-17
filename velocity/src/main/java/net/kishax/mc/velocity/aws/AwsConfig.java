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
    String accessKey = config.getString("AWS.ApiGateway.AccessKey", "");
    if (accessKey.isEmpty()) {
      logger.warn("AWS Access Key が設定されていません");
    }
    logger.debug("AWS Access Key loaded: {}", accessKey.isEmpty() ? "EMPTY" : "LOADED");
    return accessKey;
  }

  /**
   * AWS Secret Key を取得
   */
  public String getAwsSecretKey() {
    String secretKey = config.getString("AWS.ApiGateway.SecretKey", "");
    if (secretKey.isEmpty()) {
      logger.warn("AWS Secret Key が設定されていません");
    }
    logger.debug("AWS Secret Key loaded: {}", secretKey.isEmpty() ? "EMPTY" : "LOADED");
    return secretKey;
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
        !getApiGatewayUrl().isEmpty();
  }

  // Discord設定チェックは不要（AWS移行済み）

  /**
   * 設定の検証を行い、ログ出力
   */
  public void validateConfig() {
    logger.info("AWS設定チェック...");

    if (isAwsConfigValid()) {
      logger.info("✅ AWS設定が正常です");
      logger.info("  - Region: {}", getAwsRegion());
      logger.info("  - API Gateway URL: {}", getApiGatewayUrl());
    } else {
      logger.error("❌ AWS設定が不完全です");
      if (getAwsAccessKey().isEmpty())
        logger.error("  - AWS.ApiGateway.AccessKey が未設定");
      if (getAwsSecretKey().isEmpty())
        logger.error("  - AWS.ApiGateway.SecretKey が未設定");
      if (getApiGatewayUrl().isEmpty())
        logger.error("  - AWS.ApiGateway.Endpoint が未設定");
    }

    // Discord設定チェックは不要（AWS Discord Bot に移行済み）
  }
}
