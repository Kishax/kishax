package keyp.forev.fmc.spigot.server.cmd.sub.teleport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.StringUtil;

import com.google.inject.Inject;

import keyp.forev.fmc.common.server.Luckperms;
import keyp.forev.fmc.common.settings.PermSettings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public class TeleportAccept implements TabExecutor {
    private final JavaPlugin plugin;
    private final Luckperms lp;
    @Inject
    public TeleportAccept(JavaPlugin plugin, Luckperms lp) {
        this.plugin = plugin;
        this.lp = lp;
    }

    @Override
	public boolean onCommand(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
        if (sender instanceof Player player) {
            if (!lp.hasPermission(player.getName(), PermSettings.TPR.get())) {
                player.sendMessage(ChatColor.RED + "権限がありません。");
                return true;
            }
            if (args.length < 1) {
                player.sendMessage("引数が不足しています。");
                return true;
            }
            String targetName = args[0];
            String playerName = player.getName(); // リクエストを選択する側
            //logger.info("targetName: " + targetName);
            Player targetPlayer = plugin.getServer().getPlayer(targetName);
            if (targetPlayer == null) {
                player.sendMessage(ChatColor.RED + "プレイヤーが見つかりません。");
                return true;
            }
            String cmdName = cmd.getName();
            switch (cmdName.toLowerCase()) {
                case "tpra" -> {
                    Map<Player, List<Map<Player, BukkitTask>>> teleportMap = TeleportRequest.teleportMap;
                    if (teleportMap.containsKey(targetPlayer)) {
                        List<Map<Player, BukkitTask>> requestedPlayers = teleportMap.get(targetPlayer);
                        AtomicBoolean isRequested = new AtomicBoolean(false);
                        for (Map<Player, BukkitTask> requestedPlayer : requestedPlayers) {
                            if (requestedPlayer.containsKey(player)) {
                                isRequested.set(true);
                                BukkitTask task = requestedPlayer.get(player);
                                task.cancel();
                                requestedPlayers.remove(requestedPlayer);
                                targetPlayer.teleport(player);

                                Component message = Component.text("テレポートしました。")
                                    .color(NamedTextColor.GREEN)
                                    .decorate(TextDecoration.BOLD);

                                player.sendMessage(message);

                                targetPlayer.sendMessage(message);

                                return true;
                            }
                        }
                        if (!isRequested.get()) {
                            player.sendMessage(ChatColor.RED + "リクエストが見つかりません。");
                        }
                    } else {
                        player.sendMessage(ChatColor.RED + "リクエストが見つかりません。");
                    }
                }
                case "tprma" -> {
                    Map<Player, List<Map<Player, BukkitTask>>> teleportMeMap = TeleportRequest.teleportMeMap;
                    if (teleportMeMap.containsKey(targetPlayer)) {
                        List<Map<Player, BukkitTask>> requestedPlayers = teleportMeMap.get(targetPlayer);
                        AtomicBoolean isRequested = new AtomicBoolean(false);
                        for (Map<Player, BukkitTask> requestedPlayer : requestedPlayers) {
                            if (requestedPlayer.containsKey(player)) {
                                isRequested.set(true);
                                BukkitTask task = requestedPlayer.get(player);
                                task.cancel();
                                requestedPlayers.remove(requestedPlayer);
                                player.teleport(targetPlayer); // 逆

                                Component messages = Component.text("テレポートしました。")
                                    .color(NamedTextColor.GREEN)
                                    .decorate(TextDecoration.BOLD);

                                player.sendMessage(messages);

                                targetPlayer.sendMessage(messages);
                                
                                return true;
                            }
                        }
                        if (!isRequested.get()) {
                            player.sendMessage(ChatColor.RED + "リクエストが見つかりません。");
                        }
                    } else {
                        player.sendMessage(ChatColor.RED + "リクエストが見つかりません。");
                    }
                }
                case "tprd" -> {
                    Map<Player, List<Map<Player, BukkitTask>>> teleportMap = TeleportRequest.teleportMap;
                    if (teleportMap.containsKey(targetPlayer)) {
                        List<Map<Player, BukkitTask>> requestedPlayers = teleportMap.get(targetPlayer);
                        AtomicBoolean isRequested = new AtomicBoolean(false);
                        for (Map<Player, BukkitTask> requestedPlayer : requestedPlayers) {
                            if (requestedPlayer.containsKey(player)) {
                                isRequested.set(true);
                                BukkitTask task = requestedPlayer.get(player);
                                task.cancel();
                                requestedPlayers.remove(requestedPlayer);

                                Component messagePlayer = Component.text(targetName + "のテレポートリクエストを拒否しました。")
                                    .color(NamedTextColor.RED)
                                    .decorate(TextDecoration.BOLD);

                                player.sendMessage(messagePlayer);

                                Component messageTargetPlayer = Component.text(playerName + "がテレポートリクエストを拒否しました。")
                                    .color(NamedTextColor.RED)
                                    .decorate(TextDecoration.BOLD);

                                targetPlayer.sendMessage(messageTargetPlayer);

                                return true;
                            }
                        }
                        if (!isRequested.get()) {
                            player.sendMessage(ChatColor.RED + "リクエストが見つかりません。");
                        }
                    } else {
                        player.sendMessage(ChatColor.RED + "リクエストが見つかりません。");
                    }
                }
                case "tprmd" -> {
                    Map<Player, List<Map<Player, BukkitTask>>> teleportMeMap = TeleportRequest.teleportMeMap;
                    if (teleportMeMap.containsKey(targetPlayer)) {
                        List<Map<Player, BukkitTask>> requestedPlayers = teleportMeMap.get(targetPlayer);
                        AtomicBoolean isRequested = new AtomicBoolean(false);
                        for (Map<Player, BukkitTask> requestedPlayer : requestedPlayers) {
                            if (requestedPlayer.containsKey(player)) {
                                isRequested.set(true);
                                BukkitTask task = requestedPlayer.get(player);
                                task.cancel();
                                requestedPlayers.remove(requestedPlayer);

                                Component messagePlayer = Component.text(targetName + "からの逆テレポートリクエストを拒否しました。")
                                    .color(NamedTextColor.RED)
                                    .decorate(TextDecoration.BOLD);

                                player.sendMessage(messagePlayer);

                                Component messageTargetPlayer = Component.text(playerName + "が逆テレポートリクエストを拒否しました。")
                                    .color(NamedTextColor.RED)
                                    .decorate(TextDecoration.BOLD);

                                targetPlayer.sendMessage(messageTargetPlayer);
                                
                                return true;
                            }
                        }
                        if (!isRequested.get()) {
                            player.sendMessage(ChatColor.RED + "リクエストが見つかりません。");
                        }
                    } else {
                        player.sendMessage(ChatColor.RED + "リクエストが見つかりません。");
                    }
                }
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
        List<String> ret = new ArrayList<>();
        String cmdName = cmd.getName();
    	switch (args.length) {
	    	case 1 -> {
                if (sender instanceof Player player) {
                    switch (cmdName.toLowerCase()) {
                        case "tpra" -> {
                            Set<Player> requestedPlayers = TeleportRequest.teleportMap.entrySet().stream()
                                .filter(entry -> entry.getValue().stream().anyMatch(map -> map.containsKey(player)))
                                .map(Map.Entry::getKey)
                                .collect(Collectors.toSet());
                            for (Player requestedPlayer : requestedPlayers) {
                                if (player.equals(requestedPlayer)) continue;
                                ret.add(requestedPlayer.getName());
                            }
                        }
                        case "tprma" -> {
                            Set<Player> requestedPlayers = TeleportRequest.teleportMeMap.entrySet().stream()
                                .filter(entry -> entry.getValue().stream().anyMatch(map -> map.containsKey(player)))
                                .map(Map.Entry::getKey)
                                .collect(Collectors.toSet());
                            for (Player requestedPlayer : requestedPlayers) {
                                if (player.equals(requestedPlayer)) continue;
                                ret.add(requestedPlayer.getName());
                            }
                        }
                    }
                    
                }
				return StringUtil.copyPartialMatches(args[0].toLowerCase(), ret, new ArrayList<>());
			}
        }
        return Collections.emptyList();
    }
}
