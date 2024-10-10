package spigot;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import static org.bukkit.Bukkit.getServer;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;

import com.google.inject.Inject;

public final class EventListener implements Listener {
    private final common.Main plugin;
	private final PortalsConfig psConfig;
    @Inject
	public EventListener(common.Main plugin, PortalsConfig psConfig) {
		this.plugin = plugin;
		this.psConfig = psConfig;
		// new Location(Bukkit.getWorld("world"), 100, 64, 100);
	}
	
	@EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        Player player = e.getPlayer();
        Location loc = player.getLocation();

		// plugin.getLogger().log(Level.INFO, "Player location: {0}", loc);

        FileConfiguration portalsConfig = psConfig.getPortalsConfig();
		List<Map<?, ?>> portals = (List<Map<?, ?>>) portalsConfig.getList("portals");
		
        if (portals != null) {
            for (Map<?, ?> portal : portals) {
                String name = (String) portal.get("name");
                List<?> corner1List = (List<?>) portal.get("corner1");
                List<?> corner2List = (List<?>) portal.get("corner2");

                if (corner1List != null && corner2List != null) {
                    Location corner1 = new Location(player.getWorld(),
                            ((Number) corner1List.get(0)).doubleValue(),
                            ((Number) corner1List.get(1)).doubleValue(),
                            ((Number) corner1List.get(2)).doubleValue());
                    Location corner2 = new Location(player.getWorld(),
                            ((Number) corner2List.get(0)).doubleValue(),
                            ((Number) corner2List.get(1)).doubleValue(),
                            ((Number) corner2List.get(2)).doubleValue());
					
					// plugin.getLogger().log(Level.INFO, "Portal {0} \n - corner1: {1}\n - corner2: {2}", new Object[]{name, corner1, corner2});

                    if (isWithinBounds(loc, corner1, corner2)) {
						plugin.getLogger().log(Level.INFO, "Player {0} entered the {1}!", new Object[]{player.getName(), name});
                        player.sendMessage(ChatColor.AQUA + "You have entered the " + name + "!");
						// 詳しく && 何回もポータルに入らないようにするため、インベントリを開く処理を追加
						// インベントリを開いたら、ポータルから出るまでインベントリを開かない



						// ここでインベントリを開く処理を追加
                        player.openInventory(plugin.getServer().createInventory(null, 27, "Custom Inventory"));
						
                        break; // 一つのポータルに触れたらループを抜ける
                    }
                }
            }
        }
    }

    private boolean isWithinBounds(Location loc, Location corner1, Location corner2) {
        double x1 = Math.min(corner1.getX(), corner2.getX());
        double x2 = Math.max(corner1.getX(), corner2.getX());
        double y1 = Math.min(corner1.getY(), corner2.getY());
        double y2 = Math.max(corner1.getY(), corner2.getY());
        double z1 = Math.min(corner1.getZ(), corner2.getZ());
        double z2 = Math.max(corner1.getZ(), corner2.getZ());

        return loc.getX() >= x1 && loc.getX() < x2+1 &&
               loc.getY() >= y1 && loc.getY() < y2+1 &&
               loc.getZ() >= z1 && loc.getZ() < z2+1;
    }

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {
		e.setJoinMessage(null);
	}

	// MCVCをONにすると、ベッドで寝れなくなるため、必要なメソッド
	@EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent e) {
		if (Rcon.isMCVC) {
			// プレイヤーがベッドに入ったかどうかを確認
	        if (e.getBedEnterResult() == PlayerBedEnterEvent.BedEnterResult.OK) {
	            World world = e.getPlayer().getWorld();
	            // 時間を朝に設定 (1000 ticks = 朝6時)
	            world.setTime(1000);
	            // 天気を晴れに設定
	            world.setStorm(false);
	            world.setThundering(false);
	            // メッセージをプレイヤーに送信
	            e.getPlayer().sendMessage("おはようございます！時間を朝にしました。");
	        }
		}
    }

	public void onPlayerPortal(PlayerPortalEvent e) {
        Player player = e.getPlayer();
        if (e.getCause() == PlayerPortalEvent.TeleportCause.NETHER_PORTAL) {
            player.sendMessage(ChatColor.AQUA + "You trapped in the portal!");
            // ここでインベントリを開く処理を追加
            player.openInventory(getServer().createInventory(null, 27, "Custom Inventory"));
        }
    }
}
