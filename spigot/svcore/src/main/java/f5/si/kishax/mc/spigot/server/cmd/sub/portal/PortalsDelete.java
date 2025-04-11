package f5.si.kishax.mc.spigot.server.cmd.sub.portal;

import java.util.List;
import java.util.Map;

import org.bukkit.command.CommandSender;

import com.google.inject.Inject;

import f5.si.kishax.mc.spigot.util.config.PortalsConfig;
import net.md_5.bungee.api.ChatColor;

public class PortalsDelete {
  private final PortalsConfig psConfig;

  @Inject
  public PortalsDelete(PortalsConfig psConfig) {
    this.psConfig = psConfig;
  }

  public void execute(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
    if (args.length > 2) {
      String portalName = args[2];
      List<Map<?, ?>> portals = psConfig.getListMap("portals");
      if (portals != null) {
        portals.removeIf(portal -> 
          portalName.equals(portal.get("uuid")) ||
          portalName.equals(portal.get("name")) ||
          portalName.equals(portal.get("corner1")) ||
          portalName.equals(portal.get("corner2"))
        );

        try {
          psConfig.replaceValue("portals", portals);
          sender.sendMessage(ChatColor.GREEN + "ポータル " + portalName + " が削除されました。");
        } catch (Exception e) {
          sender.sendMessage(ChatColor.RED + "ポータルの削除に失敗しました。");
          return;
        }
      } else {
        sender.sendMessage(ChatColor.RED + "ポータルが見つかりませんでした。");
      }
    } else {
      sender.sendMessage("Usage: /kishax portal delete <portalUUID>");
    }
  }
}
