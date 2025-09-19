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
import net.kishax.mc.common.util.PlayerUtils;
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

  // kishax-api components for shutdown
  private static net.kishax.api.bridge.SqsWorker kishaxSqsWorker;
  private net.kishax.api.bridge.RedisClient kishaxRedisClient;
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
   * Get the kishax-api SqsWorker instance
   */
  public static net.kishax.api.bridge.SqsWorker getKishaxSqsWorker() {
    return kishaxSqsWorker;
  }

  /**
   * Get the kishax-api RedisClient instance
   */
  public static net.kishax.api.bridge.RedisClient getKishaxRedisClient() {
    // injectorから現在のMainインスタンスを取得してRedisClientにアクセス
    if (injector != null) {
      try {
        Main mainInstance = injector.getInstance(Main.class);
        return mainInstance.kishaxRedisClient;
      } catch (Exception e) {
        org.slf4j.LoggerFactory.getLogger(Main.class).warn("Failed to get RedisClient from injector: {}",
            e.getMessage());
        return null;
      }
    }
    return null;
  }

  /**
   * Handle OTP display request from kishax-api SqsWorker
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
   * Send OTP response to WEB using kishax-api SqsWorker
   */
  public static void sendOtpResponseToWeb(String mcid, String uuid, boolean success, String message) {
    if (kishaxSqsWorker != null) {
      try {
        // Use kishax-api SqsWorker to send OTP response
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
          .warn("kishax-api SqsWorker is not available, cannot send OTP response");
    }
  }

  private void initializeSqsServices() throws Exception {
    try {
      VelocityConfig config = getInjector().getInstance(VelocityConfig.class);

      // SQS設定を取得
      String region = config.getString("AWS.Region", "ap-northeast-1");
      String accessKey = config.getString("AWS.SQS.AccessKey", "");
      String secretKey = config.getString("AWS.SQS.SecretKey", "");
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

      // kishax-api統合のためのConfiguration作成
      System.setProperty("AWS_REGION", region);
      System.setProperty("MC_WEB_SQS_ACCESS_KEY_ID", accessKey);
      System.setProperty("MC_WEB_SQS_SECRET_ACCESS_KEY", secretKey);
      System.setProperty("TO_WEB_QUEUE_URL", mcToWebQueueUrl);
      System.setProperty("TO_MC_QUEUE_URL", webToMcQueueUrl);
      System.setProperty("REDIS_URL", redisUrl);

      net.kishax.api.bridge.BridgeConfiguration sqsConfig = new net.kishax.api.bridge.BridgeConfiguration();
      sqsConfig.validate();

      // SqsWorkerをQUEUE_MODE対応で初期化（kishax-apiが自動でキューを選択）
      net.kishax.api.bridge.SqsWorker sqsWorker = net.kishax.api.bridge.SqsWorker.createWithQueueMode(sqsConfig);

      // Register OTP display callback
      net.kishax.api.bridge.SqsWorker.setOtpDisplayCallback((playerName, playerUuid, otp) -> {
        logger.info("🔔 OTP display callback triggered: {} ({}) OTP: {}", playerName, playerUuid, otp);
        handleOtpDisplayRequest(playerName, playerUuid, otp);
      });

      // Register Auth Confirm callback to prevent SQS loop
      net.kishax.api.bridge.SqsWorker.setAuthConfirmCallback((playerName, playerUuid) -> {
        logger.info("🔔 Auth confirm callback triggered for player: {} ({})", playerName, playerUuid);
        this.server.getPlayer(playerName).ifPresent(player -> {
          player.sendMessage(Component.text("Web authentication successful!", NamedTextColor.GREEN));
          logger.info("Sent auth confirmation message to player {}", playerName);
        });
      });

      sqsWorker.start();
      logger.info("✅ kishax-api SQSワーカーが開始されました（QUEUE_MODE対応）");

      // グローバル参照のため静的フィールドに保存（後でシャットダウン時に使用）
      Main.kishaxSqsWorker = sqsWorker;
      this.kishaxRedisClient = new net.kishax.api.bridge.RedisClient(redisUrl);

    } catch (Exception e) {
      logger.error("kishax-api SQS サービスの初期化に失敗しました: {}", e.getMessage());
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

    // kishax-api SQS関連サービスの停止
    try {
      if (kishaxSqsWorker != null) {
        kishaxSqsWorker.stop();
        logger.info("✅ kishax-api SQS ワーカーが停止しました");
      }

      if (kishaxRedisClient != null) {
        kishaxRedisClient.close();
        logger.info("✅ kishax-api Redis クライアントが停止しました");
      }
    } catch (Exception ex) {
      logger.error("kishax-api SQS サービスの停止中にエラーが発生しました: {}", ex.getMessage());
    }

    getInjector().getInstance(DoServerOffline.class).updateDatabase();
    getInjector().getProvider(SocketSwitch.class).get().stopSocketClient();
    logger.info("Client Socket Stopping...");
    getInjector().getProvider(SocketSwitch.class).get().stopSocketServer();
    logger.info("Socket Server stopping...");
    logger.info("プラグインが無効になりました。");
  }
}
