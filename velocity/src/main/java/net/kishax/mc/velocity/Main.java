package net.kishax.mc.velocity;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.TimeZone;

import org.geysermc.floodgate.api.FloodgateApi;
import org.slf4j.Logger;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;

import net.kishax.mc.common.database.Database;
// リフレクション・動的ライブラリローディングは AWS移行により不要
import net.kishax.mc.common.server.Luckperms;
import net.kishax.mc.common.socket.SocketSwitch;
import net.kishax.mc.common.socket.SqsMessageProcessor;
import net.kishax.mc.common.socket.SqsClient;
import net.kishax.mc.common.util.PlayerUtils;
import net.kishax.mc.velocity.aws.AwsDiscordService;
import net.kishax.mc.velocity.aws.AwsConfig;
import net.kishax.mc.velocity.socket.VelocitySqsMessageHandler;
// VPackageManager削除済み
import net.kishax.mc.velocity.module.Module;
import net.kishax.mc.velocity.server.DoServerOffline;
import net.kishax.mc.velocity.server.DoServerOnline;
import net.kishax.mc.velocity.server.cmd.main.Command;
import net.kishax.mc.velocity.server.cmd.sub.CEnd;
import net.kishax.mc.velocity.server.cmd.sub.Hub;
import net.kishax.mc.velocity.server.cmd.sub.ServerTeleport;
import net.kishax.mc.velocity.server.events.EventListener;
import net.kishax.mc.velocity.util.SettingsSyncService;
import net.kishax.mc.velocity.util.config.VelocityConfig;
import net.kishax.mc.velocity.auth.AuthLevelChecker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.luckperms.api.LuckPermsProvider;

public class Main {
  public static boolean isVelocity = true;
  private static Injector injector = null;

  private final ProxyServer server;
  private final Logger logger;
  private final Path dataDirectory;
  private boolean isEnable = false;

  // kishax-aws components for shutdown
  private static net.kishax.aws.SqsWorker kishaxSqsWorker;
  private net.kishax.aws.RedisClient kishaxRedisClient;
  private static AuthLevelChecker authLevelChecker;

  @Inject
  public Main(ProxyServer serverinstance, Logger logger, @DataDirectory Path dataDirectory) {
    this.server = serverinstance;
    this.logger = logger;
    this.dataDirectory = dataDirectory;
  }

  @Subscribe
  public void onProxyInitialization(ProxyInitializeEvent e) {
    logger.info("detected velocity platform.");
    TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));

    try {
      startApplication();
      isEnable = true;
      logger.info("plugin has been enabled.");
    } catch (Exception e1) {
      logger.error("Failed to start plugin: {}", e1.getMessage());
      for (StackTraceElement ste : e1.getStackTrace()) {
        logger.error(ste.toString());
      }
      logger.error("Cannot start plugin.");
    }
  }

  private void startApplication() {
    injector = Guice.createInjector(new Module(this, server, logger, dataDirectory));

    // AWS設定の検証
    AwsConfig awsConfig = getInjector().getInstance(AwsConfig.class);
    awsConfig.validateConfig();

    // AWS Discord サービスへの接続
    getInjector().getInstance(AwsDiscordService.class)
        .loginDiscordBotAsync().thenAccept(success -> {
          if (success) {
            logger.info("✅ AWS Discord Bot への接続が完了しました");
            // 必要に応じて追加の初期化処理をここに追加
          } else {
            logger.warn("⚠️ AWS Discord Bot への接続に失敗しました。Discord機能は無効になります。");
          }
        }).exceptionally(e -> {
          logger.error("AWS Discord サービスの初期化中にエラーが発生しました: {}", e.getMessage());
          for (StackTraceElement ste : e.getStackTrace()) {
            logger.error(ste.toString());
          }
          return null;
        });
    Database db = getInjector().getInstance(Database.class);
    try (Connection conn = db.getConnection()) {
      // この行は削除（上記の新しい実装で置き換え）

      // Settings設定をMySQLに同期
      getInjector().getInstance(SettingsSyncService.class).syncSettingsToDatabase();
    } catch (SQLException | ClassNotFoundException e1) {
      logger.error("An error occurred while updating the database: {}", e1.getMessage());
    }
    server.getEventManager().register(this, getInjector().getInstance(EventListener.class));
    getInjector().getInstance(Luckperms.class).triggerNetworkSync();
    logger.info("linking with LuckPerms...");
    logger.info(LuckPermsProvider.get().getPlatform().toString());
    getInjector().getInstance(PlayerUtils.class).loadPlayers();
    CommandManager commandManager = server.getCommandManager();
    commandManager.register(commandManager.metaBuilder("kishaxp").build(), getInjector().getInstance(Command.class));
    commandManager.register(commandManager.metaBuilder("hub").build(), getInjector().getInstance(Hub.class));
    commandManager.register(commandManager.metaBuilder("cend").build(), getInjector().getInstance(CEnd.class));
    commandManager.register(commandManager.metaBuilder("stp").build(), getInjector().getInstance(ServerTeleport.class));
    VelocityConfig config = getInjector().getInstance(VelocityConfig.class);
    int port = config.getInt("Socket.Server_Port", 0);
    if (port != 0) {
      getInjector().getProvider(SocketSwitch.class).get().startSocketServer(port);
    }

    // データベース同期時にsocketPortを渡す
    try {
      getInjector().getInstance(DoServerOnline.class).updateAndSyncDatabase(false, port);
    } catch (Exception e) {
      logger.error("Failed to update database with socket port: {}", e.getMessage());
    }

    // SQS関連サービスの初期化
    try {
      initializeSqsServices();
    } catch (Exception e) {
      logger.error("Failed to initialize SQS services: {}", e.getMessage());
    }

    // 認証レベルチェッカーの初期化
    try {
      initializeAuthLevelChecker();
    } catch (Exception e) {
      logger.error("Failed to initialize AuthLevelChecker: {}", e.getMessage());
    }

    logger.info(FloodgateApi.getInstance().toString());
    logger.info("linking with Floodgate...");
  }

  public static Injector getInjector() {
    return injector;
  }

  /**
   * Get the kishax-aws SqsWorker instance
   */
  public static net.kishax.aws.SqsWorker getKishaxSqsWorker() {
    return kishaxSqsWorker;
  }

  /**
   * Handle OTP display request from kishax-aws SqsWorker
   */
  public static void handleOtpDisplayRequest(String playerName, String playerUuid, String otp) {
    if (injector != null) {
      try {
        VelocitySqsMessageHandler sqsHandler = injector.getInstance(VelocitySqsMessageHandler.class);
        sqsHandler.handleOtpToMinecraft(playerName, playerUuid, otp);
      } catch (Exception e) {
        org.slf4j.LoggerFactory.getLogger(Main.class).error("Failed to handle OTP display request: {}", e.getMessage(),
            e);
      }
    }
  }

  /**
   * Send OTP response to WEB using kishax-aws SqsWorker
   */
  public static void sendOtpResponseToWeb(String mcid, String uuid, boolean success, String message) {
    if (kishaxSqsWorker != null) {
      try {
        // Use kishax-aws SqsWorker to send OTP response
        long timestamp = System.currentTimeMillis();
        kishaxSqsWorker.getMcToWebSender().sendOtpResponse(mcid, uuid, success, message, timestamp);
        org.slf4j.LoggerFactory.getLogger(Main.class).info("✅ OTP response sent to WEB: {} ({}) success: {}", mcid,
            uuid, success);
      } catch (Exception e) {
        org.slf4j.LoggerFactory.getLogger(Main.class).error("Failed to send OTP response to WEB: {} ({})", mcid, uuid,
            e);
      }
    } else {
      org.slf4j.LoggerFactory.getLogger(Main.class)
          .warn("kishax-aws SqsWorker is not available, cannot send OTP response");
    }
  }

  private void initializeSqsServices() throws Exception {
    try {
      VelocityConfig config = getInjector().getInstance(VelocityConfig.class);
      AwsConfig awsConfig = getInjector().getInstance(AwsConfig.class);

      // SQS設定を取得
      String region = awsConfig.getAwsRegion();
      String accessKey = awsConfig.getSqsAccessKey();
      String secretKey = awsConfig.getSqsSecretKey();
      String webToMcQueueUrl = config.getString("AWS.SQS.WebToMcQueueUrl", "");
      String mcToWebQueueUrl = config.getString("AWS.SQS.McToWebQueueUrl", "");
      String redisUrl = config.getString("Redis.URL", "redis://localhost:6379");

      if (webToMcQueueUrl.isEmpty()) {
        logger.warn("WebToMcQueueUrl が設定されていません。SQS機能は無効になります。");
        return;
      }

      if (mcToWebQueueUrl.isEmpty()) {
        logger.warn("McToWebQueueUrl が設定されていません。SQS送信機能は無効になります。");
        return;
      }

      // kishax-aws統合のためのConfiguration作成
      System.setProperty("AWS_REGION", region);
      System.setProperty("MC_WEB_SQS_ACCESS_KEY_ID", accessKey);
      System.setProperty("MC_WEB_SQS_SECRET_ACCESS_KEY", secretKey);
      System.setProperty("MC_TO_WEB_QUEUE_URL", mcToWebQueueUrl);
      System.setProperty("WEB_TO_MC_QUEUE_URL", webToMcQueueUrl);
      System.setProperty("REDIS_URL", redisUrl);

      net.kishax.aws.Configuration kishaxConfig = new net.kishax.aws.Configuration();
      kishaxConfig.validate();

      // SqsWorkerをQUEUE_MODE対応で初期化（kishax-awsが自動でキューを選択）
      net.kishax.aws.SqsWorker sqsWorker = net.kishax.aws.SqsWorker.createWithQueueMode(kishaxConfig);

      // Register OTP display callback
      net.kishax.aws.SqsWorker.setOtpDisplayCallback((playerName, playerUuid, otp) -> {
        logger.info("🔔 OTP display callback triggered: {} ({}) OTP: {}", playerName, playerUuid, otp);
        handleOtpDisplayRequest(playerName, playerUuid, otp);
      });

      // Register Auth Confirm callback to prevent SQS loop
      net.kishax.aws.SqsWorker.setAuthConfirmCallback((playerName, playerUuid) -> {
        logger.info("🔔 Auth confirm callback triggered for player: {} ({})", playerName, playerUuid);
        this.server.getPlayer(playerName).ifPresent(player -> {
          player.sendMessage(Component.text("Web authentication successful!", NamedTextColor.GREEN));
          logger.info("Sent auth confirmation message to player {}", playerName);
        });
      });

      sqsWorker.start();
      logger.info("✅ kishax-aws SQSワーカーが開始されました（QUEUE_MODE対応）");

      // グローバル参照のため静的フィールドに保存（後でシャットダウン時に使用）
      Main.kishaxSqsWorker = sqsWorker;
      this.kishaxRedisClient = kishaxConfig.createRedisClient();

    } catch (Exception e) {
      logger.error("kishax-aws SQS サービスの初期化に失敗しました: {}", e.getMessage());
      throw e;
    }
  }

  private void initializeAuthLevelChecker() throws Exception {
    try {
      VelocityConfig config = getInjector().getInstance(VelocityConfig.class);

      // Auth API設定を取得
      String authApiUrl = config.getString("Auth.API.URL", "");
      String authApiKey = config.getString("Auth.API.KEY", "");

      if (authApiUrl.isEmpty()) {
        logger.warn("Auth.API.URL が設定されていません。認証レベルチェック機能は無効になります。");
        return;
      }

      if (authApiKey.isEmpty()) {
        logger.warn("Auth.API.KEY が設定されていません。認証レベルチェック機能は無効になります。");
        return;
      }

      // AuthLevelChecker初期化
      authLevelChecker = new AuthLevelChecker(server, authApiUrl, authApiKey);
      authLevelChecker.startPeriodicCheck();

      logger.info("✅ AuthLevelChecker が開始されました (API: {})", authApiUrl);

    } catch (Exception e) {
      logger.error("AuthLevelChecker の初期化に失敗しました: {}", e.getMessage());
      throw e;
    }
  }

  @Subscribe
  public void onProxyShutdown(ProxyShutdownEvent e) {
    if (!isEnable)
      return;

    // AuthLevelChecker の停止
    try {
      if (authLevelChecker != null) {
        authLevelChecker.stopPeriodicCheck();
        logger.info("✅ AuthLevelChecker が停止しました");
      }
    } catch (Exception ex) {
      logger.error("AuthLevelChecker の停止中にエラーが発生しました: {}", ex.getMessage());
    }

    // kishax-aws SQS関連サービスの停止
    try {
      if (kishaxSqsWorker != null) {
        kishaxSqsWorker.stop();
        logger.info("✅ kishax-aws SQS ワーカーが停止しました");
      }

      if (kishaxRedisClient != null) {
        kishaxRedisClient.close();
        logger.info("✅ kishax-aws Redis クライアントが停止しました");
      }
    } catch (Exception ex) {
      logger.error("kishax-aws SQS サービスの停止中にエラーが発生しました: {}", ex.getMessage());
    }

    getInjector().getInstance(DoServerOffline.class).updateDatabase();
    getInjector().getProvider(SocketSwitch.class).get().stopSocketClient();
    logger.info("Client Socket Stopping...");
    getInjector().getProvider(SocketSwitch.class).get().stopSocketServer();
    logger.info("Socket Server stopping...");
    logger.info("プラグインが無効になりました。");
  }
}
