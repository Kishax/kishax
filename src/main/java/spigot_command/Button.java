package spigot_command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import com.google.inject.Inject;

import net.md_5.bungee.api.ChatColor;

public class Button {
    public static final String PERSISTANT_KEY = "custom_button";
    private final common.Main plugin;
    private final List<String> buttonLores = new ArrayList<>(Arrays.asList("押せば、自動で何かが起こるボタンです。"));
    @Inject
    public Button(common.Main plugin) {
        this.plugin = plugin;
    }

    public void execute(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
        if (!sender.hasPermission("fmc." + args[0] + ".create")) {
            sender.sendMessage(ChatColor.RED + "権限がありません。");
            return;
        }
        if (sender instanceof Player player) {
            giveCustomButton(player);
        } else {
            sender.sendMessage(ChatColor.RED + "プレイヤーのみ実行可能です。");
        }
    }

    private void giveCustomButton(Player player) {
        ItemStack button = new ItemStack(Material.STONE_BUTTON);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + "特定のボタン");
            meta.setLore(buttonLores);
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, Button.PERSISTANT_KEY), PersistentDataType.STRING, "true");
            button.setItemMeta(meta);
        }
        player.getInventory().addItem(button);
        player.sendMessage(ChatColor.GREEN + "特定のボタンを渡しました！");
    }
}
