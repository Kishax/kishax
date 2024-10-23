package velocity;

import java.nio.file.Path;
import java.util.TimeZone;

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

import discord.Discord;
import net.luckperms.api.LuckPermsProvider;
import velocity_command.CEnd;
import velocity_command.FMCCommand;
import velocity_command.Hub;

public class Main {
	public static boolean isVelocity = true;
	private static Injector injector = null;
	
	private final ProxyServer server;
	private final Logger logger;
	private final Path dataDirectory;
	// Guice注入後、取得するインスタンス(フィールド)郡
    @Inject
    public Main(ProxyServer serverinstance, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = serverinstance;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent e) {
    	logger.info("Detected Velocity platform.");
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));
        injector = Guice.createInjector(new velocity.Module(this, server, logger, dataDirectory, LuckPermsProvider.get()));
    	getInjector().getInstance(Discord.class).loginDiscordBotAsync().thenAccept(jda -> {
            if (jda != null) {
                //getInjector().getInstance(MineStatusReflect.class).sendEmbedMessage(jda);
                getInjector().getInstance(MineStatusReflect.class).start(jda);
            }
        }); 		
    	getInjector().getInstance(DoServerOnline.class).updateDatabase();
    	server.getEventManager().register(this, getInjector().getInstance(EventListener.class));
    	getInjector().getInstance(Luckperms.class).triggerNetworkSync();
 		logger.info("luckpermsと連携しました。");
 		getInjector().getInstance(PlayerUtil.class).loadPlayers();
    	CommandManager commandManager = server.getCommandManager();
        commandManager.register(commandManager.metaBuilder("fmcp").build(), getInjector().getInstance(FMCCommand.class));
        commandManager.register(commandManager.metaBuilder("hub").build(), getInjector().getInstance(Hub.class));
        commandManager.register(commandManager.metaBuilder("cend").build(), getInjector().getInstance(CEnd.class));
        getInjector().getProvider(SocketSwitch.class).get().startSocketServer();
	    logger.info("プラグインが有効になりました。");
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
