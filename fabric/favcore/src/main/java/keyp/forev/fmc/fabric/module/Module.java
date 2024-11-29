package keyp.forev.fmc.fabric.module;

import java.nio.file.Path;

import org.slf4j.Logger;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import keyp.forev.fmc.common.database.Database;
import keyp.forev.fmc.common.database.interfaces.DatabaseInfo;
import keyp.forev.fmc.common.module.interfaces.binding.annotation.DataDirectory;
import keyp.forev.fmc.common.server.DoServerOffline;
import keyp.forev.fmc.common.server.DoServerOnline;
import keyp.forev.fmc.common.server.interfaces.DefaultServerHomeDir;
import keyp.forev.fmc.common.socket.PortFinder;
import keyp.forev.fmc.common.socket.SocketSwitch;
import keyp.forev.fmc.common.socket.interfaces.SocketResponse;
import keyp.forev.fmc.common.util.PlayerUtils;
import keyp.forev.fmc.fabric.cmd.sub.CommandForward;
import keyp.forev.fmc.fabric.database.FabricDatabaseInfo;
import keyp.forev.fmc.fabric.server.AutoShutdown;
import keyp.forev.fmc.fabric.server.CountdownTask;
import keyp.forev.fmc.fabric.server.FabricLuckperms;
import keyp.forev.fmc.fabric.server.FabricServerHomeDir;
import keyp.forev.fmc.fabric.server.Rcon;
import keyp.forev.fmc.fabric.socket.FabricSocketResponse;
import keyp.forev.fmc.fabric.util.config.FabricConfig;
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
		bind(SocketSwitch.class);
		bind(DoServerOnline.class);
		bind(DoServerOffline.class);
		bind(PortFinder.class);
		bind(FabricLuckperms.class);
		bind(AutoShutdown.class);
		bind(CommandForward.class);
		bind(Rcon.class);
		bind(CountdownTask.class);
	}
	
	@Provides
    @Singleton
    @DataDirectory
    public Path provideDataDirectory() {
		return fabric.getConfigDir().resolve("FMC");
    }

	@Provides
	@Singleton
	public DefaultServerHomeDir provideServerHomeDir(FabricLoader fabric) {
		return new FabricServerHomeDir(fabric);
	}
	
	@Provides
	@Singleton
	public SocketResponse provideSocketResponse() {
		return new FabricSocketResponse();
	}
}
