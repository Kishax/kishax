package keyp.forev.fmc.spigot.cmd.sub.teleport;

import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import com.google.inject.Inject;

import keyp.forev.fmc.common.server.DefaultLuckperms;
import keyp.forev.fmc.spigot.events.EventListener;
import keyp.forev.fmc.spigot.settings.FMCCoords;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;

public class Back implements TabExecutor {
    private final DefaultLuckperms lp;
    @Inject
    public Back(DefaultLuckperms lp) {
        this.lp = lp;
    }

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
        if (sender instanceof Player player) {
            String playerName = player.getName();
            int permLevel = lp.getPermLevel(playerName);
            if (permLevel < 1) {
                player.sendMessage(ChatColor.RED + "まだFMCのWEB認証が完了していません。");
                player.teleport(FMCCoords.ROOM_POINT.getLocation());
            } else {
                if (EventListener.playerBeforeLocationMap.containsKey(player)) {
                    TextComponent message = new TextComponent("テレポート前の座標に戻ります。");
                    message.setBold(true);
                    message.setColor(ChatColor.GREEN);
                    player.spigot().sendMessage(message);
                    player.teleport(EventListener.playerBeforeLocationMap.get(player));
                    EventListener.playerBeforeLocationMap.remove(player);
                } else {
                    TextComponent message = new TextComponent("テレポート前の座標がありません。");
                    message.setBold(true);
                    message.setColor(ChatColor.RED);
                    player.spigot().sendMessage(message);
                }
            }
        } else {
            if (sender != null) {
                sender.sendMessage("プレイヤーからのみ実行可能です。");
            }
        }
        return true;
    }

    @Override
	public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
        return Collections.emptyList();
    }
}
