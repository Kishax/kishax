package spigot;


import org.slf4j.Logger;

import com.google.inject.AbstractModule;

import common.Database;
import common.JedisProvider;
import common.Luckperms;
import common.PlayerUtils;
import common.SocketSwitch;
import redis.clients.jedis.Jedis;
import spigot_command.Book;
import spigot_command.CommandForward;
import spigot_command.Menu;
import spigot_command.PortalsDelete;
import spigot_command.ReloadConfig;

public class Module extends AbstractModule {
	private final common.Main plugin;
	private final Logger logger;
	private final Database db;
	private final PlayerUtils pu;
	public Module(common.Main plugin, Logger logger) {
		this.plugin = plugin;
		this.logger = logger;
		String host = plugin.getConfig().getString("MySQL.Host", null),
        	user = plugin.getConfig().getString("MySQL.User", null),
        	password = plugin.getConfig().getString("MySQL.Password", null),
			defaultDatabase = plugin.getConfig().getString("MySQL.Database", null);
		int port = plugin.getConfig().getInt("MySQL.Port", 0);
		this.db = host != null && port != 0 && defaultDatabase != null && user != null && password != null ? new Database(logger, host, user, defaultDatabase, password, port) : null;
		Database.staticInstance = db;
		this.pu = db != null ? new PlayerUtils(db, logger) : null;
    }
	
	@Override
    protected void configure() {
		bind(Database.class).toInstance(db);
		bind(org.slf4j.Logger.class).toInstance(logger);
		bind(Jedis.class).toProvider(JedisProvider.class);
		bind(common.Main.class).toInstance(plugin);
		bind(SocketSwitch.class);
		bind(ServerHomeDir.class);
		bind(DoServerOnline.class);
		bind(DoServerOffline.class);
		bind(PortalsConfig.class).in(com.google.inject.Scopes.SINGLETON);
		bind(Menu.class);
		bind(PortalsDelete.class);
		bind(EventListener.class);
		bind(WandListener.class);
		bind(ReloadConfig.class);
		bind(ServerStatusCache.class).in(com.google.inject.Scopes.SINGLETON);
		bind(PortFinder.class);
		bind(PlayerUtils.class).toInstance(pu);
		bind(AutoShutdown.class);
		bind(Rcon.class);
		bind(Luckperms.class);
		bind(ImageMap.class);
		bind(Book.class);
		bind(Inventory.class);
		bind(FMCItemFrame.class);
		bind(CommandForward.class);
    }
}
