package keyp.forev.fmc.velocity.module;

import java.nio.file.Path;

import org.slf4j.Logger;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.ProxyServer;

import keyp.forev.fmc.common.database.Database;
import keyp.forev.fmc.common.database.interfaces.DatabaseInfo;
import keyp.forev.fmc.common.server.DefaultLuckperms;
import keyp.forev.fmc.common.socket.SocketSwitch;
import keyp.forev.fmc.common.socket.interfaces.SocketResponse;
import keyp.forev.fmc.common.util.PlayerUtils;
import keyp.forev.fmc.velocity.Main;
import keyp.forev.fmc.velocity.cmd.sub.CommandForwarder;
import keyp.forev.fmc.velocity.cmd.sub.Maintenance;
import keyp.forev.fmc.velocity.cmd.sub.VelocityRequest;
import keyp.forev.fmc.velocity.cmd.sub.interfaces.Request;
import keyp.forev.fmc.velocity.database.VelocityDatabaseInfo;
import keyp.forev.fmc.velocity.discord.Discord;
import keyp.forev.fmc.velocity.discord.DiscordEventListener;
import keyp.forev.fmc.velocity.discord.EmojiManager;
import keyp.forev.fmc.velocity.discord.MessageEditor;
import keyp.forev.fmc.velocity.server.BroadCast;
import keyp.forev.fmc.velocity.server.DoServerOnline;
import keyp.forev.fmc.velocity.server.FMCBoard;
import keyp.forev.fmc.velocity.server.GeyserMC;
import keyp.forev.fmc.velocity.server.MineStatus;
import keyp.forev.fmc.velocity.server.PlayerDisconnect;
import keyp.forev.fmc.velocity.socket.VelocitySocketResponse;
import keyp.forev.fmc.velocity.util.RomaToKanji;
import keyp.forev.fmc.velocity.util.RomajiConversion;
import keyp.forev.fmc.velocity.util.config.ConfigUtils;
import keyp.forev.fmc.velocity.util.config.VelocityConfig;

public class Module extends AbstractModule {
	private final Main plugin;
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    public Module(Main plugin, ProxyServer server, Logger logger, Path dataDirectory) {
    	this.plugin = plugin;
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Override
    protected void configure() {
        bind(Main.class).toInstance(plugin);
        bind(VelocityConfig.class);
        bind(DatabaseInfo.class).to(VelocityDatabaseInfo.class).in(Singleton.class);
        bind(Database.class);
        bind(PlayerUtils.class);
        bind(ProxyServer.class).toInstance(server);
        bind(Logger.class).toInstance(logger);
        bind(Path.class).annotatedWith(DataDirectory.class).toInstance(dataDirectory);
        bind(ConsoleCommandSource.class).toInstance(server.getConsoleCommandSource());
        bind(SocketSwitch.class);
        bind(BroadCast.class);
        bind(RomaToKanji.class);
        bind(PlayerDisconnect.class);
        bind(RomajiConversion.class);
        //bind(Discord.class);
        bind(DiscordEventListener.class);
        bind(EmojiManager.class);
        bind(MessageEditor.class);
        bind(MineStatus.class);
        bind(ConfigUtils.class);
        //bind(Request.class).to(VelocityRequest.class);
        bind(GeyserMC.class);
        bind(Maintenance.class);
        bind(FMCBoard.class);
        bind(CommandForwarder.class);
        bind(DefaultLuckperms.class);

        // 試験要素
        //bind(Discord2.class);
    }

    @Provides
    @Singleton
    public Request provideRequest(
        ProxyServer server, 
        Logger logger, 
        VelocityConfig config, 
        Database db, 
        BroadCast bc, 
        Discord discord, 
        MessageEditor discordME, 
        EmojiManager emoji, 
        DefaultLuckperms lp, 
        PlayerUtils pu, 
        DoServerOnline dso
    ) {
        return new VelocityRequest(server, logger, config, db, bc, discord, discordME, emoji, lp, pu, dso);
    }

    @Provides
    @Singleton
    public Discord provideDiscord(
        Logger logger, 
        VelocityConfig config, 
        Database db, 
        Provider<Request> req
    ) {
        try {
            return new Discord(logger, config, db, req);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Provides
    @Singleton
    public SocketResponse provideVelocitySocketResponse(
            Logger logger, 
            ProxyServer server, 
            Database db, 
            VelocityConfig config, 
            DefaultLuckperms lp, 
            BroadCast bc, 
            ConsoleCommandSource console, 
            MessageEditor discordME, 
            Provider<SocketSwitch> sswProvider, 
            CommandForwarder cf) {
        return new VelocitySocketResponse(logger, server, db, config, lp, bc, console, discordME, sswProvider, cf);
    }
}