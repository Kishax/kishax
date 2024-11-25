package keyp.forev.fmc.forge.util;

import java.io.IOException;
import java.nio.file.Path;

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
import keyp.forev.fmc.forge.cmd.CommandForward;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.loading.FMLPaths;

public class Module extends AbstractModule {
	private final MinecraftServer server;
	private final Logger logger;
	private final Config config;
	private final Database db;
	private final PlayerUtils pu;
	private final Path configPath;
	public Module (Logger logger, MinecraftServer server) {
		this.configPath = FMLPaths.CONFIGDIR.get();
		
		this.config = new Config(logger, configPath);
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
		this.logger = logger;
		this.server = server;
	}
	
	@Override
    protected void configure() {
		bind(Logger.class).toInstance(logger);
		bind(MinecraftServer.class).toInstance(server);
		bind(Database.class).toInstance(db);
		bind(Config.class).toInstance(config);
		bind(SocketSwitch.class);
		bind(DoServerOnline.class);
		bind(DoServerOffline.class);
		bind(PortFinder.class);
		bind(PlayerUtils.class).toInstance(pu);
		bind(ForgeLuckperms.class);
		bind(AutoShutdown.class);
		bind(CommandForward.class);
		bind(CountdownTask.class);
    }
	
	@Provides
	@Singleton
	public Rcon providesRcon(Logger logger, Config config, MinecraftServer server) {
		return new Rcon(logger, config, server, configPath);
	}
	
	@Provides
	@Singleton
	public ServerHomeDir providesForgeServerHomeDir() {
		return new ForgeServerHomeDir(configPath);
	}
	
	@Provides
	@Singleton
	public SocketResponse provideSocketResponse() {
		return new ForgeSocketResponse();
	}
}
