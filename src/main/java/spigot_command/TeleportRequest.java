package spigot_command;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.StringUtil;
import org.slf4j.Logger;

import com.google.inject.Inject;

import common.Database;
import common.Luckperms;
import common.PermSettings;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

public class TeleportRequest implements TabExecutor {
    public static Map<Player, Set<Player>> teleportMap = new ConcurrentHashMap<>();
    private final common.Main plugin;
    private final Logger logger;
    private final Database db;
    private final Luckperms lp;
    private final Menu menu;
    @Inject
    public TeleportRequest(common.Main plugin, Logger logger, Database db, Luckperms lp, Menu menu) {
        this.plugin = plugin;
        this.logger = logger;
        this.db = db;
        this.lp = lp;
        this.menu = menu;
    }
    
    @Override
	public boolean onCommand(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
        if (sender instanceof Player player) {
            if (lp.hasPermission(player.getName(), PermSettings.TPA.get())) {
                player.sendMessage(ChatColor.RED + "権限がありません。");
                return true;
            }
            if (args.length < 1) {
                player.sendMessage(ChatColor.RED + "引数が不足しています。");
                return true;
            }
            String targetName = args[0];
            Player targetPlayer = plugin.getServer().getPlayer(targetName);
            if (targetPlayer == null) {
                player.sendMessage(ChatColor.RED + "プレイヤーが見つかりません。");
                return true;
            }
            teleportRequest(player, targetPlayer);
        } else {
            if (sender != null) {
                sender.sendMessage(ChatColor.RED + "このコマンドはプレイヤーのみ実行可能です。");
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
                    for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                        if (player.equals(onlinePlayer)) continue;
                        ret.add(onlinePlayer.getName());
                    }
                }
				return StringUtil.copyPartialMatches(args[0].toLowerCase(), ret, new ArrayList<>());
			}
        }
        return Collections.emptyList();
    }

    private void teleportRequest(Player player, Player targetPlayer) {
        if (player.equals(targetPlayer)) {
            player.sendMessage(ChatColor.RED + "自分自身にはテレポートできません。");
            return;
        }
        Set<Player> targetPlayers = new HashSet<>();
        if (TeleportRequest.teleportMap.containsKey(player)) {
            Set<Player> playerTargetPlayers = TeleportRequest.teleportMap.get(player);
            if (playerTargetPlayers.contains(targetPlayer)) {
                player.sendMessage(ChatColor.RED + "既にリクエストを送信しています。");
                return;
            }
            targetPlayers.addAll(playerTargetPlayers);
        }
        String playerName = player.getName(),
            targetName = targetPlayer.getName();
        try (Connection conn = db.getConnection()) {
            if (teleportMessageType(conn, targetName)) {
                // ここ、逆になることに注意
                menu.teleportResponseMenu(targetPlayer, player);
            } else {
                TextComponent message = new TextComponent(playerName + "があなたにテレポートをリクエストしています。\n");
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
            }
            TextComponent message1 = new TextComponent("リクエストを送信しました。\n");
                message1.setColor(ChatColor.GREEN);
                message1.setBold(true);
                TextComponent message2 = new TextComponent("リクエストは60秒後に自動的にキャンセルされます。");
                message2.setColor(ChatColor.GRAY);
                message2.setItalic(true);
                player.spigot().sendMessage(message1, message2);
        } catch (ClassNotFoundException | SQLException e) {
            player.closeInventory();
            player.sendMessage(ChatColor.RED + "データベースとの通信に失敗しました。");
			logger.error("A ClassNotFoundException | SQLException error occurred: {}", e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                logger.error(element.toString());
            }
		}
        targetPlayers.add(targetPlayer);
        TeleportRequest.teleportMap.put(player, targetPlayers);
        new BukkitRunnable() {
            int countdown = 60;
            @Override
            public void run() {
                if (countdown <= 0) {
                    // teleportMapの値のセットからtargetPlayerを削除
                    Set<Player> targetPlayers = TeleportRequest.teleportMap.get(player);
                    targetPlayers.removeIf(entry -> entry.equals(targetPlayer));
                    TeleportRequest.teleportMap.put(player, targetPlayers);
                    cancel();
                    return;
                }
                countdown--;
            }
        }.runTaskTimer(plugin, 0, 60);
    }

    private boolean teleportMessageType(Connection conn, String playerName) throws SQLException, ClassNotFoundException {
        String query = "SELECT tptype FROM members WHERE name = ?;";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, playerName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("tptype");
                }
            }
        }
        return false;
    }
}
