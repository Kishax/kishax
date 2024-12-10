package keyp.forev.fmc.spigot.events;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.slf4j.Logger;

import com.google.inject.Inject;

import keyp.forev.fmc.common.server.Luckperms;
import keyp.forev.fmc.common.server.ServerStatusCache;
import keyp.forev.fmc.spigot.cmd.sub.Confirm;
import keyp.forev.fmc.spigot.cmd.sub.Menu;
import keyp.forev.fmc.spigot.server.FMCItemFrame;
import keyp.forev.fmc.spigot.server.Inventory;
import keyp.forev.fmc.spigot.server.Rcon;
import keyp.forev.fmc.spigot.settings.FMCCoords;

import org.bukkit.plugin.java.JavaPlugin;

import keyp.forev.fmc.spigot.util.RunnableTaskUtil;
import keyp.forev.fmc.spigot.util.config.PortalsConfig;
import keyp.forev.fmc.spigot.util.interfaces.MessageRunnable;
import net.md_5.bungee.api.ChatColor;

public final class EventListener implements Listener {
    public static Map<Player, Map<String, MessageRunnable>> playerInputerMap = new HashMap<>();
    public static Map<Player, Map<String, BukkitTask>> playerTaskMap = new HashMap<>();
    public static final AtomicBoolean isHub = new AtomicBoolean(false);
    public static final Map<Player, Location> playerBeforeLocationMap = new HashMap<>();
    private final JavaPlugin plugin;
    private final Logger logger;
	private final PortalsConfig psConfig;
    private final Menu menu;
    private final ServerStatusCache ssc;
    private final Luckperms lp;
    private final Inventory inv;
    private final FMCItemFrame fif;
    private final Set<Player> playersInPortal = new HashSet<>(); // プレイヤーの状態を管理するためのセット

    @Inject
	public EventListener(JavaPlugin plugin, Logger logger, PortalsConfig psConfig, Menu menu, ServerStatusCache ssc, Luckperms lp, Inventory inv, FMCItemFrame fif) {
		this.plugin = plugin;
        this.logger = logger;
		this.psConfig = psConfig;
        this.menu = menu;
        this.ssc = ssc;
        this.lp = lp;
        this.inv = inv;
        this.fif = fif;
	}

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        //fif.loadWorldsItemFrames();
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Confirm.confirmMap.remove(player);
        playerInputerMap.remove(player);
        playerTaskMap.remove(player);
        playerBeforeLocationMap.remove(player);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();
        //Block clickedBlock = event.getClickedBlock();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        switch (action) {
            case RIGHT_CLICK_BLOCK, RIGHT_CLICK_AIR -> {
                switch (itemInHand.getType()) {
                    case ENCHANTED_BOOK -> {
                        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) itemInHand.getItemMeta();
                        if (meta != null && meta.getPersistentDataContainer().has(new NamespacedKey(plugin, Menu.PERSISTANT_KEY), PersistentDataType.STRING)) {
                            player.performCommand("fmc menu");
                        }
                    }
                    default -> {
                    }
                }
            }
            default -> {
            }
        }
    }
    
    @EventHandler
    public void onChat(org.bukkit.event.player.AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        if (EventListener.playerInputerMap.containsKey(player)) {
            event.setCancelled(true);
            Map<String, MessageRunnable> map = EventListener.playerInputerMap.get(player);
            map.entrySet().forEach(action -> {
                String key = action.getKey();
                List<String> taskKeys = RunnableTaskUtil.Key.getKeys();
                if (taskKeys.contains(key)) {
                    MessageRunnable runnable = action.getValue();
                    runnable.run(message);
                }
            });
        }
    }

    @SuppressWarnings("deprecation")
	@EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        fif.loadWorldsItemFrames();
        event.setJoinMessage(null);
        Player player = event.getPlayer();
        if (EventListener.isHub.get()) {
            int permLevel = lp.getPermLevel(player.getName());
            if (permLevel < 1) {
                player.teleport(FMCCoords.LOAD_POINT.getLocation());
            } else {
                player.teleport(FMCCoords.HUB_POINT.getLocation());
                if (player.getGameMode() != GameMode.CREATIVE) {
                    player.setGameMode(GameMode.CREATIVE);
                    player.sendMessage(ChatColor.GREEN + "クリエイティブモードに変更しました。");
                }
            }
        }
        inv.updatePlayerInventory(player);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) throws SQLException {
        if (event.getWhoClicked() instanceof Player player) {
            ClickType click = event.getClick();
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null) {
                switch (clickedItem.getType()) {
                    case ENCHANTED_BOOK -> {
                        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) clickedItem.getItemMeta();
                        if (meta != null && meta.getPersistentDataContainer().has(new NamespacedKey(plugin, Menu.PERSISTANT_KEY), PersistentDataType.STRING)) {
                            if (click.isRightClick() || click == ClickType.CREATIVE) {
                                player.closeInventory();
                                player.performCommand("fmc menu");
                                return;
                            }
                        }
                    }
                    default -> {
                    }
                }
            }
            String title = event.getView().getTitle();
            if (Menu.menuNames.contains(title)) {
                event.setCancelled(true);
                menu.runMenuAction(player, title, event.getRawSlot());
            } else if (title.endsWith(" server")) {
                event.setCancelled(true);
                Map<String, Map<String, Map<String, Object>>> serverStatusMap = ssc.getStatusMap();
                String serverName = title.split(" ")[0];
                boolean iskey = serverStatusMap.entrySet().stream()
                    .anyMatch(e -> e.getValue().entrySet().stream()
                    .anyMatch(e2 -> {
                        if (e2.getKey() instanceof String) {
                            String statusServerName = (String) e2.getKey();
                            return statusServerName.equals(serverName);
                        }
                        return false;
                    }));
                if (iskey) {
                    menu.runMenuAction(player, Menu.serverInventoryName, event.getRawSlot());
                }
            } else if (title.endsWith(" servers")) {
                event.setCancelled(true);
                menu.runMenuAction(player, Menu.serverTypeInventoryName, event.getRawSlot());
            }
        }
    }
    
	@EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (plugin.getConfig().getBoolean("Portals.Move", false)) {
            Player player = event.getPlayer();
            Location loc = player.getLocation();
            if (loc != null) {
                Block block = loc.getBlock();
                if (WandListener.isMakePortal) {
                    WandListener.isMakePortal = false;
                }
                List<Map<?, ?>> portals = psConfig.getListMap("portals");
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
                                    logger.info("Player {} entered the gate: {}!", new Object[]{player.getName(), name});
                                    switch (name) {
                                        case "survival","minigame","mod","others","online","dev" -> {
                                            event.setCancelled(true);
                                            player.performCommand("fmc menu server " + name);
                                        }
                                        case "waterGate" -> {
                                            if (block.getType() == Material.WATER) {
                                                // 水面に入ったときの音を流す
                                                player.playSound(player, Sound.AMBIENT_UNDERWATER_ENTER, 1.0f, 1.0f);
                                                player.performCommand("fmc check");
                                            }
                                        }
                                        case "confirm" -> {
                                            player.performCommand("fmc confirm");
                                        }
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
                if (!isInAnyPortal) {
                    playersInPortal.remove(player);
                }
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
}
