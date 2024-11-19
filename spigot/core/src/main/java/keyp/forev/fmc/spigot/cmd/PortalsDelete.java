package keyp.forev.fmc.cmd;

import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import com.google.inject.Inject;

import spigot.core.main.PortalsConfig;

public class PortalsDelete {
    private final PortalsConfig psConfig;

    @Inject
    public PortalsDelete(PortalsConfig psConfig) {
        this.psConfig = psConfig;
    }

    public void execute(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
        if (args.length > 2) {
            String portalName = args[2];
            FileConfiguration portalsConfig = psConfig.getPortalsConfig();
            @SuppressWarnings("unchecked")
            List<Map<?, ?>> portals = (List<Map<?, ?>>) portalsConfig.getList("portals");

            if (portals != null) {
                portals.removeIf(portal -> 
                    portalName.equals(portal.get("uuid")) ||
                    portalName.equals(portal.get("name")) ||
                    portalName.equals(portal.get("corner1")) ||
                    portalName.equals(portal.get("corner2"))
                );
                portalsConfig.set("portals", portals);
                psConfig.savePortalsConfig();
                psConfig.reloadPortalsConfig();
                sender.sendMessage(ChatColor.GREEN + "ポータル " + portalName + " が削除されました。");
            } else {
                sender.sendMessage(ChatColor.RED + "ポータルが見つかりませんでした。");
            }
        } else {
            sender.sendMessage("Usage: /fmc portal delete <portalUUID>");
        }
    }
}