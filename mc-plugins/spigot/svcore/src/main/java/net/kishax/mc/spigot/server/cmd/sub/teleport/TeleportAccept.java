package net.kishax.mc.spigot.server.cmd.sub.teleport;

import java.sql.Connection;
import java.sql.SQLException;
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
import com.google.inject.Provider;

import net.kishax.mc.common.database.Database;
import net.kishax.mc.common.server.Luckperms;
import net.kishax.mc.common.settings.PermSettings;
import net.kishax.mc.common.socket.SocketSwitch;
import net.kishax.mc.common.socket.message.Message;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

public class TeleportAccept implements TabExecutor {
  private final JavaPlugin plugin;
  private final BukkitAudiences audiences;
  private final Logger logger;
  private final Database db;
  private final Luckperms lp;
  private final Provider<SocketSwitch> sswProvider;

  @Inject
  public TeleportAccept(JavaPlugin plugin, BukkitAudiences audiences, Logger logger, Database db, Luckperms lp,
      Provider<SocketSwitch> sswProvider) {
    this.plugin = plugin;
    this.audiences = audiences;
    this.logger = logger;
    this.db = db;
    this.lp = lp;
    this.sswProvider = sswProvider;
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
                                            // logger.info("targetName: " + targetName);
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

                audiences.player(player).sendMessage(message);

                audiences.player(targetPlayer).sendMessage(message);

                Message msg = new Message();
                msg.mc = new Message.Minecraft();
                msg.mc.cmd = new Message.Minecraft.Command();
                msg.mc.cmd.teleport = new Message.Minecraft.Command.Teleport();
                msg.mc.cmd.teleport.player = new Message.Minecraft.Command.Teleport.Player();
                msg.mc.cmd.teleport.player.who = new Message.Minecraft.Who();
                msg.mc.cmd.teleport.player.who.name = targetName;
                msg.mc.cmd.teleport.player.target = playerName;
                msg.mc.cmd.teleport.player.reverse = false;

                SocketSwitch ssw = sswProvider.get();
                try (Connection conn = db.getConnection()) {
                  ssw.sendVelocityServer(conn, msg);
                } catch (SQLException | ClassNotFoundException e) {
                  logger.info("An error occurred at Menu#teleportPointMenu: {}", e);
                }

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

                audiences.player(player).sendMessage(messages);

                audiences.player(targetPlayer).sendMessage(messages);

                Message msg = new Message();
                msg.mc = new Message.Minecraft();
                msg.mc.cmd = new Message.Minecraft.Command();
                msg.mc.cmd.teleport = new Message.Minecraft.Command.Teleport();
                msg.mc.cmd.teleport.player = new Message.Minecraft.Command.Teleport.Player();
                msg.mc.cmd.teleport.player.who = new Message.Minecraft.Who();
                msg.mc.cmd.teleport.player.who.name = targetName;
                msg.mc.cmd.teleport.player.target = playerName;
                msg.mc.cmd.teleport.player.reverse = true;

                SocketSwitch ssw = sswProvider.get();
                try (Connection conn = db.getConnection()) {
                  ssw.sendVelocityServer(conn, msg);
                } catch (SQLException | ClassNotFoundException e) {
                  logger.info("An error occurred at Menu#teleportPointMenu: {}", e);
                }

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

                audiences.player(player).sendMessage(messagePlayer);

                Component messageTargetPlayer = Component.text(playerName + "がテレポートリクエストを拒否しました。")
                    .color(NamedTextColor.RED)
                    .decorate(TextDecoration.BOLD);

                audiences.player(targetPlayer).sendMessage(messageTargetPlayer);

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

                audiences.player(player).sendMessage(messagePlayer);

                Component messageTargetPlayer = Component.text(playerName + "が逆テレポートリクエストを拒否しました。")
                    .color(NamedTextColor.RED)
                    .decorate(TextDecoration.BOLD);

                audiences.player(targetPlayer).sendMessage(messageTargetPlayer);

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
                if (player.equals(requestedPlayer))
                  continue;
                ret.add(requestedPlayer.getName());
              }
            }
            case "tprma" -> {
              Set<Player> requestedPlayers = TeleportRequest.teleportMeMap.entrySet().stream()
                  .filter(entry -> entry.getValue().stream().anyMatch(map -> map.containsKey(player)))
                  .map(Map.Entry::getKey)
                  .collect(Collectors.toSet());
              for (Player requestedPlayer : requestedPlayers) {
                if (player.equals(requestedPlayer))
                  continue;
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
