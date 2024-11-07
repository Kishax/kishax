package spigot_command;

import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import common.Database;
import common.Luckperms;
import spigot.ImageMap;
import spigot.Main;
import spigot.ServerHomeDir;
import spigot.ServerStatusCache;

@Singleton
public class Menu {
    public static String menuInventoryKey = "menu",
        menuInventoryName = "fmc menu",
        serverInventoryKey = "servers",
        onlineServerInventoryKey = "onlineServers",
        onlineServerInventoryName = "online servers",
        serverTypeInventoryKey = "serverType",
        serverTypeInventoryName = "server type",
        imageInventoryKey = "image",
        imageInventoryName = "image maps";
    public static List<String> args1 = new ArrayList<>(Arrays.asList("server", "image"));
    public static List<String> args2 = new ArrayList<>(Arrays.asList("online","life","distibuted","mod"));
    public static final int[] SLOT_POSITIONS = {11, 13, 15, 29, 31, 33};
    public static final int[] FACE_POSITIONS = {46, 47, 48, 49, 50, 51, 52};
    private static final List<Material> ORE_BLOCKS = Arrays.asList(
        Material.NETHERITE_BLOCK, Material.GOLD_BLOCK, Material.REDSTONE_BLOCK, 
        Material.EMERALD_BLOCK, Material.DIAMOND_BLOCK, Material.IRON_BLOCK,
        Material.COAL_BLOCK, Material.LAPIS_BLOCK, Material.QUARTZ_BLOCK,
        Material.COPPER_BLOCK
    );
    private final common.Main plugin;
    private final Logger logger;
    private final Database db;
    private final ServerStatusCache ssc;
    private final Luckperms lp;
    private final ImageMap im;
    private final ServerHomeDir shd;
    private final Book book;
    private final Map<Player, Map<String, Integer>> playerOpenningInventoryMap = new HashMap<>();
    private final Map<Player, Map<String, Map<Integer, Runnable>>> menuActions = new ConcurrentHashMap<>();
    private int currentOreIndex = 0; // 現在のインデックスを管理するフィールド

	@Inject
	public Menu(common.Main plugin, Logger logger, Database db, ServerStatusCache ssc, Luckperms lp, ImageMap im, ServerHomeDir shd, Book book) {  
		this.plugin = plugin;
        this.logger = logger;
        this.db = db;
        this.ssc = ssc;
        this.lp = lp;
        this.im = im;
        this.shd = shd;
        this.book = book;
	}

    // fmc menu <server|image> <server: serverType>
	public void execute(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
        if (sender instanceof Player player) {
            if (args.length == 1) {
                generalMenu(player, 1);
            } else if (args.length > 1) {
                switch (args[1].toLowerCase()) {
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
                                    case "online" -> {
                                        openOnlineServerInventory(player, 1);
                                        return;
                                    }
                                    case "life", "distributed", "mod" -> {
                                        int page = getPage(player, serverType);
                                        openServerEachInventory((Player) sender, serverType, page);
                                        return;
                                    }
                                    default -> {
                                        sender.sendMessage("Usage: /fmc menu server <life|distribution|mod>");
                                        return;
                                    }
                                }
                            } else {
                                Main.getInjector().getInstance(Menu.class).openServerTypeInventory((Player) sender);
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
        return this.menuActions.get(player);
    }

    public void generalMenu(Player player, int page) {
        Map<Integer, Runnable> playerMenuActions = new HashMap<>();
        Inventory inv = Bukkit.createInventory(null, 27, Menu.menuInventoryName);
        switch (page) {
            case 1 -> {
                playerMenuActions.put(11, () -> openServerTypeInventory(player));
                playerMenuActions.put(13, () -> book.giveRuleBook(player));
                playerMenuActions.put(15, () -> openImageMenu(player, 1));
                playerMenuActions.put(26, () -> {
                    setPage(player, menuInventoryKey, page + 1);
                    generalMenu(player, page + 1);
                });
                // サーバーメニューと画像メニューへ誘導するアイテムを追加(クリック時、メニューが開く)
                ItemStack serverItem = new ItemStack(Material.COMPASS);
                ItemMeta serverMeta = serverItem.getItemMeta();
                if (serverMeta != null) {
                    serverMeta.setDisplayName(ChatColor.GREEN + "サーバーメニュー");
                    serverMeta.setLore(Arrays.asList(ChatColor.GRAY + "各サーバーの情報を確認できるよ。"));
                    serverItem.setItemMeta(serverMeta);
                }
                inv.setItem(11, serverItem);
                ItemStack imageItem = new ItemStack(Material.MAP);
                ItemMeta imageMeta = imageItem.getItemMeta();
                if (imageMeta != null) {
                    imageMeta.setDisplayName(ChatColor.GREEN + "画像マップメニュー");
                    imageMeta.setLore(Arrays.asList(ChatColor.GRAY + "今までアップされた画像マップを確認できるよ。"));
                    imageItem.setItemMeta(imageMeta);
                }
                inv.setItem(15, imageItem);
                ItemStack ruleBookItem = new ItemStack(Material.WRITTEN_BOOK);
                ItemMeta ruleBookMeta = ruleBookItem.getItemMeta();
                if (ruleBookMeta != null) {
                    ruleBookMeta.setDisplayName(ChatColor.GREEN + "ルールブック");
                    ruleBookMeta.setLore(Arrays.asList(ChatColor.GRAY + "サーバーのルールを確認できるよ。"));
                    ruleBookItem.setItemMeta(ruleBookMeta);
                }
                inv.setItem(13, ruleBookItem);
                ItemStack nextPageItem = new ItemStack(Material.ARROW);
                ItemMeta nextPageMeta = nextPageItem.getItemMeta();
                if (nextPageMeta != null) {
                    nextPageMeta.setDisplayName(ChatColor.GREEN + "次のページ");
                    nextPageItem.setItemMeta(nextPageMeta);
                }
                inv.setItem(26, nextPageItem);
            }
            case 2 -> {
                playerMenuActions.put(13, () -> {
                    player.closeInventory();
                    player.sendMessage("ここにあればいいなと思う機能があればDiscordで教えてね");
                });
                playerMenuActions.put(18, () -> {
                    setPage(player, Menu.menuInventoryKey, page - 1);
                    generalMenu(player, page - 1);
                });
                ItemStack anyItem = new ItemStack(Material.COOKIE);
                ItemMeta anyMeta = anyItem.getItemMeta();
                if (anyMeta != null) {
                    anyMeta.setDisplayName(ChatColor.GREEN + "未定");
                    anyItem.setItemMeta(anyMeta);
                }
                inv.setItem(13, anyItem);
                ItemStack prevPageItem = new ItemStack(Material.ARROW);
                ItemMeta prevPageMeta = prevPageItem.getItemMeta();
                if (prevPageMeta != null) {
                    prevPageMeta.setDisplayName(ChatColor.RED + "前のページ");
                    prevPageItem.setItemMeta(prevPageMeta);
                }
                inv.setItem(18, prevPageItem);
            }
        }
        this.menuActions.computeIfAbsent(player, _ -> new HashMap<>()).put(Menu.menuInventoryKey, playerMenuActions);
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
            playerMenuActions.put(0, () -> {
                resetPage(player, Menu.imageInventoryKey);
                generalMenu(player, 1);
            });
            if (page < totalPages) {
                ItemStack nextPageItem = new ItemStack(Material.ARROW);
                ItemMeta nextPageMeta = nextPageItem.getItemMeta();
                if (nextPageMeta != null) {
                    nextPageMeta.setDisplayName(ChatColor.GREEN + "次のページ");
                    nextPageItem.setItemMeta(nextPageMeta);
                }
                inv.setItem(53, nextPageItem);
                playerMenuActions.put(53, () -> {
                    setPage(player, Menu.imageInventoryKey, page + 1);
                    openImageMenu(player, page + 1);
                });
            }
            if (page > 1) {
                ItemStack prevPageItem = new ItemStack(Material.ARROW);
                ItemMeta prevPageMeta = prevPageItem.getItemMeta();
                if (prevPageMeta != null) {
                    prevPageMeta.setDisplayName(ChatColor.GREEN + "前のページ");
                    prevPageItem.setItemMeta(prevPageMeta);
                }
                inv.setItem(45, prevPageItem);
                playerMenuActions.put(45, () -> {
                    setPage(player, Menu.imageInventoryKey, page - 1);
                    openImageMenu(player, page - 1);
                });
            }
            //int slot = 0;
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
                        date = ((Date) imageInfo.get("date")).toString();
                    boolean fromDiscord = Optional.ofNullable(imageInfo.get("d"))
                        .map(value -> value instanceof Boolean ? (Boolean) value : (Integer) value != 0)
                        .orElse(false),
                        isQr = Optional.ofNullable(imageInfo.get("isqr"))
                            .map(value -> value instanceof Boolean ? (Boolean) value : (Integer) value != 0)
                            .orElse(false),
                        locked = imageInfo.get("otp") != null;
                        //isQr = imageInfo.get("isqr") != null && (imageInfo.get("isqr") instanceof Boolean ? (Boolean) imageInfo.get("isqr") : (Integer) imageInfo.get("isqr") != 0);
                    List<String> lores = new ArrayList<>();
                    lores.add(isQr ? "<QRコード>" : "<イメージマップ>");
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
                        lores.add(ChatColor.RED + "ロックされています");
                    }
                    ItemStack item = new ItemStack(Material.MAP);
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName(ChatColor.GREEN + title);
                        meta.setLore(lores);
                        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, imageUUID), PersistentDataType.STRING, "true");
                        item.setItemMeta(meta);
                    }
                    inv.addItem(item);
                    //inv.setItem(slot, item);
                    if (imageInfo.get("mapid") instanceof Integer mapId && thisServerImageInfo.containsKey(mapId) && server != null && server.equals(thisServer)) {
                        // そのサーバーで、データベースに保存されているmapIdをもつマップがあるとは限らない
                        // 
                        //Map<String, Object> thisServerImage = thisServerImageInfo.get(mapId);
                        playerMenuActions.put(inv.first(item), () -> {
                            im.giveMapToPlayer(player, mapId);
                        });
                    } else if (locked) {
                        playerMenuActions.put(inv.first(item), () -> {
                            player.closeInventory();
                            player.sendMessage(ChatColor.RED + "この画像はロックされています。\n"
                                + ChatColor.GRAY + "この画像を取得するには、/qコマンドにて、OTPを入力してください。");
                        });
                    } else {
                        playerMenuActions.put(inv.first(item), () -> {
                            player.closeInventory();
                            im.executeImageMapFromMenu(player, new Object[] {id, isQr, authorName, imageUUID, title, comment, ext, date});
                        });
                    }
                    //slot++;
                }
            }
            this.menuActions.computeIfAbsent(player, _ -> new HashMap<>()).put(Menu.imageInventoryKey, playerMenuActions);
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
        playerMenuActions.put(0, () -> {
            resetPage(player, Menu.onlineServerInventoryKey);
            openServerTypeInventory(player);
        });
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
                prevPageMeta.setDisplayName(ChatColor.RED + "前のページ");
                prevPageItem.setItemMeta(prevPageMeta);
            }
            inv.setItem(45, prevPageItem);
            playerMenuActions.put(45, () -> {
                setPage(player, Menu.onlineServerInventoryKey, page - 1);
                openOnlineServerInventory(player, page - 1);
            });
        }
        if (page < totalPages) {
            ItemStack nextPageItem = new ItemStack(Material.ARROW);
            ItemMeta nextPageMeta = nextPageItem.getItemMeta();
            if (nextPageMeta != null) {
                nextPageMeta.setDisplayName(ChatColor.GREEN + "次のページ");
                nextPageItem.setItemMeta(nextPageMeta);
            }
            inv.setItem(53, nextPageItem);
            playerMenuActions.put(53, () -> {
                setPage(player, Menu.onlineServerInventoryKey, page + 1);
                openOnlineServerInventory(player, page + 1);
            });
        }
        this.menuActions.computeIfAbsent(player, _ -> new HashMap<>()).put(Menu.onlineServerInventoryKey, playerMenuActions);
        //logger.info("menuActions: {}", menuActions);
        player.openInventory(inv);
        setPage(player, Menu.onlineServerInventoryKey, page);
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
            resetPage(player, serverType);
            openServerTypeInventory(player);
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
                prevPageMeta.setDisplayName(ChatColor.RED + "前のページ");
                prevPageItem.setItemMeta(prevPageMeta);
            }
            inv.setItem(45, prevPageItem);
            playerMenuActions.put(45, () -> {
                setPage(player, serverType, page - 1);
                openServerEachInventory(player, serverType, page - 1);
            });
        }
        if (page < totalPages) {
            ItemStack nextPageItem = new ItemStack(Material.ARROW);
            ItemMeta nextPageMeta = nextPageItem.getItemMeta();
            if (nextPageMeta != null) {
                nextPageMeta.setDisplayName(ChatColor.GREEN + "次のページ");
                nextPageItem.setItemMeta(nextPageMeta);
            }
            inv.setItem(53, nextPageItem);
            playerMenuActions.put(53, () -> {
                setPage(player, serverType, page + 1);
                openServerEachInventory(player, serverType, page + 1);
            });
        }
        this.menuActions.computeIfAbsent(player, _ -> new HashMap<>()).put(Menu.serverTypeInventoryKey, playerMenuActions);
        player.openInventory(inv);
        setPage(player, serverType, page);
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
                                if (isAllowedToEnter(serverName) || permLevel >= 2) {
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
                                        playerMenuActions.put(23, () -> enterServer(player, serverName));
                                    }
                                    inv.setItem(23, doorItem);
                                } else {
                                    ItemStack barrierItem = new ItemStack(Material.BARRIER);
                                    ItemMeta barrierMeta = barrierItem.getItemMeta();
                                    if (barrierMeta != null) {
                                        barrierMeta.setDisplayName(ChatColor.RED + "許可されていません。");
                                        barrierItem.setItemMeta(barrierMeta);
                                    }
                                    inv.setItem(23, barrierItem);
                                    inv.setItem(22, barrierItem);
                                }
                            } else {
                                ItemStack offlineItem = new ItemStack(Material.RED_WOOL);
                                ItemMeta offlineMeta = offlineItem.getItemMeta();
                                if (offlineMeta != null) {
                                    offlineMeta.setDisplayName(ChatColor.RED + "オフライン");
                                    offlineItem.setItemMeta(offlineMeta);
                                }
                                inv.setItem(8, offlineItem);
                                if (isAllowedToEnter(serverName) || permLevel >= 2) {
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
                                    inv.setItem(23, doorItem);
                                } else {
                                    ItemStack barrierItem = new ItemStack(Material.BARRIER);
                                    ItemMeta barrierMeta = barrierItem.getItemMeta();
                                    if (barrierMeta != null) {
                                        barrierMeta.setDisplayName(ChatColor.RED + "許可されていません。");
                                        barrierItem.setItemMeta(barrierMeta);
                                    }
                                    inv.setItem(22, barrierItem);
                                    inv.setItem(23, barrierItem);
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
                                        prevPageMeta.setDisplayName(ChatColor.RED + "前のページ");
                                        prevPageItem.setItemMeta(prevPageMeta);
                                    }
                                    inv.setItem(45, prevPageItem);
                                    playerMenuActions.put(45, () -> {
                                        setPage(player, serverName, page - 1);
                                        openServerInventory(player, serverName, page - 1);
                                    });
                                }
                                // ページ進むブロックを配置
                                if (page < totalPages) {
                                    ItemStack nextPageItem = new ItemStack(Material.ARROW);
                                    ItemMeta nextPageMeta = nextPageItem.getItemMeta();
                                    if (nextPageMeta != null) {
                                        nextPageMeta.setDisplayName(ChatColor.GREEN + "次のページ");
                                        nextPageItem.setItemMeta(nextPageMeta);
                                    }
                                    inv.setItem(53, nextPageItem);
                                    playerMenuActions.put(53, () -> {
                                        setPage(player, serverName, page + 1);
                                        openServerInventory(player, serverName, page + 1);
                                    });
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
        this.menuActions.computeIfAbsent(player, _ -> new HashMap<>()).put(serverInventoryKey, playerMenuActions);
        player.openInventory(inv);
    }

    public void openServerTypeInventory(Player player) {
        Map<Integer, Runnable> playerMenuActions = new HashMap<>();
        playerMenuActions.put(0, () -> generalMenu(player, 1));
        playerMenuActions.put(11, () -> openServerEachInventory(player, "life", 1));
        playerMenuActions.put(13, () -> openServerEachInventory(player, "distributed", 1));
        playerMenuActions.put(15, () -> openServerEachInventory(player, "mod", 1));
        playerMenuActions.put(18, () -> openOnlineServerInventory(player, 1));
        this.menuActions.computeIfAbsent(player, _ -> new HashMap<>()).put(Menu.serverTypeInventoryKey, playerMenuActions);
        Inventory inv = Bukkit.createInventory(null, 27, "server type");
        ItemStack backItem = new ItemStack(Material.STICK);
        ItemMeta backMeta = backItem.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.GOLD + "戻る");
            backItem.setItemMeta(backMeta);
        }
        inv.setItem(0, backItem);
        ItemStack lifeServerItem = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta lifeMeta = lifeServerItem.getItemMeta();
        if (lifeMeta != null) {
            lifeMeta.setDisplayName(ChatColor.GREEN + "生活鯖");
            lifeServerItem.setItemMeta(lifeMeta);
        }
        inv.setItem(11, lifeServerItem);
        ItemStack distributionServerItem = new ItemStack(Material.CHEST);
        ItemMeta distributionMeta = distributionServerItem.getItemMeta();
        if (distributionMeta != null) {
            distributionMeta.setDisplayName(ChatColor.YELLOW + "配布鯖");
            distributionServerItem.setItemMeta(distributionMeta);
        }
        inv.setItem(13, distributionServerItem);
        ItemStack modServerItem = new ItemStack(Material.IRON_BLOCK);
        ItemMeta modMeta = modServerItem.getItemMeta();
        if (modMeta != null) {
            modMeta.setDisplayName(ChatColor.BLUE + "モッド鯖");
            modServerItem.setItemMeta(modMeta);
        }
        inv.setItem(15, modServerItem);
        // 現在オンラインのサーバーを表示する
        ItemStack onlineServerItem = new ItemStack(Material.GREEN_WOOL);
        ItemMeta onlineMeta = onlineServerItem.getItemMeta();
        if (onlineMeta != null) {
            onlineMeta.setDisplayName(ChatColor.GREEN + "オンラインサーバー");
            onlineServerItem.setItemMeta(onlineMeta);
        }
        inv.setItem(18, onlineServerItem);
        player.openInventory(inv);
    }

    public void enterServer(Player player, String serverName) {
        String playerName = player.getName();
        player.closeInventory();
        player.performCommand("fmc fv " + playerName + " fmcp stp " + serverName);
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
        String playerName = player.getName();
        int permLevel = lp.getPermLevel(player.getName());
        if (checkServerOnline(serverName)) {
            if (permLevel >= 2) {
                player.performCommand("fmc fv " + playerName + " fmcp stop " + serverName);
                player.closeInventory();
            }
        } else {
            if (permLevel == 1) {
                player.performCommand("fmc fv " + playerName + " fmcp req " + serverName);
            } else if (permLevel >= 2) {
                player.performCommand("fmc fv " + playerName + " fmcp start " + serverName);
            }
            player.closeInventory();
        }
    }

    public boolean isAllowedToEnter(String serverName) {
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
    
    public void resetPage(Player player, String serverType) {
        Map<String, Integer> inventoryMap = playerOpenningInventoryMap.get(player);
        if (inventoryMap != null) {
            inventoryMap.entrySet().removeIf(entry -> entry.getKey().equals(serverType));
        }
    }

    public void setPage(Player player, String serverType, int page) {
        Map<String, Integer> inventoryMap = playerOpenningInventoryMap.get(player);
        if (inventoryMap == null) {
            inventoryMap = new HashMap<>();
            playerOpenningInventoryMap.put(player, inventoryMap);
        }
        inventoryMap.put(serverType, page);
    }
    
    public int getPage(Player player, String serverType) {
        Map<String, Integer> inventoryMap = playerOpenningInventoryMap.get(player);
        if (inventoryMap == null) {
            inventoryMap = new HashMap<>();
            playerOpenningInventoryMap.put(player, inventoryMap);
        }
        return inventoryMap.getOrDefault(serverType, 1);
    }
}
