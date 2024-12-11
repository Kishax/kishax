package keyp.forev.fmc.spigot.cmd.sub.teleport;

import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import com.google.inject.Inject;

import keyp.forev.fmc.common.server.Luckperms;
import keyp.forev.fmc.spigot.events.EventListener;
import keyp.forev.fmc.spigot.settings.FMCCoords;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatColor;

public class TeleportBack implements TabExecutor {
    private final Luckperms lp;
    @Inject
    public TeleportBack(Luckperms lp) {
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
                    Component message = Component.text("テレポート前の座標に戻りました。")
                        .color(NamedTextColor.GREEN)
                        .decorate(TextDecoration.BOLD);

                    player.sendMessage(message);

                    player.teleport(EventListener.playerBeforeLocationMap.get(player));
                    EventListener.playerBeforeLocationMap.remove(player);
                } else {
                    Component message = Component.text("テレポート前の座標がありません。")
                        .color(NamedTextColor.RED)
                        .decorate(TextDecoration.BOLD);
                    player.sendMessage(message);
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
