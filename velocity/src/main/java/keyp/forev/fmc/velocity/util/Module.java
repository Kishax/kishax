package keyp.forev.fmc.velocity.util;

import java.io.IOException;
import java.nio.file.Path;

import org.slf4j.Logger;

import com.google.inject.AbstractModule;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.ProxyServer;

import common.src.main.java.keyp.forev.fmc.main.Database;
import common.src.main.java.keyp.forev.fmc.main.PlayerUtils;
import common.src.main.java.keyp.forev.fmc.main.SocketSwitch;
import velocity.src.main.java.keyp.forev.fmc.core.command.Maintenance;
import velocity.src.main.java.keyp.forev.fmc.core.command.RequestInterface;
import velocity.src.main.java.keyp.forev.fmc.core.discord.Discord;
import velocity.src.main.java.keyp.forev.fmc.core.discord.DiscordEventListener;
import velocity.src.main.java.keyp.forev.fmc.core.discord.DiscordInterface;
import velocity.src.main.java.keyp.forev.fmc.core.discord.EmojiManager;
import velocity.src.main.java.keyp.forev.fmc.core.discord.MessageEditor;
import velocity.src.main.java.keyp.forev.fmc.core.discord.MessageEditorInterface;

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
        bind(SocketResponse.class);
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
        bind(RequestInterface.class).to(velocity.src.main.java.keyp.forev.fmc.core.command.Request.class);
        bind(GeyserMC.class);
        bind(Maintenance.class);
        bind(FMCBoard.class).in(com.google.inject.Scopes.SINGLETON);
    }
}