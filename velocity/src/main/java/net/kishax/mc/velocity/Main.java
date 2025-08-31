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
import net.kishax.mc.velocity.aws.AwsDiscordService;
import net.kishax.mc.velocity.aws.AwsConfig;
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
import net.luckperms.api.LuckPermsProvider;

public class Main {
  public static boolean isVelocity = true;
  private static Injector injector = null;

  private final ProxyServer server;
  private final Logger logger;
  private final Path dataDirectory;
  private boolean isEnable = false;

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
    logger.info(FloodgateApi.getInstance().toString());
    logger.info("linking with Floodgate...");
  }

  public static Injector getInjector() {
    return injector;
  }

  @Subscribe
  public void onProxyShutdown(ProxyShutdownEvent e) {
    if (!isEnable)
      return;
    getInjector().getInstance(DoServerOffline.class).updateDatabase();
    getInjector().getProvider(SocketSwitch.class).get().stopSocketClient();
    logger.info("Client Socket Stopping...");
    getInjector().getProvider(SocketSwitch.class).get().stopSocketServer();
    logger.info("Socket Server stopping...");
    logger.info("プラグインが無効になりました。");
  }
}
