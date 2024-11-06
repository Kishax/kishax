package spigot_command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.google.inject.Inject;

import spigot.FMCCoords;
import spigot.Luckperms;

public class Check {
    private final Luckperms lp;
    @Inject
    public Check(Luckperms lp) {
        this.lp = lp;
    }
    public void execute(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
        if (sender instanceof Player player) {
            String playerName = player.getName();
            if (lp.hasPermission(playerName, "new-fmc-user")) {
                player.teleport(FMCCoords.ROOM_POINT.getLocation());
            } else {
                player.teleport(FMCCoords.HUB_POINT.getLocation());
            }
        } else {
            if (sender != null) {
                sender.sendMessage("プレイヤーからのみ実行可能です。");
            }
        }
    }
}
