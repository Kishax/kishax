package net.kishax.mc.velocity.server.cmd.sub;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import net.kishax.mc.common.database.Database;
import net.kishax.mc.common.server.Luckperms;
import net.kishax.mc.common.settings.PermSettings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class Hub implements SimpleCommand {
  private final ProxyServer server;
  private final Logger logger;
  private final Database db;
  private final Luckperms lp;
  private final String[] subcommands = { "hub" };

  @Inject
  public Hub(ProxyServer server, Logger logger, Database db, Luckperms lp) {
    this.server = server;
    this.logger = logger;
    this.db = db;
    this.lp = lp;
  }

  @Override
  public void execute(Invocation invocation) {
    CommandSource source = invocation.source();
    if (!(source instanceof Player)) {
      if (source != null) {
        source.sendMessage(Component.text("このコマンドはプレイヤーのみが実行できます。").color(NamedTextColor.RED));
      }
      return;
    }
    Player player = (Player) source;
    if (!lp.hasPermission(player.getUsername(), PermSettings.HUB.get())) {
      source.sendMessage(Component.text("権限がありません。").color(NamedTextColor.RED));
      return;
    }
    String query = "SELECT * FROM status WHERE hub = 1 LIMIT 1;";
    try (Connection conn = db.getConnection();
        PreparedStatement ps = conn.prepareStatement(query)) {
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          String serverName = rs.getString("name");
          player.sendMessage(Component.text("Sending you to the hub..."));
          this.server.getServer(serverName)
              .ifPresent(presentServer -> player.createConnectionRequest(presentServer).fireAndForget());
        } else {
          source.sendMessage(Component.text("Hub server is not available.").color(NamedTextColor.RED));
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

  @Override
  public List<String> suggest(Invocation invocation) {
    CommandSource source = invocation.source();
    String[] args = invocation.arguments();
    List<String> ret = new ArrayList<>();
    switch (args.length) {
      case 0, 1 -> {
        for (String subcmd : subcommands) {
          if (!source.hasPermission("kishax.proxy." + subcmd))
            continue;
          ret.add(subcmd);
        }
        return ret;
      }
    }
    return Collections.emptyList();
  }
}
