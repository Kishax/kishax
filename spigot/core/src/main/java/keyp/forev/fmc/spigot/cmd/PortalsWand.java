package keyp.forev.fmc.spigot.cmd;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import com.google.inject.Inject;
import org.bukkit.plugin.java.JavaPlugin;

public class PortalsWand {
    public static final String PERSISTANT_KEY = "custom_wand";
    private final JavaPlugin plugin;
	@Inject
	public PortalsWand(JavaPlugin plugin) {
        this.plugin = plugin;
	}
	
	public void execute(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
        if (plugin.getConfig().getBoolean("Portals.Wand", false)) {
            if (sender instanceof Player player) {
                ItemStack wand = new ItemStack(Material.STONE_AXE);
                ItemMeta meta = wand.getItemMeta();
                if (meta != null) {
                    meta.getPersistentDataContainer().set(new NamespacedKey(plugin, PortalsWand.PERSISTANT_KEY), PersistentDataType.STRING, "true");
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
