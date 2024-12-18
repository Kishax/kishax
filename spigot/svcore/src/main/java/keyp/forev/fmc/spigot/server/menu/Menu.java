package keyp.forev.fmc.spigot.server.menu;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import keyp.forev.fmc.common.database.Database;
import keyp.forev.fmc.common.server.Luckperms;
import keyp.forev.fmc.common.server.ServerStatusCache;
import keyp.forev.fmc.common.util.JavaUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatColor;
import keyp.forev.fmc.spigot.server.ImageMap;
import keyp.forev.fmc.spigot.server.cmd.sub.Book;
import keyp.forev.fmc.spigot.server.cmd.sub.CommandForward;
import keyp.forev.fmc.spigot.server.cmd.sub.teleport.TeleportRequest;
import keyp.forev.fmc.spigot.server.events.EventListener;
import keyp.forev.fmc.spigot.server.menu.interfaces.MenuEventRunnable;
import keyp.forev.fmc.spigot.server.menu.interfaces.PlayerRunnable;
import keyp.forev.fmc.spigot.server.textcomponent.TCUtils;
import keyp.forev.fmc.common.server.interfaces.ServerHomeDir;
import keyp.forev.fmc.common.socket.SocketSwitch;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

@Singleton
public class Menu {
    public static final Map<Player, Map<Type, Map<Integer, MenuEventRunnable>>> menuEventActions = new ConcurrentHashMap<>();
    public static final Map<Player, Map<Type, AtomicBoolean>> menuEventFlags = new ConcurrentHashMap<>();
    public static final int[] SLOT_POSITIONS = {11, 13, 15, 29, 31, 33};
    public static final int[] FACE_POSITIONS = {46, 47, 48, 49, 50, 51, 52};
    private final List<Material> ORE_BLOCKS = Arrays.asList(
        Material.NETHERITE_BLOCK, Material.GOLD_BLOCK, Material.REDSTONE_BLOCK, 
        Material.EMERALD_BLOCK, Material.DIAMOND_BLOCK, Material.IRON_BLOCK,
        Material.COAL_BLOCK, Material.LAPIS_BLOCK, Material.QUARTZ_BLOCK,
        Material.COPPER_BLOCK
    );
    private final JavaPlugin plugin;
    private final Logger logger;
    private final Database db;
    private final ServerStatusCache ssc;
    private final Luckperms lp;
    private final ImageMap im;
    private final ServerHomeDir shd;
    private final Book book;
    private final CommandForward cf;
    private final Provider<SocketSwitch> sswProvider;
    private int currentOreIndex = 0;

	@Inject
	public Menu(JavaPlugin plugin, Logger logger, Database db, ServerStatusCache ssc, Luckperms lp, ImageMap im, ServerHomeDir shd, Book book, CommandForward cf, Provider<SocketSwitch> sswProvider) {  
		this.plugin = plugin;
        this.logger = logger;
        this.db = db;
        this.ssc = ssc;
        this.lp = lp;
        this.im = im;
        this.shd = shd;
        this.book = book;
        this.cf = cf;
        this.sswProvider = sswProvider;
	}

    private final Map<Type, PlayerRunnable> shortCutMap = Map.ofEntries(
        new SimpleEntry<>(Type.GENERAL, this::generalMenu),
        new SimpleEntry<>(Type.IMAGE_MAP_LIST, this::imageMapMenu),
        new SimpleEntry<>(Type.ONLINE_SERVER, this::onlineServerMenu),
        new SimpleEntry<>(Type.PLAYER_TELEPORT, this::playerTeleportMenu),
        new SimpleEntry<>(Type.SETTING, this::settingMenu),
        new SimpleEntry<>(Type.TELEPORT, this::teleportMenu),
        new SimpleEntry<>(Type.TELEPORT_REQUEST, this::teleportRequestMenu),
        new SimpleEntry<>(Type.TELEPORT_REQUEST_ME, this::teleportMeRequestMenu),
        new SimpleEntry<>(Type.TELEPORT_POINT_PRIVATE, this::teleportPointPrivateHandler),
        new SimpleEntry<>(Type.TELEPORT_POINT_PUBLIC, this::teleportPointPublicHandler),
        new SimpleEntry<>(Type.TELEPORT_POINT_TYPE, this::teleportPointTypeMenu),
        new SimpleEntry<>(Type.TELEPORT_NV_PLAYER, this::faceIconNaviMenu)
    );

    public void shortCutMenu(Player player) {
        shortCutMenu(player, 1);
    }

    public Map<Type, PlayerRunnable> getShortCutMap() {
        return shortCutMap;
    }
    
    @Deprecated
    private void shortCutMenu(Player player, int page) {
        Map<Integer, MenuEventRunnable> playerMenuActions = new HashMap<>();
        Inventory inv = Bukkit.createInventory(null, 27, Type.SHORTCUT.get());
        playerMenuActions.put(0, (event) -> generalMenu(player));
        
        int inventorySize = 27;
        int usingSlots = page == 1 ? 2 : 3; // 戻るボタンやページネーションボタンに使用するスロット数
        int itemsPerPage = inventorySize - usingSlots; // 各ページに表示するアイテムの数
        
        Map<Type, PlayerRunnable> thisShortCutMap = shortCutMap.entrySet().stream()
            .filter(entry -> !entry.getKey().equals(Type.GENERAL))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        int totalItems = thisShortCutMap.size();
        int totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);
        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, totalItems);
        
        ItemStack backItem = new ItemStack(Material.STICK);
        ItemMeta backMeta = backItem.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.GOLD + "戻る");
            backItem.setItemMeta(backMeta);
        }
        inv.setItem(0, backItem);

        if (page < totalPages) {
            ItemStack nextPageItem = new ItemStack(Material.ARROW);
            ItemMeta nextPageMeta = nextPageItem.getItemMeta();
            if (nextPageMeta != null) {
                nextPageMeta.setDisplayName(ChatColor.GOLD + "次のページ");
                nextPageItem.setItemMeta(nextPageMeta);
            }
            inv.setItem(53, nextPageItem);
            playerMenuActions.put(53, (event) -> shortCutMenu(player, page + 1));
        }
        if (page > 1) {
            ItemStack prevPageItem = new ItemStack(Material.ARROW);
            ItemMeta prevPageMeta = prevPageItem.getItemMeta();
            if (prevPageMeta != null) {
                prevPageMeta.setDisplayName(ChatColor.GOLD + "前のページ");
                prevPageItem.setItemMeta(prevPageMeta);
            }
            inv.setItem(45, prevPageItem);
            playerMenuActions.put(45, (event) -> shortCutMenu(player, page - 1));
        }

        Iterator<Map.Entry<Type, PlayerRunnable>> iterator = thisShortCutMap.entrySet().iterator();

        int i = 0;
        while (iterator.hasNext()) {
            Map.Entry<Type, PlayerRunnable> entry = iterator.next();

            if (i >= startIndex && i < endIndex) {
                Type type = entry.getKey();
                String typeName = type.get();
                String typePersistantKey = type.getPersistantKey();

                ItemStack item = new ItemStack(type.getMaterial());
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(type.get());
                    meta.getPersistentDataContainer().set(new NamespacedKey(plugin, typePersistantKey), PersistentDataType.STRING, "true");
                    item.setItemMeta(meta);
                }
                inv.addItem(item);

                playerMenuActions.put(inv.first(item), (event) -> {
                    player.closeInventory();
                    HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(item);

                    Component message = Component.text("ショートカットアイテム(to: " + typeName + ")を渡しました。")
                        .color(NamedTextColor.GRAY)
                        .decorate(TextDecoration.ITALIC);
                    
                    player.sendMessage(message);

                    if (!remaining.isEmpty()) {
                        World world = player.getWorld();
                        Location playerLocation = player.getLocation();
                        Block block = playerLocation.getBlock();

                        if (block.getType() != Material.AIR) {
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                Component message2 = Component.text("インベントリに入り切らないマップは、ドロップしました。")
                                    .color(NamedTextColor.GREEN)
                                    .decorate(TextDecoration.BOLD);
                                
                                player.sendMessage(message2);

                                remaining.forEach((key, value) -> {
                                    world.dropItemNaturally(playerLocation, value);
                                });
                            });
                        } else {
                            Component message2 = Component.text("空中で実行しないでください！")
                                .color(NamedTextColor.RED)
                                .decorate(TextDecoration.BOLD);

                            player.sendMessage(message2);
                        }
                    }
                });
            }
            i++;
        }

        Menu.menuEventActions.computeIfAbsent(player, _p -> new HashMap<>()).put(Type.SHORTCUT, playerMenuActions);
        player.openInventory(inv);
    }

    public void teleportPointPublicHandler(Player player) {
        teleportPointMenu(player, Type.TELEPORT_POINT_PUBLIC);
    }

    public void teleportPointPrivateHandler(Player player) {
        teleportPointMenu(player, Type.TELEPORT_POINT_PRIVATE);
    }

    public void runMenuEventAction(Player player, Type menuType, int slot, InventoryClickEvent event) {
        Map<Type, Map<Integer, MenuEventRunnable>> playerMenuActions = getPlayerMenuEventActions(player);
        if (playerMenuActions != null) {
            // コレクションをコピーしてから反復処理を行う
            Map<Type, Map<Integer, MenuEventRunnable>> copiedMenuActions = new HashMap<>(playerMenuActions);
            copiedMenuActions.entrySet().stream()
                .filter(entry -> entry.getKey().equals(menuType))
                .map(Map.Entry::getValue)
                .filter(actions -> actions.containsKey(slot))
                .map(actions -> actions.get(slot))
                .forEach(action -> action.run(event));
        }
    }

    public Map<Type, Map<Integer, MenuEventRunnable>> getPlayerMenuEventActions(Player player) {
        return Menu.menuEventActions.get(player);
    }

    public void settingMenu(Player player) {
        settingMenu(player, 1);
    }

    public void generalMenu(Player player) {
        generalMenu(player, 1);
    }

    @Deprecated
    private void generalMenu(Player player, int page) {
        Map<Integer, MenuEventRunnable> playerMenuActions = new HashMap<>();
        Inventory inv = Bukkit.createInventory(null, 27, Type.GENERAL.get());
        switch (page) {
            case 1 -> {
                playerMenuActions.put(11, (event) -> teleportMenu(player));
                playerMenuActions.put(13, (event) -> serverTypeMenu(player));
                playerMenuActions.put(15, (event) -> imageMapMenu(player));
                playerMenuActions.put(18, (event) -> shortCutMenu(player));
                playerMenuActions.put(26, (event) -> generalMenu(player, page + 1));

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

                ItemStack shortCutItem = new ItemStack(Material.LEAD);
                ItemMeta shortCutMeta = shortCutItem.getItemMeta();
                if (shortCutMeta != null) {
                    shortCutMeta.setDisplayName(ChatColor.GREEN + "ショートカットメニュー");
                    shortCutMeta.setLore(Arrays.asList(ChatColor.GRAY + "特定のメニューまでのショートカットアイテムをゲットできるよ。"));
                    shortCutItem.setItemMeta(shortCutMeta);
                }
                inv.setItem(18, shortCutItem);

                ItemStack nextPageItem = new ItemStack(Material.ARROW);
                ItemMeta nextPageMeta = nextPageItem.getItemMeta();
                if (nextPageMeta != null) {
                    nextPageMeta.setDisplayName(ChatColor.GOLD + "次のページ");
                    nextPageItem.setItemMeta(nextPageMeta);
                }
                inv.setItem(26, nextPageItem);
            }
            case 2 -> {
                playerMenuActions.put(11, (event) -> settingMenu(player));
                playerMenuActions.put(13, (event) -> book.giveRuleBook(player));
                playerMenuActions.put(15, (event) -> {
                    player.closeInventory();
                    player.sendMessage(ChatColor.GREEN + "ここにあればいいなと思う機能があればDiscordで教えてね");
                });
                playerMenuActions.put(18, (event) -> generalMenu(player, page - 1));

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
        Menu.menuEventActions.computeIfAbsent(player, _p -> new HashMap<>()).put(Type.GENERAL, playerMenuActions);
        player.openInventory(inv);
    }

    public void teleportPointTypeMenu(Player player) {
        teleportPointTypeMenu(player, 1);
    }

    @Deprecated
    private void teleportPointTypeMenu(Player player, int page) {
        Map<Integer, MenuEventRunnable> playerMenuActions = new HashMap<>();
        Inventory inv = Bukkit.createInventory(null, 27, Type.TELEPORT_POINT_TYPE.get());
        playerMenuActions.put(0, (event) -> teleportMenu(player));
        playerMenuActions.put(11, (event) -> teleportPointMenu(player, Type.TELEPORT_POINT_PRIVATE));
        playerMenuActions.put(13, (event) -> {
            player.closeInventory();
            player.performCommand("registerpoint");
        });
        playerMenuActions.put(15, (event) -> teleportPointMenu(player, Type.TELEPORT_POINT_PUBLIC));

        ItemStack backItem = new ItemStack(Material.STICK);
        ItemMeta backMeta = backItem.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.GOLD + "戻る");
            backItem.setItemMeta(backMeta);
        }
        inv.setItem(0, backItem);

        ItemStack privatePointItem = new ItemStack(Material.OBSERVER);
        ItemMeta privatePointMeta = privatePointItem.getItemMeta();
        if (privatePointMeta != null) {
            privatePointMeta.setDisplayName(ChatColor.GREEN + "プライベートポイント");
            privatePointMeta.setLore(new ArrayList<>(
                Arrays.asList(ChatColor.GRAY + "自分だけのポイントを確認できるよ。"))
                );
            privatePointItem.setItemMeta(privatePointMeta);
        }
        inv.setItem(11, privatePointItem);

        ItemStack newPointItem = new ItemStack(Material.REDSTONE_TORCH);
        ItemMeta newPointMeta = newPointItem.getItemMeta();
        if (newPointMeta != null) {
            newPointMeta.setDisplayName(ChatColor.GREEN + "ポイントセット");
            newPointMeta.setLore(new ArrayList<>(
                Arrays.asList(ChatColor.GRAY + "現在地を新規ポイントとして登録できるよ。"))
                );
            newPointItem.setItemMeta(newPointMeta);
        }
        inv.setItem(13, newPointItem);

        ItemStack publicPointItem = new ItemStack(Material.DISPENSER);
        ItemMeta publicPointMeta = publicPointItem.getItemMeta();
        if (publicPointMeta != null) {
            publicPointMeta.setDisplayName(ChatColor.GREEN + "パブリックポイント");
            publicPointMeta.setLore(new ArrayList<>(
                Arrays.asList(ChatColor.GRAY + "共有のポイントを確認できるよ。"))
                );
            publicPointItem.setItemMeta(publicPointMeta);
        }
        inv.setItem(15, publicPointItem);
        Menu.menuEventActions.computeIfAbsent(player, _p -> new HashMap<>()).put(Type.TELEPORT_POINT_TYPE, playerMenuActions);
        player.openInventory(inv);
    }

    public void faceIconNaviMenu(Player player) {
        faceIconNaviMenu(player, 1);
    }

    @Deprecated
    private void faceIconNaviMenu(Player player, int page) {
        int inventorySize = 27;
        int usingSlots = page == 1 ? 2 : 3; // 戻るボタンやページネーションボタンに使用するスロット数
        int itemsPerPage = inventorySize - usingSlots; // 各ページに表示するアイテムの数
        Inventory inv = Bukkit.createInventory(null, inventorySize, Type.TELEPORT_NV_PLAYER.get());

        Map<Integer, MenuEventRunnable> playerMenuActions = new HashMap<>();

        playerMenuActions.put(0, (event) -> teleportMenu(player));

        ItemStack backItem = new ItemStack(Material.STICK);
        ItemMeta backMeta = backItem.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.GOLD + "戻る");
            backItem.setItemMeta(backMeta);
        }
        inv.setItem(0, backItem);

        Collection<? extends Player> onlinePlayersCollection = Bukkit.getOnlinePlayers();
        List<Player> onlinePlayers = onlinePlayersCollection.stream()
            .filter(entry -> entry != player)
            .collect(Collectors.toList());
            
        if (onlinePlayers.size() == 0) {
            ItemStack noPlayerItem = new ItemStack(Material.BARRIER);
            ItemMeta noPlayerMeta = noPlayerItem.getItemMeta();
            if (noPlayerMeta != null) {
                noPlayerMeta.setDisplayName(ChatColor.RED + "オンラインプレイヤーが見つかりません。");
                noPlayerItem.setItemMeta(noPlayerMeta);
            }
            inv.setItem(13, noPlayerItem);
            Menu.menuEventActions.computeIfAbsent(player, _p -> new HashMap<>()).put(Type.TELEPORT_NV_PLAYER, playerMenuActions);
            player.openInventory(inv);
            return;
        }
        
        int totalItems = onlinePlayers.size();
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
            inv.setItem(53, nextPageItem);
            playerMenuActions.put(53, (event) -> faceIconNaviMenu(player, page + 1));
        }
        if (page > 1) {
            ItemStack prevPageItem = new ItemStack(Material.ARROW);
            ItemMeta prevPageMeta = prevPageItem.getItemMeta();
            if (prevPageMeta != null) {
                prevPageMeta.setDisplayName(ChatColor.GOLD + "前のページ");
                prevPageItem.setItemMeta(prevPageMeta);
            }
            inv.setItem(45, prevPageItem);
            playerMenuActions.put(45, (event) -> faceIconNaviMenu(player, page - 1));
        }

        for (int i = startIndex; i < endIndex; i++) {
            Player onlinePlayer = onlinePlayers.get(i);
            String onlinePlayerName = onlinePlayer.getName();
            UUID onlinePlayerUUID = onlinePlayer.getUniqueId();
            ItemStack playerItem = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta playerMeta = (SkullMeta) playerItem.getItemMeta();
            if (playerMeta != null) {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(onlinePlayerUUID);
                playerMeta.setOwningPlayer(offlinePlayer);
                playerMeta.setDisplayName(ChatColor.GREEN + onlinePlayerName.trim());
                playerItem.setItemMeta(playerMeta);
            }

            inv.addItem(playerItem);
            playerMenuActions.put(inv.first(playerItem), (event) -> {
                player.closeInventory();
                player.performCommand("navplayer " + onlinePlayerName);
            });
        }

        Menu.menuEventActions.computeIfAbsent(player, _p -> new HashMap<>()).put(Type.TELEPORT_NV_PLAYER, playerMenuActions);
        player.openInventory(inv);
    }

    @Deprecated
    public void teleportMenu(Player player) {
        Map<Integer, MenuEventRunnable> playerMenuActions = new HashMap<>();
        Inventory inv = Bukkit.createInventory(null, 27, Type.TELEPORT.get());

        playerMenuActions.put(0, (event) -> generalMenu(player));
        playerMenuActions.put(11, (event) -> teleportPointTypeMenu(player));
        playerMenuActions.put(13, (event) -> playerTeleportMenu(player));
        playerMenuActions.put(15, (event) -> faceIconNaviMenu(player));

        ItemStack backItem = new ItemStack(Material.STICK);
        ItemMeta backMeta = backItem.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.GOLD + "戻る");
            backItem.setItemMeta(backMeta);
        }
        inv.setItem(0, backItem);

        ItemStack teleportPointItem = new ItemStack(Material.LIGHTNING_ROD);
        ItemMeta teleportPointMeta = teleportPointItem.getItemMeta();
        if (teleportPointMeta != null) {
            teleportPointMeta.setDisplayName(ChatColor.GREEN + "ポイントテレポート");
            teleportPointMeta.setLore(new ArrayList<>(Arrays.asList(
                "設定されたポイントに飛べる！"
            )));
            teleportPointItem.setItemMeta(teleportPointMeta);
        }
        inv.setItem(11, teleportPointItem);

        ItemStack playerTeleportItem = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta playerTeleportMeta = (SkullMeta) playerTeleportItem.getItemMeta();
        if (playerTeleportMeta != null) {
            playerTeleportMeta.setDisplayName(ChatColor.GREEN + "プレイヤーテレポート");
            playerTeleportMeta.setLore(new ArrayList<>(Arrays.asList(
                "プレイヤーにテレポートできる！"
            )));
            playerTeleportItem.setItemMeta(playerTeleportMeta);
        }
        inv.setItem(13, playerTeleportItem);

        ItemStack playerNaviItem = new ItemStack(Material.ENDER_EYE);
        ItemMeta playerNaviMeta = playerNaviItem.getItemMeta();
        if (playerNaviMeta != null) {
            playerNaviMeta.setDisplayName(ChatColor.GREEN + "プレイヤーナビ");
            List<String> lores = new ArrayList<>();
            lores.add("指定のプレイヤーまでナビゲートしてくれる！");
            playerNaviMeta.setLore(lores);
            playerNaviItem.setItemMeta(playerNaviMeta);
        }
        inv.setItem(15, playerNaviItem);

        Menu.menuEventActions.computeIfAbsent(player, _p -> new HashMap<>()).put(Type.TELEPORT, playerMenuActions);
        player.openInventory(inv);
    }

    public void teleportResponseHeadMenu(Player player) {
        teleportResponseHeadMenu(player, 1);
    }
    
    @Deprecated
    private void teleportResponseHeadMenu(Player player, int page) {
        int inventorySize = 27;
        int usingSlots = page == 1 ? 2 : 3; // 戻るボタンやページネーションボタンに使用するスロット数
        int itemsPerPage = inventorySize - usingSlots; // 各ページに表示するアイテムの数
        Map<Integer, MenuEventRunnable> playerMenuActions = new HashMap<>();
        Inventory inv = Bukkit.createInventory(null, inventorySize, Type.TELEPORT_RESPONSE_HEAD.get());
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
                playerMenuActions.put(26, (event) -> teleportResponseHeadMenu(player, page + 1));
            }
            if (page > 1) {
                ItemStack prevPageItem = new ItemStack(Material.ARROW);
                ItemMeta prevPageMeta = prevPageItem.getItemMeta();
                if (prevPageMeta != null) {
                    prevPageMeta.setDisplayName(ChatColor.GOLD + "前のページ");
                    prevPageItem.setItemMeta(prevPageMeta);
                }
                inv.setItem(18, prevPageItem);
                playerMenuActions.put(18, (event) -> teleportResponseHeadMenu(player, page - 1));
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
                playerMenuActions.put(inv.first(playerItem), (event) -> teleportResponseMenu(targetPlayer, player));
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
        playerMenuActions.put(0, (event) -> playerTeleportMenu(player));
        Menu.menuEventActions.computeIfAbsent(player, _p -> new HashMap<>()).put(Type.TELEPORT_RESPONSE_HEAD, playerMenuActions);
        player.openInventory(inv);
    }

    public void teleportResponseMenu(Player player, Player targetPlayer) {
        teleportResponseMenu(player, targetPlayer, false);
    }

    public void teleportMeResponseMenu(Player player, Player targetPlayer) {
        teleportResponseMenu(player, targetPlayer, true);
    }

    @Deprecated
    public void changeMaterial(Player player, int id) {
        Menu.menuEventFlags.computeIfAbsent(player, (key) -> new HashMap<>()).put(Type.CHANGE_MATERIAL, new AtomicBoolean(false));

        Map<Integer, MenuEventRunnable> playerMenuActions = new HashMap<>();

        playerMenuActions.put(0, (event) -> teleportPointManager(player, id));

        final int invSize = 27;
        final int setSlot = 13;
        Inventory inv = Bukkit.createInventory(null, invSize, Type.CHANGE_MATERIAL.get());

        try (Connection conn = db.getConnection()) {
            try (PreparedStatement ps1 = conn.prepareStatement("SELECT * FROM tp_points WHERE id = " + id + ";");
                ResultSet rs = ps1.executeQuery()) {
                if (rs.next()) {
                    final String defaultMaterialName = rs.getString("material");
                    final String typeName = rs.getString("type");
                    Optional<Type> optionalType = Type.search(typeName);
                    if (optionalType.isPresent()) {
                        Type type = optionalType.get();
                        for (int i = 1; i <= invSize -1; i++) {
                            if (i == setSlot) {
                                // 何も置かない。プレイヤーがアイテムを置くのを待つ
                                playerMenuActions.put(i, (event) -> {
                                    ItemStack currentItem = event.getCurrentItem();
                                    
                                    if (currentItem == null) {
                                        return;
                                    }
    
                                    boolean isEnchanted = EnchantmentUtils.isEnchanted(currentItem);
                                    Material material = currentItem.getType();
    
                                    String newMaterialName = material.name();
    
                                    try (Connection connection = db.getConnection()) {
                                        db.updateLog(connection, "UPDATE tp_points SET `material`=?, `enchanted`=? WHERE `id`=?", new Object[] {newMaterialName, isEnchanted, id});
    
                                        event.setCancelled(true);
    
                                        Component success = Component.text("アイテムタイプを変更しました。")
                                            .color(NamedTextColor.GREEN)
                                            .decorate(TextDecoration.BOLD);
    
                                        Component changeContent = Component.text(defaultMaterialName + " -> " + newMaterialName)
                                            .color(NamedTextColor.GRAY)
                                            .decorate(TextDecoration.ITALIC);
    
                                        Component back = Component.text("※もとのメニューに戻ります。")
                                            .color(NamedTextColor.GRAY)
                                            .decorate(TextDecoration.ITALIC);
    
                                        TextComponent messages = Component.text()
                                            .append(changeContent)
                                            .appendNewline()
                                            .append(success)
                                            .appendNewline()
                                            .append(TCUtils.LATER_OPEN_INV_3.get())
                                            .appendNewline()
                                            .append(back)
                                            .build();
    
                                        new BukkitRunnable() {
                                            @Override
                                            public void run() {
                                                teleportPointMenu(player, type);
                                            }
                                        }.runTaskLater(plugin, 20 * 3);
    
                                        player.sendMessage(messages);
                                    } catch (SQLException | ClassNotFoundException e) {
                                        player.closeInventory();
                                        player.sendMessage(ChatColor.RED + "データベースとの通信にエラーが発生しました。");
                                        logger.error("An error occurred in Menu#changeMaterial method: {}", e);
                                    }
    
                                    Menu.menuEventFlags.get(player).get(Type.CHANGE_MATERIAL).set(true);
                                    player.closeInventory();
                                    player.getInventory().addItem(currentItem);
                                });
                                continue;
                            }
    
                            ItemStack glassItem = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
                            ItemMeta glassMeta = glassItem.getItemMeta();
                            if (glassMeta != null) {
                                glassMeta.setDisplayName(ChatColor.GREEN + "アイテムタイプの変更");
                                glassMeta.setLore(new ArrayList<>(
                                    Arrays.asList(
                                        "真ん中の空いているスロットに",
                                        "アイテムをセットしよう。",
                                        "最後にそれをクリック。",
                                        "※アイテムは返却されます。"
                                    )
                                ));
                                glassItem.setItemMeta(glassMeta);
                            }
                            inv.setItem(i, glassItem);
                            playerMenuActions.put(i, (event) -> event.setCancelled(true));
                        }
                    }
                }
            }
        } catch (SQLException | ClassNotFoundException e) {
            player.closeInventory();
            player.sendMessage(ChatColor.RED + "データベースとの通信にエラーが発生しました。");
            logger.error("An error occurred in Menu#changeMaterial method: {}", e);
            return;
        }

        ItemStack backItem = new ItemStack(Material.STICK);
        ItemMeta backMeta = backItem.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.GOLD + "戻る");
            backItem.setItemMeta(backMeta);
        }
        inv.setItem(0, backItem);

        Menu.menuEventActions.computeIfAbsent(player, _p -> new HashMap<>()).put(Type.CHANGE_MATERIAL, playerMenuActions);
        player.openInventory(inv);
    }

    public void teleportRequestMenu(Player player) {
        teleportRequestMenu(player, 1, false);
    }

    public void teleportMeRequestMenu(Player player) {
        teleportRequestMenu(player, 1, true);
    }

    public void playerTeleportMenu(Player player) {
        playerTeleportMenu(player, 1);
    }

    @Deprecated
    private void playerTeleportMenu(Player player, int page) {
        Map<Integer, MenuEventRunnable> playerMenuActions = new HashMap<>();
        Inventory inv = Bukkit.createInventory(null, 27, Type.PLAYER_TELEPORT.get());
        switch (page) {
            case 1 -> {
                playerMenuActions.put(0, (event) -> teleportMenu(player));
                playerMenuActions.put(11, (event) -> teleportRequestMenu(player));
                playerMenuActions.put(13, (event) -> teleportMeRequestMenu(player));
                playerMenuActions.put(15, (event) -> teleportResponseHeadMenu(player));
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
        Menu.menuEventActions.computeIfAbsent(player, _p -> new HashMap<>()).put(Type.PLAYER_TELEPORT, playerMenuActions);
        player.openInventory(inv);
    }

    @Deprecated
    public void imageMapMenu(Player player) {
        Map<Integer, MenuEventRunnable> playerMenuActions = new HashMap<>();
        Inventory inv = Bukkit.createInventory(null, 27, Type.IMAGE_MAP.get());
        playerMenuActions.put(0, (event) -> generalMenu(player));
        playerMenuActions.put(11, (event) -> {
            player.closeInventory();
            player.performCommand("registerimagemap");
        });
        playerMenuActions.put(15, (event) -> imageMapListMenu(player));

        ItemStack backItem = new ItemStack(Material.STICK);
        ItemMeta backMeta = backItem.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.GOLD + "戻る");
            backItem.setItemMeta(backMeta);
        }
        inv.setItem(0, backItem);

        ItemStack registerImageItem = new ItemStack(Material.CARTOGRAPHY_TABLE);
        ItemMeta registerImageMeta = registerImageItem.getItemMeta();
        if (registerImageMeta != null) {
            registerImageMeta.setDisplayName(ChatColor.GREEN + "イメージマップの登録");
            registerImageMeta.setLore(new ArrayList<>(
                Arrays.asList("画像URLよりイメージマップを登録できるよ。"))
                );
                registerImageItem.setItemMeta(registerImageMeta);
        }
        inv.setItem(11, registerImageItem);

        ItemStack imageMapListItem = new ItemStack(Material.MAP);
        ItemMeta imageMapListMeta = imageMapListItem.getItemMeta();
        if (imageMapListMeta != null) {
            imageMapListMeta.setDisplayName(ChatColor.GREEN + "イメージマップリスト");
            imageMapListMeta.setLore(new ArrayList<>(
                Arrays.asList("今までに登録されたイメージマップが見れる！"))
                );
                imageMapListItem.setItemMeta(imageMapListMeta);
        }
        inv.setItem(15, imageMapListItem);

        Menu.menuEventActions.computeIfAbsent(player, _p -> new HashMap<>()).put(Type.IMAGE_MAP, playerMenuActions);
        player.openInventory(inv);
    }

    public void imageMapListMenu(Player player) {
        imageMapListMenu(player, 1);
    }

    @Deprecated
    private void imageMapListMenu(Player player, int page) {
        int inventorySize = 54;
        int usingSlots = page == 1 ? 2 : 3; // 戻るボタンやページネーションボタンに使用するスロット数
        int itemsPerPage = inventorySize - usingSlots; // 各ページに表示するアイテムの数
        Inventory inv = Bukkit.createInventory(null, inventorySize, Type.IMAGE_MAP_LIST.get());

        Map<Integer, MenuEventRunnable> playerMenuActions = new HashMap<>();
        playerMenuActions.put(0, (event) -> imageMapMenu(player));

        try (Connection conn = db.getConnection()) {
            Map<Integer, Map<String, Object>> thisServerImageInfo = im.getThisServerImages(conn);
            Map<Integer, Map<String, Object>> imageMap = im.getImageMap(conn);

            int totalItems = imageMap.size();
            int totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);
            int startIndex = (page - 1) * itemsPerPage;
            int endIndex = Math.min(startIndex + itemsPerPage, totalItems);

            ItemStack backItem = new ItemStack(Material.STICK);
            ItemMeta backMeta = backItem.getItemMeta();
            if (backMeta != null) {
                backMeta.setDisplayName(ChatColor.GOLD + "戻る");
                backItem.setItemMeta(backMeta);
            }
            inv.setItem(0, backItem);
            
            if (page < totalPages) {
                ItemStack nextPageItem = new ItemStack(Material.ARROW);
                ItemMeta nextPageMeta = nextPageItem.getItemMeta();
                if (nextPageMeta != null) {
                    nextPageMeta.setDisplayName(ChatColor.GOLD + "次のページ");
                    nextPageItem.setItemMeta(nextPageMeta);
                }
                inv.setItem(53, nextPageItem);
                playerMenuActions.put(53, (event) -> imageMapListMenu(player, page + 1));
            }
            if (page > 1) {
                ItemStack prevPageItem = new ItemStack(Material.ARROW);
                ItemMeta prevPageMeta = prevPageItem.getItemMeta();
                if (prevPageMeta != null) {
                    prevPageMeta.setDisplayName(ChatColor.GOLD + "前のページ");
                    prevPageItem.setItemMeta(prevPageMeta);
                }
                inv.setItem(45, prevPageItem);
                playerMenuActions.put(45, (event) -> imageMapListMenu(player, page - 1));
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
                    lores.add("created at " + date.replace("-", "/"));
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
                        playerMenuActions.put(inv.first(item), (event) -> {
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
                            playerMenuActions.put(inv.first(item), (event) -> {
                                //logger.info("through into executeQFromMenu");
                                player.closeInventory();
                                if (imageInfo.get("otp") instanceof String otp) {
                                    im.executeQFromMenu(player, new Object[] {otp, title, comment, url, date});
                                }
                            });
                        } else {
                            // 作成者以外の場合
                            playerMenuActions.put(inv.first(item), (event) -> {
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
                        playerMenuActions.put(inv.first(item), (event) -> {
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
                        playerMenuActions.put(inv.first(item), (event) -> {
                            //logger.info("through into giveMap");
                            player.closeInventory();
                            im.giveMapToPlayer(player, mapId);
                        }); 
                    } else {
                        playerMenuActions.put(inv.first(item), (event) -> {
                            //logger.info("through into executeImageMapFromMenu");
                            player.closeInventory();
                            im.executeImageMapFromMenu(player, new Object[] {id, isQr, authorName, imageUUID, title, comment, ext, date});
                        });
                    }
                    index++;
                }
            }
            Menu.menuEventActions.computeIfAbsent(player, _p -> new HashMap<>()).put(Type.IMAGE_MAP_LIST, playerMenuActions);
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

    public void onlineServerMenu(Player player) {
        onlineServerMenu(player, 1);
    }

    @Deprecated
    private void onlineServerMenu(Player player, int page) {
        Map<Integer, MenuEventRunnable> playerMenuActions = new HashMap<>();
        Inventory inv = Bukkit.createInventory(null, 54, Type.ONLINE_SERVER.get());
        ItemStack backItem = new ItemStack(Material.STICK);
        ItemMeta backMeta = backItem.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.GOLD + "戻る");
            backItem.setItemMeta(backMeta);
        }
        inv.setItem(0, backItem);
        playerMenuActions.put(0, (event) -> serverTypeMenu(player));
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
            playerMenuActions.put(slot, (event) -> serverMenuFromOnlineServerMenu(player, serverName));
        }
        if (page > 1) {
            ItemStack prevPageItem = new ItemStack(Material.ARROW);
            ItemMeta prevPageMeta = prevPageItem.getItemMeta();
            if (prevPageMeta != null) {
                prevPageMeta.setDisplayName(ChatColor.GOLD + "前のページ");
                prevPageItem.setItemMeta(prevPageMeta);
            }
            inv.setItem(45, prevPageItem);
            playerMenuActions.put(45, (event) -> onlineServerMenu(player, page - 1));
        }
        if (page < totalPages) {
            ItemStack nextPageItem = new ItemStack(Material.ARROW);
            ItemMeta nextPageMeta = nextPageItem.getItemMeta();
            if (nextPageMeta != null) {
                nextPageMeta.setDisplayName(ChatColor.GOLD + "次のページ");
                nextPageItem.setItemMeta(nextPageMeta);
            }
            inv.setItem(53, nextPageItem);
            playerMenuActions.put(53, (event) -> onlineServerMenu(player, page + 1));
        }
        Menu.menuEventActions.computeIfAbsent(player, _p -> new HashMap<>()).put(Type.ONLINE_SERVER, playerMenuActions);
        //logger.info("menuActions: {}", menuActions);
        player.openInventory(inv);
    }

    public void serverEachTypeMenu(Player player, String serverType) {
        serverEachTypeMenu(player, serverType, 1);
    }

    @Deprecated
    private void serverEachTypeMenu(Player player, String serverType, int page) {
        Map<Integer, MenuEventRunnable> playerMenuActions = new HashMap<>();
        Inventory inv = Bukkit.createInventory(null, 54, serverType + " servers");
        ItemStack backItem = new ItemStack(Material.STICK);
        ItemMeta backMeta = backItem.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.GOLD + "戻る");
            backItem.setItemMeta(backMeta);
        }
        inv.setItem(0, backItem);
        playerMenuActions.put(0, (event) -> {
            serverTypeMenu(player);
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
            playerMenuActions.put(slot, (event) -> serverMenu(player, serverName));
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
            playerMenuActions.put(45, (event) -> serverEachTypeMenu(player, serverType, page - 1));
        }
        if (page < totalPages) {
            ItemStack nextPageItem = new ItemStack(Material.ARROW);
            ItemMeta nextPageMeta = nextPageItem.getItemMeta();
            if (nextPageMeta != null) {
                nextPageMeta.setDisplayName(ChatColor.GOLD + "次のページ");
                nextPageItem.setItemMeta(nextPageMeta);
            }
            inv.setItem(53, nextPageItem);
            playerMenuActions.put(53, (event) -> serverEachTypeMenu(player, serverType, page + 1));
        }
        Menu.menuEventActions.computeIfAbsent(player, _p -> new HashMap<>()).put(Type.SERVER_EACH_TYPE, playerMenuActions);
        player.openInventory(inv);
    }

    public void serverMenu(Player player, String serverName) {
        serverMenu(player, serverName, 1, false);
    }

    private void serverMenu(Player player, String serverName, int page) {
        serverMenu(player, serverName, page, false);
    }

    public void serverMenuFromOnlineServerMenu(Player player, String serverName) {
        serverMenu(player, serverName, 1, true);
    }

    public void teleportPointMenu(Player player, Type type) {
        teleportPointMenu(player, 1, type);
    }

    @Deprecated
    private void teleportPointMenu(Player player, int page, Type type) {
        boolean isPrivate = type.equals(Type.TELEPORT_POINT_PRIVATE);
        String playerUUID = player.getUniqueId().toString();

        int inventorySize = 54;
        int usingSlots = page == 1 ? 2 : 3; // 戻るボタンやページネーションボタンに使用するスロット数
        int itemsPerPage = inventorySize - usingSlots; // 各ページに表示するアイテムの数
        Inventory inv = Bukkit.createInventory(null, inventorySize, type.get());

        Map<Integer, MenuEventRunnable> playerMenuActions = new HashMap<>();

        playerMenuActions.put(0, (event) -> teleportPointTypeMenu(player));

        ItemStack backItem = new ItemStack(Material.STICK);
        ItemMeta backMeta = backItem.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.GOLD + "戻る");
            backItem.setItemMeta(backMeta);
        }
        inv.setItem(0, backItem);

        try (Connection conn = db.getConnection()) {
            Map<Integer, Map<String, Object>> thisServerTeleportPoints = new HashMap<>();
            String thisServerName = shd.getServerName();
            String query;
            if (isPrivate) {
                query = "SELECT * FROM tp_points WHERE server=? AND type=? AND uuid=?;";
            } else {
                query = "SELECT * FROM tp_points WHERE server=? AND type=?;";
            }
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setString(1, thisServerName);
                ps.setString(2, type.get());
                if (isPrivate) {
                    ps.setString(3, playerUUID);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    int rsIndex = 0;
                    while (rs.next()) {
                        Map<String, Object> rowMap = new HashMap<>();
                        int columnCount = rs.getMetaData().getColumnCount();
                        for (int i = 1; i <= columnCount; i++) {
                            String columnName = rs.getMetaData().getColumnName(i);
                            rowMap.put(columnName, rs.getObject(columnName));
                        }
                        thisServerTeleportPoints.computeIfAbsent(rsIndex, _p -> rowMap);
                        rsIndex++;
                    }
                }
            }
            
            if (thisServerTeleportPoints.isEmpty()) {
                ItemStack barrierItem = new ItemStack(Material.BARRIER);
                ItemMeta barrierMeta = barrierItem.getItemMeta();
                if (barrierMeta != null) {
                    barrierMeta.setDisplayName(ChatColor.GOLD + "データがありません。");
                    barrierItem.setItemMeta(barrierMeta);
                }
                inv.setItem(22, barrierItem);

                Menu.menuEventActions.computeIfAbsent(player, _p -> new HashMap<>()).put(type, playerMenuActions);
                player.openInventory(inv);
                return;
            }

            int totalItems = thisServerTeleportPoints.size();
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
                inv.setItem(53, nextPageItem);
                playerMenuActions.put(53, (event) -> teleportPointMenu(player, page + 1, type));
            }
            if (page > 1) {
                ItemStack prevPageItem = new ItemStack(Material.ARROW);
                ItemMeta prevPageMeta = prevPageItem.getItemMeta();
                if (prevPageMeta != null) {
                    prevPageMeta.setDisplayName(ChatColor.GOLD + "前のページ");
                    prevPageItem.setItemMeta(prevPageMeta);
                }
                inv.setItem(45, prevPageItem);
                playerMenuActions.put(45, (event) -> teleportPointMenu(player, page - 1, type));
            }
            int index = 0;
            for (int i = startIndex; i < endIndex; i++) {
                Map<String, Object> tpPoint = thisServerTeleportPoints.get(i);
                if (tpPoint != null) {
                    String authorName = (String) tpPoint.get("name");
                    String authorUUID = (String) tpPoint.get("uuid");
                    boolean isAuthor = authorUUID.equals(playerUUID);
                    String title = (String) tpPoint.get("title");
                    String comment  = (String) tpPoint.get("comment");
                    double x = (Double) tpPoint.get("x");
                    double y = (Double) tpPoint.get("y");
                    double z =  (Double) tpPoint.get("z");
                    float yaw = (Float) tpPoint.get("yaw");
                    float pitch = (Float) tpPoint.get("pitch");
                    String worldName = (String) tpPoint.get("world");
                    Timestamp ts = (Timestamp) tpPoint.get("created_at");
                    String date = JavaUtils.Time.Format.YYYY_MM_DD.format(ts);
                    int id = (int) tpPoint.get("id"); // 管理画面用
                    String materialName = (String) tpPoint.get("material");
                    boolean isEnchanted = (boolean) tpPoint.get("enchanted");

                    double roundx = JavaUtils.roundToFirstDecimalPlace(x);
                    double roundy = JavaUtils.roundToFirstDecimalPlace(y);
                    double roundz = JavaUtils.roundToFirstDecimalPlace(z);

                    List<String> lores = new ArrayList<>();

                    if (!comment.isBlank()) {
                        List<String> commentLines = Arrays.stream(comment.split("\n"))
                            .map(String::trim)
                            .collect(Collectors.toList());
                        lores.addAll(commentLines);
                    }

                    lores.add("World: " + worldName);
                    lores.add("Location: (" + roundx + ", " + roundy + ", " + roundz + ")");
                    lores.add("created by " + authorName);
                    lores.add("created at " + date.replace("-", "/"));
                    if (isAuthor) {
                        lores.add("右クリックで" + ChatColor.GREEN + "管理メニュー");
                    }

                    Material material = MaterialUtil.stringToMaterial(materialName);
                    ItemStack item = new ItemStack(material);
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName(ChatColor.GREEN + title);
                        meta.setLore(lores);
                        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, String.valueOf(index)), PersistentDataType.STRING, "true");
                        if (isEnchanted) {
                            meta.addEnchant(Enchantment.LUCK, 1, true);
                            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS); 
                        }
                        item.setItemMeta(meta);
                    }
                    inv.addItem(item);
                    //inv.setItem(slot, item);
                    playerMenuActions.put(inv.first(item), (event) -> {
                        ClickType clickType = event.getClick();

                        // 右クリックかつ作成者本人なら削除ダイアログへ
                        if (clickType.isRightClick() && isAuthor) {
                            teleportPointManager(player, id);
                            return;
                        }

                        player.closeInventory();

                        int permLevel = lp.getPermLevel(player.getName());
                        if (permLevel < 1) {
                            player.sendMessage(ChatColor.RED + "まだFMCのWEB認証が完了していません。");
                            return;
                        }

                        // ここで、テレポートさせる前の座標を記録し、/backコマンドで戻れるようにする
                        final Location beforeLoc = player.getLocation();

                        Component message = Component.text("3秒後にテレポートします。")
                            .color(NamedTextColor.GOLD)
                            .decorate(TextDecoration.BOLD, TextDecoration.UNDERLINED);
                        
                        player.sendMessage(message);
                        
                        new BukkitRunnable() {
                            @Override
                            public void run() {
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
                                    
                                Component backMessage = Component.text("※/backコマンドで戻れます。")
                                    .color(NamedTextColor.GRAY)
                                    .decorate(TextDecoration.ITALIC);
                                
                                TextComponent messages = Component.text()
                                    .append(message)
                                    .appendNewline()
                                    .append(backMessage)
                                    .build();

                                player.sendMessage(messages);

                                SocketSwitch ssw = sswProvider.get();
                                try (Connection conn = db.getConnection()) {
                                    ssw.sendVelocityServer(conn, "teleport->point->name->" + player.getName() +"->at->" + title + "->");
                                } catch (SQLException | ClassNotFoundException e) {
                                    logger.info("An error occurred at Menu#teleportPointMenu: {}", e);
                                }
                            }
                        }.runTaskLater(plugin, 20 * 3);
                    });
                    index++;
                }
            }

            Menu.menuEventActions.computeIfAbsent(player, _p -> new HashMap<>()).put(type, playerMenuActions);
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

    public void serverTypeMenu(Player player) {
        serverTypeMenu(player, 1);
    }

    @Deprecated
    private void serverTypeMenu(Player player, int page) {
        Map<Integer, MenuEventRunnable> playerMenuActions = new HashMap<>();
        playerMenuActions.put(0, (event) -> generalMenu(player));
        Inventory inv = Bukkit.createInventory(null, 27, Type.SERVER_TYPE.get());
        ItemStack backItem = new ItemStack(Material.STICK);
        ItemMeta backMeta = backItem.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.GOLD + "戻る");
            backItem.setItemMeta(backMeta);
        }
        inv.setItem(0, backItem);
        switch (page) {
            case 1 -> {
                playerMenuActions.put(11, (event) -> serverEachTypeMenu(player, "survival"));
                playerMenuActions.put(13, (event) -> serverEachTypeMenu(player, "minigame"));
                playerMenuActions.put(15, (event) -> serverEachTypeMenu(player, "dev"));
                playerMenuActions.put(18, (event) -> onlineServerMenu(player));

                playerMenuActions.put(26, (event) -> serverTypeMenu(player, 2));

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
                playerMenuActions.put(11, (event) -> serverEachTypeMenu(player, "distributed"));
                playerMenuActions.put(13, (event) -> serverEachTypeMenu(player, "mod"));
                playerMenuActions.put(15, (event) -> serverEachTypeMenu(player, "others"));
                playerMenuActions.put(18, (event) -> serverTypeMenu(player));
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
        Menu.menuEventActions.computeIfAbsent(player, _p -> new HashMap<>()).put(Type.SERVER_TYPE, playerMenuActions);
        player.openInventory(inv);
    }

    public void enterServer(Player player, String serverName) {
        player.closeInventory();
        cf.executeProxyCommand(player, "fmcp stp " + serverName);
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

    @Deprecated
    private void settingMenu(Player player, int page) {
        Map<Integer, MenuEventRunnable> playerMenuActions = new HashMap<>();
        Inventory inv = Bukkit.createInventory(null, 27,Type.SETTING.get());
        try (Connection conn = db.getConnection()) {
            Map<String, Object> memberMap = db.getMemberMap(conn, player.getUniqueId().toString());
            switch (page) {
                case 1 -> {
                    if (memberMap.get("hubinv") instanceof Boolean hubinv) {
                        playerMenuActions.put(11, (event) -> {
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
                            settingMenu(player);
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
                        playerMenuActions.put(13, (event) -> {
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
                            settingMenu(player);
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
            Menu.menuEventActions.computeIfAbsent(player, _p -> new HashMap<>()).put(Type.SETTING, playerMenuActions);
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
        playerMenuActions.put(0, (event) -> generalMenu(player, 2));
        Menu.menuEventActions.computeIfAbsent(player, _p -> new HashMap<>()).put(Type.SETTING, playerMenuActions);
        player.openInventory(inv);
    }

    @Deprecated
    private void teleportResponseMenu(Player player, Player targetPlayer, boolean me) {
        Map<Integer, MenuEventRunnable> playerMenuActions = new HashMap<>();
        Inventory inv = Bukkit.createInventory(null, 27, Type.TELEPORT_RESPONSE.get());
        ItemStack backItem = new ItemStack(Material.STICK);
        ItemMeta backMeta = backItem.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.GOLD + "戻る");
            backItem.setItemMeta(backMeta);
        }
        inv.setItem(0, backItem);
        playerMenuActions.put(0, (event) -> playerTeleportMenu(player));
        ItemStack acceptItem = new ItemStack(Material.DROPPER);
        ItemMeta acceptMeta = acceptItem.getItemMeta();
        if (acceptMeta != null) {
            acceptMeta.setDisplayName(ChatColor.GREEN + "受け入れる");
            acceptItem.setItemMeta(acceptMeta);
        }
        inv.setItem(11, acceptItem);
        playerMenuActions.put(11, (event) -> {
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
        playerMenuActions.put(15, (event) -> {
            player.closeInventory();
            if (!me) {
                player.performCommand("tprd " + targetPlayer.getName());
            } else {
                player.performCommand("tprmd " + targetPlayer.getName());
            }
        });
        // ここ、targetPlayerがキーになっているのがあってるかわからない
        Menu.menuEventActions.computeIfAbsent(player, _p -> new HashMap<>()).put(Type.TELEPORT_RESPONSE, playerMenuActions);
        player.openInventory(inv);
    }
    
    @Deprecated
    private void teleportRequestMenu(Player player, int page, boolean me) {
        Type thisKey = me ? Type.TELEPORT_REQUEST_ME : Type.TELEPORT_REQUEST;

        int inventorySize = 27;
        int usingSlots = page == 1 ? 2 : 3; // 戻るボタンやページネーションボタンに使用するスロット数
        int itemsPerPage = inventorySize - usingSlots; // 各ページに表示するアイテムの数

        Map<Integer, MenuEventRunnable> playerMenuActions = new HashMap<>();
        Inventory inv = Bukkit.createInventory(null, inventorySize, thisKey.get());

        ItemStack backItem = new ItemStack(Material.STICK);
        ItemMeta backMeta = backItem.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.GOLD + "戻る");
            backItem.setItemMeta(backMeta);
        }
        inv.setItem(0, backItem);
        playerMenuActions.put(0, (event) -> playerTeleportMenu(player));
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
                playerMenuActions.put(26, (event) -> teleportRequestMenu(player, page + 1, me));
            }
            if (page > 1) {
                ItemStack prevPageItem = new ItemStack(Material.ARROW);
                ItemMeta prevPageMeta = prevPageItem.getItemMeta();
                if (prevPageMeta != null) {
                    prevPageMeta.setDisplayName(ChatColor.GOLD + "前のページ");
                    prevPageItem.setItemMeta(prevPageMeta);
                }
                inv.setItem(18, prevPageItem);
                playerMenuActions.put(18, (event) -> teleportRequestMenu(player, page - 1, me));
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
                playerMenuActions.put(inv.first(playerItem), (event) -> {
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
        Menu.menuEventActions.computeIfAbsent(player, _p -> new HashMap<>()).put(thisKey, playerMenuActions);
        player.openInventory(inv);
    }
    
    @Deprecated
    private void serverMenu(Player player, String serverName, int page, boolean fromOnlineServerInventory) {
        Map<Integer, MenuEventRunnable> playerMenuActions = new HashMap<>();
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
                                                playerMenuActions.put(22, (event) -> serverSwitch(player, serverName));
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
                                            playerMenuActions.put(24, (event) -> enterServer(player, serverName));
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
                                                playerMenuActions.put(22, (event) -> serverSwitch(player, serverName));
                                            } else if (permLevel >= 2) {
                                                leverMeta.setDisplayName(ChatColor.GREEN + serverName + "サーバーの起動");
                                                leverMeta.setLore(Arrays.asList(ChatColor.BLUE + "アドミン権限より起動する。"));
                                                playerMenuActions.put(22, (event) -> serverSwitch(player, serverName));
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
                                        playerMenuActions.put(45, (event) -> serverMenu(player, serverName, page - 1));
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
                                        playerMenuActions.put(53, (event) -> serverMenu(player, serverName, page + 1));
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
        playerMenuActions.put(0, (event) -> {
            if (fromOnlineServerInventory) {
                onlineServerMenu(player);
            } else {
                serverEachTypeMenu(player, thisServerType.get());
            }
        });
        Menu.menuEventActions.computeIfAbsent(player, _p -> new HashMap<>()).put(Type.SERVER, playerMenuActions);
        player.openInventory(inv);
    }

    @Deprecated
    private void teleportPointManager(Player player, int id) {
        Map<Integer, MenuEventRunnable> playerMenuActions = new HashMap<>();
        Inventory inv = Bukkit.createInventory(null, 27, Type.TP_POINT_MANAGER.get());

        playerMenuActions.put(11, (event) -> changeMaterial(player, id));
        playerMenuActions.put(15, (event) -> deleteTeleportPointMenu(player, id));

        try (Connection dconn = db.getConnection()) {
            try (PreparedStatement ps = dconn.prepareStatement("SELECT * FROM tp_points WHERE id = " + id + " LIMIT 1;")) {
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String typeName = rs.getString("type");
                        Optional<Type> optionalType = Type.search(typeName);
                        if (optionalType.isPresent()) {
                            Type type = optionalType.get();
                            playerMenuActions.put(0, (event) -> teleportPointMenu(player, type));
                        }
                    }
                }
            }
        } catch (ClassNotFoundException | SQLException e) {
            player.closeInventory();
            player.sendMessage(ChatColor.RED + "データベースとの通信にエラーが発生しました。");
            logger.error("A SQLException occurred: ", e);
            return;
        }

        ItemStack changeMaterialItem = new ItemStack(Material.NAME_TAG);
        ItemMeta changeMaterialMeta = changeMaterialItem.getItemMeta();
        if (changeMaterialMeta != null) {
            changeMaterialMeta.setDisplayName(ChatColor.GREEN + "表示アイテム変更");
            changeMaterialMeta.setLore(new ArrayList<>(Arrays.asList(
                "メニュー内でのポイントの",
                    "アイテムを変更できる！",
                    "デフォルト: エンダーパール"
                )));
            changeMaterialItem.setItemMeta(changeMaterialMeta);
        }
        inv.setItem(11, changeMaterialItem);

        ItemStack deleteItem = new ItemStack(Material.FLINT_AND_STEEL);
        ItemMeta deleteMeta = deleteItem.getItemMeta();
        if (deleteMeta != null) {
            deleteMeta.setDisplayName(ChatColor.RED + "削除画面");
            deleteMeta.setLore(new ArrayList<>(Arrays.asList(
                "次の画面でポイントを削除するかどうか選べる！"
                )));
            deleteItem.setItemMeta(deleteMeta);
        }
        inv.setItem(15, deleteItem);

        ItemStack backItem = new ItemStack(Material.STICK);
        ItemMeta backMeta = backItem.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.GOLD + "戻る");
            backItem.setItemMeta(backMeta);
        }
        inv.setItem(0, backItem);

        Menu.menuEventActions.computeIfAbsent(player, _p -> new HashMap<>()).put(Type.TP_POINT_MANAGER, playerMenuActions);
        player.openInventory(inv);
    }

    @Deprecated
    private void deleteTeleportPointMenu(Player player, int id) {
        Map<Integer, MenuEventRunnable> playerMenuActions = new HashMap<>();

        Inventory inv = Bukkit.createInventory(null, 27, Type.DELETE.get());

        try (Connection conn = db.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM tp_points WHERE id = " + id + " LIMIT 1;")) {
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        final String title = rs.getString("title");
                        final String comment = rs.getString("comment");
                        final String typeName = rs.getString("type");

                        Optional<Type> optionalType = Type.search(typeName);
                        if (optionalType.isPresent()) {
                            Type type = optionalType.get();
                            playerMenuActions.put(0, (event) -> teleportPointManager(player, id));
                            playerMenuActions.put(13, (event) -> {
                                try (Connection dconn = db.getConnection()) {
                                    try (PreparedStatement dps = dconn.prepareStatement("DELETE FROM tp_points WHERE id = " + id + ";")) {
                                        int dpsAffected = dps.executeUpdate();
                                        if (dpsAffected > 0) {
                                            player.closeInventory();
                    
                                            Component success = Component.text("削除しました。(id: " + id + ")")
                                                .color(NamedTextColor.RED)
                                                .decorate(TextDecoration.BOLD);
                    
                                            Component deleteContent = Component.text("タイトル: " + title)
                                                .appendNewline()
                                                .append(Component.text("コメント: " + (comment.isBlank() ? "なし" : comment)))
                                                .color(NamedTextColor.GRAY)
                                                .decorate(TextDecoration.ITALIC);
                    
                                            Component back = Component.text("※もとのメニューに戻ります。")
                                                .color(NamedTextColor.GRAY)
                                                .decorate(TextDecoration.ITALIC);
                    
                                            TextComponent messages = Component.text()
                                                .append(deleteContent)
                                                .appendNewline()
                                                .append(success)
                                                .appendNewline()
                                                .append(TCUtils.LATER_OPEN_INV_3.get())
                                                .appendNewline()
                                                .append(back)
                                                .build();
                    
                                            new BukkitRunnable() {
                                                @Override
                                                public void run() {
                                                    teleportPointMenu(player, type);
                                                }
                                            }.runTaskLater(plugin, 20 * 3);
                    
                                            player.sendMessage(messages);
                                        }
                                    } 
                                } catch (SQLException | ClassNotFoundException e) {
                                    player.closeInventory();
                                    player.sendMessage(ChatColor.RED + "削除中にデータベースとの通信にエラーが発生しました。");
                                    logger.error("A SQLException occurred: ", e);
                                    return;
                                }
                            });
                        }
                    } else {
                        player.closeInventory();
                        player.sendMessage(ChatColor.RED + "エラーが発生しました。");
                        throw new IllegalAccessError("The line not Found in tp_points (id: " + id + ")");
                    }
                }
            }
        } catch (ClassNotFoundException | SQLException e) {
            player.closeInventory();
            player.sendMessage(ChatColor.RED + "データベースとの通信にエラーが発生しました。");
            logger.error("A SQLException occurred: ", e);
            return;
        }

        ItemStack tntItem = new ItemStack(Material.TNT);
        ItemMeta tntMeta = tntItem.getItemMeta();
        if (tntMeta != null) {
            tntMeta.setDisplayName(ChatColor.RED + "削除する");
            List<String> lores = new ArrayList<>();
            lores.add(ChatColor.RED + "この操作はもとに戻せません！");
            tntMeta.setLore(lores);
            tntItem.setItemMeta(tntMeta);
        }
        inv.setItem(13, tntItem);

        ItemStack backItem = new ItemStack(Material.STICK);
        ItemMeta backMeta = backItem.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.GOLD + "戻る");
            backItem.setItemMeta(backMeta);
        }
        inv.setItem(0, backItem);

        Menu.menuEventActions.computeIfAbsent(player, _p -> new HashMap<>()).put(Type.DELETE, playerMenuActions);
        player.openInventory(inv);
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
}
