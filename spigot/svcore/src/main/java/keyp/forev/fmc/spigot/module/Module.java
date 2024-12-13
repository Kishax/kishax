package keyp.forev.fmc.spigot.module;

import org.slf4j.Logger;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import keyp.forev.fmc.common.database.Database;
import keyp.forev.fmc.common.database.interfaces.DatabaseInfo;
import keyp.forev.fmc.common.module.interfaces.binding.annotation.DataDirectory;
import keyp.forev.fmc.common.server.Luckperms;
import keyp.forev.fmc.common.server.DoServerOffline;
import keyp.forev.fmc.common.server.DoServerOnline;
import keyp.forev.fmc.common.server.JedisProvider;
import keyp.forev.fmc.common.server.ServerStatusCache;
import keyp.forev.fmc.common.server.interfaces.ServerHomeDir;
import keyp.forev.fmc.common.socket.PortFinder;
import keyp.forev.fmc.common.socket.SocketSwitch;
import keyp.forev.fmc.common.socket.interfaces.SocketResponse;
import keyp.forev.fmc.common.util.PlayerUtils;
import redis.clients.jedis.Jedis;
import keyp.forev.fmc.spigot.database.SpigotDatabaseInfo;
import keyp.forev.fmc.spigot.server.AutoShutdown;
import keyp.forev.fmc.spigot.server.BroadCast;
import keyp.forev.fmc.spigot.server.FMCItemFrame;
import keyp.forev.fmc.spigot.server.ImageMap;
import keyp.forev.fmc.spigot.server.Inventory;
import keyp.forev.fmc.spigot.server.Rcon;
import keyp.forev.fmc.spigot.server.SpigotServerHomeDir;
import keyp.forev.fmc.spigot.server.cmd.sub.Book;
import keyp.forev.fmc.spigot.server.cmd.sub.CommandForward;
import keyp.forev.fmc.spigot.server.cmd.sub.ReloadConfig;
import keyp.forev.fmc.spigot.server.cmd.sub.portal.PortalsDelete;
import keyp.forev.fmc.spigot.server.events.EventListener;
import keyp.forev.fmc.spigot.server.events.WandListener;
import keyp.forev.fmc.spigot.server.menu.Menu;
import keyp.forev.fmc.spigot.socket.SpigotSocketResponse;
import keyp.forev.fmc.spigot.util.RunnableTaskUtil;
import keyp.forev.fmc.spigot.util.config.PortalsConfig;

import java.nio.file.Path;

import org.bukkit.plugin.java.JavaPlugin;
import com.google.inject.Provider;
import com.google.inject.Singleton;

public class Module extends AbstractModule {
	private final JavaPlugin plugin;
	private final Logger logger;
	public Module(JavaPlugin plugin, Logger logger) {
		this.plugin = plugin;
		this.logger = logger;
    }
	
	@Override
    protected void configure() {
		bind(JavaPlugin.class).toInstance(plugin);
		bind(PortalsConfig.class);
		bind(DatabaseInfo.class).to(SpigotDatabaseInfo.class).in(Singleton.class);
		bind(Database.class);
		bind(PlayerUtils.class);
		bind(Logger.class).toInstance(logger);
		bind(Jedis.class).toProvider(JedisProvider.class);
		bind(SocketSwitch.class);
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
		bind(Rcon.class);
		bind(Luckperms.class);
		bind(ImageMap.class);
		bind(Book.class);
		bind(Inventory.class);
		bind(FMCItemFrame.class);
		bind(CommandForward.class);
		bind(BroadCast.class);
		bind(RunnableTaskUtil.class);
    }

	@Provides
    @Singleton
    @DataDirectory
    public Path provideDataDirectory() {
        return plugin.getDataFolder().toPath();
    }

	@Provides
	@Singleton
	public SocketResponse provideSpigotSocketResponse(
	        JavaPlugin plugin,
	        Logger logger,
	        Database db,
	        ServerStatusCache ssc,
	        SpigotServerHomeDir shd,
	        Provider<SocketSwitch> sswProvider,
	        AutoShutdown asd,
	        Inventory inv,
	        Menu menu,
	        Luckperms lp,
	        BroadCast bc) {
	    return new SpigotSocketResponse(plugin, logger, db, ssc, shd, sswProvider, asd, inv, menu, lp, bc);
	}
	
	@Provides
	@Singleton
	public ServerHomeDir provideServerHomeDir(JavaPlugin plugin) {
		return new SpigotServerHomeDir(plugin);
	}
}
