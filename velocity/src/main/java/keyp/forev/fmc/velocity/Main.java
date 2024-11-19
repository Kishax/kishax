package keyp.forev.fmc.velocity;

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

import common.src.main.java.keyp.forev.fmc.main.Database;
import common.src.main.java.keyp.forev.fmc.main.Luckperms;
import common.src.main.java.keyp.forev.fmc.main.PlayerUtils;
import common.src.main.java.keyp.forev.fmc.main.SocketServerThread;
import common.src.main.java.keyp.forev.fmc.main.SocketSwitch;
import de.timongcraft.veloboard.VeloBoardRegistry;
import net.luckperms.api.LuckPermsProvider;
import velocity.src.main.java.keyp.forev.fmc.core.command.CEnd;
import velocity.src.main.java.keyp.forev.fmc.core.command.FMCCommand;
import velocity.src.main.java.keyp.forev.fmc.core.command.Hub;
import velocity.src.main.java.keyp.forev.fmc.core.command.Retry;
import velocity.src.main.java.keyp.forev.fmc.core.command.ServerTeleport;
import velocity.src.main.java.keyp.forev.fmc.core.discord.Discord;
import velocity.src.main.java.keyp.forev.fmc.core.discord.EmojiManager;

public class Main {
	public static boolean isVelocity = true;
	private static Injector injector = null;
	
	private final ProxyServer server;
	private final Logger logger;
	private final Path dataDirectory;
    @Inject
    public Main(ProxyServer serverinstance, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = serverinstance;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent e) {
    	logger.info("detected velocity platform.");
        SocketServerThread.platform.set("velocity");
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));
        VeloBoardRegistry.register();
        injector = Guice.createInjector(new velocity.src.main.java.keyp.forev.fmc.core.main.Module(this, server, logger, dataDirectory));
        getInjector().getInstance(FMCBoard.class).updateScheduler();
    	getInjector().getInstance(Discord.class).loginDiscordBotAsync().thenAccept(jda -> {
            if (jda != null) {
                //getInjector().getInstance(MineStatusReflect.class).sendEmbedMessage(jda);
                getInjector().getInstance(MineStatusReflect.class).start(jda);
                getInjector().getInstance(EmojiManager.class).updateDefaultEmojiId();
            }
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
        Config config = getInjector().getInstance(Config.class);
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
        getInjector().getInstance(DoServerOffline.class).updateDatabase();
    	getInjector().getProvider(SocketSwitch.class).get().stopSocketClient();
		logger.info( "Client Socket Stopping..." );
		getInjector().getProvider(SocketSwitch.class).get().stopSocketServer();
    	logger.info("Socket Server stopping...");
		logger.info( "プラグインが無効になりました。" );
    }
}
