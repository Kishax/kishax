package keyp.forev.fmc.spigot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.TimeZone;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.inject.Guice;
import com.google.inject.Injector;

import keyp.forev.fmc.common.database.Database;
import keyp.forev.fmc.common.server.Luckperms;
import keyp.forev.fmc.common.server.DoServerOffline;
import keyp.forev.fmc.common.server.ServerStatusCache;
import keyp.forev.fmc.common.util.PlayerUtils;
import net.luckperms.api.LuckPermsProvider;
import keyp.forev.fmc.spigot.cmd.main.FMCCommand;
import keyp.forev.fmc.spigot.cmd.sub.Q;
import keyp.forev.fmc.spigot.cmd.sub.teleport.TeleportAccept;
import keyp.forev.fmc.spigot.cmd.sub.teleport.TeleportRequest;
import keyp.forev.fmc.spigot.events.EventListener;
import keyp.forev.fmc.spigot.events.WandListener;
import keyp.forev.fmc.spigot.module.Module;
import keyp.forev.fmc.spigot.server.AutoShutdown;
import keyp.forev.fmc.spigot.server.ImageMap;
import keyp.forev.fmc.spigot.server.Rcon;
import keyp.forev.fmc.common.server.interfaces.ServerHomeDir;

public class Main extends JavaPlugin {
	private static Injector injector = null;
	private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("FMC-Plugin");
	
	@Override
	public void onEnable() {
		logger.info("detected spigot platform.");
        injector = Guice.createInjector(new Module(this, logger));
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));
		Database db = getInjector().getInstance(Database.class);
		saveDefaultConfig();
		try (Connection conn = db.getConnection()) {
			ifHubThenUpdate(conn);
			ImageMap.imagesColumnsList = db.defineImageColumnNamesList(conn, "images");
		} catch (SQLException | ClassNotFoundException e) {
			logger.error("An error occurred while updating the database: {}", e.getMessage());
		}
		getInjector().getInstance(AutoShutdown.class).startCheckForPlayers();
    	getServer().getPluginManager().registerEvents(getInjector().getInstance(EventListener.class), this);
        getServer().getPluginManager().registerEvents(getInjector().getInstance(WandListener.class), this);
		PluginCommand fmcCmd = getCommand("fmc"),
			qCmd = getCommand("q"),
			tprCmd = getCommand("tpr"),
			tprmCmd = getCommand("tprm"),
			tprmaCmd = getCommand("tprma"),
			tpraCmd = getCommand("tpra");
		if (fmcCmd != null) {
			fmcCmd.setExecutor(getInjector().getInstance(FMCCommand.class));
		}
		if (qCmd != null) {
			qCmd.setExecutor(getInjector().getInstance(Q.class));
		}
		if (tprCmd != null && tprmCmd != null && tpraCmd != null && tprmaCmd != null) {
			tprCmd.setExecutor(getInjector().getInstance(TeleportRequest.class));
			tprmCmd.setExecutor(getInjector().getInstance(TeleportRequest.class));
			tpraCmd.setExecutor(getInjector().getInstance(TeleportAccept.class));
			tprmaCmd.setExecutor(getInjector().getInstance(TeleportAccept.class));
		}
    	if (getConfig().getBoolean("MCVC.Mode",false)) {
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
	
	@Override
    public void onDisable() {
		try {
			getInjector().getInstance(DoServerOffline.class).updateDatabase();
		} catch (Exception e) {
			logger.error( "An error occurred while updating the database: {}", e.getMessage());
		}
    	if (getConfig().getBoolean("MCVC.Mode",false)) {
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
