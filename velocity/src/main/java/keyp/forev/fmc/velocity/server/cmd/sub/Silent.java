package keyp.forev.fmc.velocity.server.cmd.sub;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;

import keyp.forev.fmc.common.database.Database;
import keyp.forev.fmc.common.server.Luckperms;
import keyp.forev.fmc.common.settings.PermSettings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class Silent {
  private final Logger logger;
  private final Database db;
  private final Luckperms lp;
  public static List<String> args1 = new ArrayList<>(Arrays.asList("add","remove","list"));

  @Inject
  public Silent(Logger logger, Database db, Luckperms lp) {
    this.logger = logger;
    this.db = db;
    this.lp = lp;
  }

  public void execute(@NotNull CommandSource source, String[] args) {
    if (source instanceof Player) {
      if (source != null) {
        Player player = (Player) source;
        if (!lp.hasPermission(player.getUsername(), PermSettings.SILENT.get())) {
          source.sendMessage(Component.text("権限がありません。").color(NamedTextColor.RED));
          return;
        }
      }
    }

    switch (args.length) {
      case 0, 1 -> source.sendMessage(Component.text("usage: /fmcp silent <add|remove|list> <player>").color(NamedTextColor.GREEN));
      case 2 -> {
        switch (args[1].toLowerCase()) {
          case "add", "remove" -> source.sendMessage(Component.text("プレイヤー名を入力してください。").color(NamedTextColor.RED));
          case "list" -> {
            TextComponent componentBuilder = Component.text()
              .append(Component.text("FMC Silent Player List")
              .color(NamedTextColor.GOLD)
              .decorate(
                TextDecoration.BOLD,
                TextDecoration.UNDERLINED))
              .build();

            List<CommandSource> silentPlayers = new ArrayList<>();
            String query = "SELECT name FROM members WHERE silent = ?;";
            try (Connection conn = db.getConnection();
                PreparedStatement ps = conn.prepareStatement(query)) {
              ps.setBoolean(1, true);
              try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                  Component addition = Component.newline()
                    .appendSpace()
                    .appendSpace()
                    .append(Component.text("-")
                    .appendSpace()
                    .appendSpace()
                    .append(Component.text(rs.getString("name")))
                    .color(NamedTextColor.GREEN));

                  componentBuilder = componentBuilder.append(addition);

                  silentPlayers.add(source);
                }
              }
            } catch (SQLException | ClassNotFoundException e) {
              logger.error("Error executing query: {}", e.getMessage());
              for (StackTraceElement ste : e.getStackTrace()) {
                logger.error(ste.toString());
              }
              source.sendMessage(Component.text("エラーが発生しました。").color(NamedTextColor.RED));
            }
            if (silentPlayers.isEmpty()) {
              source.sendMessage(Component.text("サイレントモードのプレイヤーはいません。").color(NamedTextColor.RED));
              return;
            } else {
              source.sendMessage(componentBuilder);
            }
          }
          default -> source.sendMessage(Component.text("usage: /fmcp silent <add|remove|list> <player>").color(NamedTextColor.GREEN));
        }
      }
      case 3 -> {
        if (!args1.contains(args[1].toLowerCase())) {
          source.sendMessage(Component.text("usage: /fmcp silent <add|remove|list> <player>").color(NamedTextColor.GREEN));
          return;
        }
        String query = "SELECT * FROM members WHERE name = ?;";
        try (Connection conn = db.getConnection();
            PreparedStatement ps = conn.prepareStatement(query)) {
          ps.setString(1, args[2]);
          try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
              switch (args[1].toLowerCase()) {
                case "add" -> {
                  if (rs.getBoolean("silent")) {
                    source.sendMessage(Component.text("既にサイレントモードです。").color(NamedTextColor.RED));
                    return;
                  }
                  db.updateMemberToggle(conn, "silent", true, args[2]);
                  source.sendMessage(Component.text("サイレントモードに設定しました。").color(NamedTextColor.GREEN));
                }
                case "remove" -> {
                  if (!rs.getBoolean("silent")) {
                    source.sendMessage(Component.text("サイレントモードではありません。").color(NamedTextColor.RED));
                    return;
                  }
                  db.updateMemberToggle(conn, "silent", false, args[2]);
                  source.sendMessage(Component.text("サイレントモードを解除しました。").color(NamedTextColor.GREEN));
                }
              }
            } else {
              source.sendMessage(Component.text("プレイヤーが見つかりません。").color(NamedTextColor.RED));
            }
          }
        } catch (SQLException | ClassNotFoundException e) {
          logger.error("Error executing query: {}", e.getMessage());
          for (StackTraceElement ste : e.getStackTrace()) {
            logger.error(ste.toString());
          }
          source.sendMessage(Component.text("エラーが発生しました。").color(NamedTextColor.RED));
        }
      }
    }
  }

  public List<String> getSilentPlayers(boolean isSilent) {
    List<String> silentPlayers = new ArrayList<>();
    try (Connection conn = db.getConnection()) {
      try (PreparedStatement ps = conn.prepareStatement("SELECT name FROM members WHERE silent = ?;")) {
        ps.setBoolean(1, isSilent);
        try (ResultSet rs = ps.executeQuery()) {
          while (rs.next()) {
            silentPlayers.add(rs.getString("name"));
          }
        }
      }
    } catch (SQLException | ClassNotFoundException e) {
      logger.info("An error occurred while Silent#getSilentPlayers: {}", e) ;
    }
    return silentPlayers;
  }
}
