package velocity;

import java.io.IOException;
import java.nio.file.Path;

import org.slf4j.Logger;

import com.google.inject.AbstractModule;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.ProxyServer;

import discord.Discord;
import discord.DiscordEventListener;
import discord.DiscordInterface;
import discord.EmojiManager;
import discord.MessageEditor;
import discord.MessageEditorInterface;
import net.luckperms.api.LuckPerms;

public class Module extends AbstractModule
{
	private final Main plugin;
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private final LuckPerms lpapi;
    private final Config config;
    
    public Module(Main plugin, ProxyServer server, Logger logger, Path dataDirectory, LuckPerms lpapi)
    {
    	this.plugin = plugin;
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.lpapi = lpapi;
        
        // Config インスタンスの作成とバインド
        // Configをロードする前に、Guiceによってインスタンスが開始されてしまうために、Config not given状態になる。
    	// ゆえ、それよりも前にconfigを手動でインスタンス開始する。
        this.config = new Config(server, logger, dataDirectory);
    	try
        {
            config.loadConfig(); // 一度だけロードする
        }
        catch (IOException e1)
        {
            logger.error("Error loading config", e1);
        }
    }

    @Override
    protected void configure()
    {
    	// 以下、Guiceが、クラス同士の依存性を自動判別するため、bindを書く順番はインジェクションの依存関係に関係しない。
    	bind(Main.class).toInstance(plugin);
        bind(ProxyServer.class).toInstance(server);
        bind(Logger.class).toInstance(logger);
        bind(Path.class).annotatedWith(DataDirectory.class).toInstance(dataDirectory);
        bind(LuckPerms.class).toInstance(lpapi);
        bind(ConsoleCommandSource.class).toInstance(server.getConsoleCommandSource());
        bind(Config.class).toInstance(config);
        bind(SocketSwitch.class);
        bind(DatabaseInterface.class).to(Database.class);
        bind(BroadCast.class);
        bind(SocketResponse.class);
        bind(velocity.PlayerUtil.class);
        bind(RomaToKanji.class);
        bind(PlayerDisconnect.class);
        bind(RomajiConversion.class);
        bind(DiscordInterface.class).to(Discord.class);
        bind(DiscordEventListener.class);
        bind(EmojiManager.class);
        bind(MessageEditorInterface.class).to(MessageEditor.class);
    }
}