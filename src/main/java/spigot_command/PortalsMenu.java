package spigot_command;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.google.inject.Inject;

public class PortalsMenu {
    public static Map<String, Map<Player, Integer>> playerOpenningInventoryMap;
	private final common.Main plugin;
    private static final List<Material> ORE_BLOCKS = Arrays.asList(
        Material.NETHERITE_BLOCK, Material.GOLD_BLOCK, Material.REDSTONE_BLOCK, Material.EMERALD_BLOCK
    );
    private static final int[] SLOT_POSITIONS = {11, 13, 15, 29, 31, 33};
    private final ServerStatusCache serverStatusCache;
    
	@Inject
	public PortalsMenu(common.Main plugin, ServerStatusCache serverStatusCache) {  
		this.plugin = plugin;
        this.serverStatusCache = serverStatusCache;
	}

	public void execute(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
        if (sender instanceof Player player) {
            player.openInventory(plugin.getServer().createInventory(null, 27, "Custom Inventory"));
        }
	}

    public void OpenServerTypeInventory(Player player) {
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

    public void openInventory(Player player, String serverType) {
        // プレイヤーが現在どのページを見ているかによって、
        // 次ページ、戻るページのボタンを表示するかどうか決め、
        // ブロックに仕込むメソッドも変更しなくてはならない
        final int[] page = {1};
        List<Map<Player, Integer>> playerOpenningInventoryList = PortalsMenu.playerOpenningInventoryMap.values().stream()
            .filter(map -> map.containsKey(player))
            .toList();
        if (playerOpenningInventoryList.isEmpty()) {
            page[0] = 1;
            Map<Player, Integer> playerMap = new HashMap<>();
            playerMap.put(player, 1);
            PortalsMenu.playerOpenningInventoryMap.put(serverType, playerMap);
        } else {
            playerOpenningInventoryList.forEach(map -> {
                map.forEach((key, value) -> {
                    value += 1;
                    map.put(key, value);
                    if (key.equals(player)) {
                        page[0] = value;
                    }
                });
            });
        }
        
        Inventory inv = Bukkit.createInventory(null, 54, serverType + " servers");

        Map<String, Map<String, String>> serverStatusMap = serverStatusCache.getStatusMap();
        List<Map<String, String>> serverStatusList = serverStatusMap.values().stream()
            .filter(map -> serverType.equals(map.get("type")))
            .collect(Collectors.toList());
        int totalItems = serverStatusList.size();
        int totalPages = (totalItems + SLOT_POSITIONS.length - 1) / SLOT_POSITIONS.length;

        int startIndex = (page[0] - 1) * SLOT_POSITIONS.length;
        int endIndex = Math.min(startIndex + SLOT_POSITIONS.length, totalItems);

        // デバッグ用のログ出力
        /*plugin.getLogger().log(Level.INFO, "startIndex: {0}", startIndex);
        plugin.getLogger().log(Level.INFO, "endIndex: {0}", endIndex);
        plugin.getLogger().log(Level.INFO, "totalItems: {0}", totalItems);
        plugin.getLogger().log(Level.INFO, "page: {0}", page);*/

        for (int i = startIndex; i < endIndex; i++) {
            Map<String, String> serverData = serverStatusList.get(i);
            String serverName = serverData.get("name");
            Material randomOre = ORE_BLOCKS.get(new Random().nextInt(ORE_BLOCKS.size()));
            ItemStack serverItem = new ItemStack(randomOre);
            ItemMeta serverMeta = serverItem.getItemMeta();
            if (serverMeta != null) {
                serverMeta.setDisplayName(ChatColor.GREEN + serverName);
                serverItem.setItemMeta(serverMeta);
            }
            inv.setItem(SLOT_POSITIONS[i - startIndex], serverItem);
        }

        // ページ戻るブロックを配置
        if (page[0] > 1) {
            ItemStack prevPageItem = new ItemStack(Material.ARROW);
            ItemMeta prevPageMeta = prevPageItem.getItemMeta();
            if (prevPageMeta != null) {
                prevPageMeta.setDisplayName(ChatColor.RED + "前のページ");
                prevPageItem.setItemMeta(prevPageMeta);
            }
            inv.setItem(45, prevPageItem);
        }

        // ページ進むブロックを配置
        if (page[0] < totalPages) {
            ItemStack nextPageItem = new ItemStack(Material.ARROW);
            ItemMeta nextPageMeta = nextPageItem.getItemMeta();
            if (nextPageMeta != null) {
                nextPageMeta.setDisplayName(ChatColor.GREEN + "次のページ");
                nextPageItem.setItemMeta(nextPageMeta);
            }
            inv.setItem(53, nextPageItem);
        }

        // プレイヤーにインベントリを開かせる
        player.openInventory(inv);
    }

    public void openEachServerInventory(Player player, String serverType) {
        /*Map<String, Map<String, String>> serverStatusMap = serverStatusCache.getStatusMap();
        List<Map<String, String>> serverStatusList = serverStatusMap.values().stream()
            .filter(map -> serverType.equals(map.get("type")))
            .collect(Collectors.toList());
        int totalItems = serverStatusList.size();
        int totalPages = (totalItems + SLOT_POSITIONS.length - 1) / SLOT_POSITIONS.length;*/

        openInventory(player, serverType);
    }
}
