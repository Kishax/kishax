package keyp.forev.fmc.fabric.util;

import java.io.IOException;

import org.slf4j.Logger;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import keyp.forev.fmc.common.Database;
import keyp.forev.fmc.common.DoServerOffline;
import keyp.forev.fmc.common.DoServerOnline;
import keyp.forev.fmc.common.PlayerUtils;
import keyp.forev.fmc.common.PortFinder;
import keyp.forev.fmc.common.ServerHomeDir;
import keyp.forev.fmc.common.SocketResponse;
import keyp.forev.fmc.common.SocketSwitch;
import keyp.forev.fmc.fabric.cmd.CommandForward;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;

public class Module extends AbstractModule {
	private final FabricLoader fabric;
	private final Database db;
	private final Config config;
	private final Logger logger;
	private final MinecraftServer server;
	private final PlayerUtils pu;
	public Module(FabricLoader fabric, Logger logger, MinecraftServer server) {
		this.fabric = fabric;
		this.logger = logger;
		this.server = server;
		this.config = new Config(fabric, logger);
		String host = null,
		    user = null,
		    password = null, 
		    defaultDatabase = null;
		int port = 0;
		try {
		    config.loadConfig();
		    host = config.getString("MySQL.Host", null);
		    user = config.getString("MySQL.User", null);
		    password = config.getString("MySQL.Password", null);
		    defaultDatabase = config.getString("MySQL.Database", null);
		    port = config.getInt("MySQL.Port", 0);
		} catch (IOException e1) {
		    logger.error("Error loading config", e1);
		}
		this.db = host != null && port != 0 && defaultDatabase != null && user != null && password != null ? new Database(logger, host, user, defaultDatabase, password, port) : null;
		Database.staticInstance = db;
		this.pu = db != null ? new PlayerUtils(db, logger) : null;
	}
	
	@Override
	protected void configure() {
		bind(FabricLoader.class).toInstance(fabric);
		bind(Logger.class).toInstance(logger);
		bind(MinecraftServer.class).toInstance(server);
		bind(Database.class).toInstance(db);
		bind(Config.class).toInstance(config);
		bind(SocketSwitch.class);
		bind(DoServerOnline.class);
		bind(DoServerOffline.class);
		bind(PortFinder.class);
		bind(PlayerUtils.class).toInstance(pu);
		bind(FabricLuckperms.class);
		bind(AutoShutdown.class);
		bind(CommandForward.class);
		bind(Rcon.class);
		bind(CountdownTask.class);
	}
	
	@Provides
	@Singleton
	public ServerHomeDir provideServerHomeDir(FabricLoader fabric) {
		return new FabricServerHomeDir(fabric);
	}
	
	@Provides
	@Singleton
	public SocketResponse provideSocketResponse() {
		return new FabricSocketResponse();
	}
}
