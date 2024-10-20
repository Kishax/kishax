package spigot;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import static org.bukkit.Bukkit.getServer;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;

import com.google.inject.Inject;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import spigot_command.PortalsMenu;

public final class EventListener implements Listener {
    private final common.Main plugin;
	private final PortalsConfig psConfig;
    private final PortalsMenu pm;
    private final ServerStatusCache serverStatusCache;
    private final Set<Player> playersInPortal = new HashSet<>(); // プレイヤーの状態を管理するためのセット
    private final Set<Player> playersOpeningNewInventory = new HashSet<>();

    @Inject
	public EventListener(common.Main plugin, PortalsConfig psConfig, PortalsMenu pm, ServerStatusCache serverStatusCache) {
		this.plugin = plugin;
		this.psConfig = psConfig;
        this.pm = pm;
        this.serverStatusCache = serverStatusCache;
		// new Location(Bukkit.getWorld("world"), 100, 64, 100);
	}

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        String title = event.getView().getTitle();

        // インベントリのタイトルに基づいて処理を行う
        if (title.endsWith(" servers")) {
            if (playersOpeningNewInventory.contains(player)) {
                // プレイヤーが新しいインベントリを開いている場合はリセットしない
                playersOpeningNewInventory.remove(player);
            } else {
                // インベントリを閉じたときに、プレイヤーのページをリセット
                String serverType = title.split(" ")[0];
                pm.resetPage(player, serverType);
            }
        }
    }
    
    //player.performCommand("");
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) throws SQLException {
        if (event.getWhoClicked() instanceof Player player) {
            playersOpeningNewInventory.add(player);
            String playerName = player.getName();
            String title = event.getView().getTitle();
            // プレイヤーフェイスブロックのインベントリを作るときに使う
            /*ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && clickedItem.hasItemMeta()) {
                ItemMeta itemMeta = clickedItem.getItemMeta();
                String displayName;
                if (itemMeta != null && itemMeta.hasDisplayName()) {
                    displayName = itemMeta.getDisplayName();
                } else {
                    displayName = null;
                }
            }*/

            if (title.endsWith(" server")) {
                event.setCancelled(true);
                String serverName = title.split(" ")[0];
                int slot = event.getRawSlot();
                switch (slot) {
                    case 0 -> {
                        pm.OpenServerTypeInventory(player);
                    }
                }
            } else if (title.endsWith(" servers")) {
                event.setCancelled(true);
                String serverType = title.split(" ")[0];
                int slot = event.getRawSlot();

                switch (slot) {
                    case 0 -> {
                        pm.resetPage(player, serverType);
                        pm.OpenServerTypeInventory(player);
                    }
                    case 11, 13, 15, 29, 31, 33 -> {
                        Map<String, Map<String, Map<String, String>>> serverStatusMap = serverStatusCache.getStatusMap();
                        Map<String, Map<String, String>> serverStatusList = serverStatusMap.get(serverType);
                        //plugin.getLogger().log(Level.INFO, "slot: {0}", slot);
                        if (serverStatusList != null) {
                            //plugin.getLogger().log(Level.INFO, "serverStatusList: {0}", serverStatusList);
                            int page = pm.getPage(player, serverType);
                            int slotIndex = Arrays.asList(Arrays.stream(PortalsMenu.SLOT_POSITIONS).boxed().toArray(Integer[]::new)).indexOf(slot);
                            int index = PortalsMenu.SLOT_POSITIONS.length * (page - 1) + slotIndex;
                            if (index < serverStatusList.size()) {
                                //plugin.getLogger().info("YES");
                                String serverName = (String) serverStatusList.keySet().toArray()[index];
                                //player.performCommand("fmc fv " + playerName + " fmcp ss " + serverName);
                                pm.openServerInventory(player, serverName);
                            }
                        }
                    }
                    case 45 -> {
                        // 戻るボタンがあれば
                        int page = pm.getPage(player, serverType);
                        if (page > 1) {
                            pm.openServerEachInventory(player, serverType, page - 1);
                        }
                    }
                    case 53 -> {
                        // 進むボタンがあれば
                        int page = pm.getPage(player, serverType);
                        int totalServers = pm.getTotalServers(serverType); // 総サーバー数を取得
                        int totalPages = (int) Math.ceil((double) totalServers / PortalsMenu.SLOT_POSITIONS.length); // 総ページ数を計算
                        if (page < totalPages) {
                            pm.openServerEachInventory(player, serverType, page + 1);
                        }
                    }
                }
            } else if (title.equals("server type")) {
                event.setCancelled(true);
                int slot = event.getRawSlot();

                switch (slot) {
                    case 11 -> {
                        int page = pm.getPage(player, "life");
                        pm.openServerEachInventory(player, "life", page);
                    }
                    case 13 -> {
                        int page = pm.getPage(player, "distributed");
                        pm.openServerEachInventory(player, "distributed", page);
                    }
                    case 15 -> {
                        int page = pm.getPage(player, "mod");
                        pm.openServerEachInventory(player, "mod", page);
                    }
                }
            }
        }
    }

	@EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        if (plugin.getConfig().getBoolean("Portals.Move", false)) {
            Player player = e.getPlayer();
            Location loc = player.getLocation();

            // plugin.getLogger().log(Level.INFO, "Player location: {0}", loc);

            FileConfiguration portalsConfig;
            List<Map<?, ?>> portals;
            if (WandListener.isMakePortal) {
                WandListener.isMakePortal = false;
                portalsConfig = psConfig.getPortalsConfig();
                portals = (List<Map<?, ?>>) portalsConfig.getList("portals");
            } else {
                portalsConfig = psConfig.getPortalsConfig();
                portals = (List<Map<?, ?>>) portalsConfig.getList("portals");
            }
            
            boolean isInAnyPortal = false;
            
            if (portals != null) {
                for (Map<?, ?> portal : portals) {
                    String name = (String) portal.get("name");
                    // portalの名前によって、処理を分ける
                    // (例) /fmc portal menu live, /fmc portal menu distributed

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
                            isInAnyPortal = true;
                            if (!playersInPortal.contains(player)) {
                                playersInPortal.add(player);
                                plugin.getLogger().log(Level.INFO, "Player {0} entered the {1}!", new Object[]{player.getName(), name});
                                BaseComponent[] component = new ComponentBuilder()
                                    .append(ChatColor.WHITE + "ゲート: ")
                                    .append(ChatColor.AQUA + name)
                                        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("クリックしてコピー").create()))
                                        .event(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.COPY_TO_CLIPBOARD, name))
                                    .append(ChatColor.WHITE + " に入りました！")
                                    .create();
                                player.spigot().sendMessage(component);
                                switch (name) {
                                    case "life","distributed","mod" -> {
                                        player.performCommand("fmc portal menu server " + name);
                                    }
                                }
                            }
                            
                            break; // 一つのポータルに触れたらループを抜ける
                        }
                    }
                }
            }

            // プレイヤーがどのポータルにもいない場合、セットから削除
            // もしくは、ワールドから出た場合
            if (!isInAnyPortal) {
                playersInPortal.remove(player);
            }
        }
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent e) {
        Player player = e.getPlayer();
        if (playersInPortal.contains(player)) {
            playersInPortal.remove(player);
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
