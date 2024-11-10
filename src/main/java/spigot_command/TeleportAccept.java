package spigot_command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import com.google.inject.Inject;

import common.PermSettings;
import net.md_5.bungee.api.ChatColor;

public class TeleportAccept implements TabExecutor {
    private final common.Main plugin;
    private final common.Luckperms lp;
    @Inject
    public TeleportAccept(common.Main plugin, common.Luckperms lp) {
        this.plugin = plugin;
        this.lp = lp;
    }

    @Override
	public boolean onCommand(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
        if (sender instanceof Player player) {
            if (lp.hasPermission(player.getName(), PermSettings.TPA.get())) {
                player.sendMessage(ChatColor.RED + "権限がありません。");
                return true;
            }
            if (args.length < 1) {
                player.sendMessage("引数が不足しています。");
                return true;
            }
            String targetName = args[0];
            Player targetPlayer = plugin.getServer().getPlayer(targetName);
            if (targetPlayer == null) {
                player.sendMessage(ChatColor.RED + "プレイヤーが見つかりません。");
                return true;
            }
            Map<Player, Set<Player>> teleportMap = TeleportRequest.teleportMap;
            if (teleportMap.containsKey(targetPlayer) && teleportMap.get(targetPlayer).contains(player)) {
                targetPlayer.teleport(player);
                teleportMap.get(targetPlayer).remove(player);
                player.sendMessage(ChatColor.GREEN + "テレポートしました。");
            } else {
                player.sendMessage(ChatColor.RED + "リクエストが見つかりません。");
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
        List<String> ret = new ArrayList<>();
    	switch (args.length) {
	    	case 0 -> {
                if (sender instanceof Player player) {
                    Map<Player, Set<Player>> teleportMap = TeleportRequest.teleportMap;
                    Set<Player> requestedPlayers = teleportMap.entrySet().stream()
                        .filter(entry -> entry.getValue().contains(player))
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toSet());
                    for (Player requestedPlayer : requestedPlayers) {
                        if (player.equals(requestedPlayer)) continue;
                        ret.add(requestedPlayer.getName());
                    }
                }
				return StringUtil.copyPartialMatches(args[0].toLowerCase(), ret, new ArrayList<>());
			}
        }
        return Collections.emptyList();
    }
}
