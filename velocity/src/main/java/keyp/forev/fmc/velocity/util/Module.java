package keyp.forev.fmc.velocity.util;

import java.io.IOException;
import java.nio.file.Path;

import org.slf4j.Logger;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.ProxyServer;

import keyp.forev.fmc.common.Database;
import keyp.forev.fmc.common.PlayerUtils;
import keyp.forev.fmc.common.SocketSwitch;
import keyp.forev.fmc.common.Luckperms;
import keyp.forev.fmc.common.SocketResponse;
import keyp.forev.fmc.velocity.Main;
import keyp.forev.fmc.velocity.cmd.CommandForwarder;
import keyp.forev.fmc.velocity.cmd.Maintenance;
import keyp.forev.fmc.velocity.cmd.RequestInterface;
import keyp.forev.fmc.velocity.discord.Discord;
import keyp.forev.fmc.velocity.discord.DiscordEventListener;
import keyp.forev.fmc.velocity.discord.DiscordInterface;
import keyp.forev.fmc.velocity.discord.EmojiManager;
import keyp.forev.fmc.velocity.discord.MessageEditor;
import keyp.forev.fmc.velocity.discord.MessageEditorInterface;

public class Module extends AbstractModule {
	private final Main plugin;
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private final Config config;
    private final Database db;
    private final PlayerUtils pu;
    public Module(Main plugin, ProxyServer server, Logger logger, Path dataDirectory) {
    	this.plugin = plugin;
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.config = new Config(server, logger, dataDirectory);
        String host = null,
            user = null,
            password = null, 
            defaultDatabase = null;
        int port = 0;
    	try {
            config.loadConfig(); // 一度だけロードする
            host = config.getString("MySQL.Host", null);
            user = config.getString("MySQL.User", null);
            password = config.getString("MySQL.Password", null);
            defaultDatabase = config.getString("MySQL.Database", null);
		    port = config.getInt("MySQL.Port", 0);
        } catch (IOException e1) {
            logger.error("Error loading config", e1);
        }
        this.db = host != null && port != 0 && defaultDatabase != null && user != null && password != null ? new Database(logger, host, user, defaultDatabase, password, port) : null;
        Database.staticInstance = db;
        this.pu = db != null ? new PlayerUtils(db, logger) : null;
    }

    @Override
    protected void configure() {
        bind(Database.class).toInstance(db);
        bind(Main.class).toInstance(plugin);
        bind(ProxyServer.class).toInstance(server);
        bind(Logger.class).toInstance(logger);
        bind(Path.class).annotatedWith(DataDirectory.class).toInstance(dataDirectory);
        bind(ConsoleCommandSource.class).toInstance(server.getConsoleCommandSource());
        bind(Config.class).toInstance(config);
        bind(SocketSwitch.class);
        bind(BroadCast.class);
        bind(PlayerUtils.class).toInstance(pu);
        bind(RomaToKanji.class);
        bind(PlayerDisconnect.class);
        bind(RomajiConversion.class);
        bind(DiscordInterface.class).to(Discord.class);
        bind(DiscordEventListener.class);
        bind(EmojiManager.class);
        bind(MessageEditorInterface.class).to(MessageEditor.class);
        bind(MineStatus.class);
        bind(ConfigUtils.class);
        bind(RequestInterface.class).to(keyp.forev.fmc.velocity.cmd.Request.class);
        bind(GeyserMC.class);
        bind(Maintenance.class);
        bind(FMCBoard.class).in(com.google.inject.Scopes.SINGLETON);
        bind(CommandForwarder.class);
        bind(Luckperms.class);
        //bind(SocketResponse.class).to(VelocitySocketResponse.class).in(Singleton.class);
    }

    @Provides
    @Singleton
    public SocketResponse provideVelocitySocketResponse(
            Logger logger, 
            ProxyServer server, 
            Database db, 
            Config config, 
            Luckperms lp, 
            BroadCast bc, 
            ConsoleCommandSource console, 
            MessageEditorInterface discordME, 
            Provider<SocketSwitch> sswProvider, 
            CommandForwarder cf) {
        return new VelocitySocketResponse(logger, server, db, config, lp, bc, console, discordME, sswProvider, cf);
    }
}