package keyp.forev.fmc.spigot.server.events;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.slf4j.Logger;

import com.google.inject.Inject;

import keyp.forev.fmc.common.server.Luckperms;
import keyp.forev.fmc.common.server.ServerStatusCache;
import keyp.forev.fmc.spigot.server.FMCItemFrame;
import keyp.forev.fmc.spigot.server.InvSaver;
import keyp.forev.fmc.spigot.server.InventoryCheck;
import keyp.forev.fmc.spigot.server.cmd.sub.Confirm;
import keyp.forev.fmc.spigot.server.menu.Menu;
import keyp.forev.fmc.spigot.server.menu.Type;
import keyp.forev.fmc.spigot.settings.FMCCoords;

import org.bukkit.plugin.java.JavaPlugin;

import keyp.forev.fmc.spigot.util.RunnableTaskUtil;
import keyp.forev.fmc.spigot.util.config.PortalsConfig;
import keyp.forev.fmc.spigot.util.interfaces.MessageRunnable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatColor;

public final class EventListener implements Listener {
    public static Map<Player, Map<RunnableTaskUtil.Key, MessageRunnable>> playerInputerMap = new HashMap<>();
    public static Map<Player, Map<RunnableTaskUtil.Key, BukkitTask>> playerTaskMap = new HashMap<>();
    public static final Map<Player, Location> playerBeforeLocationMap = new HashMap<>();
    public static final AtomicBoolean isHub = new AtomicBoolean(false);
    private final JavaPlugin plugin;
    private final Logger logger;
	private final PortalsConfig psConfig;
    private final Menu menu;
    private final ServerStatusCache ssc;
    private final Luckperms lp;
    private final InventoryCheck inv;
    private final FMCItemFrame fif;
    private final Set<Player> playersInPortal = new HashSet<>();

    @Inject
	public EventListener(JavaPlugin plugin, Logger logger, PortalsConfig psConfig, Menu menu, ServerStatusCache ssc, Luckperms lp, InventoryCheck inv, FMCItemFrame fif) {
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
    public void onBowShoot(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player && event.getProjectile() instanceof Arrow) {
            Player player = (Player) event.getEntity();
            Arrow arrowTile = (Arrow) event.getProjectile();
            ItemStack arrow = arrowTile.getItemStack();
            if (arrow != null) {
                ItemMeta arrowMeta = arrow.getItemMeta();
                if (arrowMeta != null) {
                    if (arrowMeta.getPersistentDataContainer().has(new NamespacedKey(plugin, Type.TELEPORT_REQUEST.getPersistantKey()), PersistentDataType.STRING)) {
                        event.setCancelled(true);
                        Component message = Component.text("ショートカットメニュー属性の矢を放つことはできません！")
                            .color(NamedTextColor.RED)
                            .decorate(TextDecoration.BOLD);
                        
                        player.sendMessage(message);
                        return;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        if (event.getView().getPlayer() instanceof Player player) {
            ItemStack[] items = event.getInventory().getMatrix();
            for (ItemStack item : items) {
                if (item != null) {
                    Material material = item.getType();
                    Set<Material> materials = Type.getMaterials();
                    if (materials.contains(material)) {
                        ItemMeta meta = item.getItemMeta();
                        if (meta != null) {
                            menu.getShortCutMap().forEach((key, value) -> {
                                if (meta.getPersistentDataContainer().has(new NamespacedKey(plugin, key.getPersistantKey()), PersistentDataType.STRING)) {
                                    player.closeInventory();
                                    Component message = Component.text("ショートカットメニュー属性のアイテムを作業台にセットすることはできません！")
                                        .color(NamedTextColor.RED)
                                        .decorate(TextDecoration.BOLD);
                                    
                                    player.sendMessage(message);
                                    return;
                                }
                            });
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item != null) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                if (action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR) {
                    Material material = item.getType();
                    Set<Material> materials = Type.getMaterials();
                    if (materials.contains(material)) {
                        menu.getShortCutMap().forEach((key, value) -> {
                            if (meta.getPersistentDataContainer().has(new NamespacedKey(plugin, key.getPersistantKey()), PersistentDataType.STRING)) {
                                event.setCancelled(true);
                                player.closeInventory();
                                value.run(player);
                                return;
                            }
                        });
                    }
                }
            }
        }
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) throws SQLException {
        if (event.getWhoClicked() instanceof Player player) {
            ClickType clickType = event.getClick();
            int slot = event.getRawSlot();
            String title = event.getView().getOriginalTitle();

            ItemStack item = event.getCurrentItem();
            if (item != null) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    if (clickType.isRightClick() || clickType == ClickType.CREATIVE) {
                        Material material = item.getType();
                        Set<Material> materials = Type.getMaterials();
                        if (materials.contains(material)) {
                            menu.getShortCutMap().forEach((key, value) -> {
                                if (meta.getPersistentDataContainer().has(new NamespacedKey(plugin, key.getPersistantKey()), PersistentDataType.STRING)) {
                                    event.setCancelled(true);
                                    player.closeInventory();
                                    value.run(player);
                                    return;
                                }
                            });
                        }
                    }
                }
            }

            Optional<Type> type = Type.search(title);
            if (type.isPresent()) {
                Type menuType = type.get();
                if (!menuType.equals(Type.CHANGE_MATERIAL)) {
                    event.setCancelled(true);
                }
                menu.runMenuEventAction(player, menuType, slot, event);
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
                    menu.runMenuEventAction(player, Type.SERVER, slot, event);
                }
            } else if (title.endsWith(" servers")) {
                event.setCancelled(true);
                menu.runMenuEventAction(player, Type.SERVER_EACH_TYPE, slot, event);
            }
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player) {
            String title = event.getView().getOriginalTitle();

            Optional<Type> type = Type.search(title);
            if (type.isPresent()) {
                Type menuType = type.get();
                if (menuType == Type.CHANGE_MATERIAL) {
                    InvSaver.save(player);
                    logger.info("Inventory snapshot saved for " + player.getName());
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            String title = event.getView().getOriginalTitle();
            Optional<Type> type = Type.search(title);
            if (type.isPresent()) {
                Type menuType = type.get();
                if (menuType == Type.CHANGE_MATERIAL) {
                    AtomicBoolean isDone = Menu.menuEventFlags.get(player).get(Type.CHANGE_MATERIAL);
                    if (isDone.compareAndSet(false, true)) {
                        InvSaver.getDiffItems(player).forEach(item -> {
                            player.getInventory().addItem(item);
                            TextComponent message = Component.text()
                                .append(Component.text("アイテムセット中にインベントリから離れました。").color(NamedTextColor.RED).decorate(TextDecoration.BOLD))
                                .appendNewline()
                                .append(Component.text("アイテム名: " + item.getType()).color(NamedTextColor.GRAY).decorate(TextDecoration.ITALIC))
                                .appendNewline()
                                .append(Component.text("かしこみかしこみ、謹んでお返し申す。").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
                                .appendNewline()
                                .append(Component.text("※アイテムを返却しました。").color(NamedTextColor.GRAY).decorate(TextDecoration.ITALIC))
                                .build();
                            
                            player.sendMessage(message);
                        });
                    }
                    
                    InvSaver.playerInventorySnapshot.remove(player);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Confirm.confirmMap.remove(player);
        playerInputerMap.remove(player);
        playerTaskMap.remove(player);
        EventListener.playerBeforeLocationMap.remove(player);
    }
    
    @Deprecated
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

    @Deprecated
    @EventHandler
    public void onChat(org.bukkit.event.player.AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        if (EventListener.playerInputerMap.containsKey(player)) {
            event.setCancelled(true);
            Map<RunnableTaskUtil.Key, MessageRunnable> map = EventListener.playerInputerMap.get(player);
            map.entrySet().forEach(action -> {
                RunnableTaskUtil.Key key = action.getKey();

                List<RunnableTaskUtil.Key> keys = Arrays.stream(RunnableTaskUtil.Key.values())
                    .collect(Collectors.toList());
                
                if (keys.contains(key)) {
                    MessageRunnable runnable = action.getValue();
                    runnable.run(message);
                }
            });
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
