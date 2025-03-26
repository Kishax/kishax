package keyp.forev.fmc.spigot.module;

import org.slf4j.Logger;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;

import keyp.forev.fmc.common.database.Database;
import keyp.forev.fmc.common.database.interfaces.DatabaseInfo;
import keyp.forev.fmc.common.module.interfaces.binding.annotation.DataDirectory;
import keyp.forev.fmc.common.server.Luckperms;
import keyp.forev.fmc.common.server.DoServerOffline;
import keyp.forev.fmc.common.server.DoServerOnline;
import keyp.forev.fmc.common.server.JedisProvider;
import keyp.forev.fmc.common.server.ServerStatusCache;
import keyp.forev.fmc.common.server.interfaces.ServerHomeDir;
import keyp.forev.fmc.common.socket.PortFinder;
import keyp.forev.fmc.common.socket.SocketSwitch;
import keyp.forev.fmc.common.socket.message.handlers.interfaces.discord.RuleBookSyncHandler;
import keyp.forev.fmc.common.socket.message.handlers.interfaces.minecraft.ServerActionHandler;
import keyp.forev.fmc.common.socket.message.handlers.interfaces.minecraft.SyncContentHandler;
import keyp.forev.fmc.common.util.PlayerUtils;
import redis.clients.jedis.Jedis;
import keyp.forev.fmc.spigot.database.SpigotDatabaseInfo;
import keyp.forev.fmc.spigot.server.AutoShutdown;
import keyp.forev.fmc.spigot.server.BroadCast;
import keyp.forev.fmc.spigot.server.FMCItemFrame;
import keyp.forev.fmc.spigot.server.ImageMap;
import keyp.forev.fmc.spigot.server.InventoryCheck;
import keyp.forev.fmc.spigot.server.SpigotServerHomeDir;
import keyp.forev.fmc.spigot.server.cmd.sub.Book;
import keyp.forev.fmc.spigot.server.cmd.sub.CommandForward;
import keyp.forev.fmc.spigot.server.cmd.sub.ReloadConfig;
import keyp.forev.fmc.spigot.server.cmd.sub.portal.PortalsDelete;
import keyp.forev.fmc.spigot.server.events.EventListener;
import keyp.forev.fmc.spigot.server.events.WandListener;
import keyp.forev.fmc.spigot.server.menu.Menu;
import keyp.forev.fmc.spigot.socket.message.handlers.discord.SpigotRuleBookSyncHandler;
import keyp.forev.fmc.spigot.socket.message.handlers.minecraft.SpigotSyncContentHandler;
import keyp.forev.fmc.spigot.socket.message.handlers.minecraft.server.SpigotServerActionHandler;
import keyp.forev.fmc.spigot.util.RunnableTaskUtil;
import keyp.forev.fmc.spigot.util.config.PortalsConfig;

import java.nio.file.Path;

import org.bukkit.plugin.java.JavaPlugin;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;

public class Module extends AbstractModule {
  private final JavaPlugin plugin;
  private final Logger logger;
  private final BukkitAudiences audiences;

  public Module(JavaPlugin plugin, Logger logger, BukkitAudiences audiences) {
    this.plugin = plugin;
    this.logger = logger;
    this.audiences = audiences;
  }

  @Override
  protected void configure() {
    bind(JavaPlugin.class).toInstance(plugin);
    bind(BukkitAudiences.class).toInstance(audiences);
    bind(PortalsConfig.class);
    bind(DatabaseInfo.class).to(SpigotDatabaseInfo.class).in(Singleton.class);
    bind(Database.class);
    bind(PlayerUtils.class);
    bind(Logger.class).toInstance(logger);
    bind(Jedis.class).toProvider(JedisProvider.class);
    bind(DoServerOnline.class);
    bind(DoServerOffline.class);
    bind(Menu.class);
    bind(PortalsDelete.class);
    bind(EventListener.class);
    bind(WandListener.class);
    bind(ReloadConfig.class);
    bind(ServerStatusCache.class).in(com.google.inject.Scopes.SINGLETON);
    bind(PortFinder.class);
    bind(AutoShutdown.class);
    bind(Luckperms.class);
    bind(ImageMap.class);
    bind(Book.class);
    bind(InventoryCheck.class);
    bind(FMCItemFrame.class);
    bind(CommandForward.class);
    bind(BroadCast.class);
    bind(RunnableTaskUtil.class);

    bind(RuleBookSyncHandler.class).to(SpigotRuleBookSyncHandler.class);
    bind(ServerActionHandler.class).to(SpigotServerActionHandler.class);
    bind(SyncContentHandler.class).to(SpigotSyncContentHandler.class);
  }

  @Provides
  @Singleton
  @DataDirectory
  public Path provideDataDirectory() {
    return plugin.getDataFolder().toPath();
  }

  @Provides
  @Singleton
  public SocketSwitch provideSocketSwitch(Logger logger, Injector injector) {
    return new SocketSwitch(logger, injector);
  }

  @Provides
  @Singleton
  public ServerHomeDir provideServerHomeDir(JavaPlugin plugin) {
    return new SpigotServerHomeDir(plugin);
  }
}
