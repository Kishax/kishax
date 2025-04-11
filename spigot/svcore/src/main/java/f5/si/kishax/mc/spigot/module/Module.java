package f5.si.kishax.mc.spigot.module;

import org.slf4j.Logger;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import redis.clients.jedis.Jedis;

import java.nio.file.Path;

import org.bukkit.plugin.java.JavaPlugin;
import f5.si.kishax.mc.common.database.Database;
import f5.si.kishax.mc.common.database.interfaces.DatabaseInfo;
import f5.si.kishax.mc.common.module.interfaces.binding.annotation.DataDirectory;
import f5.si.kishax.mc.common.server.DoServerOffline;
import f5.si.kishax.mc.common.server.DoServerOnline;
import f5.si.kishax.mc.common.server.JedisProvider;
import f5.si.kishax.mc.common.server.Luckperms;
import f5.si.kishax.mc.common.server.ServerStatusCache;
import f5.si.kishax.mc.common.server.interfaces.ServerHomeDir;
import f5.si.kishax.mc.common.socket.PortFinder;
import f5.si.kishax.mc.common.socket.SocketSwitch;
import f5.si.kishax.mc.common.socket.message.handlers.interfaces.discord.RuleBookSyncHandler;
import f5.si.kishax.mc.common.socket.message.handlers.interfaces.minecraft.ServerActionHandler;
import f5.si.kishax.mc.common.socket.message.handlers.interfaces.minecraft.SyncContentHandler;
import f5.si.kishax.mc.common.util.PlayerUtils;
import f5.si.kishax.mc.spigot.database.SpigotDatabaseInfo;
import f5.si.kishax.mc.spigot.server.AutoShutdown;
import f5.si.kishax.mc.spigot.server.BroadCast;
import f5.si.kishax.mc.spigot.server.ItemFrames;
import f5.si.kishax.mc.spigot.server.ImageMap;
import f5.si.kishax.mc.spigot.server.InventoryCheck;
import f5.si.kishax.mc.spigot.server.SpigotServerHomeDir;
import f5.si.kishax.mc.spigot.server.cmd.sub.Book;
import f5.si.kishax.mc.spigot.server.cmd.sub.CommandForward;
import f5.si.kishax.mc.spigot.server.cmd.sub.ReloadConfig;
import f5.si.kishax.mc.spigot.server.cmd.sub.portal.PortalsDelete;
import f5.si.kishax.mc.spigot.server.events.EventListener;
import f5.si.kishax.mc.spigot.server.events.WandListener;
import f5.si.kishax.mc.spigot.server.menu.Menu;
import f5.si.kishax.mc.spigot.socket.message.handlers.discord.SpigotRuleBookSyncHandler;
import f5.si.kishax.mc.spigot.socket.message.handlers.minecraft.SpigotSyncContentHandler;
import f5.si.kishax.mc.spigot.socket.message.handlers.minecraft.server.SpigotServerActionHandler;
import f5.si.kishax.mc.spigot.util.RunnableTaskUtil;
import f5.si.kishax.mc.spigot.util.config.PortalsConfig;
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
    bind(ItemFrames.class);
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
