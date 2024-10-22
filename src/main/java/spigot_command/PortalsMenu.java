package spigot_command;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import spigot.ServerStatusCache;

@Singleton
public class PortalsMenu {
    public static final int[] SLOT_POSITIONS = {11, 13, 15, 29, 31, 33};
    public static final int[] FACE_POSITIONS = {46, 47, 48, 49, 50, 51, 52};
	private final common.Main plugin;
    private static final List<Material> ORE_BLOCKS = Arrays.asList(
        Material.NETHERITE_BLOCK, Material.GOLD_BLOCK, Material.REDSTONE_BLOCK, 
        Material.EMERALD_BLOCK, Material.DIAMOND_BLOCK, Material.IRON_BLOCK,
        Material.COAL_BLOCK, Material.LAPIS_BLOCK, Material.QUARTZ_BLOCK,
        Material.COPPER_BLOCK
    );
    private final ServerStatusCache ssc;
    private final Map<Player, Map<String, Integer>> playerOpenningInventoryMap = new HashMap<>();
    private int currentOreIndex = 0; // 現在のインデックスを管理するフィールド

	@Inject
	public PortalsMenu(common.Main plugin, ServerStatusCache ssc) {  
		this.plugin = plugin;
        this.ssc = ssc;
	}

	public void execute(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
        if (sender instanceof Player player) {
            player.openInventory(plugin.getServer().createInventory(null, 27, "Custom Inventory"));
        }
	}

    public void openServerTypeInventory(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "server type");

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

        player.openInventory(inv);
    }

    public void openServerEachInventory(Player player, String serverType, int page) {
        Inventory inv = Bukkit.createInventory(null, 54, serverType + " servers");

        Map<String, Map<String, Map<String, String>>> serverStatusMap = ssc.getStatusMap();
        Map<String, Map<String, String>> serverStatusList = serverStatusMap.get(serverType);
        int totalItems = serverStatusList.size();
        int totalPages = (totalItems + SLOT_POSITIONS.length - 1) / SLOT_POSITIONS.length;

        int startIndex = (page - 1) * SLOT_POSITIONS.length;
        int endIndex = Math.min(startIndex + SLOT_POSITIONS.length, totalItems);

        List<Map<String, String>> serverDataList = serverStatusList.values().stream().collect(Collectors.toList());
        for (int i = startIndex; i < endIndex; i++) {
            Map<String, String> serverData = serverDataList.get(i);
            String serverName = serverData.get("name");
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
            inv.setItem(SLOT_POSITIONS[i - startIndex], serverItem);
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
        }

        // インベントリの0個目にブロックを配置
        ItemStack backItem = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = backItem.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.RED + "戻る");
            backItem.setItemMeta(backMeta);
        }
        inv.setItem(0, backItem);

        // プレイヤーにインベントリを開かせる
        player.openInventory(inv);

        // プレイヤーのページを更新
        setPage(player, serverType, page);
    }

    public void openServerInventory(Player player, String serverName, int page) {
        Inventory inv = Bukkit.createInventory(null, 54, serverName + " server");
        ItemStack backItem = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = backItem.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.RED + "戻る");
            backItem.setItemMeta(backMeta);
        }
        inv.setItem(0, backItem);

        Map<String, Map<String, Map<String, String>>> serverStatusMap = ssc.getStatusMap();
        for (Map.Entry<String, Map<String, Map<String, String>>> entry : serverStatusMap.entrySet()) {
            //String serverType = entry.getKey();
            Map<String, Map<String, String>> serverStatusList = entry.getValue();
            for (Map.Entry<String, Map<String, String>> serverEntry : serverStatusList.entrySet()) {
                String name = serverEntry.getKey();
                if (name.equals(serverName)) {
                    Map<String, String> serverData = serverEntry.getValue();
                    for (Map.Entry<String, String> dataEntry : serverData.entrySet()) {
                        String key = dataEntry.getKey();
                        String value = dataEntry.getValue();
                        if (key.equals("online")) {
                            if (value.equals("1")) {
                                ItemStack onlineItem = new ItemStack(Material.GREEN_WOOL);
                                ItemMeta onlineMeta = onlineItem.getItemMeta();
                                if (onlineMeta != null) {
                                    onlineMeta.setDisplayName(ChatColor.GREEN + "オンライン");
                                    onlineItem.setItemMeta(onlineMeta);
                                }
                                inv.setItem(8, onlineItem);
                            } else {
                                ItemStack offlineItem = new ItemStack(Material.RED_WOOL);
                                ItemMeta offlineMeta = offlineItem.getItemMeta();
                                if (offlineMeta != null) {
                                    offlineMeta.setDisplayName(ChatColor.RED + "オフライン");
                                    offlineItem.setItemMeta(offlineMeta);
                                }
                                inv.setItem(8, offlineItem);
                            }
                        }

                        if (key.equals("player_list")) {
                            //plugin.getLogger().log(Level.INFO, "value: {0}", value);
                            if (value != null && !value.isEmpty() && !value.equals("None")) {
                                String[] playerArray = value.split(",\\s*");
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
                                        playerMeta.setLore(Arrays.asList(ChatColor.GRAY + value));
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
                        ItemStack serverItem = new ItemStack(Material.PAPER);
                        ItemMeta serverMeta = serverItem.getItemMeta();
                        if (serverMeta != null) {
                            serverMeta.setDisplayName(ChatColor.GREEN + key);
                            serverMeta.setLore(Arrays.asList(ChatColor.GRAY + value));
                            serverItem.setItemMeta(serverMeta);
                        }
                        inv.addItem(serverItem);
                    }
                }
            }
            
        }
        player.openInventory(inv);
    }

    public int getTotalPlayers(String serverName) {
        Map<String, Map<String, Map<String, String>>> serverStatusMap = ssc.getStatusMap();
        for (Map.Entry<String, Map<String, Map<String, String>>> serverEntry : serverStatusMap.entrySet()) {
            Map<String, Map<String, String>> serverStatusList = serverEntry.getValue();
            for (Map.Entry<String, Map<String, String>> serverDataEntry : serverStatusList.entrySet()) {
                String name = serverDataEntry.getKey();
                if (name.equals(serverName)) {
                    Map<String, String> serverData = serverDataEntry.getValue();
                    String playerList = serverData.get("player_list");
                    if (playerList != null && !playerList.isEmpty() && !playerList.equals("None")) {
                        String[] playerArray = playerList.split(",\\s*");
                        return playerArray.length;
                    }
                }
            }
        }
        return 0;
    }

    public int getTotalServers(String serverType) {
        Map<String, Map<String, Map<String, String>>> serverStatusMap = ssc.getStatusMap();
        Map<String, Map<String, String>> serverStatusList = serverStatusMap.get(serverType);
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
