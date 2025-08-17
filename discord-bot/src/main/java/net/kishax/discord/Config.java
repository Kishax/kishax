package net.kishax.discord;

import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 設定管理クラス
 */
public class Config {
  private static final Logger logger = LoggerFactory.getLogger(Config.class);

  private final com.typesafe.config.Config config;

  public Config() {
    // 環境変数優先で設定を読み込み
    config = ConfigFactory.systemEnvironment()
        .withFallback(ConfigFactory.systemProperties())
        .withFallback(ConfigFactory.parseResources("application.conf"))
        .resolve();

    logger.info("設定を読み込みました");
  }

  // Discord設定
  public String getDiscordToken() {
    return config.getString("discord.token");
  }

  public String getDiscordChannelId() {
    return config.getString("discord.channel.id");
  }

  public String getDiscordChatChannelId() {
    return config.getString("discord.chat.channel.id");
  }

  public String getDiscordAdminChannelId() {
    return config.getString("discord.admin.channel.id");
  }

  public String getDiscordRuleChannelId() {
    return config.getString("discord.rule.channel.id");
  }

  public String getDiscordRuleMessageId() {
    return config.getString("discord.rule.message.id");
  }

  public String getDiscordPresenceActivity() {
    return config.hasPath("discord.presence.activity")
        ? config.getString("discord.presence.activity")
        : "Kishaxサーバー";
  }

  // AWS設定
  public String getAwsRegion() {
    return config.hasPath("aws.region")
        ? config.getString("aws.region")
        : "ap-northeast-1";
  }

  public String getSqsQueueUrl() {
    return config.getString("aws.sqs.queue.url");
  }

  public int getSqsMaxMessages() {
    return config.hasPath("aws.sqs.max.messages")
        ? config.getInt("aws.sqs.max.messages")
        : 10;
  }

  public int getSqsWaitTimeSeconds() {
    return config.hasPath("aws.sqs.wait.time.seconds")
        ? config.getInt("aws.sqs.wait.time.seconds")
        : 20;
  }

  // 絵文字関連設定
  public long getGuildId() {
    return config.hasPath("discord.guild.id")
        ? config.getLong("discord.guild.id")
        : 0L;
  }

  public String getBEDefaultEmojiName() {
    return config.hasPath("discord.emoji.default.name")
        ? config.getString("discord.emoji.default.name")
        : "steve";
  }
}
