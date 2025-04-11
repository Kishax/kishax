package f5.si.kishax.mc.velocity.socket.message.handlers.minecraft.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import f5.si.kishax.mc.common.database.Database;
import f5.si.kishax.mc.common.socket.message.Message;
import f5.si.kishax.mc.common.socket.message.handlers.interfaces.minecraft.ServerActionHandler;
import f5.si.kishax.mc.velocity.server.BroadCast;
import f5.si.kishax.mc.velocity.server.events.EventListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class VelocityServerActionHandler implements ServerActionHandler {
  private final Logger logger;
  private final ProxyServer server;
  private final Database db;
  private final BroadCast bc;
  private final ConsoleCommandSource console;

  @Inject
  public VelocityServerActionHandler(Logger logger, ProxyServer server, Database db, BroadCast bc,
      ConsoleCommandSource console) {
    this.logger = logger;
    this.server = server;
    this.db = db;
    this.bc = bc;
    this.console = console;
  }

  @Override
  public void handle(Message.Minecraft.Server mserver) {
    String serverName = mserver.name;

    switch (mserver.action) {
      case "START" -> {
        TextComponent component = Component.text()
            .append(
                Component.text(serverName + "サーバーが起動しました。").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
            .build();
        for (Player player : server.getAllPlayers()) {
          if (player.hasPermission("group.new-user")) {
            player.sendMessage(component);
          }
        }
        if (EventListener.startingServers.contains(serverName)) {
          EventListener.startingServers.remove(serverName);
        }

        console.sendMessage(Component.text(serverName + "サーバーが起動しました。").color(NamedTextColor.GREEN));
      }
      case "STOP" -> {
        TextComponent component = Component.text(serverName + "サーバーが停止しました。").color(NamedTextColor.RED);
        for (Player player : server.getAllPlayers()) {
          if (player.hasPermission("group.new-user")) {
            player.sendMessage(component);
          }
        }

        console.sendMessage(Component.text(serverName + "サーバーが停止しました。").color(NamedTextColor.DARK_PURPLE));
        String endPath = getEndScriptPath(serverName);
        if (endPath != null) {
          execScript(endPath);
        }
      }
      case "EMPTY_STOP" -> {
        bc.broadCastMessage(Component.text("プレイヤー不在のため、" + serverName + "サーバーを停止させます。").color(NamedTextColor.RED));
      }
    }
  }

  private void execScript(String scriptPath) {
    try {
      ProcessBuilder processBuilder = new ProcessBuilder(scriptPath);
      processBuilder.redirectErrorStream(true);

      Process process = processBuilder.start();

      try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
        String line;
        while ((line = reader.readLine()) != null) {
          logger.info(line);
        }
      }

      // int exitCode = process.waitFor();
      // logger.info("Bash script exited with code: " + exitCode);
    } catch (Exception e) {
      logger.error("An error occurred at VelocitySocketResponse#execScript: ", e);
    }
  }

  private String getEndScriptPath(String serverName) {
    try (Connection conn = db.getConnection()) {
      try (PreparedStatement ps = conn.prepareStatement(
          "SELECT * FROM status WHERE name=?;")) {
        ps.setString(1, serverName);
        try (ResultSet rs = ps.executeQuery()) {
          if (rs.next()) {
            String path = rs.getString("end_script");
            if (path != null && !path.isBlank()) {
              return path;
            }
          }
        }
      }
    } catch (SQLException | ClassNotFoundException e) {
      logger.error("An error occurred at VelocitySocketResponse#execEndScript: ", e);
    }

    return null;
  }
}
