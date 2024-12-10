package keyp.forev.fmc.spigot.cmd.sub;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import keyp.forev.fmc.common.database.Database;
import keyp.forev.fmc.common.server.DefaultLuckperms;
import keyp.forev.fmc.common.server.ServerStatusCache;
import keyp.forev.fmc.common.settings.FMCSettings;
import keyp.forev.fmc.common.util.JavaUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatColor;
import keyp.forev.fmc.spigot.Main;
import keyp.forev.fmc.spigot.cmd.sub.teleport.TeleportRequest;
import keyp.forev.fmc.spigot.events.EventListener;
import keyp.forev.fmc.spigot.server.ImageMap;
import keyp.forev.fmc.spigot.server.SpigotServerHomeDir;

import org.bukkit.plugin.java.JavaPlugin;

@Singleton
public class Menu {
    public static final Map<Player, Map<String, Map<Integer, Runnable>>> menuActions = new ConcurrentHashMap<>();
    public static final String PUBLIC = "public", PRIVATE = "private";
    public static final String PERSISTANT_KEY = "fmcmenu";
    public static final String serverInventoryName = "server",
        menuInventoryName = "fmc menu",
        onlineServerInventoryName = "online servers",
        serverTypeInventoryName = "server type",
        imageInventoryName = "image maps",
        settingInventoryName = "settings",
        teleportInventoryName = "teleport",
        playerTeleportInventoryName = "player teleport",
        teleportRequestInventoryName = "teleport request",
        teleportResponseInventoryName = "teleport response",
        teleportResponseHeadInventoryName = "teleport response head",
        chooseColorInventoryName = "choose color",
        teleportPointInventoryName = "teleport point";
    public static final Set<String> menuNames = Set.of(
        Menu.serverInventoryName,
        Menu.menuInventoryName,
        Menu.onlineServerInventoryName,
        Menu.serverTypeInventoryName,
        Menu.imageInventoryName,
        Menu.settingInventoryName,
        Menu.playerTeleportInventoryName,
        Menu.teleportRequestInventoryName,
        Menu.teleportResponseInventoryName,
        Menu.teleportResponseHeadInventoryName,
        Menu.chooseColorInventoryName,
        Menu.teleportPointInventoryName,
        Menu.teleportInventoryName);
    public static final List<String> args1 = new ArrayList<>(Arrays.asList("server", "image", "get"));
    public static final List<String> args2 = new ArrayList<>(Arrays.asList("online","survival","minigame","dev","mod","distributed","others","before"));
    public static final int[] SLOT_POSITIONS = {11, 13, 15, 29, 31, 33};
    public static final int[] FACE_POSITIONS = {46, 47, 48, 49, 50, 51, 52};
    private static final List<Material> ORE_BLOCKS = Arrays.asList(
        Material.NETHERITE_BLOCK, Material.GOLD_BLOCK, Material.REDSTONE_BLOCK, 
        Material.EMERALD_BLOCK, Material.DIAMOND_BLOCK, Material.IRON_BLOCK,
        Material.COAL_BLOCK, Material.LAPIS_BLOCK, Material.QUARTZ_BLOCK,
        Material.COPPER_BLOCK
    );
    private final JavaPlugin plugin;
    private final Logger logger;
    private final Database db;
    private final ServerStatusCache ssc;
    private final DefaultLuckperms lp;
    private final ImageMap im;
    private final SpigotServerHomeDir shd;
    private final Book book;
    private final CommandForward cf;
    private int currentOreIndex = 0; // 現在のインデックスを管理するフィールド

	@Inject
	public Menu(JavaPlugin plugin, Logger logger, Database db, ServerStatusCache ssc, DefaultLuckperms lp, ImageMap im, SpigotServerHomeDir shd, Book book, CommandForward cf) {  
		this.plugin = plugin;
        this.logger = logger;
        this.db = db;
        this.ssc = ssc;
        this.lp = lp;
        this.im = im;
        this.shd = shd;
        this.book = book;
        this.cf = cf;
	}

	public void execute(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
        if (sender instanceof Player player) {
            if (args.length == 1) {
                generalMenu(player, 1);
            } else if (args.length > 1) {
                switch (args[1].toLowerCase()) {
                    case "get" -> {
                        boolean hasMenuBook = false;
                        for (ItemStack item : player.getInventory().getContents()) {
                            if (item != null) {
                                switch (item.getType()) {
                                    case ENCHANTED_BOOK -> {
                                        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
                                        if (meta != null && meta.getPersistentDataContainer().has(new NamespacedKey(plugin, Menu.PERSISTANT_KEY), PersistentDataType.STRING)) {
                                            hasMenuBook = true;
                                        }
                                    }
                                    default -> {
                                    }
                                }
                            }
                        }
                        if (!hasMenuBook) {
                            // エンチャント本を渡す
                            ItemStack menuBook = new ItemStack(Material.ENCHANTED_BOOK);
                            EnchantmentStorageMeta meta = (EnchantmentStorageMeta) menuBook.getItemMeta();
                            if (meta != null) {
                                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, Menu.PERSISTANT_KEY), PersistentDataType.STRING, "true");
                                meta.setDisplayName(ChatColor.GOLD + "FMC Menu Book");
                                meta.setLore(Arrays.asList(ChatColor.GRAY + "右クリックでメニューを開くことができます。"));
                                menuBook.setItemMeta(meta);
                            }
                            player.getInventory().addItem(menuBook);
                            player.sendMessage(ChatColor.GREEN + "メニューブックを受け取りました。");
                        } else {
                            player.sendMessage(ChatColor.RED + "既にメニューブックを持っています。");
                        }
                    }
                    case "server" -> {
                        if (plugin.getConfig().getBoolean("Menu.Server", false)) {
                            int permLevel = lp.getPermLevel(player.getName());
                            if (permLevel < 1) {
                                player.sendMessage("先にWEB認証を完了させてください。");
                                return;
                            }
                            if (args.length > 2) {
                                String serverType = args[2].toLowerCase();
                                switch (serverType) {
                                    case "online" -> openOnlineServerInventory(player, 1);
                                    case "survival", "minigame", "mod", "distributed", "others", "dev" -> openServerEachInventory(player, serverType, 1);
                                    case "before" -> openServerInventory(player, FMCSettings.NOW_ONLINE.getValue(), 1);
                                    default -> sender.sendMessage("Usage: /fmc menu server <survival|minigame|dev|mod|distributed|others>");
                                }
                            } else {
                                Main.getInjector().getInstance(Menu.class).openServerTypeInventory((Player) sender, 1);
                            }
                        } else {
                            sender.sendMessage(ChatColor.RED + "このサーバーでは、この機能は無効になっています。");
                        }
                    }
                    case "image" -> {
                        if (plugin.getConfig().getBoolean("Menu.ImageMap", false)) {
                            openImageMenu(player, 1);
                        } else {
                            sender.sendMessage(ChatColor.RED + "このサーバーでは、この機能は無効になっています。");
                        }
                    }
                }
            } else {
                sender.sendMessage("Usage: /fmc menu <server|image> <server: serverType>");
            }
        } else {
            if (sender != null) {
                sender.sendMessage(ChatColor.RED + "このコマンドはプレイヤーのみ実行可能です。");
            }
        }
	}

    public void runMenuAction(Player player, String menuType, int slot) {
        Map<String, Map<Integer, Runnable>> playerMenuActions = getPlayerMenuActions(player);
        if (playerMenuActions != null) {
            // コレクションをコピーしてから反復処理を行う
            Map<String, Map<Integer, Runnable>> copiedMenuActions = new HashMap<>(playerMenuActions);
            copiedMenuActions.entrySet().stream()
                .filter(entry -> entry.getKey().equals(menuType))
                .map(Map.Entry::getValue)
                .filter(actions -> actions.containsKey(slot))
                .map(actions -> actions.get(slot))
                .forEach(Runnable::run);
        }
    }

    public Map<String, Map<Integer, Runnable>> getPlayerMenuActions(Player player) {
        return Menu.menuActions.get(player);
    }

    public void settingMenu(Player player, int page) {
        Map<Integer, Runnable> playerMenuActions = new HashMap<>();
        Inventory inv = Bukkit.createInventory(null, 27, Menu.settingInventoryName);
        try (Connection conn = db.getConnection()) {
            Map<String, Object> memberMap = db.getMemberMap(conn, player.getUniqueId().toString());
            switch (page) {
                case 1 -> {
                    if (memberMap.get("hubinv") instanceof Boolean hubinv) {
                        playerMenuActions.put(11, () -> {
                            try (Connection connection = db.getConnection()) {
                                db.updateMemberToggle(connection, "hubinv", !hubinv, player.getUniqueId().toString());
                                player.sendMessage(ChatColor.GRAY + "サーバー起動時のアクションを" + (hubinv ? "チャット" : "オープンインベントリ") + "タイプに設定しました。");
                            } catch (SQLException | ClassNotFoundException e) {
                                player.closeInventory();
                                player.sendMessage(ChatColor.RED + "データベースとの通信に失敗しました。");
                                logger.error("An error occurred while communicating with the database: {}", e.getMessage());
                                for (StackTraceElement ste : e.getStackTrace()) {
                                    logger.error(ste.toString());
                                }
                            }
                            settingMenu(player, 1);
                        });
                        if (hubinv) {
                            ItemStack hubinvItem = new ItemStack(Material.OAK_SIGN);
                            ItemMeta hubinvMeta = hubinvItem.getItemMeta();
                            if (hubinvMeta != null) {
                                hubinvMeta.setDisplayName(ChatColor.GOLD + "サーバー起動時のアクション");
                                hubinvMeta.setLore(Arrays.asList(ChatColor.GRAY + "チャットタイプに切り替えます。"));
                                hubinvItem.setItemMeta(hubinvMeta);
                            }
                            inv.setItem(11, hubinvItem);
                        } else {
                            ItemStack hubinvItem = new ItemStack(Material.CHEST);
                            ItemMeta hubinvMeta = hubinvItem.getItemMeta();
                            if (hubinvMeta != null) {
                                hubinvMeta.setDisplayName(ChatColor.GOLD + "サーバー起動時のアクション");
                                hubinvMeta.setLore(Arrays.asList(ChatColor.GRAY + "オープンインベントリタイプに切り替えます。"));
                                hubinvItem.setItemMeta(hubinvMeta);
                            }
                            inv.setItem(11, hubinvItem);
                        }
                    }
                    if (memberMap.get("tptype") instanceof Boolean tptype) {
                        playerMenuActions.put(13, () -> {
                            try (Connection connection = db.getConnection()) {
                                db.updateMemberToggle(connection, "tptype", !tptype, player.getUniqueId().toString());
                                player.sendMessage(ChatColor.GRAY + "サーバー起動時のアクションを" + (tptype ? "チャット" : "オープンインベントリ") + "タイプに設定しました。");
                            } catch (SQLException | ClassNotFoundException e) {
                                player.closeInventory();
                                player.sendMessage(ChatColor.RED + "データベースとの通信に失敗しました。");
                                logger.error("An error occurred while communicating with the database: {}", e.getMessage());
                                for (StackTraceElement ste : e.getStackTrace()) {
                                    logger.error(ste.toString());
                                }
                            }
                            settingMenu(player, 1);
                        });
                        if (tptype) {
                            ItemStack hubinvItem = new ItemStack(Material.OAK_SIGN);
                            ItemMeta hubinvMeta = hubinvItem.getItemMeta();
                            if (hubinvMeta != null) {
                                hubinvMeta.setDisplayName(ChatColor.GOLD + "テレポート時のアクション");
                                hubinvMeta.setLore(Arrays.asList(ChatColor.GRAY + "チャットタイプに切り替えます。"));
                                hubinvItem.setItemMeta(hubinvMeta);
                            }
                            inv.setItem(13, hubinvItem);
                        } else {
                            ItemStack hubinvItem = new ItemStack(Material.CHEST);
                            ItemMeta hubinvMeta = hubinvItem.getItemMeta();
                            if (hubinvMeta != null) {
                                hubinvMeta.setDisplayName(ChatColor.GOLD + "テレポート時のアクション");
                                hubinvMeta.setLore(Arrays.asList(ChatColor.GRAY + "オープンインベントリタイプに切り替えます。"));
                                hubinvItem.setItemMeta(hubinvMeta);
                            }
                            inv.setItem(13, hubinvItem);
                        }
                    }
                }
            }
            Menu.menuActions.computeIfAbsent(player, _p -> new HashMap<>()).put(Menu.settingInventoryName, playerMenuActions);
        } catch (SQLException | ClassNotFoundException e) {
            player.closeInventory();
            player.sendMessage(ChatColor.RED + "データベースとの通信に失敗しました。");
            logger.error("An error occurred while communicating with the database: {}", e.getMessage());
            for (StackTraceElement ste : e.getStackTrace()) {
                logger.error(ste.toString());
            }
        }
        ItemStack backItem = new ItemStack(Material.STICK);
        ItemMeta backMeta = backItem.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.GOLD + "戻る");
            backItem.setItemMeta(backMeta);
        }
        inv.setItem(0, backItem);
        playerMenuActions.put(0, () -> generalMenu(player, 2));
        Menu.menuActions.computeIfAbsent(player, _p -> new HashMap<>()).put(Menu.menuInventoryName, playerMenuActions);
        player.openInventory(inv);
    }

    public void generalMenu(Player player, int page) {
        Map<Integer, Runnable> playerMenuActions = new HashMap<>();
        Inventory inv = Bukkit.createInventory(null, 27, Menu.menuInventoryName);
        switch (page) {
            case 1 -> {
                playerMenuActions.put(11, () -> teleportMenu(player, 1));
                playerMenuActions.put(13, () -> openServerTypeInventory(player, 1));
                playerMenuActions.put(15, () -> openImageMenu(player, 1));
                playerMenuActions.put(26, () -> generalMenu(player, page + 1));
                ItemStack teleportItem = new ItemStack(Material.ENDER_PEARL);
                ItemMeta teleportMeta = teleportItem.getItemMeta();
                if (teleportMeta != null) {
                    teleportMeta.setDisplayName(ChatColor.GREEN + "テレポートメニュー");
                    teleportMeta.setLore(Arrays.asList(ChatColor.GRAY + "テレポート関連の機能を使えるよ。"));
                    teleportItem.setItemMeta(teleportMeta);
                }
                inv.setItem(11, teleportItem);
                ItemStack serverItem = new ItemStack(Material.COMPASS);
                ItemMeta serverMeta = serverItem.getItemMeta();
                if (serverMeta != null) {
                    serverMeta.setDisplayName(ChatColor.GREEN + "サーバーメニュー");
                    serverMeta.setLore(Arrays.asList(ChatColor.GRAY + "各サーバーの情報を確認できるよ。"));
                    serverItem.setItemMeta(serverMeta);
                }
                inv.setItem(13, serverItem);
                ItemStack imageItem = new ItemStack(Material.MAP);
                ItemMeta imageMeta = imageItem.getItemMeta();
                if (imageMeta != null) {
                    imageMeta.setDisplayName(ChatColor.GREEN + "画像マップメニュー");
                    imageMeta.setLore(Arrays.asList(ChatColor.GRAY + "今までアップされた画像マップを確認できるよ。"));
                    imageItem.setItemMeta(imageMeta);
                }
                inv.setItem(15, imageItem);
                ItemStack nextPageItem = new ItemStack(Material.ARROW);
                ItemMeta nextPageMeta = nextPageItem.getItemMeta();
                if (nextPageMeta != null) {
                    nextPageMeta.setDisplayName(ChatColor.GOLD + "次のページ");
                    nextPageItem.setItemMeta(nextPageMeta);
                }
                inv.setItem(26, nextPageItem);
            }
            case 2 -> {
                playerMenuActions.put(11, () -> settingMenu(player, 1));
                playerMenuActions.put(13, () -> book.giveRuleBook(player));
                playerMenuActions.put(15, () -> {
                    player.closeInventory();
                    player.sendMessage(ChatColor.GREEN + "ここにあればいいなと思う機能があればDiscordで教えてね");
                });
                playerMenuActions.put(18, () -> generalMenu(player, page - 1));
                ItemStack settingItem = new ItemStack(Material.ENCHANTING_TABLE);
                ItemMeta settingMeta = settingItem.getItemMeta();
                if (settingMeta != null) {
                    settingMeta.setDisplayName(ChatColor.GREEN + "設定メニュー");
                    settingMeta.setLore(Arrays.asList(ChatColor.GRAY + "プレイヤーの設定を変更できるよ。"));
                    settingItem.setItemMeta(settingMeta);
                }
                inv.setItem(11, settingItem);
                ItemStack ruleBookItem = new ItemStack(Material.WRITTEN_BOOK);
                ItemMeta ruleBookMeta = ruleBookItem.getItemMeta();
                if (ruleBookMeta != null) {
                    ruleBookMeta.setDisplayName(ChatColor.GREEN + "ルールブック");
                    ruleBookMeta.setLore(Arrays.asList(ChatColor.GRAY + "サーバーのルールを確認できるよ。"));
                    ruleBookItem.setItemMeta(ruleBookMeta);
                }
                inv.setItem(13, ruleBookItem);
                ItemStack anyItem = new ItemStack(Material.COOKIE);
                ItemMeta anyMeta = anyItem.getItemMeta();
                if (anyMeta != null) {
                    anyMeta.setDisplayName(ChatColor.GREEN + "未定");
                    anyItem.setItemMeta(anyMeta);
                }
                inv.setItem(15, anyItem);
                ItemStack prevPageItem = new ItemStack(Material.ARROW);
                ItemMeta prevPageMeta = prevPageItem.getItemMeta();
                if (prevPageMeta != null) {
                    prevPageMeta.setDisplayName(ChatColor.GOLD + "前のページ");
                    prevPageItem.setItemMeta(prevPageMeta);
                }
                inv.setItem(18, prevPageItem);
            }
        }
        Menu.menuActions.computeIfAbsent(player, _p -> new HashMap<>()).put(Menu.menuInventoryName, playerMenuActions);
        player.openInventory(inv);
    }

    public void teleportPointTypeMenu(Player player, int page) {
        Map<Integer, Runnable> playerMenuActions = new HashMap<>();
        Inventory inv = Bukkit.createInventory(null, 27, Menu.teleportInventoryName);
        playerMenuActions.put(11, () -> openTeleportPointMenu(player, 1, Menu.PRIVATE));
        //playerMenuActions.put(13, () -> player.performCommand());
        playerMenuActions.put(15, () -> openTeleportPointMenu(player, 1, Menu.PUBLIC));
        playerMenuActions.put(26, () -> teleportMenu(player, 1));
        ItemStack privatePointItem = new ItemStack(Material.OBSERVER);
        ItemMeta privatePointMeta = privatePointItem.getItemMeta();
        if (privatePointMeta != null) {
            privatePointMeta.setDisplayName(ChatColor.GREEN + "プライベートポイント");
            privatePointItem.setItemMeta(privatePointMeta);
        }
        inv.setItem(11, privatePointItem);
        ItemStack newPointItem = new ItemStack(Material.REDSTONE_TORCH);
        ItemMeta newPointMeta = newPointItem.getItemMeta();
        if (newPointMeta != null) {
            newPointMeta.setDisplayName(ChatColor.GREEN + "ポイントセット");
            newPointItem.setLore(new ArrayList<>(
                Arrays.asList(ChatColor.GRAY + "新しいポイントを設定できるよ。"))
                );
            newPointItem.setItemMeta(newPointMeta);
        }
        inv.setItem(13, privatePointItem);
        ItemStack publicPointItem = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta publicPointMeta = publicPointItem.getItemMeta();
        if (publicPointMeta != null) {
            publicPointMeta.setDisplayName(ChatColor.GREEN + "パブリックポイント");
            publicPointItem.setItemMeta(publicPointMeta);
        }
        inv.setItem(15, publicPointItem);
        Menu.menuActions.computeIfAbsent(player, _p -> new HashMap<>()).put(Menu.teleportInventoryName, playerMenuActions);
        player.openInventory(inv);
    }

    public void teleportMenu(Player player, int page) {
        Map<Integer, Runnable> playerMenuActions = new HashMap<>();
        Inventory inv = Bukkit.createInventory(null, 27, Menu.teleportInventoryName);
        playerMenuActions.put(12, () -> teleportPointTypeMenu(player, 1));
        playerMenuActions.put(14, () -> playerTeleportMenu(player, 1));
        playerMenuActions.put(26, () -> generalMenu(player, 1));
        ItemStack teleportPointItem = new ItemStack(Material.LIGHTNING_ROD);
        ItemMeta teleportPointMeta = teleportPointItem.getItemMeta();
        if (teleportPointMeta != null) {
            teleportPointMeta.setDisplayName(ChatColor.GREEN + "ポイントテレポート");
            teleportPointItem.setItemMeta(teleportPointMeta);
        }
        inv.setItem(12, teleportPointItem);
        ItemStack playerTeleportItem = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta playerTeleportMeta = (SkullMeta) playerTeleportItem.getItemMeta();
        if (playerTeleportMeta != null) {
            playerTeleportMeta.setDisplayName(ChatColor.GREEN + "プレイヤーテレポート");
            playerTeleportItem.setItemMeta(playerTeleportMeta);
        }
        inv.setItem(14, playerTeleportItem);
        ItemStack nextPageItem = new ItemStack(Material.ARROW);
        ItemMeta nextPageMeta = nextPageItem.getItemMeta();
        if (nextPageMeta != null) {
            nextPageMeta.setDisplayName(ChatColor.GOLD + "次のページ");
            nextPageItem.setItemMeta(nextPageMeta);
        }
        inv.setItem(26, nextPageItem);
        Menu.menuActions.computeIfAbsent(player, _p -> new HashMap<>()).put(Menu.teleportInventoryName, playerMenuActions);
        player.openInventory(inv);
    }

    public void teleportResponseHeadMenu(Player player, int page) {
        int inventorySize = 27;
        int usingSlots = 3; // 戻るボタンやページネーションボタンに使用するスロット数
        int itemsPerPage = inventorySize - usingSlots; // 各ページに表示するアイテムの数
        Map<Integer, Runnable> playerMenuActions = new HashMap<>();
        Inventory inv = Bukkit.createInventory(null, inventorySize, teleportResponseHeadInventoryName);
        // valueにplayerが含まれていたら、keyplayerをコレクト
        Set<Player> requestedPlayers = TeleportRequest.teleportMap.entrySet().stream()
            .filter(entry -> entry.getValue().stream().anyMatch(map -> map.containsKey(player)))
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
        if (!requestedPlayers.isEmpty()) {
            int totalItems = requestedPlayers.size();
            int totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);
            int startIndex = (page - 1) * itemsPerPage;
            int endIndex = Math.min(startIndex + itemsPerPage, totalItems);
            if (page < totalPages) {
                ItemStack nextPageItem = new ItemStack(Material.ARROW);
                ItemMeta nextPageMeta = nextPageItem.getItemMeta();
                if (nextPageMeta != null) {
                    nextPageMeta.setDisplayName(ChatColor.GOLD + "次のページ");
                    nextPageItem.setItemMeta(nextPageMeta);
                }
                inv.setItem(26, nextPageItem);
                playerMenuActions.put(26, () -> teleportResponseHeadMenu(player, page + 1));
            }
            if (page > 1) {
                ItemStack prevPageItem = new ItemStack(Material.ARROW);
                ItemMeta prevPageMeta = prevPageItem.getItemMeta();
                if (prevPageMeta != null) {
                    prevPageMeta.setDisplayName(ChatColor.GOLD + "前のページ");
                    prevPageItem.setItemMeta(prevPageMeta);
                }
                inv.setItem(18, prevPageItem);
                playerMenuActions.put(18, () -> teleportResponseHeadMenu(player, page - 1));
            }
            for (int i = startIndex; i < endIndex; i++) {
                Player targetPlayer = (Player) requestedPlayers.toArray()[i];
                ItemStack playerItem = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta playerMeta = (SkullMeta) playerItem.getItemMeta();
                if (playerMeta != null) {
                    playerMeta.setOwningPlayer(targetPlayer);
                    playerMeta.setDisplayName(ChatColor.GREEN + targetPlayer.getName());
                    playerItem.setItemMeta(playerMeta);
                }
                inv.addItem(playerItem); // i - startIndex + 1
                playerMenuActions.put(inv.first(playerItem), () -> teleportResponseMenu(targetPlayer, player));
            }
        } else {
            ItemStack noRequestItem = new ItemStack(Material.BARRIER);
            ItemMeta noRequestMeta = noRequestItem.getItemMeta();
            if (noRequestMeta != null) {
                noRequestMeta.setDisplayName(ChatColor.RED + "リクエストはありません");
                noRequestItem.setItemMeta(noRequestMeta);
            }
            inv.setItem(13, noRequestItem);
        }
        ItemStack backItem = new ItemStack(Material.STICK);
        ItemMeta backMeta = backItem.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.GOLD + "戻る");
            backItem.setItemMeta(backMeta);
        }
        inv.setItem(0, backItem);
        playerMenuActions.put(0, () -> playerTeleportMenu(player, 1));
        Menu.menuActions.computeIfAbsent(player, _p -> new HashMap<>()).put(teleportResponseHeadInventoryName, playerMenuActions);
        player.openInventory(inv);
    }

    public void teleportResponseMenu(Player player, Player targetPlayer) {
        teleportResponseMenu(player, targetPlayer, false);
    }

    public void teleportMeResponseMenu(Player player, Player targetPlayer) {
        teleportResponseMenu(player, targetPlayer, true);
    }

    private void teleportResponseMenu(Player player, Player targetPlayer, boolean me) {
        Map<Integer, Runnable> playerMenuActions = new HashMap<>();
        Inventory inv = Bukkit.createInventory(null, 27, Menu.teleportResponseInventoryName);
        ItemStack backItem = new ItemStack(Material.STICK);
        ItemMeta backMeta = backItem.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.GOLD + "戻る");
            backItem.setItemMeta(backMeta);
        }
        inv.setItem(0, backItem);
        playerMenuActions.put(0, () -> playerTeleportMenu(player, 1));
        ItemStack acceptItem = new ItemStack(Material.DROPPER);
        ItemMeta acceptMeta = acceptItem.getItemMeta();
        if (acceptMeta != null) {
            acceptMeta.setDisplayName(ChatColor.GREEN + "受け入れる");
            acceptItem.setItemMeta(acceptMeta);
        }
        inv.setItem(11, acceptItem);
        playerMenuActions.put(11, () -> {
            player.closeInventory();
            if (!me) {
                player.performCommand("tpra " + targetPlayer.getName());
            } else {
                player.performCommand("tprma " + targetPlayer.getName());
            }
        });
        ItemStack denyItem = new ItemStack(Material.BARRIER);
        ItemMeta denyMeta = denyItem.getItemMeta();
        if (denyMeta != null) {
            denyMeta.setDisplayName(ChatColor.RED + "拒否する");
            denyItem.setItemMeta(denyMeta);
        }
        inv.setItem(15, denyItem);
        playerMenuActions.put(15, () -> {
            player.closeInventory();
            if (!me) {
                player.performCommand("tprd " + targetPlayer.getName());
            } else {
                player.performCommand("tprmd " + targetPlayer.getName());
            }
        });
        // ここ、targetPlayerがキーになっているのがあってるかわからない
        Menu.menuActions.computeIfAbsent(player, _p -> new HashMap<>()).put(Menu.teleportResponseInventoryName, playerMenuActions);
        player.openInventory(inv);
    }

    public void teleportRequestMenu(Player player, int page) {
        teleportRequestMenu(player, page, false);
    }

    public void teleportMeRequestMenu(Player player, int page) {
        teleportRequestMenu(player, page, true);
    }

    private void teleportRequestMenu(Player player, int page, boolean me) {
        int inventorySize = 27;
        int usingSlots = 3; // 戻るボタンやページネーションボタンに使用するスロット数
        int itemsPerPage = inventorySize - usingSlots; // 各ページに表示するアイテムの数
        Map<Integer, Runnable> playerMenuActions = new HashMap<>();
        Inventory inv = Bukkit.createInventory(null, inventorySize, teleportRequestInventoryName);
        ItemStack backItem = new ItemStack(Material.STICK);
        ItemMeta backMeta = backItem.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.GOLD + "戻る");
            backItem.setItemMeta(backMeta);
        }
        inv.setItem(0, backItem);
        playerMenuActions.put(0, () -> playerTeleportMenu(player, 1));
        int totalItems = Bukkit.getOnlinePlayers().size();
        if (totalItems != 1) {
            int totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);
            int startIndex = (page - 1) * itemsPerPage;
            int endIndex = Math.min(startIndex + itemsPerPage, totalItems);
            if (page < totalPages) {
                ItemStack nextPageItem = new ItemStack(Material.ARROW);
                ItemMeta nextPageMeta = nextPageItem.getItemMeta();
                if (nextPageMeta != null) {
                    nextPageMeta.setDisplayName(ChatColor.GOLD + "次のページ");
                    nextPageItem.setItemMeta(nextPageMeta);
                }
                inv.setItem(26, nextPageItem);
                playerMenuActions.put(26, () -> teleportRequestMenu(player, page + 1));
            }
            if (page > 1) {
                ItemStack prevPageItem = new ItemStack(Material.ARROW);
                ItemMeta prevPageMeta = prevPageItem.getItemMeta();
                if (prevPageMeta != null) {
                    prevPageMeta.setDisplayName(ChatColor.GOLD + "前のページ");
                    prevPageItem.setItemMeta(prevPageMeta);
                }
                inv.setItem(18, prevPageItem);
                playerMenuActions.put(18, () -> teleportRequestMenu(player, page - 1));
            }
            for (int i = startIndex; i < endIndex; i++) {
                Player targetPlayer = (Player) Bukkit.getOnlinePlayers().toArray()[i];
                if (targetPlayer.equals(player)) continue;
                ItemStack playerItem = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta playerMeta = (SkullMeta) playerItem.getItemMeta();
                if (playerMeta != null) {
                    playerMeta.setOwningPlayer(targetPlayer);
                    playerMeta.setDisplayName(ChatColor.GREEN + targetPlayer.getName());
                    playerItem.setItemMeta(playerMeta);
                }
                inv.addItem(playerItem);
                playerMenuActions.put(inv.first(playerItem), () -> {
                    player.closeInventory();
                    if (!me) {
                        player.performCommand("tpr " + targetPlayer.getName());
                    } else {
                        player.performCommand("tprm " + targetPlayer.getName());
                    }
                });
            }
        } else {
            ItemStack noRequestItem = new ItemStack(Material.BARRIER);
            ItemMeta noRequestMeta = noRequestItem.getItemMeta();
            if (noRequestMeta != null) {
                noRequestMeta.setDisplayName(ChatColor.RED + "テレポートできるプレイヤーがいません");
                noRequestItem.setItemMeta(noRequestMeta);
            }
            inv.setItem(13, noRequestItem);
        }
        Menu.menuActions.computeIfAbsent(player, _p -> new HashMap<>()).put(Menu.teleportRequestInventoryName, playerMenuActions);
        player.openInventory(inv);
    }

    public void playerTeleportMenu(Player player, int page) {
        Map<Integer, Runnable> playerMenuActions = new HashMap<>();
        Inventory inv = Bukkit.createInventory(null, 27, Menu.playerTeleportInventoryName);
        switch (page) {
            case 1 -> {
                playerMenuActions.put(0, () -> teleportMenu(player, 1));
                playerMenuActions.put(11, () -> teleportRequestMenu(player, 1));
                playerMenuActions.put(13, () -> teleportMeRequestMenu(player, 1));
                playerMenuActions.put(15, () -> teleportResponseHeadMenu(player, 1));
                ItemStack backItem = new ItemStack(Material.STICK);
                ItemMeta backMeta = backItem.getItemMeta();
                if (backMeta != null) {
                    backMeta.setDisplayName(ChatColor.GOLD + "戻る");
                    backItem.setItemMeta(backMeta);
                }
                inv.setItem(0, backItem);
                ItemStack requestItem = new ItemStack(Material.ARROW);
                ItemMeta requestMeta = requestItem.getItemMeta();
                if (requestMeta != null) {
                    requestMeta.setDisplayName(ChatColor.GREEN + "テレポートリクエスト");
                    requestMeta.setLore(Arrays.asList(ChatColor.GRAY + "テレポートリクエストを送信できるよ。"));
                    requestItem.setItemMeta(requestMeta);
                }
                inv.setItem(11, requestItem);
                ItemStack meRequestItem = new ItemStack(Material.TARGET);
                ItemMeta meRequestMeta = meRequestItem.getItemMeta();
                if (meRequestMeta != null) {
                    meRequestMeta.setDisplayName(ChatColor.GREEN + "逆テレポートリクエスト");
                    meRequestMeta.setLore(Arrays.asList(ChatColor.GRAY + "逆テレポートリクエストを送信できるよ。"));
                    meRequestItem.setItemMeta(meRequestMeta);
                }
                inv.setItem(13, meRequestItem);
                ItemStack responseItem = new ItemStack(Material.SHIELD);
                ItemMeta responseMeta = responseItem.getItemMeta();
                if (responseMeta != null) {
                    responseMeta.setDisplayName(ChatColor.GREEN + "テレポートリクエストBOX");
                    responseMeta.setLore(Arrays.asList(ChatColor.GRAY + "テレポートリクエストを管理できるよ。"));
                    responseItem.setItemMeta(responseMeta);
                }
                inv.setItem(15, responseItem);
            }
        }
        Menu.menuActions.computeIfAbsent(player, _p -> new HashMap<>()).put(Menu.playerTeleportInventoryName, playerMenuActions);
        player.openInventory(inv);
    }

    public void openImageMenu(Player player, int page) {
        int inventorySize = 54;
        int usingSlots = 3; // 戻るボタンやページネーションボタンに使用するスロット数
        int itemsPerPage = inventorySize - usingSlots; // 各ページに表示するアイテムの数
        Inventory inv = Bukkit.createInventory(null, inventorySize, Menu.imageInventoryName);
        try (Connection conn = db.getConnection()) {
            Map<Integer, Map<String, Object>> thisServerImageInfo = im.getThisServerImages(conn);
            Map<Integer, Map<String, Object>> imageMap = im.getImageMap(conn);
            int totalItems = imageMap.size();
            int totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);
            int startIndex = (page - 1) * itemsPerPage;
            int endIndex = Math.min(startIndex + itemsPerPage, totalItems);
            Map<Integer, Runnable> playerMenuActions = new HashMap<>();
            ItemStack backItem = new ItemStack(Material.STICK);
            ItemMeta backMeta = backItem.getItemMeta();
            if (backMeta != null) {
                backMeta.setDisplayName(ChatColor.GOLD + "戻る");
                backItem.setItemMeta(backMeta);
            }
            inv.setItem(0, backItem);
            playerMenuActions.put(0, () -> generalMenu(player, 1));
            if (page < totalPages) {
                ItemStack nextPageItem = new ItemStack(Material.ARROW);
                ItemMeta nextPageMeta = nextPageItem.getItemMeta();
                if (nextPageMeta != null) {
                    nextPageMeta.setDisplayName(ChatColor.GOLD + "次のページ");
                    nextPageItem.setItemMeta(nextPageMeta);
                }
                inv.setItem(53, nextPageItem);
                playerMenuActions.put(53, () -> openImageMenu(player, page + 1));
            }
            if (page > 1) {
                ItemStack prevPageItem = new ItemStack(Material.ARROW);
                ItemMeta prevPageMeta = prevPageItem.getItemMeta();
                if (prevPageMeta != null) {
                    prevPageMeta.setDisplayName(ChatColor.GOLD + "前のページ");
                    prevPageItem.setItemMeta(prevPageMeta);
                }
                inv.setItem(45, prevPageItem);
                playerMenuActions.put(45, () -> openImageMenu(player, page - 1));
            }
            int index = 0;
            for (int i = startIndex; i < endIndex; i++) {
                Map<String, Object> imageInfo = imageMap.get(i);
                if (imageInfo != null) {
                    int id = (int) imageInfo.get("id");
                    String server = imageInfo.get("server") instanceof String authorServer ? authorServer : null, 
                        thisServer = shd.getServerName(),
                        title = (String) imageInfo.get("title"),
                        authorName = (String) imageInfo.get("name"),
                        comment = (String) imageInfo.get("comment"),
                        imageUUID = (String) imageInfo.get("imuuid"),
                        ext = (String) imageInfo.get("ext"),
                        date = ((Date) imageInfo.get("date")).toString(),
                        url = (String) imageInfo.get("url");
                    boolean fromDiscord = Optional.ofNullable(imageInfo.get("d"))
                        .map(value -> value instanceof Boolean ? (Boolean) value : (Integer) value != 0)
                        .orElse(false),
                        isQr = Optional.ofNullable(imageInfo.get("isqr"))
                            .map(value -> value instanceof Boolean ? (Boolean) value : (Integer) value != 0)
                            .orElse(false),
                        locked = (boolean) imageInfo.get("locked"),
                        lockedAction = (boolean) imageInfo.get("locked_action"),
                        large = (boolean) imageInfo.get("large");
                        //isQr = imageInfo.get("isqr") != null && (imageInfo.get("isqr") instanceof Boolean ? (Boolean) imageInfo.get("isqr") : (Integer) imageInfo.get("isqr") != 0);
                    List<String> lores = new ArrayList<>();
                    lores.add(large ? "<ラージマップ>" : isQr ? "<QRコード>" : "<イメージマップ>");
                    List<String> commentLines = Arrays.stream(comment.split("\n"))
                                        .map(String::trim)
                                        .collect(Collectors.toList());
                    lores.addAll(commentLines);
                    lores.add("created by " + authorName);
                    lores.add("at " + date.replace("-", "/"));
                    if (fromDiscord) {
                        lores.add("from " + ChatColor.BLUE + "Discord");
                    }
                    if (locked) {
                        lores.add(ChatColor.RED + "ロックされています。");
                    } else if (!lockedAction) {
                        lores.add(ChatColor.RED + "ロック解除済みです。");
                        lores.add(ChatColor.RED + "作成者のみロック後のアクションを選択できます。");
                    } else if (large) {
                        lores.add(ChatColor.RED + "取得できません。");
                    } else {
                        lores.add(ChatColor.GREEN + "取得できます。");
                    }
                    ItemStack item = new ItemStack(Material.MAP);
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName(ChatColor.GREEN + title);
                        meta.setLore(lores);
                        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, String.valueOf(index)), PersistentDataType.STRING, "true");
                        item.setItemMeta(meta);
                    }
                    inv.addItem(item);
                    //inv.setItem(slot, item);
                    if (locked) {
                        // discord未認証(/qコマンドでロック解除されていない)の場合
                        playerMenuActions.put(inv.first(item), () -> {
                            //logger.info("through into lockedMap");
                            player.closeInventory();
                            player.sendMessage(ChatColor.RED + "この画像はロックされています。\n"
                                + ChatColor.GRAY + "この画像を取得するには、/qコマンドにて、OTPを入力してください。");
                        });
                    } else if (!lockedAction) {
                        // 認証はされているが、1✕1のマップかラージマップかを選んでいない場合
                        if (authorName.equals(player.getName())) {
                            // 作成者の場合
                            // OTPがまだデータベースに残っているのが確定している
                            playerMenuActions.put(inv.first(item), () -> {
                                //logger.info("through into executeQFromMenu");
                                player.closeInventory();
                                if (imageInfo.get("otp") instanceof String otp) {
                                    im.executeQFromMenu(player, new Object[] {otp, title, comment, url, date});
                                }
                            });
                        } else {
                            // 作成者以外の場合
                            playerMenuActions.put(inv.first(item), () -> {
                                player.closeInventory();

                                Component alert = Component.text("ロック解除済みです。")
                                    .color(NamedTextColor.RED);
                                
                                TextComponent messages = Component.text()
                                    .append(alert)
                                    .append(Component.text("作成者のみロック後のアクションを選択できます。"))
                                    .build();

                                player.sendMessage(messages);
                            });
                        }
                    } else if (large) {
                        playerMenuActions.put(inv.first(item), () -> {
                            player.closeInventory();
                            Component serverComponent = Component.text(server + "サーバー")
                                .color(NamedTextColor.GOLD)
                                .decorate(
                                    TextDecoration.BOLD,
                                    TextDecoration.UNDERLINED)
                                .color(NamedTextColor.GOLD);

                            Component alert = Component.text("ラージマップは取得できません。")
                                .color(NamedTextColor.RED);

                            TextComponent messages = Component.text()
                                .append(Component.text("この画像は"))
                                .append(serverComponent)
                                .append(Component.text("で作られたラージマップです。"))
                                .appendNewline()
                                .append(alert)
                                .build();

                            player.sendMessage(messages);
                        });
                    } else if (imageInfo.get("mapid") instanceof Integer mapId && thisServerImageInfo.containsKey(mapId) && server != null && server.equals(thisServer)) {
                        // そのサーバーで、データベースに保存されているmapIdをもつマップがあるとは限らない
                        //Map<String, Object> thisServerImage = thisServerImageInfo.get(mapId);
                        playerMenuActions.put(inv.first(item), () -> {
                            //logger.info("through into giveMap");
                            player.closeInventory();
                            im.giveMapToPlayer(player, mapId);
                        }); 
                    } else {
                        playerMenuActions.put(inv.first(item), () -> {
                            //logger.info("through into executeImageMapFromMenu");
                            player.closeInventory();
                            im.executeImageMapFromMenu(player, new Object[] {id, isQr, authorName, imageUUID, title, comment, ext, date});
                        });
                    }
                    index++;
                }
            }
            Menu.menuActions.computeIfAbsent(player, _p -> new HashMap<>()).put(Menu.imageInventoryName, playerMenuActions);
            player.openInventory(inv);
        } catch (SQLException | ClassNotFoundException e) {
            player.openInventory(inv);
            player.closeInventory();
            player.sendMessage(ChatColor.RED + "データベースとの通信に失敗しました。");
            logger.error("An error occurred while communicating with the database: {}", e.getMessage());
            for (StackTraceElement ste : e.getStackTrace()) {
                logger.error(ste.toString());
            }
        }
    }

    public void openOnlineServerInventory(Player player, int page) {
        Map<Integer, Runnable> playerMenuActions = new HashMap<>();
        Inventory inv = Bukkit.createInventory(null, 54, Menu.onlineServerInventoryName);
        ItemStack backItem = new ItemStack(Material.STICK);
        ItemMeta backMeta = backItem.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.GOLD + "戻る");
            backItem.setItemMeta(backMeta);
        }
        inv.setItem(0, backItem);
        playerMenuActions.put(0, () -> openServerTypeInventory(player, 1));
        Map<String, Map<String, Map<String, Object>>> serverStatusMap = ssc.getStatusMap();
        // オンラインサーバーのみを抽出
        Map<String, Map<String, Object>> serverStatusOnlineMap = serverStatusMap.values().stream()
            .flatMap(serverStatusList -> serverStatusList.entrySet().stream())
            .filter(serverEntry -> serverEntry.getValue().get("online") instanceof Boolean online && online)
            .filter(serverEntry -> serverEntry.getValue().get("name") instanceof String name && !name.equals("proxy") && !name.equals("maintenance"))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        int totalItems = serverStatusOnlineMap.size(),
            totalPages = (totalItems + SLOT_POSITIONS.length - 1) / SLOT_POSITIONS.length,
            startIndex = (page - 1) * SLOT_POSITIONS.length,
            endIndex = Math.min(startIndex + SLOT_POSITIONS.length, totalItems);
        List<Map<String, Object>> serverDataList = serverStatusOnlineMap.values().stream().collect(Collectors.toList());
        for (int i = startIndex; i < endIndex; i++) {
            Map<String, Object> serverData = serverDataList.get(i);
            String serverName = (String) serverData.get("name");
            if (page == 1 && i == 0) {
                currentOreIndex = 0; // ページが1の場合はインデックスをリセット
            }
            Material oreMaterial = ORE_BLOCKS.get(currentOreIndex);
            currentOreIndex = (currentOreIndex + 1) % ORE_BLOCKS.size(); // インデックスを更新
            ItemStack serverItem = new ItemStack(oreMaterial);
            ItemMeta serverMeta = serverItem.getItemMeta();
            if (serverMeta != null) {
                serverMeta.setDisplayName(ChatColor.GREEN + serverName);
                serverItem.setItemMeta(serverMeta);
            }
            int slot = SLOT_POSITIONS[i - startIndex];
            inv.setItem(slot, serverItem);
            playerMenuActions.put(slot, () -> openServerInventoryFromOnlineServerInventory(player, serverName, 1));
        }
        if (page > 1) {
            ItemStack prevPageItem = new ItemStack(Material.ARROW);
            ItemMeta prevPageMeta = prevPageItem.getItemMeta();
            if (prevPageMeta != null) {
                prevPageMeta.setDisplayName(ChatColor.GOLD + "前のページ");
                prevPageItem.setItemMeta(prevPageMeta);
            }
            inv.setItem(45, prevPageItem);
            playerMenuActions.put(45, () -> openOnlineServerInventory(player, page - 1));
        }
        if (page < totalPages) {
            ItemStack nextPageItem = new ItemStack(Material.ARROW);
            ItemMeta nextPageMeta = nextPageItem.getItemMeta();
            if (nextPageMeta != null) {
                nextPageMeta.setDisplayName(ChatColor.GOLD + "次のページ");
                nextPageItem.setItemMeta(nextPageMeta);
            }
            inv.setItem(53, nextPageItem);
            playerMenuActions.put(53, () -> openOnlineServerInventory(player, page + 1));
        }
        Menu.menuActions.computeIfAbsent(player, _p -> new HashMap<>()).put(Menu.onlineServerInventoryName, playerMenuActions);
        //logger.info("menuActions: {}", menuActions);
        player.openInventory(inv);
    }

    public void openServerEachInventory(Player player, String serverType, int page) {
        Map<Integer, Runnable> playerMenuActions = new HashMap<>();
        Inventory inv = Bukkit.createInventory(null, 54, serverType + " servers");
        ItemStack backItem = new ItemStack(Material.STICK);
        ItemMeta backMeta = backItem.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.GOLD + "戻る");
            backItem.setItemMeta(backMeta);
        }
        inv.setItem(0, backItem);
        playerMenuActions.put(0, () -> {
            openServerTypeInventory(player, 1);
        });
        Map<String, Map<String, Map<String, Object>>> serverStatusMap = ssc.getStatusMap();
        Map<String, Map<String, Object>> serverStatusTypeMap = serverStatusMap.get(serverType);
        int totalItems = serverStatusTypeMap.size(),
            totalPages = (totalItems + SLOT_POSITIONS.length - 1) / SLOT_POSITIONS.length,
            startIndex = (page - 1) * SLOT_POSITIONS.length,
            endIndex = Math.min(startIndex + SLOT_POSITIONS.length, totalItems);
        List<Map<String, Object>> serverDataList = serverStatusTypeMap.values().stream().collect(Collectors.toList());
        for (int i = startIndex; i < endIndex; i++) {
            Map<String, Object> serverData = serverDataList.get(i);
            String serverName = (String) serverData.get("name");
            if (page == 1 && i == 0) {
                currentOreIndex = 0; // ページが1の場合はインデックスをリセット
            }
            Material oreMaterial = ORE_BLOCKS.get(currentOreIndex);
            currentOreIndex = (currentOreIndex + 1) % ORE_BLOCKS.size(); // インデックスを更新
            ItemStack serverItem = new ItemStack(oreMaterial);
            ItemMeta serverMeta = serverItem.getItemMeta();
            if (serverMeta != null) {
                serverMeta.setDisplayName(ChatColor.GREEN + serverName);
                serverItem.setItemMeta(serverMeta);
            }
            int slot = SLOT_POSITIONS[i - startIndex];
            playerMenuActions.put(slot, () -> openServerInventory(player, serverName, 1));
            inv.setItem(slot, serverItem);
        }
        if (page > 1) {
            ItemStack prevPageItem = new ItemStack(Material.ARROW);
            ItemMeta prevPageMeta = prevPageItem.getItemMeta();
            if (prevPageMeta != null) {
                prevPageMeta.setDisplayName(ChatColor.GOLD + "前のページ");
                prevPageItem.setItemMeta(prevPageMeta);
            }
            inv.setItem(45, prevPageItem);
            playerMenuActions.put(45, () -> openServerEachInventory(player, serverType, page - 1));
        }
        if (page < totalPages) {
            ItemStack nextPageItem = new ItemStack(Material.ARROW);
            ItemMeta nextPageMeta = nextPageItem.getItemMeta();
            if (nextPageMeta != null) {
                nextPageMeta.setDisplayName(ChatColor.GOLD + "次のページ");
                nextPageItem.setItemMeta(nextPageMeta);
            }
            inv.setItem(53, nextPageItem);
            playerMenuActions.put(53, () -> openServerEachInventory(player, serverType, page + 1));
        }
        Menu.menuActions.computeIfAbsent(player, _p -> new HashMap<>()).put(Menu.serverTypeInventoryName, playerMenuActions);
        player.openInventory(inv);
    }

    public void openServerInventory(Player player, String serverName, int page) {
        openServerInventory(player, serverName, page, false);
    }

    public void openServerInventoryFromOnlineServerInventory(Player player, String serverName, int page) {
        openServerInventory(player, serverName, page, true);
    }

    private void openServerInventory(Player player, String serverName, int page, boolean fromOnlineServerInventory) {
        Map<Integer, Runnable> playerMenuActions = new HashMap<>();
        int permLevel = lp.getPermLevel(player.getName());
        Inventory inv = Bukkit.createInventory(null, 54, serverName + " server");
        ItemStack backItem = new ItemStack(Material.STICK);
        ItemMeta backMeta = backItem.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.GOLD + "戻る");
            backItem.setItemMeta(backMeta);
        }
        inv.setItem(0, backItem);
        AtomicReference<String> thisServerType = new AtomicReference<>(null);
        Map<String, Map<String, Map<String, Object>>> serverStatusMap = ssc.getStatusMap();
        for (Map.Entry<String, Map<String, Map<String, Object>>> entry : serverStatusMap.entrySet()) {
            String serverType = entry.getKey();
            Map<String, Map<String, Object>> serverStatusList = entry.getValue();
            for (Map.Entry<String, Map<String, Object>> serverEntry : serverStatusList.entrySet()) {
                String name = serverEntry.getKey();
                if (name.equals(serverName)) {
                    thisServerType.set(serverType);
                    try (Connection conn = db.getConnection()) {
                        boolean isAllowedToEnter = isAllowedToEnter(conn, serverName);
                        Map<String, Object> serverData = serverEntry.getValue();
                        for (Map.Entry<String, Object> dataEntry : serverData.entrySet()) {
                            String key = dataEntry.getKey();
                            Object value = dataEntry.getValue();
                            if (key.equals("online")) { 
                                if (value instanceof Boolean online && online) {
                                    ItemStack onlineItem = new ItemStack(Material.GREEN_WOOL);
                                    ItemMeta onlineMeta = onlineItem.getItemMeta();
                                    if (onlineMeta != null) {
                                        onlineMeta.setDisplayName(ChatColor.GREEN + "オンライン");
                                        onlineItem.setItemMeta(onlineMeta);
                                    }
                                    inv.setItem(8, onlineItem);
                                    if (isAllowedToEnter && permLevel >= 1) {
                                        ItemStack leverItem = new ItemStack(Material.LEVER);
                                        ItemMeta leverMeta = leverItem.getItemMeta();
                                        if (leverMeta != null) {
                                            if (permLevel == 1) {
                                                leverMeta.setDisplayName(ChatColor.GREEN + serverName + "サーバーが起動中です！");
                                                //leverMeta.setLore(Arrays.asList(ChatColor.BLUE + "Discord" + ChatColor.GRAY + "でリクエストを送信する"));
                                            } else if (permLevel >= 2) {
                                                leverMeta.setDisplayName(ChatColor.GREEN + serverName + "サーバーを停止する");
                                                playerMenuActions.put(22, () -> serverSwitch(player, serverName));
                                                //leverMeta.setLore(Arrays.asList(ChatColor.BLUE + ""));
                                            }
                                            leverItem.setItemMeta(leverMeta);
                                        }
                                        inv.setItem(22, leverItem);
                                        ItemStack doorItem = new ItemStack(Material.IRON_DOOR);
                                        ItemMeta doorMeta = doorItem.getItemMeta();
                                        if (doorMeta != null) {
                                            doorMeta.setDisplayName(ChatColor.GREEN + serverName + "サーバーに入る");
                                            doorItem.setItemMeta(doorMeta);
                                            playerMenuActions.put(24, () -> enterServer(player, serverName));
                                        }
                                        inv.setItem(24, doorItem);
                                    } else {
                                        ItemStack barrierItem = new ItemStack(Material.BARRIER);
                                        ItemMeta barrierMeta = barrierItem.getItemMeta();
                                        if (barrierMeta != null) {
                                            barrierMeta.setDisplayName(ChatColor.RED + "許可されていません。");
                                            barrierItem.setItemMeta(barrierMeta);
                                        }
                                        inv.setItem(22, barrierItem);
                                        inv.setItem(24, barrierItem);
                                    }
                                } else {
                                    ItemStack offlineItem = new ItemStack(Material.RED_WOOL);
                                    ItemMeta offlineMeta = offlineItem.getItemMeta();
                                    if (offlineMeta != null) {
                                        offlineMeta.setDisplayName(ChatColor.RED + "オフライン");
                                        offlineItem.setItemMeta(offlineMeta);
                                    }
                                    inv.setItem(8, offlineItem);
                                    if (isAllowedToEnter && permLevel >= 1) {
                                        ItemStack leverItem = new ItemStack(Material.LEVER);
                                        ItemMeta leverMeta = leverItem.getItemMeta();
                                        if (leverMeta != null) {
                                            if (permLevel == 1) {
                                                leverMeta.setDisplayName(ChatColor.GREEN + serverName + "サーバーの起動リクエスト");
                                                leverMeta.setLore(Arrays.asList(ChatColor.BLUE + "Discord" + ChatColor.GRAY + "でリクエストを送信する"));
                                                playerMenuActions.put(22, () -> serverSwitch(player, serverName));
                                            } else if (permLevel >= 2) {
                                                leverMeta.setDisplayName(ChatColor.GREEN + serverName + "サーバーの起動");
                                                leverMeta.setLore(Arrays.asList(ChatColor.BLUE + "アドミン権限より起動する。"));
                                                playerMenuActions.put(22, () -> serverSwitch(player, serverName));
                                            }
                                            leverItem.setItemMeta(leverMeta);
                                        }
                                        inv.setItem(22, leverItem);
                                        ItemStack doorItem = new ItemStack(Material.IRON_DOOR);
                                        ItemMeta doorMeta = doorItem.getItemMeta();
                                        if (doorMeta != null) {
                                            doorMeta.setDisplayName(ChatColor.GREEN + serverName + "サーバーがオンラインでないため、入れません。");
                                            doorItem.setItemMeta(doorMeta);
                                        }
                                        inv.setItem(24, doorItem);
                                    } else {
                                        ItemStack barrierItem = new ItemStack(Material.BARRIER);
                                        ItemMeta barrierMeta = barrierItem.getItemMeta();
                                        if (barrierMeta != null) {
                                            barrierMeta.setDisplayName(ChatColor.RED + "許可されていません。");
                                            barrierItem.setItemMeta(barrierMeta);
                                        }
                                        inv.setItem(22, barrierItem);
                                        inv.setItem(24, barrierItem);
                                    }
                                }
                            }
                            if (key.equals("player_list")) {
                                if (value instanceof String playerList) {
                                    String[] playerArray = playerList.split(",\\s*");
                                    List<String> players = Arrays.asList(playerArray);
                                    int totalItems = players.size();
                                    int totalPages = (totalItems + FACE_POSITIONS.length - 1) / SLOT_POSITIONS.length;
                                    int startIndex = (page - 1) * SLOT_POSITIONS.length;
                                    int endIndex = Math.min(startIndex + SLOT_POSITIONS.length, totalItems);
                                    for (int i = startIndex; i < endIndex; i++) {
                                        String playerName = players.get(i);
                                        ItemStack playerItem = new ItemStack(Material.PLAYER_HEAD);
                                        SkullMeta playerMeta = (SkullMeta) playerItem.getItemMeta();
                                        if (playerMeta != null) {
                                            Map<String, Map<String, String>> memberMap = ssc.getMemberMap();
                                            UUID playerUUID = UUID.fromString(memberMap.get(playerName).get("uuid"));
                                            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUUID);
                                            // プレイヤーが一度でもサーバーに参加したことがあるか確認
                                            if (offlinePlayer.hasPlayedBefore() || offlinePlayer.isOnline()) {
                                                playerMeta.setOwningPlayer(offlinePlayer);
                                            } else {
                                                // プレイヤーが存在しない場合の処理
                                                playerMeta.setDisplayName(ChatColor.RED + "Unknown Player");
                                            }
                                            playerMeta.setDisplayName(ChatColor.GREEN + playerName.trim());
                                            playerMeta.setLore(Arrays.asList(ChatColor.GRAY + "現在オンラインです！"));
                                            playerItem.setItemMeta(playerMeta);
                                        }
                                        inv.setItem(FACE_POSITIONS[i - startIndex], playerItem);
                                    }
                                    // ページ戻るブロックを配置
                                    if (page > 1) {
                                        ItemStack prevPageItem = new ItemStack(Material.ARROW);
                                        ItemMeta prevPageMeta = prevPageItem.getItemMeta();
                                        if (prevPageMeta != null) {
                                            prevPageMeta.setDisplayName(ChatColor.GOLD + "前のページ");
                                            prevPageItem.setItemMeta(prevPageMeta);
                                        }
                                        inv.setItem(45, prevPageItem);
                                        playerMenuActions.put(45, () -> openServerInventory(player, serverName, page - 1));
                                    }
                                    // ページ進むブロックを配置
                                    if (page < totalPages) {
                                        ItemStack nextPageItem = new ItemStack(Material.ARROW);
                                        ItemMeta nextPageMeta = nextPageItem.getItemMeta();
                                        if (nextPageMeta != null) {
                                            nextPageMeta.setDisplayName(ChatColor.GOLD + "次のページ");
                                            nextPageItem.setItemMeta(nextPageMeta);
                                        }
                                        inv.setItem(53, nextPageItem);
                                        playerMenuActions.put(53, () -> openServerInventory(player, serverName, page + 1));
                                    }
                                } else {
                                    ItemStack noPlayerItem = new ItemStack(Material.BARRIER);
                                    ItemMeta noPlayerMeta = noPlayerItem.getItemMeta();
                                    if (noPlayerMeta != null) {
                                        noPlayerMeta.setDisplayName(ChatColor.RED + "プレイヤーがいません");
                                        noPlayerItem.setItemMeta(noPlayerMeta);
                                    }
                                    inv.setItem(36, noPlayerItem);
                                }
                            }
                        }
                    } catch (SQLException | ClassNotFoundException e) {
                        player.closeInventory();
                        player.sendMessage(ChatColor.RED + "データベースとの通信に失敗しました。");
                        logger.error("An error occurred while communicating with the database: {}", e.getMessage());
                        for (StackTraceElement ste : e.getStackTrace()) {
                            logger.error(ste.toString());
                        }
                    }
                }
            }
        }
        playerMenuActions.put(0, () -> {
            if (fromOnlineServerInventory) {
                openOnlineServerInventory(player, 1);
            } else {
                openServerEachInventory(player, thisServerType.get(), 1);
            }
        });
        Menu.menuActions.computeIfAbsent(player, _p -> new HashMap<>()).put(Menu.serverInventoryName, playerMenuActions);
        player.openInventory(inv);
    }

    public void openTeleportPointMenu(Player player, int page, String type) {
        int inventorySize = 54;
        int usingSlots = 3; // 戻るボタンやページネーションボタンに使用するスロット数
        int itemsPerPage = inventorySize - usingSlots; // 各ページに表示するアイテムの数
        Inventory inv = Bukkit.createInventory(null, inventorySize, Menu.teleportPointInventoryName);
        try (Connection conn = db.getConnection()) {
            Map<Integer, Map<String, Object>> thisServerTeleportPoints = new HashMap<>();
            String thisServerName = shd.getServerName();
            String query = "SELECT * FROM tp_points WHERE server=? AND type=?;";
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setString(1, thisServerName);
                ps.setString(2, type);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> rowMap = new HashMap<>();
                        int id = rs.getInt("id");
                        int columnCount = rs.getMetaData().getColumnCount();
                        for (int i = 1; i <= columnCount; i++) {
                            String columnName = rs.getMetaData().getColumnName(i);
                            if (!columnName.equals("id")) {
                                rowMap.put(columnName, rs.getObject(columnName));
                            }
                        }
                        thisServerTeleportPoints.computeIfAbsent(id, _p -> rowMap);
                    }
                }
            }
            
            int totalItems = thisServerTeleportPoints.size();
            int totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);
            int startIndex = (page - 1) * itemsPerPage;
            int endIndex = Math.min(startIndex + itemsPerPage, totalItems);
            Map<Integer, Runnable> playerMenuActions = new HashMap<>();
            ItemStack backItem = new ItemStack(Material.STICK);
            ItemMeta backMeta = backItem.getItemMeta();
            if (backMeta != null) {
                backMeta.setDisplayName(ChatColor.GOLD + "戻る");
                backItem.setItemMeta(backMeta);
            }
            inv.setItem(0, backItem);
            playerMenuActions.put(0, () -> teleportPointTypeMenu(player, 1));
            if (page < totalPages) {
                ItemStack nextPageItem = new ItemStack(Material.ARROW);
                ItemMeta nextPageMeta = nextPageItem.getItemMeta();
                if (nextPageMeta != null) {
                    nextPageMeta.setDisplayName(ChatColor.GOLD + "次のページ");
                    nextPageItem.setItemMeta(nextPageMeta);
                }
                inv.setItem(53, nextPageItem);
                playerMenuActions.put(53, () -> openTeleportPointMenu(player, page + 1, type));
            }
            if (page > 1) {
                ItemStack prevPageItem = new ItemStack(Material.ARROW);
                ItemMeta prevPageMeta = prevPageItem.getItemMeta();
                if (prevPageMeta != null) {
                    prevPageMeta.setDisplayName(ChatColor.GOLD + "前のページ");
                    prevPageItem.setItemMeta(prevPageMeta);
                }
                inv.setItem(45, prevPageItem);
                playerMenuActions.put(45, () -> openTeleportPointMenu(player, page - 1, type));
            }
            int index = 0;
            for (int i = startIndex; i < endIndex; i++) {
                Map<String, Object> tpPoint = thisServerTeleportPoints.get(i);
                if (tpPoint != null) {
                    String authorName = (String) tpPoint.get("name");
                    String title = (String) tpPoint.get("title");
                    String comment  = (String) tpPoint.get("comment");
                    String tpType = (String) tpPoint.get("type");
                    double x = (Double) tpPoint.get("x");
                    double y = (Double) tpPoint.get("y");
                    double z =  (Double) tpPoint.get("z");
                    float yaw = (Float) tpPoint.get("yaw");
                    float pitch = (Float) tpPoint.get("pitch");
                    String worldName = (String) tpPoint.get("world");
                    //String date = ((Date) tpPoint.get("date")).toString();
                    Date dateTime = (Date) tpPoint.get("date");
                    String date = JavaUtils.Time.Format.YYYY_MM_DD_HH_MM_SS.format(dateTime);
                    
                    List<String> lores = new ArrayList<>();
                    lores.add("<" + tpType + ">");
                    lores.add("World: " + worldName);
                    lores.add("Location: (" + x + ", " + y + ", " + z + ")");
                    List<String> commentLines = Arrays.stream(comment.split("\n"))
                        .map(String::trim)
                        .collect(Collectors.toList());
                    lores.addAll(commentLines);
                    lores.add("created by " + authorName);
                    lores.add("at " + date.replace("-", "/"));

                    ItemStack item = new ItemStack(Material.ENDER_PEARL);
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName(ChatColor.GREEN + title);
                        meta.setLore(lores);
                        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, String.valueOf(index)), PersistentDataType.STRING, "true");
                        item.setItemMeta(meta);
                    }
                    inv.addItem(item);
                    //inv.setItem(slot, item);
                    playerMenuActions.put(inv.first(item), () -> {
                        player.closeInventory();
                        // ここで、テレポートさせる前の座標を記録し、/backコマンドで戻れるようにする
                        Location beforeLoc = player.getLocation();
                        EventListener.playerBeforeLocationMap.put(player, beforeLoc);
                        World world = Bukkit.getWorld(worldName);
                        if (world == null) {
                            throw new IllegalArgumentException("World not found: " + worldName);
                        }
                        Location loc = new Location(world, x, y, z, yaw, pitch);
                        player.teleport(loc);

                        Component message = Component.text("テレポートしました。")
                            .color(NamedTextColor.GREEN)
                            .decorate(TextDecoration.BOLD);
                            
                        player.sendMessage(message);
                    });
                    index++;
                }
            }
            Menu.menuActions.computeIfAbsent(player, _p -> new HashMap<>()).put(Menu.teleportPointInventoryName, playerMenuActions);
            player.openInventory(inv);
        } catch (SQLException | ClassNotFoundException e) {
            player.openInventory(inv);
            player.closeInventory();
            player.sendMessage(ChatColor.RED + "データベースとの通信に失敗しました。");
            logger.error("An error occurred while communicating with the database: {}", e.getMessage());
            for (StackTraceElement ste : e.getStackTrace()) {
                logger.error(ste.toString());
            }
        }
    }

    public void openServerTypeInventory(Player player, int page) {
        Map<Integer, Runnable> playerMenuActions = new HashMap<>();
        playerMenuActions.put(0, () -> generalMenu(player, 1));
        Inventory inv = Bukkit.createInventory(null, 27, "server type");
        ItemStack backItem = new ItemStack(Material.STICK);
        ItemMeta backMeta = backItem.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.GOLD + "戻る");
            backItem.setItemMeta(backMeta);
        }
        inv.setItem(0, backItem);
        switch (page) {
            case 1 -> {
                playerMenuActions.put(11, () -> openServerEachInventory(player, "survival", 1));
                playerMenuActions.put(13, () -> openServerEachInventory(player, "minigame", 1));
                playerMenuActions.put(15, () -> openServerEachInventory(player, "dev", 1));
                playerMenuActions.put(18, () -> openOnlineServerInventory(player, 1));
                playerMenuActions.put(26, () -> openServerTypeInventory(player, 2));
                ItemStack lifeServerItem = new ItemStack(Material.BREAD);
                ItemMeta lifeMeta = lifeServerItem.getItemMeta();
                if (lifeMeta != null) {
                    lifeMeta.setDisplayName(ChatColor.GREEN + "サバイバル鯖");
                    lifeServerItem.setItemMeta(lifeMeta);
                }
                inv.setItem(11, lifeServerItem);
                ItemStack eventServerItem = new ItemStack(Material.EGG);
                ItemMeta eventMeta = eventServerItem.getItemMeta();
                if (eventMeta != null) {
                    eventMeta.setDisplayName(ChatColor.YELLOW + "ミニゲーム鯖");
                    eventServerItem.setItemMeta(eventMeta);
                }
                inv.setItem(13, eventServerItem);
                ItemStack devServerItem = new ItemStack(Material.PUFFERFISH);
                ItemMeta devMeta = devServerItem.getItemMeta();
                if (devMeta != null) {
                    devMeta.setDisplayName(ChatColor.AQUA + "開発鯖");
                    devServerItem.setItemMeta(devMeta);
                }
                inv.setItem(15, devServerItem);
                ItemStack onlineServerItem = new ItemStack(Material.GREEN_WOOL);
                ItemMeta onlineMeta = onlineServerItem.getItemMeta();
                if (onlineMeta != null) {
                    onlineMeta.setDisplayName(ChatColor.GREEN + "オンラインサーバー");
                    onlineServerItem.setItemMeta(onlineMeta);
                }
                inv.setItem(18, onlineServerItem);
                ItemStack nextPageItem = new ItemStack(Material.ARROW);
                ItemMeta nextPageMeta = nextPageItem.getItemMeta();
                if (nextPageMeta != null) {
                    nextPageMeta.setDisplayName(ChatColor.GOLD + "次のページ");
                    nextPageItem.setItemMeta(nextPageMeta);
                }
                inv.setItem(26, nextPageItem);
            }
            case 2 -> {
                playerMenuActions.put(11, () -> openServerEachInventory(player, "distributed", 1));
                playerMenuActions.put(13, () -> openServerEachInventory(player, "mod", 1));
                playerMenuActions.put(15, () -> openServerEachInventory(player, "others", 1));
                playerMenuActions.put(18, () -> openServerTypeInventory(player, 1));
                ItemStack distributionServerItem = new ItemStack(Material.CHORUS_FRUIT);
                ItemMeta distributionMeta = distributionServerItem.getItemMeta();
                if (distributionMeta != null) {
                    distributionMeta.setDisplayName(ChatColor.YELLOW + "配布鯖");
                    distributionServerItem.setItemMeta(distributionMeta);
                }
                inv.setItem(11, distributionServerItem);
                ItemStack modServerItem = new ItemStack(Material.MELON_SLICE);
                ItemMeta modMeta = modServerItem.getItemMeta();
                if (modMeta != null) {
                    modMeta.setDisplayName(ChatColor.BLUE + "モッド鯖");
                    modServerItem.setItemMeta(modMeta);
                }
                inv.setItem(13, modServerItem);
                ItemStack otherServerItem = new ItemStack(Material.ROTTEN_FLESH);
                ItemMeta otherMeta = otherServerItem.getItemMeta();
                if (otherMeta != null) {
                    otherMeta.setDisplayName(ChatColor.RED + "その他");
                    otherServerItem.setItemMeta(otherMeta);
                }
                inv.setItem(15, otherServerItem);
                ItemStack prevPageItem = new ItemStack(Material.ARROW);
                ItemMeta prevPageMeta = prevPageItem.getItemMeta();
                if (prevPageMeta != null) {
                    prevPageMeta.setDisplayName(ChatColor.GOLD + "前のページ");
                    prevPageItem.setItemMeta(prevPageMeta);
                }
                inv.setItem(18, prevPageItem);
            }
        }
        Menu.menuActions.computeIfAbsent(player, _p -> new HashMap<>()).put(Menu.serverTypeInventoryName, playerMenuActions);
        player.openInventory(inv);
    }

    public void enterServer(Player player, String serverName) {
        player.closeInventory();
        cf.executeProxyCommand(player, "fmcp stp " + serverName);
    }

    private boolean checkServerOnline(String serverName) {
        return ssc.getStatusMap().values().stream()
            .flatMap(serverStatusList -> serverStatusList.entrySet().stream())
            .filter(serverDataEntry -> serverDataEntry.getKey().equals(serverName))
            .map(serverDataEntry -> serverDataEntry.getValue().get("online"))
            .filter(online -> online instanceof Boolean)
            .map(online -> (Boolean) online)
            .findFirst()
            .orElse(false);
    }

    public void serverSwitch(Player player, String serverName) {
        int permLevel = lp.getPermLevel(player.getName());
        if (checkServerOnline(serverName)) {
            if (permLevel >= 2) {
                player.closeInventory();
                cf.executeProxyCommand(player, "fmcp stop " + serverName);
            }
        } else {
            player.closeInventory();
            if (permLevel == 1) {
                cf.executeProxyCommand(player, "fmcp req " + serverName);
            } else if (permLevel >= 2) {
                cf.executeProxyCommand(player, "fmcp start " + serverName);
            }
        }
    }

    public boolean isAllowedToEnter(Connection conn, String serverName) throws SQLException, ClassNotFoundException {
        String query = "SELECT entry FROM status WHERE name = ?;";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, serverName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("entry");
                }
            }
        }
        return false;
    }

    public int getTotalPlayers(String serverName) {
        return ssc.getStatusMap().values().stream()
            .flatMap(serverStatusList -> serverStatusList.entrySet().stream())
            .filter(serverDataEntry -> serverDataEntry.getKey().equals(serverName))
            .map(serverDataEntry -> serverDataEntry.getValue().get("player_list"))
            .filter(playerList -> playerList instanceof String)
            .map(playerList -> (String) playerList)
            .mapToInt(playerList -> playerList.split(",\\s*").length)
            .findFirst()
            .orElse(0);
    }

    public int getTotalServers(String serverType) {
        Map<String, Map<String, Map<String, Object>>> serverStatusMap = ssc.getStatusMap();
        Map<String, Map<String, Object>> serverStatusList = serverStatusMap.get(serverType);
        return serverStatusList != null ? serverStatusList.size() : 0;
    }
}
