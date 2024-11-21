package keyp.forev.fmc.spigot.util;

import org.slf4j.Logger;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import keyp.forev.fmc.common.SocketResponse;
import keyp.forev.fmc.common.Database;
import keyp.forev.fmc.common.JedisProvider;
import keyp.forev.fmc.common.Luckperms;
import keyp.forev.fmc.common.PlayerUtils;
import keyp.forev.fmc.common.SocketSwitch;
import redis.clients.jedis.Jedis;
import keyp.forev.fmc.spigot.cmd.Book;
import keyp.forev.fmc.spigot.cmd.CommandForward;
import keyp.forev.fmc.spigot.cmd.Menu;
import keyp.forev.fmc.spigot.cmd.PortalsDelete;
import keyp.forev.fmc.spigot.cmd.ReloadConfig;
import keyp.forev.fmc.spigot.events.EventListener;
import org.bukkit.plugin.java.JavaPlugin;
import com.google.inject.Provider;
import com.google.inject.Singleton;

public class Module extends AbstractModule {
	private final JavaPlugin plugin;
	private final Logger logger;
	private final Database db;
	private final PlayerUtils pu;
	public Module(JavaPlugin plugin, Logger logger) {
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
		bind(JavaPlugin.class).toInstance(plugin);
		bind(Database.class).toInstance(db);
		bind(org.slf4j.Logger.class).toInstance(logger);
		bind(Jedis.class).toProvider(JedisProvider.class);
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

	@Provides
    @Singleton
    public SocketResponse provideSpigotSocketResponse(
            JavaPlugin plugin,
            Logger logger,
            Database db,
            ServerStatusCache ssc,
            ServerHomeDir shd,
            Provider<SocketSwitch> sswProvider,
            AutoShutdown asd,
            Inventory inv,
            Menu menu,
            Luckperms lp) {
        return new SpigotSocketResponse(plugin, logger, db, ssc, shd, sswProvider, asd, inv, menu, lp);
    }
}
