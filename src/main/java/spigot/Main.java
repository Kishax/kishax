package spigot;

import java.sql.Connection;
import java.util.TimeZone;

import org.bukkit.command.PluginCommand;
import org.slf4j.Logger;

import com.google.inject.Guice;
import com.google.inject.Injector;

import common.Database;
import net.luckperms.api.LuckPermsProvider;
import spigot_command.FMCCommand;
import spigot_command.Q;

public class Main {
	private static Injector injector = null;
	private final common.Main plugin;
	private final Logger logger;
	public Main(common.Main plugin, Logger logger) {
		this.plugin = plugin;
		this.logger = logger;
	}
	
	public void onEnable() {
		logger.info("detected spigot platform.");
		String host = plugin.getConfig().getString("MySQL.Host", ""),
        	user = plugin.getConfig().getString("MySQL.User", ""),
        	password = plugin.getConfig().getString("MySQL.Password", ""),
			defaultDatabase = plugin.getConfig().getString("MySQL.Database", "");
		int port = plugin.getConfig().getInt("MySQL.Port", 0);
		Database db = new Database(logger, host, user, defaultDatabase, password, port);
        injector = Guice.createInjector(new spigot.Module(plugin, logger, db));
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));
		getInjector().getInstance(AutoShutdown.class).startCheckForPlayers();
	    plugin.saveDefaultConfig();
		getInjector().getInstance(PortalsConfig.class).createPortalsConfig();
    	plugin.getServer().getPluginManager().registerEvents(getInjector().getInstance(EventListener.class), plugin);
        plugin.getServer().getPluginManager().registerEvents(getInjector().getInstance(WandListener.class), plugin);
		PluginCommand fmcCmd = plugin.getCommand("fmc"),
			qCmd = plugin.getCommand("q");
		if (fmcCmd != null) {
			fmcCmd.setExecutor(getInjector().getInstance(FMCCommand.class));
		}
		if (qCmd != null) {
			qCmd.setExecutor(getInjector().getInstance(Q.class));
		}
    	if (plugin.getConfig().getBoolean("MCVC.Mode",false)) {
    		getInjector().getInstance(Rcon.class).startMCVC();
		}
		getInjector().getInstance(Luckperms.class).triggerNetworkSync();
		logger.info("linking with LuckPerms...");
		logger.info(LuckPermsProvider.get().getPlatform().toString());
		getInjector().getInstance(PlayerUtils.class).loadPlayers();
    	// DoServerOnlineとPortFinderとSocketの処理を統合
		getInjector().getInstance(ServerStatusCache.class).serverStatusCache();
		try (Connection conn = getInjector().getInstance(Database.class).getConnection()) {
			//getInjector().getInstance(ImageMap.class).loadAllImages(conn);
			getInjector().getInstance(ImageMap.class).loadAllItemInThisServerFrames(conn);
		} catch (Exception e) {
			logger.error( "既存マップの画像置換中にエラーが発生しました: {}", e.getMessage());
			logger.error( "An error occurred while loading images: {}", e.getMessage());
			for (StackTraceElement element : e.getStackTrace()) {
				logger.error(element.toString());
			}
		}
		logger.info("fmc plugin has been enabled.");
    }
    
	public static Injector getInjector() {
        return injector;
    }
	
    public void onDisable() {
		try {
			getInjector().getInstance(DoServerOffline.class).UpdateDatabase();
		} catch (Exception e) {
			logger.error( "An error occurred while updating the database: {}", e.getMessage());
		}
    	if (plugin.getConfig().getBoolean("MCVC.Mode",false)) {
    		getInjector().getInstance(Rcon.class).stopMCVC();
		}
    	getInjector().getInstance(AutoShutdown.class).stopCheckForPlayers();
    	logger.info("Socket Server stopping...");
    	logger.info("プラグインが無効になりました。");
    }
}
