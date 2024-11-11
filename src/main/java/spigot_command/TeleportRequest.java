package spigot_command;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
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
import spigot.TCUtils;

public class TeleportRequest implements TabExecutor {
    //public static Map<Player, Set<Player>> teleportMap = new ConcurrentHashMap<>();
    //public static final Map<Player, BukkitTask> teleportTasks = new ConcurrentHashMap<>();
    public static Map<Player, List<Map<Player, BukkitTask>>> teleportMap = new ConcurrentHashMap<>();
    public static Map<Player ,List<Map<Player, BukkitTask>>> teleportMeMap = new ConcurrentHashMap<>();
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
            if (lp.hasPermission(player.getName(), PermSettings.TPR.get())) {
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
            String cmdName = cmd.getName();
            switch (cmdName.toLowerCase()) {
                case "tpr" -> teleportRequest(player, targetPlayer);
                case "tprm" -> teleportMeRequest(player, targetPlayer);
            }
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
        String cmdName = cmd.getName();
    	switch (args.length) {
	    	case 1 -> {
                if (sender instanceof Player player) {
                    switch (cmdName.toLowerCase()) {
                        case "tpr", "tprm" -> {
                            for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                                if (player.equals(onlinePlayer)) continue;
                                ret.add(onlinePlayer.getName());
                            }
                        }
                    }
                }
				return StringUtil.copyPartialMatches(args[0].toLowerCase(), ret, new ArrayList<>());
			}
        }
        return Collections.emptyList();
    }

    private void teleportMeRequest(Player player, Player targetPlayer) {
        teleportRequest(player, targetPlayer, true);
    }

    private void teleportRequest(Player player, Player targetPlayer) {
        teleportRequest(player, targetPlayer, false);
    }

    private void teleportRequest(Player player, Player targetPlayer, boolean me) {
        if (player.equals(targetPlayer)) {
            player.sendMessage(ChatColor.RED + "自分自身にはテレポートできません。");
            return;
        }
        if (!me ? TeleportRequest.teleportMap.containsKey(player) : TeleportRequest.teleportMeMap.containsKey(player)) {
            List<Map<Player, BukkitTask>> requestedPlayers = !me ? TeleportRequest.teleportMap.getOrDefault(targetPlayer, new ArrayList<>()) : TeleportRequest.teleportMeMap.getOrDefault(targetPlayer, new ArrayList<>());
            if (!requestedPlayers.isEmpty()) {
                boolean isRequested = false;
                for (Map<Player, BukkitTask> requestedPlayer : requestedPlayers) {
                    if (requestedPlayer.containsKey(player)) {
                        isRequested = true;
                    }
                }
                if (isRequested) {
                    player.sendMessage(ChatColor.RED + "既にリクエストを送信しています。");
                    return;
                }
            }
        }
        String playerName = player.getName(),
            targetName = targetPlayer.getName();
        try (Connection conn = db.getConnection()) {
            TextComponent message = new TextComponent(playerName + "があなたにテレポートを" + (!me ? "" : "逆") +"リクエストしています。\n");
            message.setColor(ChatColor.GOLD);
            if (teleportMessageType(conn, targetName)) {
                TextComponent message1 = new TextComponent("3秒後にインベントリを開きます。\n");
                message1.setBold(true);
                message1.setUnderlined(true);
                message1.setColor(ChatColor.GOLD);
                // fmc menu tp requests playerNameをまだ実装していないため、以下コメント
                //message1.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/fmc menu server before"));
                //message1.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("直近で起動したサーバーインベントリを開きます。")));
                targetPlayer.spigot().sendMessage(message, message1, TCUtils.SETTINGS_ENTER.get());
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (!me) {
                        menu.teleportResponseMenu(targetPlayer, player);
                    } else {
                        menu.teleportMeResponseMenu(targetPlayer, player);
                    }
                }, 60L);
            } else {
                TextComponent accept = new TextComponent("[受け入れる]");
                accept.setBold(true);
                accept.setColor(ChatColor.GREEN);
                accept.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("クリックしてリクエストを受け入れます。")));
                accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, !me ? "/tpra " + player.getName() : "/tprma " + player.getName()));
                TextComponent deny = new TextComponent("[拒否する]");
                deny.setBold(true);
                deny.setColor(ChatColor.RED);
                deny.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("クリックしてリクエストを拒否します。")));
                deny.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, !me ? "/tprd " + player.getName() : "/tprmd " + player.getName()));
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
        List<Map<Player, BukkitTask>> requestedPlayers = !me ? teleportMap.getOrDefault(targetPlayer, new ArrayList<>()) : teleportMeMap.getOrDefault(targetPlayer, new ArrayList<>());
        BukkitTask task = new BukkitRunnable() {
            int countdown = 60;
            @Override
            public void run() {
                if (countdown <= 0) {
                    TextComponent message = new TextComponent("リクエストがタイムアウトしました。");
                    message.setColor(ChatColor.RED);
                    message.setBold(true);
                    player.spigot().sendMessage(message);
                    targetPlayer.spigot().sendMessage(message);
                    List<Map<Player, BukkitTask>> futureList = !me ? TeleportRequest.teleportMap.get(player) : TeleportRequest.teleportMeMap.get(player);
                    if (futureList != null) {
                        futureList.removeIf(future -> {
                            if (future.containsKey(targetPlayer)) {
                                future.get(targetPlayer).cancel();
                                return true;
                            }
                            return false;
                        });
                        if (futureList.isEmpty()) {
                            if (!me) {
                                TeleportRequest.teleportMap.remove(player);
                            } else {
                                TeleportRequest.teleportMeMap.remove(player);
                            }
                        }
                    }
                    cancel();
                    return;
                }
                countdown--;
            }
        }.runTaskTimer(plugin, 0, 20); // 20 ticks = 1 second
        requestedPlayers.add(Map.of(targetPlayer, task));
        if (!me) {
            teleportMap.put(player, requestedPlayers);
        } else {
            teleportMeMap.put(player, requestedPlayers);
        }
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
