package f5.si.kishax.mc.velocity.module;

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

import f5.si.kishax.mc.common.database.Database;
import f5.si.kishax.mc.common.database.interfaces.DatabaseInfo;
import f5.si.kishax.mc.common.server.Luckperms;
import f5.si.kishax.mc.common.socket.SocketSwitch;
import f5.si.kishax.mc.common.socket.message.handlers.interfaces.minecraft.ServerActionHandler;
import f5.si.kishax.mc.common.socket.message.handlers.interfaces.minecraft.commands.ForwardHandler;
import f5.si.kishax.mc.common.socket.message.handlers.interfaces.minecraft.commands.ImageMapHandler;
import f5.si.kishax.mc.common.socket.message.handlers.interfaces.minecraft.commands.InputHandler;
import f5.si.kishax.mc.common.socket.message.handlers.interfaces.minecraft.commands.TeleportPlayerHandler;
import f5.si.kishax.mc.common.socket.message.handlers.interfaces.minecraft.commands.TeleportPointHandler;
import f5.si.kishax.mc.common.socket.message.handlers.interfaces.web.MinecraftWebConfirmHandler;
import f5.si.kishax.mc.common.util.PlayerUtils;
import f5.si.kishax.mc.velocity.Main;
import f5.si.kishax.mc.velocity.database.VelocityDatabaseInfo;
import f5.si.kishax.mc.velocity.discord.Discord;
import f5.si.kishax.mc.velocity.discord.DiscordEventListener;
import f5.si.kishax.mc.velocity.discord.EmojiManager;
import f5.si.kishax.mc.velocity.discord.MessageEditor;
import f5.si.kishax.mc.velocity.discord.Webhooker;
import f5.si.kishax.mc.velocity.server.BroadCast;
import f5.si.kishax.mc.velocity.server.DoServerOnline;
import f5.si.kishax.mc.velocity.server.Board;
import f5.si.kishax.mc.velocity.server.GeyserMC;
import f5.si.kishax.mc.velocity.server.MineStatus;
import f5.si.kishax.mc.velocity.server.PlayerDisconnect;
import f5.si.kishax.mc.velocity.server.cmd.sub.CommandForwarder;
import f5.si.kishax.mc.velocity.server.cmd.sub.Maintenance;
import f5.si.kishax.mc.velocity.server.cmd.sub.VelocityRequest;
import f5.si.kishax.mc.velocity.server.cmd.sub.interfaces.Request;
import f5.si.kishax.mc.velocity.socket.message.handlers.minecraft.command.VelocityForwardHandler;
import f5.si.kishax.mc.velocity.socket.message.handlers.minecraft.command.VelocityImageMapHandler;
import f5.si.kishax.mc.velocity.socket.message.handlers.minecraft.command.VelocityInputHandler;
import f5.si.kishax.mc.velocity.socket.message.handlers.minecraft.command.VelocityTeleportPlayerHandler;
import f5.si.kishax.mc.velocity.socket.message.handlers.minecraft.command.VelocityTeleportPointHandler;
import f5.si.kishax.mc.velocity.socket.message.handlers.minecraft.server.VelocityServerActionHandler;
import f5.si.kishax.mc.velocity.socket.message.handlers.web.VelocityMinecraftWebConfirmHandler;
import f5.si.kishax.mc.velocity.util.RomaToKanji;
import f5.si.kishax.mc.velocity.util.RomajiConversion;
import f5.si.kishax.mc.velocity.util.config.ConfigUtils;
import f5.si.kishax.mc.velocity.util.config.VelocityConfig;

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
    bind(Board.class);
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
