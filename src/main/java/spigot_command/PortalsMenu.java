package spigot_command;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.google.inject.Inject;

import spigot.Database;

public class PortalsMenu {
	private final common.Main plugin;
    private final Database db;
	@Inject
	public PortalsMenu(common.Main plugin, Database db) {
		this.plugin = plugin;
        this.db = db;
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

    public void openEachServerInventory(Player player, String serverType) throws SQLException {
        Inventory inv = Bukkit.createInventory(null, 27, serverType + " servers");
        ItemStack backServerItem = new ItemStack(Material.BLAZE_ROD);
        ItemMeta backMeta = backServerItem.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName(ChatColor.BLUE + "サーバーメニューへ戻る");
            backServerItem.setItemMeta(backMeta);
        }
        inv.setItem(0, backServerItem);

        try (Connection conn = db.getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM status WHERE type=?;")) {
            @SuppressWarnings("unused")
            String sql = "SELECT * FROM status WHERE type=?;";
            ps.setString(1, serverType);
            ResultSet status = ps.executeQuery();
            while (status.next()) {
                String serverName = status.getString("name");
                ItemStack serverItem = new ItemStack(Material.DIAMOND);
                ItemMeta serverMeta = serverItem.getItemMeta();
                if (serverMeta != null) {
                    serverMeta.setDisplayName(ChatColor.GREEN + serverName);
                    serverItem.setItemMeta(serverMeta);
                }
                inv.addItem(serverItem);
            }
        } catch (ClassNotFoundException | SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "An Exception error occurred: {0}", e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                plugin.getLogger().severe(element.toString());
            }
        }

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
