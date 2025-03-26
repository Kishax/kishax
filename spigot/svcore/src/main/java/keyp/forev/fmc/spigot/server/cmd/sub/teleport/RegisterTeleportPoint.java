package keyp.forev.fmc.spigot.server.cmd.sub.teleport;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.google.inject.Inject;
import com.google.inject.Provider;

import keyp.forev.fmc.common.settings.PermSettings;
import keyp.forev.fmc.common.socket.message.Message;
import keyp.forev.fmc.common.socket.SocketSwitch;
import keyp.forev.fmc.common.util.JavaUtils;
import keyp.forev.fmc.spigot.server.textcomponent.TCUtils;
import keyp.forev.fmc.spigot.server.textcomponent.TCUtils2;
import keyp.forev.fmc.spigot.util.RunnableTaskUtil;
import keyp.forev.fmc.spigot.util.interfaces.MessageRunnable;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatColor;
import keyp.forev.fmc.common.server.Luckperms;
import keyp.forev.fmc.common.server.interfaces.ServerHomeDir;
import keyp.forev.fmc.common.database.Database;
import org.slf4j.Logger;
import keyp.forev.fmc.spigot.server.menu.Type;

public class RegisterTeleportPoint implements TabExecutor {
  private final BukkitAudiences audiences;
  private final Logger logger;
  private final Database db;
  private final Luckperms lp;
  private final RunnableTaskUtil rt;
  private final String thisServerName;
  private final Provider<SocketSwitch> sswProvider;

  @Inject
  public RegisterTeleportPoint(BukkitAudiences audiences, Logger logger, Database db, Luckperms lp, RunnableTaskUtil rt, ServerHomeDir shd, Provider<SocketSwitch> sswProvider) {
    this.audiences = audiences;
    this.logger = logger;
    this.db = db;
    this.lp = lp;
    this.rt = rt;
    this.sswProvider = sswProvider;
    this.thisServerName = shd.getServerName();
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
    if (sender instanceof Player) {
      Player player = (Player) sender;
      String playerName = player.getName();
      String playerUUID = player.getUniqueId().toString();
      if (!lp.hasPermission(playerName, PermSettings.TELEPORT_REGISTER_POINT.get())) {
        player.sendMessage(ChatColor.RED + "権限がありません。");
        return true;
      }

      if (rt.checkIsOtherInputMode(player, RunnableTaskUtil.Key.TELEPORT_REGISTER_POINT)) {
        player.sendMessage(ChatColor.RED + "他でインプット中のため処理を続行できません。");
        return true;
      }

      Location loc = player.getLocation();
      if (loc == null) {
        player.sendMessage(ChatColor.RED + "座標の取得に失敗しました。");
        return true;
      }

      final double x = loc.getX();
      final double y = loc.getY();
      final double z = loc.getZ();
      final float yaw = loc.getYaw();
      final float pitch = loc.getPitch();

      if (loc.getWorld() instanceof World) {
        World playerWorld = loc.getWorld();
        final String worldName = playerWorld.getName();
        Component titleOrComment = Component.text("タイトル:コメント")
          .color(NamedTextColor.GOLD)
          .decorate(
            TextDecoration.BOLD,
            TextDecoration.ITALIC,
            TextDecoration.UNDERLINED)
          .hoverEvent(HoverEvent.showText(Component.text("クリックして入力")))
          .clickEvent(ClickEvent.suggestCommand("タイトル:コメント"));

        Component example = Component.text("(例)")
          .appendSpace()
          .append(Component.text("おうち:いえにかえれるよ。"))
          .color(NamedTextColor.GRAY);

        Component note = Component.text("※タイトルとコメントは「:」で区切ってください。")
          .color(NamedTextColor.GRAY);

        Component note2 = Component.text("※タイトルのみの場合、「:」は必要ありません。")
          .color(NamedTextColor.GRAY);

        final double roundX = JavaUtils.roundToFirstDecimalPlace(x);
        final double roundY = JavaUtils.roundToFirstDecimalPlace(y);
        final double roundZ = JavaUtils.roundToFirstDecimalPlace(z);

        Component currentCoord = Component.text("現在地: " + worldName + "(" + roundX + ", " + roundY + ", " + roundZ + ")")
          .color(NamedTextColor.GOLD)
          .decorate(
            TextDecoration.BOLD,
            TextDecoration.UNDERLINED);

        TextComponent messages = Component.text()
          .append(currentCoord)
          .appendNewline()
          .append(Component.text("現在地をテレポートポイントに登録しますか？"))
          .appendNewline()
          .append(Component.text("登録する場合は"))
          .append(titleOrComment)
          .append(Component.text("を入力してください。"))
          .appendNewline()
          .append(note)
          .appendNewline()
          .append(note2)
          .appendNewline()
          .append(example)
          .appendNewline()
          .appendNewline()
          .append(Component.text("処理を中断する場合は"))
          .append(TCUtils.ZERO.get())
          .append(Component.text("と入力してください。"))
          .appendNewline()
          .append(TCUtils.INPUT_MODE.get())
          .build();

        audiences.player(player).sendMessage(messages);

        Map<RunnableTaskUtil.Key, MessageRunnable> playerActions = new HashMap<>();
        playerActions.put(RunnableTaskUtil.Key.TELEPORT_REGISTER_POINT, (input) -> {
          audiences.player(player).sendMessage(TCUtils2.getResponseComponent(input));
          switch (input) {
            case "0" -> {
              rt.removeCancelTaskRunnable(player, RunnableTaskUtil.Key.TELEPORT_REGISTER_POINT);
              Component message = Component.text("処理を中断しました。")
                .color(NamedTextColor.RED)
                .decorate(TextDecoration.BOLD);

              audiences.player(player).sendMessage(message);
            }
            default -> {
              final String title;
              final String comment;
              if (input.contains(":") || input.contains("：")) {
                // 「:」もしくは全角の「：」が複数ある場合は処理を中断
                if (input.split("[:：]").length > 2) {
                  Component errorMessage = Component.text("コメントの開始位置が決定できません。")
                    .appendNewline()
                    .append(Component.text("「:」もしくは全角の「：」は1つだけ入力してください。"))
                    .color(NamedTextColor.RED);

                  audiences.player(player).sendMessage(errorMessage);

                  rt.extendTask(player, RunnableTaskUtil.Key.TELEPORT_REGISTER_POINT);
                  return;
                }

                String[] titleAndComment = input.split("[:：]", 2);
                title = titleAndComment[0];
                comment = titleAndComment[1];
              } else {
                title = input;
                comment = "";
              }

              try {
                rt.removeCancelTaskRunnable(player, RunnableTaskUtil.Key.TELEPORT_REGISTER_POINT);

                TextComponent note3 = Component.text()
                  .append(Component.text("プライベートは自分だけが飛べるポイントを、"))
                  .appendNewline()
                  .append(Component.text("パブリックは全員が共有で飛べるポイントを指します。"))
                  .color(NamedTextColor.GRAY)
                  .decorate(TextDecoration.ITALIC)
                  .build();

                TextComponent message = Component.text()
                  .append(Component.text("パブリック")
                    .decorate(TextDecoration.UNDERLINED))
                  .append(Component.text("にする場合は"))
                  .append(TCUtils.ONE.get())
                  .append(Component.text("と入力してください。"))
                  .appendNewline()
                  .append(Component.text("プライベート")
                    .decorate(TextDecoration.UNDERLINED))
                  .append(Component.text("にする場合は"))
                  .append(TCUtils.TWO.get())
                  .append(Component.text("と入力してください。"))
                  .appendNewline()
                  .appendNewline()
                  .append(note3)
                  .appendNewline()
                  .append(Component.text("処理を中断する場合は"))
                  .append(TCUtils.ZERO.get())
                  .append(Component.text("と入力してください。"))
                  .appendNewline()
                  .append(TCUtils.INPUT_MODE.get())
                  .build();

                audiences.player(player).sendMessage(message);

                Map<RunnableTaskUtil.Key, MessageRunnable> playerActions2 = new HashMap<>();
                playerActions2.put(RunnableTaskUtil.Key.TELEPORT_REGISTER_POINT, (input2) -> {
                  audiences.player(player).sendMessage(TCUtils2.getResponseComponent(input2));
                  switch (input2) {
                    case "0" -> {
                      rt.removeCancelTaskRunnable(player, RunnableTaskUtil.Key.TELEPORT_REGISTER_POINT);
                      Component message2 = Component.text("処理を中断しました。")
                        .color(NamedTextColor.RED)
                        .decorate(TextDecoration.BOLD);

                      audiences.player(player).sendMessage(message2);
                      return;
                    }
                    case "1", "2" -> {
                      //boolean isPublic = input2.equals("1");
                      final String type;
                      switch (input2) {
                        case "1" -> type = Type.TELEPORT_POINT_PUBLIC.getPersistantKey();
                        case "2" -> type = Type.TELEPORT_POINT_PRIVATE.getPersistantKey();
                        default -> {
                          rt.removeCancelTaskRunnable(player, RunnableTaskUtil.Key.TELEPORT_REGISTER_POINT);
                          player.sendMessage(ChatColor.RED + "処理中にエラーが発生しました。");
                          throw new IllegalArgumentException("Invalid input: " + input2);
                        }
                      }

                      try {
                        rt.removeCancelTaskRunnable(player, RunnableTaskUtil.Key.TELEPORT_REGISTER_POINT);
                        try (Connection conn = db.getConnection()) {
                          db.updateLog(conn, "INSERT INTO tp_points (name, uuid, title, comment, x, y, z, yaw, pitch, world, server, type) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", new Object[] {playerName, playerUUID, title, comment, x, y, z, yaw, pitch, worldName, thisServerName, type});

                          Component message3 = Component.text("テレポートポイントを登録しました。")
                            .color(NamedTextColor.GREEN)
                            .decorate(TextDecoration.BOLD);

                          audiences.player(player).sendMessage(message3);
                        } catch (SQLException | ClassNotFoundException e) {
                          player.sendMessage(ChatColor.RED + "座標の保存に失敗しました。");
                          logger.error("A SQLException | ClassNotFoundException error occurred: " + e.getMessage());
                          for (StackTraceElement element : e.getStackTrace()) {
                            logger.error(element.toString());
                          }
                        }

                        Message msg = new Message();
                        msg.mc = new Message.Minecraft();
                        msg.mc.cmd = new Message.Minecraft.Command();
                        msg.mc.cmd.teleport = new Message.Minecraft.Command.Teleport();
                        msg.mc.cmd.teleport.point = new Message.Minecraft.Command.Teleport.Point();
                        msg.mc.cmd.teleport.point.who = new Message.Minecraft.Who();
                        msg.mc.cmd.teleport.point.who.name = playerName;
                        msg.mc.cmd.teleport.point.name = title;
                        msg.mc.cmd.teleport.point.register = true;

                        SocketSwitch ssw = sswProvider.get();
                        try (Connection conn2 = db.getConnection()) {
                          ssw.sendVelocityServer(conn2, msg);
                        } catch (SQLException | ClassNotFoundException e) {
                          logger.info("An error occurred at Menu#teleportPointMenu: {}", e);
                        }
                      } catch (Exception e) {
                        player.sendMessage("処理中にエラーが発生しました。");
                      }
                    }
                    default -> {
                      Component errorMessage2 = Component.text("無効な入力です。")
                        .color(NamedTextColor.RED);

                      audiences.player(player).sendMessage(errorMessage2);
                      rt.extendTask(player, RunnableTaskUtil.Key.TELEPORT_REGISTER_POINT);
                    }
                  }
                });
                rt.addTaskRunnable(player, playerActions2, RunnableTaskUtil.Key.TELEPORT_REGISTER_POINT);
              } catch (Exception e) {
                player.sendMessage("処理中にエラーが発生しました。");
              }
            }
          }
        });
        rt.addTaskRunnable(player, playerActions, RunnableTaskUtil.Key.TELEPORT_REGISTER_POINT);
      } else {
        player.sendMessage(ChatColor.RED + "ワールドの取得に失敗しました。");
        rt.removeCancelTaskRunnable(player, RunnableTaskUtil.Key.TELEPORT_REGISTER_POINT);
        throw new NullPointerException("World is null.");
      }
    } else {
      Component errorMessage = Component.text("プレイヤーのみが実行できます。")
        .color(NamedTextColor.RED);
      audiences.console().sendMessage(errorMessage);
    }
    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
    return Collections.emptyList();
  }
}
