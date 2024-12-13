package keyp.forev.fmc.spigot.server.cmd.sub.teleport;

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

import keyp.forev.fmc.common.database.Database;
import keyp.forev.fmc.common.server.Luckperms;
import keyp.forev.fmc.common.settings.PermSettings;
import keyp.forev.fmc.spigot.server.menu.Menu;
import keyp.forev.fmc.spigot.server.textcomponent.TCUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class TeleportRequest implements TabExecutor {
    //public static Map<Player, Set<Player>> teleportMap = new ConcurrentHashMap<>();
    //public static final Map<Player, BukkitTask> teleportTasks = new ConcurrentHashMap<>();
    public static Map<Player, List<Map<Player, BukkitTask>>> teleportMap = new ConcurrentHashMap<>();
    public static Map<Player ,List<Map<Player, BukkitTask>>> teleportMeMap = new ConcurrentHashMap<>();
    private final JavaPlugin plugin;
    private final Logger logger;
    private final Database db;
    private final Luckperms lp;
    private final Menu menu;
    @Inject
    public TeleportRequest(JavaPlugin plugin, Logger logger, Database db, Luckperms lp, Menu menu) {
        this.plugin = plugin;
        this.logger = logger;
        this.db = db;
        this.lp = lp;
        this.menu = menu;
    }
    
    @Override
	public boolean onCommand(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
        if (sender instanceof Player player) {
            if (!lp.hasPermission(player.getName(), PermSettings.TPR.get())) {
                Component errorMessage = Component.text("権限がありません。")
                    .color(NamedTextColor.RED);
                player.sendMessage(errorMessage);
                return true;
            }
            if (args.length < 1) {
                Component errorMessage = Component.text("引数が不足しています。")
                    .color(NamedTextColor.RED);
                player.sendMessage(errorMessage);
                return true;
            }
            String targetName = args[0];
            Player targetPlayer = plugin.getServer().getPlayer(targetName);
            if (targetPlayer == null) {
                Component errorMessage = Component.text("プレイヤーが見つかりません。")
                    .color(NamedTextColor.RED);
                player.sendMessage(errorMessage);
                return true;
            }
            String cmdName = cmd.getName();
            switch (cmdName.toLowerCase()) {
                case "tpr" -> teleportRequest(player, targetPlayer);
                case "tprm" -> teleportMeRequest(player, targetPlayer);
            }
        } else {
            if (sender != null) {
                Component errorMessage = Component.text("このコマンドはプレイヤーのみ実行可能です。")
                    .color(NamedTextColor.RED);
                sender.sendMessage(errorMessage);
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
            Component errorMessage = Component.text("自分自身にはテレポートできません。")
                .color(NamedTextColor.RED);
            player.sendMessage(errorMessage);
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
                    Component errorMessage = Component.text("既にリクエストを送信しています。")
                        .color(NamedTextColor.RED);
                    player.sendMessage(errorMessage);
                    return;
                }
            }
        }
        String playerName = player.getName(),
            targetName = targetPlayer.getName();
        try (Connection conn = db.getConnection()) {
            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append(playerName)
                .append("があなたにテレポートを")
                .append(!me ? "" : "逆")
                .append("リクエストしています。");
            Component message = Component.text(messageBuilder.toString())
                .color(NamedTextColor.GOLD);
            
            if (teleportMessageType(conn, targetName)) {
                TextComponent messages = Component.text()
                    .append(message)
                    .appendNewline()
                    .append(TCUtils.LATER_OPEN_INV_3.get())
                    .appendNewline()
                    .append(TCUtils.SETTINGS_ENTER.get())
                    .build();

                targetPlayer.sendMessage(messages);

                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (!me) {
                        menu.teleportResponseMenu(targetPlayer, player);
                    } else {
                        menu.teleportMeResponseMenu(targetPlayer, player);
                    }
                }, 60L);
            } else {
                StringBuilder cmdAccept = new StringBuilder();
                cmdAccept.append(!me ? "/tpra " : "/tprma ")
                    .append(player.getName());

                Component accept = Component.text("[受け入れる]")
                    .color(NamedTextColor.GREEN)
                    .decorate(TextDecoration.BOLD)
                    .hoverEvent(Component.text("クリックしてリクエストを受け入れます。"))
                    .clickEvent(ClickEvent.runCommand(cmdAccept.toString()));

                StringBuilder cmdDeny = new StringBuilder();
                cmdDeny.append(!me ? "/tprd " : "/tprmd ")
                    .append(player.getName());

                Component deny = Component.text("[拒否する]")
                    .color(NamedTextColor.RED)
                    .decorate(TextDecoration.BOLD)
                    .hoverEvent(Component.text("クリックしてリクエストを拒否します。"))
                    .clickEvent(ClickEvent.runCommand(cmdDeny.toString()));
                
                TextComponent messages = Component.text()
                    .append(message)
                    .append(accept)
                    .appendSpace()
                    .append(deny)
                    .build();

                targetPlayer.sendMessage(messages);
            }

            Component message2 = Component.text("リクエストを送信しました。")
                .appendNewline()
                .color(NamedTextColor.GREEN)
                .decorate(TextDecoration.BOLD);
            
            Component message3 = Component.text("リクエストは60秒後に自動的にキャンセルされます。")
                .color(NamedTextColor.GRAY)
                .decorate(TextDecoration.ITALIC);

            player.sendMessage(message2.append(message3));
        } catch (ClassNotFoundException | SQLException e) {
            player.closeInventory();
            Component errorMessage = Component.text("データベースとの通信に失敗しました。")
                .color(NamedTextColor.RED);
            player.sendMessage(errorMessage);
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
                    Component timeout = Component.text("リクエストがタイムアウトしました。")
                        .color(NamedTextColor.RED)
                        .decorate(TextDecoration.BOLD);
                    
                    player.sendMessage(timeout);

                    targetPlayer.sendMessage(timeout);

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
