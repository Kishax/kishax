package keyp.forev.fmc.spigot.server.cmd.sub.imagemap;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.google.inject.Inject;

import keyp.forev.fmc.common.settings.PermSettings;
import keyp.forev.fmc.common.util.ExtUtil;
import keyp.forev.fmc.common.util.PatternUtil;
import keyp.forev.fmc.spigot.server.ImageMap;
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
import keyp.forev.fmc.common.database.Database;
import org.slf4j.Logger;

public class RegisterImageMap implements TabExecutor {
  private final BukkitAudiences audiences;
  private final Logger logger;
  private final Database db;
  private final Luckperms lp;
  private final RunnableTaskUtil rt;
  private final ImageMap im;

  @Inject
  public RegisterImageMap(BukkitAudiences audiences, Logger logger, Database db, Luckperms lp, RunnableTaskUtil rt, ImageMap im) {
    this.audiences = audiences;
    this.logger = logger;
    this.db = db;
    this.lp = lp;
    this.rt = rt;
    this.im = im;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
    if (sender instanceof Player) {
      Player player = (Player) sender;
      String playerName = player.getName();
      if (!lp.hasPermission(playerName, PermSettings.IMAGEMAP_REGISTER_MAP.get())) {
        player.sendMessage(ChatColor.RED + "権限がありません。");
        return true;
      }

      if (rt.checkIsOtherInputMode(player, RunnableTaskUtil.Key.IMAGEMAP_REGISTER_MAP)) {
        player.sendMessage(ChatColor.RED + "他でインプット中のため処理を続行できません。");
        return true;
      }

      Component imageUrl = Component.text("画像URL")
        .color(NamedTextColor.GOLD)
        .decorate(
          TextDecoration.BOLD, 
          TextDecoration.UNDERLINED, 
          TextDecoration.ITALIC);

      Component discord = Component.text("discord")
        .color(NamedTextColor.BLUE)
        .decorate(TextDecoration.BOLD, TextDecoration.UNDERLINED);

      TextComponent cmdNote = Component.text()
        .append(Component.text("URLの長さが255文字を越える場合は、").color(NamedTextColor.GRAY))
        .append(discord)
        .append(Component.text("でも登録できます！").color(NamedTextColor.GRAY))
        .appendNewline()
        .append(Component.text("コマンド名:").color(NamedTextColor.GRAY))
        .appendSpace()
        .append(Component.text("/fmc image_add_q").color(NamedTextColor.GRAY).decorate(TextDecoration.UNDERLINED))
        .decorate(TextDecoration.ITALIC)
        .build();

      TextComponent messages = Component.text()
        .append(Component.text("画像マップを登録しますか？"))
        .appendNewline()
        .append(Component.text("登録する場合は"))
        .append(imageUrl)
        .append(Component.text("を入力してください。"))
        .appendNewline()
        .append(cmdNote)
        .appendNewline()
        .append(Component.text("処理を中断する場合は"))
        .append(TCUtils.ZERO.get())
        .append(Component.text("と入力してください。"))
        .appendNewline()
        .append(TCUtils.INPUT_MODE.get())
        .build();

      audiences.player(player).sendMessage(messages);

      Map<RunnableTaskUtil.Key, MessageRunnable> playerActions = new HashMap<>();

      playerActions.put(RunnableTaskUtil.Key.IMAGEMAP_REGISTER_MAP, (input) -> {
        audiences.player(player).sendMessage(TCUtils2.getResponseComponent(input));

        switch (input) {
          case "0" -> {
            rt.removeCancelTaskRunnable(player, RunnableTaskUtil.Key.IMAGEMAP_REGISTER_MAP);
            Component message = Component.text("処理を中断しました。")
              .color(NamedTextColor.RED)
              .decorate(TextDecoration.BOLD);

            audiences.player(player).sendMessage(message);
          }
          default -> {
            if (!PatternUtil.URL.check(input)) {
              Component errorMessage = Component.text("URLパターンに一致しません！")
                .color(NamedTextColor.RED)
                .decorate(TextDecoration.BOLD);

              TextComponent errorMessages = Component.text()
                .append(errorMessage)
                .appendNewline()
                .append(Component.text("適切な"))
                .append(imageUrl)
                .append(Component.text("を再入力してください。"))
                .build();

              audiences.player(player).sendMessage(errorMessages);
              rt.extendTask(player, RunnableTaskUtil.Key.IMAGEMAP_REGISTER_MAP);
              return;
            }

            try {
              URL getUrl = new URI(input).toURL();
              if (ExtUtil.getExtension(getUrl) == null) {
                Component errorMessage = Component.text("指定のURLは規定の拡張子を持ちません。")
                  .color(NamedTextColor.RED)
                  .decorate(TextDecoration.BOLD);

                TextComponent errorMessages = Component.text()
                  .append(errorMessage)
                  .appendNewline()
                  .append(Component.text("適切な"))
                  .append(imageUrl)
                  .append(Component.text("を再入力してください。"))
                  .build();

                audiences.player(player).sendMessage(errorMessages);
                rt.extendTask(player, RunnableTaskUtil.Key.IMAGEMAP_REGISTER_MAP);
                return;
              }
            } catch (IOException | URISyntaxException e) {
              Component errorMessage = Component.text("指定の画像URLより通信ができませんでした。")
                .color(NamedTextColor.RED)
                .decorate(TextDecoration.BOLD);

              TextComponent errorMessages = Component.text()
                .append(errorMessage)
                .appendNewline()
                .append(Component.text("適切な"))
                .append(imageUrl)
                .append(Component.text("を再入力してください。"))
                .build();

              audiences.player(player).sendMessage(errorMessages);
              rt.extendTask(player, RunnableTaskUtil.Key.IMAGEMAP_REGISTER_MAP);

              logger.info("An error occurred at RegisterImageMap#onCommand: {}", e);
              return;
            }

            final String url = input;
            try {
              rt.removeCancelTaskRunnable(player, RunnableTaskUtil.Key.IMAGEMAP_REGISTER_MAP);

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
                .append(Component.text("がぞう:いいかんじのがぞう。"))
                .color(NamedTextColor.GRAY);

              Component note = Component.text("※タイトルとコメントは「:」で区切ってください。")
                .color(NamedTextColor.GRAY);

              Component note2 = Component.text("※タイトルのみの場合、「:」は必要ありません。")
                .color(NamedTextColor.GRAY);

              TextComponent nextMessages = Component.text()
                .append(Component.text("次に、"))
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

              audiences.player(player).sendMessage(nextMessages);

              Map<RunnableTaskUtil.Key, MessageRunnable> playerActions2 = new HashMap<>();
              playerActions2.put(RunnableTaskUtil.Key.IMAGEMAP_REGISTER_MAP, (input2) -> {
                audiences.player(player).sendMessage(TCUtils2.getResponseComponent(input2));

                final String title;
                final String comment;

                if (input2.contains(":") || input2.contains("：")) {
                  // 「:」もしくは全角の「：」が複数ある場合は処理を中断
                  if (input2.split("[:：]").length > 2) {
                    Component errorMessage = Component.text("コメントの開始位置が決定できません。")
                      .appendNewline()
                      .append(Component.text("「:」もしくは全角の「：」は1つだけ入力してください。"))
                      .color(NamedTextColor.RED);

                    audiences.player(player).sendMessage(errorMessage);

                    rt.extendTask(player, RunnableTaskUtil.Key.IMAGEMAP_REGISTER_MAP);
                    return;
                  }

                  String[] titleAndComment = input2.split("[:：]", 2);
                  title = titleAndComment[0];
                  comment = titleAndComment[1];
                } else {
                  title = input2;
                  comment = "";
                }

                rt.removeCancelTaskRunnable(player, RunnableTaskUtil.Key.IMAGEMAP_REGISTER_MAP);

                String[] passArgs = new String[] {url, title, comment};
                try (Connection conn = db.getConnection()) {
                  im.leadAction(conn, player, RunnableTaskUtil.Key.IMAGEMAP_CREATE_IMAGE_MAP_FROM_MENU, passArgs, null);
                } catch (ClassNotFoundException | SQLException e) {
                  player.sendMessage(ChatColor.RED + "データベースとの通信にエラーが発生しました。");
                  logger.info("An error occurred at RegisterImage#onCommand: {}", e);
                }
              });
              rt.addTaskRunnable(player, playerActions2, RunnableTaskUtil.Key.IMAGEMAP_REGISTER_MAP);
            } catch (Exception e) {
              logger.error("An error occurred in RegisterImageMap##onCommand: {}", e);
            }
          }
        }
      });
    rt.addTaskRunnable(player, playerActions, RunnableTaskUtil.Key.IMAGEMAP_REGISTER_MAP);
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

