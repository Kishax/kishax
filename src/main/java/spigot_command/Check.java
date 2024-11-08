package spigot_command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.google.inject.Inject;

import common.Luckperms;
import net.md_5.bungee.api.ChatColor;
import spigot.FMCCoords;

public class Check {
    private final common.Main plugin;
    private final Luckperms lp;
    @Inject
    public Check(common.Main plugin, Luckperms lp) {
        this.plugin = plugin;
        this.lp = lp;
    }

    public void execute(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
        if (sender instanceof Player player) {
            String playerName = player.getName();
            int permLevel = lp.getPermLevel(playerName);
            if (permLevel < 1) {
                player.sendMessage(ChatColor.RED + "まだFMCのWEB認証が完了していません。");
                player.teleport(FMCCoords.ROOM_POINT.getLocation());
            } else {
                player.sendMessage(ChatColor.GREEN + "WEB認証...PASS\n\nALL CORRECT");
                player.sendMessage(ChatColor.GREEN + "3秒後にハブに移動します。");
                new BukkitRunnable() {
                    int countdown = 3;
                    @Override
                    public void run() {
                        if (countdown <= 0) {
                            player.teleport(FMCCoords.HUB_POINT.getLocation());
                            cancel();
                            return;
                        }
                        player.sendMessage(ChatColor.AQUA + String.valueOf(countdown));
                        countdown--;
                    }
                }.runTaskTimer(plugin, 0, 20);
            }
        } else {
            if (sender != null) {
                sender.sendMessage("プレイヤーからのみ実行可能です。");
            }
        }
    }
}
