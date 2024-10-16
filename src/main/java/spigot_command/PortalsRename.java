package spigot_command;

import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import com.google.inject.Inject;

import spigot.PortalsConfig;

public class PortalsRename {
    private final PortalsConfig psConfig;

    @Inject
    public PortalsRename(PortalsConfig psConfig) {
        this.psConfig = psConfig;
    }

    public void execute(CommandSender sender, String portalUUID, String newName) {
        FileConfiguration portalsConfig = psConfig.getPortalsConfig();
        List<Map<?, ?>> portals = (List<Map<?, ?>>) portalsConfig.getList("portals");

        if (portals != null) {
            boolean renamed = false;
            for (Map<String, Object> portal : (List<Map<String, Object>>) (List<?>) portals) {
                if (portalUUID.equals(portal.get("uuid"))) {
                    portal.put("name", newName);
                    renamed = true;
                    break;
                }
            }
            if (renamed) {
                portalsConfig.set("portals", portals);
                psConfig.savePortalsConfig();
                psConfig.reloadPortalsConfig();
                sender.sendMessage(ChatColor.GREEN + "ポータル " + portalUUID + " の名前が " + newName + " に変更されました。");
            } else {
                sender.sendMessage(ChatColor.RED + "ポータルが見つかりませんでした。");
            }
        } else {
            sender.sendMessage(ChatColor.RED + "ポータルが見つかりませんでした。");
        }
    }
}