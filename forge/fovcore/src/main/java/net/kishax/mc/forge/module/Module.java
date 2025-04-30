package net.kishax.mc.forge.module;

import java.nio.file.Path;

import org.slf4j.Logger;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import net.kishax.mc.common.database.Database;
import net.kishax.mc.common.database.interfaces.DatabaseInfo;
import net.kishax.mc.common.module.interfaces.binding.annotation.DataDirectory;
import net.kishax.mc.common.server.DoServerOffline;
import net.kishax.mc.common.server.DoServerOnline;
import net.kishax.mc.common.server.interfaces.ServerHomeDir;
import net.kishax.mc.common.socket.PortFinder;
import net.kishax.mc.common.socket.SocketSwitch;
import net.kishax.mc.common.util.PlayerUtils;
import net.kishax.mc.forge.Main;
import net.kishax.mc.forge.database.ForgeDatabaseInfo;
import net.kishax.mc.forge.server.AutoShutdown;
import net.kishax.mc.forge.server.CountdownTask;
import net.kishax.mc.forge.server.ForgeLuckperms;
import net.kishax.mc.forge.server.ForgeServerHomeDir;
import net.kishax.mc.forge.server.cmd.sub.CommandForward;
import net.kishax.mc.forge.util.config.ForgeConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.loading.FMLPaths;

public class Module extends AbstractModule {
  private final MinecraftServer server;
  private final Logger logger;
  private final Path configPath;
  public Module(Logger logger, MinecraftServer server) {
    this.logger = logger;
    this.server = server;
    this.configPath = FMLPaths.CONFIGDIR.get();
  }

  @Override
  protected void configure() {
    bind(MinecraftServer.class).toInstance(server);
    bind(ForgeConfig.class);
    bind(DatabaseInfo.class).to(ForgeDatabaseInfo.class).in(Singleton.class);
    bind(Database.class);
    bind(PlayerUtils.class);
    bind(Logger.class).toInstance(logger);
    bind(DoServerOnline.class);
    bind(DoServerOffline.class);
    bind(PortFinder.class);
    bind(ForgeLuckperms.class);
    bind(AutoShutdown.class);
    bind(CommandForward.class);
    bind(CountdownTask.class);
  }

  @Provides
  @Singleton
  @DataDirectory
  public Path provideDataDirectory() {
    return configPath.resolve(Main.MODID);
  }

  @Provides
  @Singleton
  public ServerHomeDir providesForgeServerHomeDir() {
    return new ForgeServerHomeDir(configPath);
  }

  @Provides
  @Singleton
  public SocketSwitch provideSocketSwitch(Logger logger, Injector injector) {
    return new SocketSwitch(logger, injector);
  }
}
