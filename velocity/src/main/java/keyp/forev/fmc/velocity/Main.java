package keyp.forev.fmc.velocity;

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

import keyp.forev.fmc.common.database.Database;
import keyp.forev.fmc.common.libs.ClassManager;
import keyp.forev.fmc.common.libs.Downloader;
import keyp.forev.fmc.common.libs.JarLoader;
import keyp.forev.fmc.common.libs.interfaces.PackageManager;
import keyp.forev.fmc.common.server.Luckperms;
import keyp.forev.fmc.common.socket.SocketSwitch;
import keyp.forev.fmc.common.util.PlayerUtils;
import de.timongcraft.veloboard.VeloBoardRegistry;
import net.luckperms.api.LuckPermsProvider;
import keyp.forev.fmc.velocity.cmd.main.FMCCommand;
import keyp.forev.fmc.velocity.cmd.sub.CEnd;
import keyp.forev.fmc.velocity.cmd.sub.Hub;
import keyp.forev.fmc.velocity.cmd.sub.Retry;
import keyp.forev.fmc.velocity.cmd.sub.ServerTeleport;
import keyp.forev.fmc.velocity.discord.Discord;
import keyp.forev.fmc.velocity.discord.EmojiManager;
import keyp.forev.fmc.velocity.discord.MineStatusReflect;
import keyp.forev.fmc.velocity.events.EventListener;
import keyp.forev.fmc.velocity.module.Module;
import keyp.forev.fmc.velocity.server.DoServerOffline;
import keyp.forev.fmc.velocity.server.DoServerOnline;
import keyp.forev.fmc.velocity.server.FMCBoard;
import keyp.forev.fmc.velocity.util.config.VelocityConfig;
import keyp.forev.fmc.velocity.libs.VPackageManager;

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
        VeloBoardRegistry.register();
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
                    logger.error("Cannot start fmc plugin.");
                    return;
                }
                // URLClassLoaderを別のクラスで保存する
                for (PackageManager pkg : packages) {
                    if (pkg != null) {
                        // logger.info("URLClassLoader saved successfully: {}", pkg.getCoordinates());
                    } else {
                        logger.error("Failed to make ClassLoader from JAR");
                        logger.error("Cannot start fmc plugin.");
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
                logger.error("Cannot start fmc plugin.");
            }
        }).exceptionally(e1 -> {
                logger.error("An error occurred while loading packages: {}", e1.getMessage());
                for (StackTraceElement ste : e1.getStackTrace()) {
                    logger.error(ste.toString());
                }
                logger.error("Cannot start fmc plugin.");
                return null;
        });
    }
    
    private void startApplication() {
        injector = Guice.createInjector(new Module(this, server, logger, dataDirectory));
        getInjector().getInstance(FMCBoard.class).updateScheduler();
    	getInjector().getInstance(Discord.class)
            .loginDiscordBotAsync().thenAccept(jda -> {
                if (jda != null) {
                    getInjector().getInstance(MineStatusReflect.class).start(jda);
                    try {
                        getInjector().getInstance(EmojiManager.class).updateDefaultEmojiId();
                    } catch (Exception e) {
                        logger.error("Failed to update default emoji ID: {}", e.getMessage());
                        for (StackTraceElement ste : e.getStackTrace()) {
                            logger.error(ste.toString());
                        }
                    }
                }
            }).exceptionally(e -> {
                logger.error("An error occurred while logging in to Discord: {}", e.getMessage());
                for (StackTraceElement ste : e.getStackTrace()) {
                    logger.error(ste.toString());
                }
                return null;
            });
        Database db = getInjector().getInstance(Database.class);
		try (Connection conn = db.getConnection()) {
            getInjector().getInstance(DoServerOnline.class).updateDatabase(conn);
		} catch (SQLException | ClassNotFoundException e1) {
			logger.error("An error occurred while updating the database: {}", e1.getMessage());
		}
    	server.getEventManager().register(this, getInjector().getInstance(EventListener.class));
    	getInjector().getInstance(Luckperms.class).triggerNetworkSync();
 		logger.info("linking with LuckPerms...");
        logger.info(LuckPermsProvider.get().getPlatform().toString());
 		getInjector().getInstance(PlayerUtils.class).loadPlayers();
    	CommandManager commandManager = server.getCommandManager();
        commandManager.register(commandManager.metaBuilder("fmcp").build(), getInjector().getInstance(FMCCommand.class));
        commandManager.register(commandManager.metaBuilder("hub").build(), getInjector().getInstance(Hub.class));
        commandManager.register(commandManager.metaBuilder("cend").build(), getInjector().getInstance(CEnd.class));
        commandManager.register(commandManager.metaBuilder("retry").build(), getInjector().getInstance(Retry.class));
        commandManager.register(commandManager.metaBuilder("stp").build(), getInjector().getInstance(ServerTeleport.class));
        VelocityConfig config = getInjector().getInstance(VelocityConfig.class);
        int port = config.getInt("Socket.Server_Port",0);
        if (port != 0) {
            getInjector().getProvider(SocketSwitch.class).get().startSocketServer(port);
		}
        logger.info(FloodgateApi.getInstance().toString());
        logger.info("linking with Floodgate...");
	    logger.info("fmc plugin has been enabled.");
    }
    
    public static Injector getInjector() {
        return injector;
    }
    
    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent e) {
        if (!isEnable) return;
        getInjector().getInstance(DoServerOffline.class).updateDatabase();
    	getInjector().getProvider(SocketSwitch.class).get().stopSocketClient();
		logger.info( "Client Socket Stopping..." );
		getInjector().getProvider(SocketSwitch.class).get().stopSocketServer();
    	logger.info("Socket Server stopping...");
		logger.info( "プラグインが無効になりました。" );
    }
}
