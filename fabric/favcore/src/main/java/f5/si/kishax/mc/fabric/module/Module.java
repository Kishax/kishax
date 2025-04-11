package f5.si.kishax.mc.fabric.module;

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
import f5.si.kishax.mc.fabric.database.FabricDatabaseInfo;
import f5.si.kishax.mc.fabric.server.AutoShutdown;
import f5.si.kishax.mc.fabric.server.CountdownTask;
import f5.si.kishax.mc.fabric.server.FabricLuckperms;
import f5.si.kishax.mc.fabric.server.FabricServerHomeDir;
import f5.si.kishax.mc.fabric.server.cmd.sub.CommandForward;
import f5.si.kishax.mc.fabric.util.config.FabricConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;

public class Module extends AbstractModule {
  private final FabricLoader fabric;
  private final Logger logger;
  private final MinecraftServer server;
  public Module(FabricLoader fabric, Logger logger, MinecraftServer server) {
    this.fabric = fabric;
    this.logger = logger;
    this.server = server;
  }

  @Override
  protected void configure() {
    bind(FabricLoader.class).toInstance(fabric);
    bind(MinecraftServer.class).toInstance(server);
    bind(FabricConfig.class);
    bind(DatabaseInfo.class).to(FabricDatabaseInfo.class).in(Singleton.class);
    bind(Database.class);
    bind(PlayerUtils.class);
    bind(Logger.class).toInstance(logger);
    bind(DoServerOnline.class);
    bind(DoServerOffline.class);
    bind(PortFinder.class);
    bind(FabricLuckperms.class);
    bind(AutoShutdown.class);
    bind(CommandForward.class);
    bind(CountdownTask.class);
  }

  @Provides
  @Singleton
  @DataDirectory
  public Path provideDataDirectory() {
    return fabric.getConfigDir().resolve("Kishax");
  }

  @Provides
  @Singleton
  public ServerHomeDir provideServerHomeDir(FabricLoader fabric) {
    return new FabricServerHomeDir(fabric);
  }

  @Provides
  @Singleton
  public SocketSwitch provideSocketSwitch(Logger logger, Injector injector) {
    return new SocketSwitch(logger, injector);
  }
}
