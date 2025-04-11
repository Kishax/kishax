package f5.si.kishax.mc.neoforge.module;

import java.nio.file.Path;

import org.slf4j.Logger;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import f5.si.kishax.mc.common.database.Database;
import f5.si.kishax.mc.common.database.interfaces.DatabaseInfo;
import f5.si.kishax.mc.common.module.interfaces.binding.annotation.DataDirectory;
import f5.si.kishax.mc.common.server.DoServerOffline;
import f5.si.kishax.mc.common.server.DoServerOnline;
import f5.si.kishax.mc.common.server.interfaces.ServerHomeDir;
import f5.si.kishax.mc.common.socket.PortFinder;
import f5.si.kishax.mc.common.socket.SocketSwitch;
import f5.si.kishax.mc.common.util.PlayerUtils;
import f5.si.kishax.mc.neoforge.Main;
import f5.si.kishax.mc.neoforge.database.NeoForgeDatabaseInfo;
import f5.si.kishax.mc.neoforge.server.AutoShutdown;
import f5.si.kishax.mc.neoforge.server.CountdownTask;
import f5.si.kishax.mc.neoforge.server.NeoForgeLuckperms;
import f5.si.kishax.mc.neoforge.server.NeoForgeServerHomeDir;
import f5.si.kishax.mc.neoforge.server.cmd.sub.CommandForward;
import f5.si.kishax.mc.neoforge.util.config.NeoForgeConfig;
import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.loading.FMLPaths;

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
    bind(NeoForgeConfig.class);
    bind(DatabaseInfo.class).to(NeoForgeDatabaseInfo.class).in(Singleton.class);
    bind(Database.class);
    bind(PlayerUtils.class);
    bind(Logger.class).toInstance(logger);
    bind(DoServerOnline.class);
    bind(DoServerOffline.class);
    bind(PortFinder.class);
    bind(NeoForgeLuckperms.class);
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
    return new NeoForgeServerHomeDir(configPath);
  }

  @Provides
  @Singleton
  public SocketSwitch provideSocketSwitch(Logger logger, Injector injector) {
    return new SocketSwitch(logger, injector);
  }
}
