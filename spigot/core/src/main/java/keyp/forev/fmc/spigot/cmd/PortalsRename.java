package keyp.forev.fmc.spigot.cmd;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import com.google.inject.Inject;

import keyp.forev.fmc.common.JavaUtils;
import keyp.forev.fmc.spigot.util.PortalsConfig;

public class PortalsRename {
    private final PortalsConfig psConfig;

    @Inject
    public PortalsRename(PortalsConfig psConfig) {
        this.psConfig = psConfig;
    }

    @SuppressWarnings("unchecked")
    public void execute(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
        if (args.length > 2) {
            if (args.length > 3) {
                String portalUUID = args[2];
                boolean isUUID = JavaUtils.isUUID(portalUUID);
                String newName = args[3];
                FileConfiguration portalsConfig = psConfig.getPortalsConfig();
                List<Map<?, ?>> portals = (List<Map<?, ?>>) portalsConfig.getList("portals");
                if (portals != null) {
                    boolean portalFind = false;
                    Map<String, Object> portalFound = new HashMap<>();
                    for (Map<String, Object> portal : (List<Map<String, Object>>) (List<?>) portals) {
                        if (isUUID && portalUUID.equals(portal.get("uuid"))) {
                            portalFound = portal;
                            portalFind = true;
                        } else if (!isUUID && portalUUID.equals(portal.get("name"))) {
                            portalFound = portal;
                            portalFind = true;
                        }
                        if (newName.equals(portal.get("name"))) {
                            sender.sendMessage(ChatColor.RED + "名前が重複しています。");
                            return;
                        }
                    }
                    if (portalFind) {
                        portalFound.put("name", newName);
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
        } else {
            sender.sendMessage("Usage: /fmc portal rename <portalUUID> <newName>");
        }
    }
}