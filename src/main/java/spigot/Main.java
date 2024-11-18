package spigot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.TimeZone;

import org.bukkit.command.PluginCommand;
import org.slf4j.Logger;

import com.google.inject.Guice;
import com.google.inject.Injector;

import common.Database;
import common.Luckperms;
import common.PlayerUtils;
import common.SocketServerThread;
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
		SocketServerThread.platform.set("spigot");
        injector = Guice.createInjector(new spigot.Module(plugin, logger));
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));
		Database db = getInjector().getInstance(Database.class);
		try (Connection conn = db.getConnection()) {
			ifHubThenUpdate(conn);
			db.defineImageColumnNamesList(conn, "images");
		} catch (SQLException | ClassNotFoundException e) {
			logger.error("An error occurred while updating the database: {}", e.getMessage());
		}
		//getInjector().getInstance(TPSUtils.class).startTickMonitor();
		//plugin.getServer().getScheduler().runTaskTimer(plugin, getInjector().getInstance(TPSUtils.class)::log, 0L, 100L);
		plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
			logger.info("TPS: {}", TPSUtils2.getTPS());
		}, 0L, 100L);
		getInjector().getInstance(AutoShutdown.class).startCheckForPlayers();
	    plugin.saveDefaultConfig();
		getInjector().getInstance(PortalsConfig.class).createPortalsConfig();
    	plugin.getServer().getPluginManager().registerEvents(getInjector().getInstance(EventListener.class), plugin);
        plugin.getServer().getPluginManager().registerEvents(getInjector().getInstance(WandListener.class), plugin);
		PluginCommand fmcCmd = plugin.getCommand("fmc"),
			qCmd = plugin.getCommand("q"),
			tprCmd = plugin.getCommand("tpr"),
			tprmCmd = plugin.getCommand("tprm"),
			tprmaCmd = plugin.getCommand("tprma"),
			tpraCmd = plugin.getCommand("tpra");
		if (fmcCmd != null) {
			fmcCmd.setExecutor(getInjector().getInstance(FMCCommand.class));
		}
		if (qCmd != null) {
			qCmd.setExecutor(getInjector().getInstance(Q.class));
		}
		if (tprCmd != null && tprmCmd != null && tpraCmd != null && tprmaCmd != null) {
			tprCmd.setExecutor(getInjector().getInstance(spigot_command.TeleportRequest.class));
			tprmCmd.setExecutor(getInjector().getInstance(spigot_command.TeleportRequest.class));
			tpraCmd.setExecutor(getInjector().getInstance(spigot_command.TeleportAccept.class));
			tprmaCmd.setExecutor(getInjector().getInstance(spigot_command.TeleportAccept.class));
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
		//getInjector().getInstance(FMCItemFrame.class).loadWorldsItemFrames();
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

	private void ifHubThenUpdate(Connection conn) throws SQLException, ClassNotFoundException {
		String thisServerName = getInjector().getInstance(ServerHomeDir.class).getServerName();
        String query = "SELECT hub FROM status WHERE name=? LIMIT 1;";
        PreparedStatement ps = conn.prepareStatement(query);
        ps.setString(1, thisServerName);
		ResultSet rs = ps.executeQuery();
        if (rs.next()) {
			if (rs.getBoolean("hub")) {
				EventListener.isHub.set(true);
			}
        }
    }
}
