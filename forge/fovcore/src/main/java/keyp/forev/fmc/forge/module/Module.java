package keyp.forev.fmc.forge.module;

import java.nio.file.Path;

import org.slf4j.Logger;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import keyp.forev.fmc.common.database.Database;
import keyp.forev.fmc.common.database.interfaces.DatabaseInfo;
import keyp.forev.fmc.common.module.interfaces.binding.annotation.DataDirectory;
import keyp.forev.fmc.common.server.DoServerOffline;
import keyp.forev.fmc.common.server.DoServerOnline;
import keyp.forev.fmc.common.server.interfaces.ServerHomeDir;
import keyp.forev.fmc.common.socket.PortFinder;
import keyp.forev.fmc.common.socket.SocketSwitch;
import keyp.forev.fmc.common.util.PlayerUtils;
import keyp.forev.fmc.forge.Main;
import keyp.forev.fmc.forge.database.ForgeDatabaseInfo;
import keyp.forev.fmc.forge.server.AutoShutdown;
import keyp.forev.fmc.forge.server.CountdownTask;
import keyp.forev.fmc.forge.server.ForgeLuckperms;
import keyp.forev.fmc.forge.server.ForgeServerHomeDir;
import keyp.forev.fmc.forge.server.cmd.sub.CommandForward;
import keyp.forev.fmc.forge.util.config.ForgeConfig;
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
