package spigot_command;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.slf4j.Logger;

import com.google.inject.Inject;

import common.Luckperms;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

public class TeleportAccepter {
    public static Map<Player, Set<Player>> teleportMap = new ConcurrentHashMap<>();
    private final common.Main plugin;
    private final Logger logger;
    private final Luckperms lp;
    @Inject
    public TeleportAccepter(common.Main plugin, Logger logger, Luckperms lp) {
        this.plugin = plugin;
        this.logger = logger;
        this.lp = lp;
    }
    
    public void execute(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
        if (sender instanceof Player player) {
            // /tp <player>
            if (args.length < 1) {
                player.sendMessage(ChatColor.RED + "引数が不足しています。");
                return;
            }
            String targetName = args[0];
            Player targetPlayer = plugin.getServer().getPlayer(targetName);
            if (targetPlayer == null) {
                player.sendMessage(ChatColor.RED + "プレイヤーが見つかりません。");
                return;
            }
            teleportRequest(player, targetPlayer);
        }
    }

    private void teleportRequest(Player player, Player targetPlayer) {
        if (player.equals(targetPlayer)) {
            player.sendMessage(ChatColor.RED + "自分自身にはテレポートできません。");
            return;
        }
        Set<Player> targetPlayers = new HashSet<>();
        if (TeleportAccepter.teleportMap.containsKey(player)) {
            Set<Player> playerTargetPlayers = TeleportAccepter.teleportMap.get(player);
            if (playerTargetPlayers.contains(targetPlayer)) {
                player.sendMessage(ChatColor.RED + "既にリクエストを送信しています。");
                return;
            }
            targetPlayers.addAll(playerTargetPlayers);
        }
        TextComponent message = new TextComponent(player.getName() + "があなたにテレポートをリクエストしています。\n");
        TextComponent accept = new TextComponent("[受け入れる]");
        accept.setBold(true);
        accept.setColor(ChatColor.GREEN);
        accept.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("クリックしてリクエストを受け入れます。")));
        accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpaccept " + player.getName()));
        TextComponent deny = new TextComponent("[拒否する]");
        deny.setBold(true);
        deny.setColor(ChatColor.RED);
        deny.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("クリックしてリクエストを拒否します。")));
        deny.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpdeny " + player.getName()));
        message.addExtra(accept);
        message.addExtra(" ");
        message.addExtra(deny);
        targetPlayer.spigot().sendMessage(message);
        TextComponent message1 = new TextComponent("リクエストを送信しました。\n");
        message1.setColor(ChatColor.GREEN);
        message1.setBold(true);
        TextComponent message2 = new TextComponent("リクエストは60秒後に自動的にキャンセルされます。");
        message2.setColor(ChatColor.GRAY);
        message2.setItalic(true);
        player.spigot().sendMessage(message1, message2);
        targetPlayers.add(targetPlayer);
        TeleportAccepter.teleportMap.put(player, targetPlayers);
        new BukkitRunnable() {
            int countdown = 60;
            @Override
            public void run() {
                if (countdown <= 0) {
                    // teleportMapの値のセットからtargetPlayerを削除
                    Set<Player> targetPlayers = TeleportAccepter.teleportMap.get(player);
                    targetPlayers.removeIf(entry -> entry.equals(targetPlayer));
                    TeleportAccepter.teleportMap.put(player, targetPlayers);
                    cancel();
                    return;
                }
                countdown--;
            }
        }.runTaskTimer(plugin, 0, 60);
    }
}
