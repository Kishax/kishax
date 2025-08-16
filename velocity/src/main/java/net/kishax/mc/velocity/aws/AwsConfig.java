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
    String accessKey = config.getString("AWS.AccessKey", "");
    if (accessKey.isEmpty()) {
      logger.warn("AWS Access Key が設定されていません");
    }
    return accessKey;
  }

  /**
   * AWS Secret Key を取得
   */
  public String getAwsSecretKey() {
    String secretKey = config.getString("AWS.SecretKey", "");
    if (secretKey.isEmpty()) {
      logger.warn("AWS Secret Key が設定されていません");
    }
    return secretKey;
  }

  /**
   * API Gateway URL を取得
   */
  public String getApiGatewayUrl() {
    String url = config.getString("AWS.ApiGateway.URL", "");
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

  /**
   * Discord チャンネルID を取得（移行用）
   */
  public String getDiscordChannelId() {
    return config.getString("Discord.ChannelId", "");
  }

  /**
   * Discord チャットチャンネルID を取得（移行用）
   */
  public String getDiscordChatChannelId() {
    return config.getString("Discord.ChatChannelId", "");
  }

  /**
   * Discord 管理チャンネルID を取得（移行用）
   */
  public String getDiscordAdminChannelId() {
    return config.getString("Discord.AdminChannelId", "");
  }

  /**
   * AWS設定が有効かチェック
   */
  public boolean isAwsConfigValid() {
    return !getAwsAccessKey().isEmpty() &&
        !getAwsSecretKey().isEmpty() &&
        !getApiGatewayUrl().isEmpty();
  }

  /**
   * Discord設定が有効かチェック
   */
  public boolean isDiscordConfigValid() {
    return !getDiscordChannelId().isEmpty();
  }

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
        logger.error("  - AWS.AccessKey が未設定");
      if (getAwsSecretKey().isEmpty())
        logger.error("  - AWS.SecretKey が未設定");
      if (getApiGatewayUrl().isEmpty())
        logger.error("  - AWS.ApiGateway.URL が未設定");
    }

    if (isDiscordConfigValid()) {
      logger.info("✅ Discord設定が正常です");
    } else {
      logger.warn("⚠️ Discord設定が不完全です");
      if (getDiscordChannelId().isEmpty())
        logger.warn("  - Discord.ChannelId が未設定");
    }
  }
}
