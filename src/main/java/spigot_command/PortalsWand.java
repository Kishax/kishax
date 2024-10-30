package spigot_command;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.google.inject.Inject;

public class PortalsWand {
    private final common.Main plugin;
	@Inject
	public PortalsWand(common.Main plugin) {
        this.plugin = plugin;
	}
	
	public void execute(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
        if (plugin.getConfig().getBoolean("Portals.Wand", false)) {
            if (sender instanceof Player player) {
                ItemStack wand = new ItemStack(Material.STONE_AXE);
                ItemMeta meta = wand.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatColor.GREEN + "Portal Wand");
                    wand.setItemMeta(meta);
                }
                player.getInventory().addItem(wand);
                player.sendMessage("カスタム名の木の斧を受け取りました。1番目のコーナーを右クリックで選択してください。");
            } else {
                if (sender != null) {
                    sender.sendMessage("プレイヤーのみがこのコマンドを実行できます。");
                }
            }
        } else {
            if (sender != null) {
                sender.sendMessage(ChatColor.RED + "このサーバーでは、この機能は無効になっています。");
            }
        }
	}
}
