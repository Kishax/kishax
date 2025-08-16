package net.kishax.mc.velocity;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;

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
import net.kishax.mc.common.libs.ClassManager;
import net.kishax.mc.common.libs.Downloader;
import net.kishax.mc.common.libs.JarLoader;
import net.kishax.mc.common.libs.interfaces.PackageManager;
import net.kishax.mc.common.server.Luckperms;
import net.kishax.mc.common.socket.SocketSwitch;
import net.kishax.mc.common.util.PlayerUtils;
import net.kishax.mc.velocity.aws.AwsDiscordService;
import net.kishax.mc.velocity.aws.AwsConfig;
import net.kishax.mc.velocity.libs.VPackageManager;
import net.kishax.mc.velocity.module.Module;
import net.kishax.mc.velocity.server.DoServerOffline;
import net.kishax.mc.velocity.server.DoServerOnline;
import net.kishax.mc.velocity.server.cmd.main.Command;
import net.kishax.mc.velocity.server.cmd.sub.CEnd;
import net.kishax.mc.velocity.server.cmd.sub.Hub;
import net.kishax.mc.velocity.server.cmd.sub.Retry;
import net.kishax.mc.velocity.server.cmd.sub.ServerTeleport;
import net.kishax.mc.velocity.server.events.EventListener;
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
    Downloader downloader = new Downloader();
    List<PackageManager> packages = Arrays.asList(VPackageManager.VPackage.values());
    CompletableFuture<List<Boolean>> downloadFuture = downloader.downloadPackages(packages, dataDirectory);
    downloadFuture.thenCompose(results -> {
      for (int i = 0; i < results.size(); i++) {
        if (!results.get(i)) {
          logger.error("Failed to download: " + packages.get(i).getUrl());
        }
      }
      if (results.contains(false)) {
        logger.error("Failed to download external package.");
        return CompletableFuture.completedFuture(null);
      } else {
        logger.info("All packages downloaded successfully.");
        ClassLoader parentLoader = this.getClass().getClassLoader();
        return JarLoader.makeURLClassLoaderFromJars(parentLoader, packages, dataDirectory);
      }
    }).thenAccept(urlClassLoader -> {
      try {
        if (urlClassLoader.isEmpty()) {
          logger.error("Failed to make ClassLoader from JAR.");
          logger.error("Cannot start plugin.");
          return;
        }
        // URLClassLoaderを別のクラスで保存する
        for (PackageManager pkg : packages) {
          if (pkg != null) {
            // logger.info("URLClassLoader saved successfully: {}", pkg.getCoordinates());
          } else {
            logger.error("Failed to make ClassLoader from JAR");
            logger.error("Cannot start plugin.");
            return;
          }
        }
        ClassManager.urlClassLoaderMap.putAll(urlClassLoader);
        startApplication();
        isEnable = true;
      } catch (Exception e1) {
        logger.error("Failed to make ClassLoader from JAR: {}", e1.getMessage());
        for (StackTraceElement ste : e1.getStackTrace()) {
          logger.error(ste.toString());
        }
        logger.error("Cannot start plugin.");
      }
    }).exceptionally(e1 -> {
      logger.error("An error occurred while loading packages: {}", e1.getMessage());
      for (StackTraceElement ste : e1.getStackTrace()) {
        logger.error(ste.toString());
      }
      logger.error("Cannot start plugin.");
      return null;
    });
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
      getInjector().getInstance(DoServerOnline.class).updateAndSyncDatabase(false);
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
    commandManager.register(commandManager.metaBuilder("retry").build(), getInjector().getInstance(Retry.class));
    commandManager.register(commandManager.metaBuilder("stp").build(), getInjector().getInstance(ServerTeleport.class));
    VelocityConfig config = getInjector().getInstance(VelocityConfig.class);
    int port = config.getInt("Socket.Server_Port", 0);
    if (port != 0) {
      getInjector().getProvider(SocketSwitch.class).get().startSocketServer(port);
    }
    logger.info(FloodgateApi.getInstance().toString());
    logger.info("linking with Floodgate...");
    logger.info("plugin has been enabled.");
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
