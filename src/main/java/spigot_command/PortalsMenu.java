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

    public void openEachServerInventory(Player player, String serverType) {
        Inventory inv = Bukkit.createInventory(null, 27, serverType + " servers");
        ItemStack backServerItem = new ItemStack(Material.BLAZE_ROD);
        ItemMeta backMeta = backServerItem.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.BLUE + "サーバーメニューへ戻る");
            backServerItem.setItemMeta(backMeta);
        }
        inv.setItem(0, backServerItem);

        switch (serverType) {
            case "life" -> {

            }
            case "distributed" -> {

            }
            case "mod" -> {

            }
        }
        player.openInventory(inv);
    }
}
