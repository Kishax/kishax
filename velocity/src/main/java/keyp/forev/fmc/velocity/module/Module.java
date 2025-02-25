package keyp.forev.fmc.velocity.module;

import java.nio.file.Path;

import org.slf4j.Logger;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.ProxyServer;

import keyp.forev.fmc.common.database.Database;
import keyp.forev.fmc.common.database.interfaces.DatabaseInfo;
import keyp.forev.fmc.common.server.Luckperms;
import keyp.forev.fmc.common.socket.SocketSwitch;
import keyp.forev.fmc.common.socket.message.handlers.interfaces.minecraft.ServerActionHandler;
import keyp.forev.fmc.common.socket.message.handlers.interfaces.minecraft.commands.ForwardHandler;
import keyp.forev.fmc.common.socket.message.handlers.interfaces.minecraft.commands.ImageMapHandler;
import keyp.forev.fmc.common.socket.message.handlers.interfaces.minecraft.commands.InputHandler;
import keyp.forev.fmc.common.socket.message.handlers.interfaces.minecraft.commands.TeleportPlayerHandler;
import keyp.forev.fmc.common.socket.message.handlers.interfaces.minecraft.commands.TeleportPointHandler;
import keyp.forev.fmc.common.socket.message.handlers.interfaces.web.MinecraftWebConfirmHandler;
import keyp.forev.fmc.common.util.PlayerUtils;
import keyp.forev.fmc.velocity.Main;
import keyp.forev.fmc.velocity.database.VelocityDatabaseInfo;
import keyp.forev.fmc.velocity.discord.Discord;
import keyp.forev.fmc.velocity.discord.DiscordEventListener;
import keyp.forev.fmc.velocity.discord.EmojiManager;
import keyp.forev.fmc.velocity.discord.MessageEditor;
import keyp.forev.fmc.velocity.discord.Webhooker;
import keyp.forev.fmc.velocity.server.BroadCast;
import keyp.forev.fmc.velocity.server.DoServerOnline;
import keyp.forev.fmc.velocity.server.FMCBoard;
import keyp.forev.fmc.velocity.server.GeyserMC;
import keyp.forev.fmc.velocity.server.MineStatus;
import keyp.forev.fmc.velocity.server.PlayerDisconnect;
import keyp.forev.fmc.velocity.server.cmd.sub.CommandForwarder;
import keyp.forev.fmc.velocity.server.cmd.sub.Maintenance;
import keyp.forev.fmc.velocity.server.cmd.sub.VelocityRequest;
import keyp.forev.fmc.velocity.server.cmd.sub.interfaces.Request;
import keyp.forev.fmc.velocity.socket.message.handlers.minecraft.command.VelocityForwardHandler;
import keyp.forev.fmc.velocity.socket.message.handlers.minecraft.command.VelocityImageMapHandler;
import keyp.forev.fmc.velocity.socket.message.handlers.minecraft.command.VelocityInputHandler;
import keyp.forev.fmc.velocity.socket.message.handlers.minecraft.command.VelocityTeleportPlayerHandler;
import keyp.forev.fmc.velocity.socket.message.handlers.minecraft.command.VelocityTeleportPointHandler;
import keyp.forev.fmc.velocity.socket.message.handlers.minecraft.server.VelocityServerActionHandler;
import keyp.forev.fmc.velocity.socket.message.handlers.web.VelocityMinecraftWebConfirmHandler;
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
        bind(Luckperms.class);
        bind(Webhooker.class);

        bind(ForwardHandler.class).to(VelocityForwardHandler.class);
        bind(ImageMapHandler.class).to(VelocityImageMapHandler.class);
        bind(InputHandler.class).to(VelocityInputHandler.class);
        bind(TeleportPlayerHandler.class).to(VelocityTeleportPlayerHandler.class);
        bind(TeleportPointHandler.class).to(VelocityTeleportPointHandler.class);

        bind(ServerActionHandler.class).to(VelocityServerActionHandler.class);

        bind(MinecraftWebConfirmHandler.class).to(VelocityMinecraftWebConfirmHandler.class);
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
        Luckperms lp, 
        PlayerUtils pu, 
        DoServerOnline dso,
        PlayerDisconnect pd
    ) {
        return new VelocityRequest(server, logger, config, db, bc, discord, discordME, emoji, lp, pu, dso, pd);
    }

    @Provides
    @Singleton
    public Discord provideDiscord(
        Logger logger, 
        VelocityConfig config, 
        Database db, 
        Provider<Request> reqProvider,
        Provider<MessageEditor> discordMEProvider,
        BroadCast bc
    ) {
        try {
            return new Discord(logger, config, db, discordMEProvider, reqProvider, bc);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Provides
    @Singleton
    public SocketSwitch provideSocketSwitch(Logger logger, Injector injector) {
        return new SocketSwitch(logger, injector);
    }
}
