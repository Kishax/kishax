package net.kishax.mc.spigot.module;

import org.slf4j.Logger;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import redis.clients.jedis.Jedis;

import java.nio.file.Path;

import org.bukkit.plugin.java.JavaPlugin;

import net.kishax.mc.common.database.Database;
import net.kishax.mc.common.database.interfaces.DatabaseInfo;
import net.kishax.mc.common.module.interfaces.binding.annotation.DataDirectory;
import net.kishax.mc.common.server.DoServerOffline;
import net.kishax.mc.common.server.DoServerOnline;
import net.kishax.mc.common.server.JedisProvider;
import net.kishax.mc.common.server.Luckperms;
import net.kishax.mc.common.server.ServerStatusCache;
import net.kishax.mc.common.server.interfaces.ServerHomeDir;
import net.kishax.mc.common.socket.PortFinder;
import net.kishax.mc.common.socket.SocketSwitch;
import net.kishax.mc.common.socket.message.handlers.interfaces.minecraft.ServerActionHandler;
import net.kishax.mc.common.socket.message.handlers.interfaces.minecraft.SyncContentHandler;
import net.kishax.mc.common.util.PlayerUtils;
import net.kishax.mc.spigot.database.SpigotDatabaseInfo;
import net.kishax.mc.spigot.server.AutoShutdown;
import net.kishax.mc.spigot.server.BroadCast;
import net.kishax.mc.spigot.server.ImageMap;
import net.kishax.mc.spigot.server.InventoryCheck;
import net.kishax.mc.spigot.server.ItemFrames;
import net.kishax.mc.spigot.server.SpigotServerHomeDir;
import net.kishax.mc.spigot.server.cmd.sub.CommandForward;
import net.kishax.mc.spigot.server.cmd.sub.ReloadConfig;
import net.kishax.mc.spigot.server.cmd.sub.portal.PortalsDelete;
import net.kishax.mc.spigot.server.events.EventListener;
import net.kishax.mc.spigot.server.events.WandListener;
import net.kishax.mc.spigot.server.menu.Menu;
import net.kishax.mc.spigot.socket.message.handlers.minecraft.SpigotSyncContentHandler;
import net.kishax.mc.spigot.socket.message.handlers.minecraft.server.SpigotServerActionHandler;
import net.kishax.mc.spigot.util.RunnableTaskUtil;
import net.kishax.mc.spigot.util.config.PortalsConfig;
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
