package net.kishax.mc.velocity.aws;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import net.kishax.mc.common.socket.SqsClient;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * AWS経由Discord操作サービス
 * 既存のDiscordクラスの代替として機能
 */
@Singleton
public class AwsDiscordService {
  private final Logger logger;
  private final AwsConfig awsConfig;
  private final Provider<AwsApiClient> apiClientProvider;
  private AwsApiClient apiClient;
  private SqsClient sqsClient;

  @Inject
  public AwsDiscordService(Logger logger, AwsConfig awsConfig, Provider<AwsApiClient> apiClientProvider) {
    this.logger = logger;
    this.awsConfig = awsConfig;
    this.apiClientProvider = apiClientProvider;

    logger.info("AWS Discord サービスを初期化しました（遅延初期化）");
  }

  private AwsApiClient getApiClient() {
    if (apiClient == null && awsConfig.isAwsConfigValid()) {
      try {
        apiClient = apiClientProvider.get();
        logger.info("AwsApiClient を遅延初期化しました");
      } catch (Exception e) {
        logger.error("AwsApiClient の初期化に失敗しました", e);
        return null;
      }
    }
    return apiClient;
  }

  private SqsClient getSqsClient() {
    if (sqsClient == null && awsConfig.isSqsConfigValid()) {
      try {
        // 手動でSqsClientを初期化
        String region = awsConfig.getAwsRegion();
        String accessKey = awsConfig.getAwsAccessKey();
        String secretKey = awsConfig.getAwsSecretKey();
        String mcToWebQueueUrl = awsConfig.getMcToWebQueueUrl();
        String apiGatewayUrl = awsConfig.getApiGatewayUrl();

        sqsClient = SqsClient.create(region, accessKey, secretKey, mcToWebQueueUrl, apiGatewayUrl);
        logger.info("SqsClient を遅延初期化しました");
      } catch (Exception e) {
        logger.error("SqsClient の初期化に失敗しました", e);
        return null;
      }
    }
    return sqsClient;
  }

  /**
   * Discordへの接続確認（旧loginDiscordBotAsyncの代替）
   */
  public CompletableFuture<Boolean> loginDiscordBotAsync() {
    AwsApiClient client = getApiClient();
    if (client == null) {
      logger.error("AWS Discord サービスが初期化されていません");
      return CompletableFuture.completedFuture(false);
    }

    logger.info("AWS Discord Bot への接続を確認中...");

    // 簡単なping用メッセージを送信してAWS接続確認
    Map<String, Object> payload = new HashMap<>();
    payload.put("action", "ping");

    return client.sendDiscordMessage("system", payload)
        .thenApply(v -> {
          logger.info("✅ AWS Discord Bot への接続が確認されました");
          return true;
        })
        .exceptionally(ex -> {
          logger.error("❌ AWS Discord Bot への接続に失敗しました", ex);
          return false;
        });
  }

  /**
   * Discord Botのログアウト（旧logoutDiscordBotの代替）
   */
  public CompletableFuture<Void> logoutDiscordBot() {
    AwsApiClient client = getApiClient();
    if (client == null) {
      return CompletableFuture.completedFuture(null);
    }

    logger.info("AWS Discord Bot からログアウト中...");

    Map<String, Object> payload = new HashMap<>();
    payload.put("action", "logout");

    return client.sendDiscordMessage("system", payload)
        .thenRun(() -> logger.info("✅ AWS Discord Bot からログアウトしました"))
        .exceptionally(ex -> {
          logger.error("Discord Bot ログアウト中にエラーが発生しました", ex);
          return null;
        });
  }

  /**
   * シンプルなメッセージ送信（旧sendBotMessageの代替）
   */
  public CompletableFuture<Void> sendBotMessage(String content) {
    AwsApiClient client = getApiClient();
    if (client == null) {
      logger.error("AWS Discord サービスが利用できません");
      return CompletableFuture.completedFuture(null);
    }

    Map<String, Object> payload = new HashMap<>();
    payload.put("content", content);
    payload.put("isChat", false);

    return client.sendDiscordMessage("broadcast", payload)
        .exceptionally(ex -> {
          logger.error("Discord メッセージ送信に失敗しました: {}", content, ex);
          return null;
        });
  }

  /**
   * Embed付きメッセージ送信（旧sendBotMessage(embed)の代替）
   */
  public CompletableFuture<Void> sendBotMessage(String content, int color) {
    AwsApiClient client = getApiClient();
    if (client == null) {
      logger.error("AWS Discord サービスが利用できません");
      return CompletableFuture.completedFuture(null);
    }

    return client.sendEmbedMessage(content, color, "", null, false)
        .exceptionally(ex -> {
          logger.error("Discord Embed メッセージ送信に失敗しました: {}", content, ex);
          return null;
        });
  }

  /**
   * メッセージIDを取得してEmbed送信（旧sendBotMessageAndgetMessageIdの代替）
   */
  public CompletableFuture<String> sendBotMessageAndgetMessageId(String content, int color) {
    AwsApiClient client = getApiClient();
    if (client == null) {
      logger.error("AWS Discord サービスが利用できません");
      return CompletableFuture.completedFuture(null);
    }

    // Note: AWS版では実際のメッセージIDは取得できないため、疑似IDを返す
    // 必要に応じてSQSレスポンス機能を実装可能
    return client.sendEmbedMessage(content, color, "", null, false)
        .thenApply(v -> {
          String pseudoId = "aws-" + System.currentTimeMillis();
          logger.debug("AWS Discord メッセージ送信完了、疑似ID: {}", pseudoId);
          return pseudoId;
        })
        .exceptionally(ex -> {
          logger.error("Discord Embed メッセージ送信に失敗しました: {}", content, ex);
          return null;
        });
  }

  /**
   * Embedメッセージの編集（旧editBotEmbedの代替）
   */
  public CompletableFuture<Void> editBotEmbed(String messageId, String additionalDescription) {
    return editBotEmbed(messageId, additionalDescription, false);
  }

  /**
   * Embedメッセージの編集（旧editBotEmbedの代替、チャットチャンネル対応）
   */
  public CompletableFuture<Void> editBotEmbed(String messageId, String additionalDescription, boolean isChat) {
    AwsApiClient client = getApiClient();
    if (client == null) {
      logger.error("AWS Discord サービスが利用できません");
      return CompletableFuture.completedFuture(null);
    }

    String channelId = ""; // AWS Discord Botが適切なチャンネルを決定

    return client.sendEmbedMessage(additionalDescription, 0x00FF00, channelId, messageId, true)
        .exceptionally(ex -> {
          logger.error("Discord Embed 編集に失敗しました: {} - {}", messageId, additionalDescription, ex);
          return null;
        });
  }

  /**
   * プレイヤー参加イベント送信
   */
  public CompletableFuture<Void> sendPlayerJoinEvent(String playerName, String playerUuid, String serverName) {
    AwsApiClient client = getApiClient();
    if (client == null) {
      return CompletableFuture.completedFuture(null);
    }

    return client.sendPlayerEvent("join", playerName, playerUuid, serverName)
        .exceptionally(ex -> {
          logger.error("プレイヤー参加イベント送信に失敗しました: {}", playerName, ex);
          return null;
        });
  }

  /**
   * プレイヤー退出イベント送信
   */
  public CompletableFuture<Void> sendPlayerLeaveEvent(String playerName, String playerUuid, String serverName) {
    AwsApiClient client = getApiClient();
    if (client == null) {
      return CompletableFuture.completedFuture(null);
    }

    return client.sendPlayerEvent("leave", playerName, playerUuid, serverName)
        .exceptionally(ex -> {
          logger.error("プレイヤー退出イベント送信に失敗しました: {}", playerName, ex);
          return null;
        });
  }

  /**
   * プレイヤー移動イベント送信
   */
  public CompletableFuture<Void> sendPlayerMoveEvent(String playerName, String playerUuid, String serverName) {
    AwsApiClient client = getApiClient();
    if (client == null) {
      return CompletableFuture.completedFuture(null);
    }

    return client.sendPlayerEvent("move", playerName, playerUuid, serverName)
        .exceptionally(ex -> {
          logger.error("プレイヤー移動イベント送信に失敗しました: {}", playerName, ex);
          return null;
        });
  }

  /**
   * チャットメッセージ送信
   */
  public CompletableFuture<Void> sendChatMessage(String playerName, String playerUuid, String message) {
    AwsApiClient client = getApiClient();
    if (client == null) {
      return CompletableFuture.completedFuture(null);
    }

    return client.sendChatMessage(playerName, playerUuid, message)
        .exceptionally(ex -> {
          logger.error("チャットメッセージ送信に失敗しました: {} - {}", playerName, message, ex);
          return null;
        });
  }

  /**
   * サーバーステータス更新送信
   */
  public CompletableFuture<Void> sendServerStatus(String serverName, String status) {
    AwsApiClient client = getApiClient();
    if (client == null) {
      return CompletableFuture.completedFuture(null);
    }

    return client.sendServerStatus(serverName, status)
        .exceptionally(ex -> {
          logger.error("サーバーステータス送信に失敗しました: {} - {}", serverName, status, ex);
          return null;
        });
  }

  /**
   * AWS Discord サービスが利用可能かチェック
   */
  public boolean isAvailable() {
    return getApiClient() != null && awsConfig.isAwsConfigValid();
  }
}
