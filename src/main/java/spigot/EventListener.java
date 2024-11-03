package spigot;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

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
import org.bukkit.event.player.PlayerMoveEvent;

import com.google.inject.Inject;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import spigot_command.Menu;

public final class EventListener implements Listener {
    private final common.Main plugin;
	private final PortalsConfig psConfig;
    private final Menu menu;
    private final ServerStatusCache ssc;
    private final Set<Player> playersInPortal = new HashSet<>(); // プレイヤーの状態を管理するためのセット
    private final Set<Player> playersOpeningNewInventory = new HashSet<>();

    @Inject
	public EventListener(common.Main plugin, PortalsConfig psConfig, Menu menu, ServerStatusCache ssc) {
		this.plugin = plugin;
		this.psConfig = psConfig;
        this.menu = menu;
        this.ssc = ssc;
	}

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        String title = event.getView().getTitle();
        // インベントリのタイトルに基づいて処理を行う
        if (title.equals("FMC Menu")) {
            menu.resetPage(player, "menu");
        } else if (title.endsWith(" servers")) {
            if (playersOpeningNewInventory.contains(player)) {
                // プレイヤーが新しいインベントリを開いている場合はリセットしない
                playersOpeningNewInventory.remove(player);
            } else {
                // インベントリを閉じたときに、プレイヤーのページをリセット
                String serverType = title.split(" ")[0];
                if (serverType != null) {
                    menu.resetPage(player, serverType);
                }
            }
        } else if (title.endsWith(" server")) {
            if (playersOpeningNewInventory.contains(player)) {
                // プレイヤーが新しいインベントリを開いている場合はリセットしない
                playersOpeningNewInventory.remove(player);
            } else {
                String serverName = title.split(" ")[0];
                if (serverName != null) {
                    menu.resetPage(player, serverName);
                }
            }
        }
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) throws SQLException {
        if (event.getWhoClicked() instanceof Player player) {
            playersOpeningNewInventory.add(player);
            String title = event.getView().getTitle();
            if (title.equals("FMC Menu")) {
                event.setCancelled(true);
                int slot = event.getRawSlot();
                // Map<String, Map<Integer, Runnable>>であるmenu.getPlayerMenuActions(player)のキーが"menu"であれば、
                // slotに対応するアクションを実行する
                menu.getPlayerMenuActions(player).entrySet().stream()
                    .filter(entry -> entry.getKey().equals("menu"))
                    .map(Map.Entry::getValue)
                    .filter(actions -> actions.containsKey(slot))
                    .map(actions -> actions.get(slot))
                    .forEach(Runnable::run);
            } else if (title.endsWith(" server")) {
                event.setCancelled(true);
                Map<String, Map<String, Map<String, Object>>> serverStatusMap = ssc.getStatusMap();
                String serverName = title.split(" ")[0];
                boolean iskey = serverStatusMap.entrySet().stream()
                    .anyMatch(e -> e.getValue().entrySet().stream()
                    .anyMatch(e2 -> e2.getKey() instanceof String statusServerName && statusServerName.equals(serverName)));
                if (iskey) {
                    int slot = event.getRawSlot();
                    switch (slot) {
                        case 0 -> {
                            menu.resetPage(player, serverName);
                            String serverType = ssc.getServerType(serverName);
                            if (serverType != null) {
                                int page = menu.getPage(player, serverType);
                                menu.openServerEachInventory(player, serverType, page);
                            } else {
                                player.closeInventory();
                                player.sendMessage("サーバーが見つかりませんでした。");
                            }
                        }
                        case 22 -> {
                            menu.serverSwitch(player, serverName);
                        }
                        case 23 -> {
                            menu.enterServer(player, serverName);
                        }
                        case 45 -> {
                            // 戻るボタンがあれば
                            int page = menu.getPage(player, serverName);
                            if (page > 1) {
                                menu.openServerInventory(player, serverName, page - 1);
                            }
                        }
                        case 53 -> {
                            // 進むボタンがあれば
                            int page = menu.getPage(player, serverName);
                            int totalServers = menu.getTotalPlayers(serverName); // 総サーバー数を取得
                            int totalPages = (int) Math.ceil((double) totalServers / Menu.FACE_POSITIONS.length); // 総ページ数を計算
                            if (page < totalPages) {
                                menu.openServerInventory(player, serverName, page + 1);
                            }
                        }
                    }
                }
            } else if (title.endsWith(" servers")) {
                event.setCancelled(true);
                Map<String, Map<String, Map<String, Object>>> serverStatusMap = ssc.getStatusMap();
                String serverType = title.split(" ")[0];
                int slot = event.getRawSlot();
                switch (slot) {
                    case 0 -> {
                        menu.resetPage(player, serverType);
                        menu.openServerTypeInventory(player);
                    }
                    case 11, 13, 15, 29, 31, 33 -> {
                        Map<String, Map<String, Object>> serverStatusList = serverStatusMap.get(serverType);
                        int page = menu.getPage(player, serverType);
                        int slotIndex = Arrays.asList(Arrays.stream(Menu.SLOT_POSITIONS).boxed().toArray(Integer[]::new)).indexOf(slot);
                        int index = Menu.SLOT_POSITIONS.length * (page - 1) + slotIndex;
                        if (index < serverStatusList.size()) {
                            if (serverStatusList.keySet().toArray()[index] instanceof String serverName) {
                                int facepage = menu.getPage(player, serverName);
                                menu.setPage(player, serverName, page);
                                menu.openServerInventory(player, serverName, facepage);
                            }
                        }
                    }
                    case 45 -> {
                        // 戻るボタンがあれば
                        int page = menu.getPage(player, serverType);
                        if (page > 1) {
                            menu.openServerEachInventory(player, serverType, page - 1);
                        }
                    }
                    case 53 -> {
                        // 進むボタンがあれば
                        int page = menu.getPage(player, serverType);
                        int totalServers = menu.getTotalServers(serverType); // 総サーバー数を取得
                        int totalPages = (int) Math.ceil((double) totalServers / Menu.SLOT_POSITIONS.length); // 総ページ数を計算
                        if (page < totalPages) {
                            menu.openServerEachInventory(player, serverType, page + 1);
                        }
                    }
                }
            } else if (title.equals("server type")) {
                event.setCancelled(true);
                int slot = event.getRawSlot();
                switch (slot) {
                    case 0 -> {
                        menu.generalMenu(player);
                    }
                    case 11 -> {
                        int page = menu.getPage(player, "life");
                        menu.openServerEachInventory(player, "life", page);
                    }
                    case 13 -> {
                        int page = menu.getPage(player, "distributed");
                        menu.openServerEachInventory(player, "distributed", page);
                    }
                    case 15 -> {
                        int page = menu.getPage(player, "mod");
                        menu.openServerEachInventory(player, "mod", page);
                    }
                }
            }
        }
    }

	@EventHandler
    @SuppressWarnings("unchecked")
    public void onPlayerMove(PlayerMoveEvent e) {
        if (plugin.getConfig().getBoolean("Portals.Move", false)) {
            Player player = e.getPlayer();
            Location loc = player.getLocation();
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
                        if (isWithinBounds(loc, corner1, corner2)) {
                            isInAnyPortal = true;
                            if (!playersInPortal.contains(player)) {
                                playersInPortal.add(player);
                                plugin.getLogger().log(Level.INFO, "Player {0} entered the {1}!", new Object[]{player.getName(), name});
                                BaseComponent[] component = new ComponentBuilder()
                                    .append(ChatColor.WHITE + "ゲート: ")
                                    .append(ChatColor.AQUA + name)
                                        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.hover.content.Text("クリックしてコピー")))
                                        .event(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.COPY_TO_CLIPBOARD, name))
                                    .append(ChatColor.WHITE + " に入りました！")
                                    .create();
                                player.spigot().sendMessage(component);
                                switch (name) {
                                    case "life","distributed","mod" -> {
                                        player.performCommand("fmc menu server " + name);
                                    }
                                }
                            }
                            break; // 一つのポータルに触れたらループを抜ける
                        }
                    }
                }
            }
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

	// MCVCをONにすると、ベッドで寝れなくなるため、必要なメソッド
	@EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent e) {
		if (Rcon.isMCVC) {
	        if (e.getBedEnterResult() == PlayerBedEnterEvent.BedEnterResult.OK) {
	            World world = e.getPlayer().getWorld();
	            world.setTime(1000);
	            world.setStorm(false);
	            world.setThundering(false);
	            e.getPlayer().sendMessage("おはようございます！時間を朝にしました。");
	        }
		}
    }
}
