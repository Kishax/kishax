package spigot_command;

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

	private final common.Main plugin;
	@Inject
	public PortalsMenu(common.Main plugin) {
		this.plugin = plugin;
	}

	public void execute(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
        if (sender instanceof Player player) {
            player.openInventory(plugin.getServer().createInventory(null, 27, "Custom Inventory"));
        }
	}

    public void OpenServerTypeInventory(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "Server Type Inventory");

        // スロット11に生活鯖のブロックを配置
        ItemStack lifeServerItem = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta lifeMeta = lifeServerItem.getItemMeta();
        if (lifeMeta != null) {
            lifeMeta.setDisplayName(ChatColor.GREEN + "生活鯖");
            lifeServerItem.setItemMeta(lifeMeta);
        }
        inv.setItem(11, lifeServerItem);

        // スロット13に配布鯖のブロックを配置
        ItemStack distributionServerItem = new ItemStack(Material.CHEST);
        ItemMeta distributionMeta = distributionServerItem.getItemMeta();
        if (distributionMeta != null) {
            distributionMeta.setDisplayName(ChatColor.YELLOW + "配布鯖");
            distributionServerItem.setItemMeta(distributionMeta);
        }
        inv.setItem(13, distributionServerItem);

        // スロット15にモッド鯖のブロックを配置
        ItemStack modServerItem = new ItemStack(Material.IRON_BLOCK);
        ItemMeta modMeta = modServerItem.getItemMeta();
        if (modMeta != null) {
            modMeta.setDisplayName(ChatColor.BLUE + "モッド鯖");
            modServerItem.setItemMeta(modMeta);
        }
        inv.setItem(15, modServerItem);

        player.openInventory(inv);
    }

    public void openModServerInventory(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "Mod Server Inventory");
        // モッド鯖のインベントリのアイテムを設定
        player.openInventory(inv);
    }

    public void openLifeServerInventory(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "Life Server Inventory");
        // 生活鯖のインベントリのアイテムを設定
        player.openInventory(inv);
    }

    public void openDistributionServerInventory(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "Distribution Server Inventory");
        // 配布鯖のインベントリのアイテムを設定
        player.openInventory(inv);
    }
}
