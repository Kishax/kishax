package keyp.forev.fmc.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitRunnable;
import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;

import common.Database;
import common.SocketSwitch;

public class AutoShutdown {

	private final keyp.forev.fmc.spigot.Main plugin;
	private final Logger logger;
	private final Database db;
	private final Provider<SocketSwitch> sswProvider;
	private final String thisServerName;
    private BukkitRunnable task = null;
    
    @Inject
	public AutoShutdown (common.Main plugin, Logger logger, Database db, Provider<SocketSwitch> sswProvider, ServerHomeDir shd) {
		this.plugin = plugin;
		this.logger = logger;
		this.db = db;
		this.sswProvider = sswProvider;
		this.thisServerName = shd.getServerName();
	}
	
	public void startCheckForPlayers() {
		if (!plugin.getConfig().getBoolean("AutoStop.Mode", false)) {	
			logger.info("auto-stop is disabled.");
			return;
		}
		
		plugin.getServer().getConsoleSender().sendMessage(ChatColor.GREEN+"Auto-Stopが有効になりました。");
		
		long NO_PLAYER_THRESHOLD = plugin.getConfig().getInt("AutoStop.Interval",3) * 60 * 20;
		
		task = new BukkitRunnable() {
	        @Override
	        public void run() {
				SocketSwitch ssw = sswProvider.get();
	            if (plugin.getServer().getOnlinePlayers().isEmpty()) {
					try (Connection conn = db.getConnection()) {
						ssw.sendVelocityServer(conn, "プレイヤー不在のため、"+thisServerName+"サーバーを停止させます。");
					} catch (SQLException | ClassNotFoundException e) {
						logger.error("An error occurred while updating the database: " + e.getMessage(), e);
						for (StackTraceElement element : e.getStackTrace()) {
							logger.error(element.toString());
						}
					}
	                plugin.getServer().broadcastMessage(ChatColor.RED+"プレイヤー不在のため、"+thisServerName+"サーバーを5秒後に停止します。");
	                countdownAndShutdown(5);
	            }
	        }
	    };
	    task.runTaskTimer(plugin, NO_PLAYER_THRESHOLD, NO_PLAYER_THRESHOLD);
	}

    public void countdownAndShutdown(int seconds) {
        new BukkitRunnable() {
            int countdown = seconds;

            @Override
            public void run() {
                if (countdown <= 0) {
                    plugin.getServer().broadcastMessage(ChatColor.RED + "サーバーを停止します。");
					try (Connection conn = db.getConnection()) {
						SocketSwitch ssw = sswProvider.get();
						ssw.sendVelocityServer(conn, thisServerName + "サーバーが停止しました。");
					} catch (SQLException | ClassNotFoundException e) {
						logger.error("An error occurred while updating the database: " + e.getMessage(), e);
						for (StackTraceElement element : e.getStackTrace()) {
							logger.error(element.toString());
						}
					}
					
                    plugin.getServer().shutdown();
                    cancel();
                    return;
                }

                plugin.getServer().broadcastMessage(String.valueOf(countdown));
                countdown--;
            }
        }.runTaskTimer(plugin, 0, 20);
    }
    
    public void stopCheckForPlayers() {
    	if (Objects.nonNull(task) && !task.isCancelled()) {
            task.cancel();
        }
    }
}
