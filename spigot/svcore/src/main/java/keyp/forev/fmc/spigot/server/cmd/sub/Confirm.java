package keyp.forev.fmc.spigot.server.cmd.sub;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.slf4j.Logger;

import com.google.inject.Inject;

import keyp.forev.fmc.common.database.Database;
import keyp.forev.fmc.common.server.Luckperms;
import keyp.forev.fmc.common.settings.FMCSettings;
import keyp.forev.fmc.common.util.OTPGenerator;
import keyp.forev.fmc.spigot.server.ImageMap;
import keyp.forev.fmc.common.server.interfaces.ServerHomeDir;
import keyp.forev.fmc.spigot.server.textcomponent.TCUtils;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.plugin.java.JavaPlugin;

public class Confirm {
  public static final Set<Player> confirmMap = new HashSet<>();
  private final JavaPlugin plugin;
  private final BukkitAudiences audiences;
  private final Logger logger;
  private final Database db;
  private final Luckperms lp;
  private final ImageMap im;
  private final String thisServerName;
  @Inject
  public Confirm(JavaPlugin plugin, BukkitAudiences audiences, Logger logger, Database db, Luckperms lp, ImageMap im, ServerHomeDir shd) {
    this.plugin = plugin;
    this.audiences = audiences;
    this.logger = logger;
    this.db = db;
    this.lp = lp;
    this.im = im;
    this.thisServerName = shd.getServerName();
  }

  public void execute(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
    if (sender instanceof Player player) {
      if (!Confirm.confirmMap.contains(player)) {
        String playerName = player.getName(),
               playerUUID = player.getUniqueId().toString();
        int permLevel = lp.getPermLevel(playerName);
        if (permLevel < 1) {
          try (Connection conn = db.getConnection()) {
            int ifMapId = checkExistConfirmMap(conn, new Object[] {thisServerName, true, playerName});
            Map<String, Object> memberMap = db.getMemberMap(conn, player.getName());
            if (!memberMap.isEmpty()) {
              if (memberMap.get("id") instanceof Integer id) {
                String confirmUrl = FMCSettings.CONFIRM_URL.getValue() + "?n=" + id;
                //player.sendMessage(ChatColor.GREEN + "WEB認証のQRコードを生成します。");
                String[] imageArgs = {"image", "createqr", confirmUrl};
                if (ifMapId == -1) {
                  im.executeImageMapForConfirm(player, imageArgs);
                } else {
                  im.giveMapToPlayer(player, ifMapId);
                }
                int ranum = OTPGenerator.generateOTPbyInt();
                int rsAffected3 = updateSecret2(conn, new Object[] {ranum, playerUUID});
                if (rsAffected3 > 0) {
                  sendConfirmationMessage(player, ranum, confirmUrl);
                } else {
                  Component errorMessage = Component.text("エラーが発生しました。")
                    .color(NamedTextColor.RED);
                  audiences.player(player).sendMessage(errorMessage);
                }
              }
            }
          } catch (SQLException | ClassNotFoundException e2) {
            Component errorMessage = Component.text("WEB認証のQRコード生成に失敗しました。")
              .color(NamedTextColor.RED);
            audiences.player(player).sendMessage(errorMessage);
            logger.error("A SQLException error occurred: " + e2.getMessage());
            for (StackTraceElement element : e2.getStackTrace()) {
              logger.error(element.toString());
            }
          }
        } else {
          Component message = Component.text("WEB認証済みプレイヤーが通過しました！")
            .color(NamedTextColor.GREEN);
          audiences.player(player).sendMessage(message);
        }
        Confirm.confirmMap.add(player);
      }
    } else {
      if (sender != null) {
        sender.sendMessage("プレイヤーからのみ実行可能です。");
      }
    }
  }

  private int updateSecret2(Connection conn, Object[] args) throws SQLException {
    String query = "UPDATE members SET secret2=? WHERE uuid=?;";
    PreparedStatement ps = conn.prepareStatement(query);
    for (int i = 0; i < args.length; i++) {
      ps.setObject(i + 1, args[i]);
    }
    return ps.executeUpdate();
  }

  private void sendConfirmationMessage(Player player, int ranum, String confirmUrl) {
    String ranumstr = Integer.toString(ranum);

    Component webAuth = Component.text("WEB認証")
      .color(NamedTextColor.GOLD)
      .decorate(
        TextDecoration.BOLD,
        TextDecoration.UNDERLINED)
      .clickEvent(ClickEvent.openUrl(confirmUrl))
      .hoverEvent(HoverEvent.showText(Component.text("クリックしてWEB認証ページを開く")));

    Component forUser = Component.newline()
      .appendNewline()
      .append(Component.text("[サイトへのアクセス方法]"))
      .appendNewline()
      .color(NamedTextColor.GOLD)
      .decorate(
        TextDecoration.BOLD,
        TextDecoration.UNDERLINED);

    Component javaUserAccess = Component.text("は、")
      .color(NamedTextColor.GRAY)
      .decorate(TextDecoration.ITALIC);

    Component here = Component.text("ココ")
      .color(NamedTextColor.GOLD)
      .decorate(
        TextDecoration.UNDERLINED,
        TextDecoration.ITALIC)
      .clickEvent(ClickEvent.openUrl(confirmUrl))
      .hoverEvent(HoverEvent.showText(Component.text("クリックしてWEB認証ページを開く")));

    Component javaUserAccess2 = Component.text("をクリックしてアクセスしてね！")
      .appendNewline()
      .color(NamedTextColor.GRAY)
      .decorate(TextDecoration.ITALIC);

    Component bedrockUserAccess = Component.text("は、配布されたQRコードを読み取ってアクセスしてね！")
      .color(NamedTextColor.GRAY)
      .decorate(TextDecoration.ITALIC);

    Component authCode = Component.newline()
      .append(Component.text("認証コードは "))
      .color(NamedTextColor.WHITE);

    Component code = Component.text(ranumstr)
      .color(NamedTextColor.BLUE)
      .decorate(TextDecoration.UNDERLINED)
      .clickEvent(ClickEvent.copyToClipboard(ranumstr))
      .hoverEvent(HoverEvent.showText(Component.text("クリックしてコピー")));

    Component endMessage = Component.text(" です。")
      .color(NamedTextColor.WHITE);

    Component regenerate = Component.newline()
      .append(Component.text("認証コードの再生成"))
      .color(NamedTextColor.GOLD)
      .decorate(
        TextDecoration.BOLD,
        TextDecoration.UNDERLINED)
      .clickEvent(ClickEvent.runCommand("/fmcp retry"))
      .hoverEvent(HoverEvent.showText(Component.text("クリックして認証コードを再生成します。")));

    Component bedrockUserGenerate = Component.text("は、 ")
      .color(NamedTextColor.GRAY)
      .decorate(TextDecoration.ITALIC);

    Component bedrockUserGenerate2 = Component.text("/retry")
      .color(NamedTextColor.GRAY)
      .decorate(
        TextDecoration.UNDERLINED,
        TextDecoration.ITALIC);

    Component bedrockUserGenerate3 = Component.text(" とコマンドを打ってね！")
      .color(NamedTextColor.GRAY)
      .decorate(TextDecoration.ITALIC);

    new BukkitRunnable() {
      int countdown = 50;
      int gCountdown = 3;
      @Override
      public void run() {
        Component message = Component.empty();
        switch (countdown) {
          case 50 -> message = message.append(Component.text("こんにちは！"));
          case 47 -> message = message.append(Component.text("FMCサーバー代表のベラです。"));
          case 44 -> message = message.append(Component.text("サーバーに参加するには、"));
          case 41 -> message = message.append(Component.text("FMCアカウントとMinecraftアカウントをリンクさせる必要があるよ！"));
            case 39 -> {
              message = message.append(webAuth)
                .append(Component.text("より、手続きを進めてね！"));
            }
            case 36 -> message = message.append(forUser);
            case 33 -> {
              message = message.append(TCUtils.JAVA_USER.get())
                .append(javaUserAccess)
                .append(here)
                .append(javaUserAccess2);
            }
            case 30 -> {
              message = message
                .append(TCUtils.BEDROCK_USER.get())
                .append(bedrockUserAccess);
            }
            case 27 -> {
              message = message
                .appendNewline()
                .append(Component.text("認証コードを生成するよ。"));
            }
            case 24, 23, 22 -> {
              message = message.append(countGenerate(gCountdown));
              gCountdown--;
            }
            case 21 -> {
              message = message.append(authCode)
                .append(code)
                .append(endMessage);
            }
            case 18 -> {
              message = message.append(Component.text("認証コードを再生成する場合は、"));
            }
            case 15 -> {
              message = message.append(regenerate)
                .append(Component.text(" をクリックしてね！"))
                .appendNewline();
            }
            case 13 -> {
              message = message.append(TCUtils.BEDROCK_USER.get())
                .append(bedrockUserGenerate)
                .append(bedrockUserGenerate2)
                .append(bedrockUserGenerate3);
            }
            case 10 -> {
              message = message
                .appendNewline()
                .append(Component.text("それでは、楽しいマイクラライフを！"));
            }
            case 0 -> {
              cancel();
              return;
            }
        }
        if (message != Component.empty()) {
          audiences.player(player).sendMessage(message);
        }
        countdown--;
      }
    }.runTaskTimer(plugin, 0, 20);
  }

  private Component countGenerate(int countdown) {
    return Component.text("生成中..." + countdown)
      .color(NamedTextColor.GRAY)
      .decorate(TextDecoration.ITALIC);
  }

  private int checkExistConfirmMap(Connection conn, Object[] args) throws SQLException {
    String query = "SELECT * FROM images WHERE server=? AND confirm=? AND name=?";
    PreparedStatement ps = conn.prepareStatement(query);
    for (int i = 0; i < args.length; i++) {
      ps.setObject(i + 1, args[i]);
    }
    try (ResultSet rs = ps.executeQuery()) {
      if (rs.next()) {
        return rs.getInt("mapid");
      } else {
        return -1;
      }
    }
  }
}
