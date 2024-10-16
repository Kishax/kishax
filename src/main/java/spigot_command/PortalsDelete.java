package spigot_command;

import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import com.google.inject.Inject;

import spigot.PortalsConfig;

public class PortalsDelete {
    private final PortalsConfig psConfig;

    @Inject
    public PortalsDelete(PortalsConfig psConfig) {
        this.psConfig = psConfig;
    }

    public void execute(CommandSender sender, String portalName) {
        FileConfiguration portalsConfig = psConfig.getPortalsConfig();
        List<Map<?, ?>> portals = (List<Map<?, ?>>) portalsConfig.getList("portals");

        if (portals != null) {
            portals.removeIf(portal -> portalName.equals(portal.get("name")));
            portalsConfig.set("portals", portals);
            psConfig.savePortalsConfig();
            psConfig.reloadPortalsConfig();
            sender.sendMessage(ChatColor.GREEN + "ポータル " + portalName + " が削除されました。");
        } else {
            sender.sendMessage(ChatColor.RED + "ポータルが見つかりませんでした。");
        }
    }
}