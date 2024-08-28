package spigot;

import java.util.Objects;

import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitRunnable;

import com.google.inject.Inject;

public class AutoShutdown {

	private final common.Main plugin;
	private final SocketSwitch ssw;
	private final ServerHomeDir shd;
    private BukkitRunnable task = null;
    
    @Inject
	public AutoShutdown (common.Main plugin, SocketSwitch ssw, ServerHomeDir shd) {
		this.plugin = plugin;
		this.ssw = ssw;
		this.shd = shd;
	}
	
	public void startCheckForPlayers() {
		if (!plugin.getConfig().getBoolean("AutoStop.Mode", false)) {	
			plugin.getServer().getConsoleSender().sendMessage(ChatColor.GREEN+"Auto-Stopはキャンセルされました。");
			return;
		}
		
		plugin.getServer().getConsoleSender().sendMessage(ChatColor.GREEN+"Auto-Stopが有効になりました。");
		
		long NO_PLAYER_THRESHOLD = plugin.getConfig().getInt("AutoStop.Interval",3) * 60 * 20;
		
		task = new BukkitRunnable() {
	        @Override
	        public void run() {
	            if (plugin.getServer().getOnlinePlayers().isEmpty()) {
	            	String serverName = shd.getServerName();
	            	ssw.startSocketClient("プレイヤー不在のため、"+serverName+"サーバーを停止させます。");
	            	
	                plugin.getServer().broadcastMessage(ChatColor.RED+"プレイヤー不在のため、"+serverName+"サーバーを5秒後に停止します。");
	                countdownAndShutdown(5);
	            }
	        }
	    };

	    task.runTaskTimer(plugin, NO_PLAYER_THRESHOLD, NO_PLAYER_THRESHOLD);
	}

    private void countdownAndShutdown(int seconds) {
        new BukkitRunnable() {
            int countdown = seconds;

            @Override
            public void run() {
                if (countdown <= 0) {
                    plugin.getServer().broadcastMessage("サーバーを停止します。");
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
