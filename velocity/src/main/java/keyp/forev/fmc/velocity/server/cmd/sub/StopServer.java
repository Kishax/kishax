package keyp.forev.fmc.velocity.server.cmd.sub;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import keyp.forev.fmc.common.database.Database;
import keyp.forev.fmc.common.server.Luckperms;
import keyp.forev.fmc.common.socket.message.Message;
import keyp.forev.fmc.common.socket.SocketSwitch;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import keyp.forev.fmc.velocity.discord.MessageEditor;
import keyp.forev.fmc.velocity.server.BroadCast;
import keyp.forev.fmc.velocity.server.DoServerOnline;

public class StopServer {
  private final ProxyServer server;
  private final Logger logger;
  private final Database db;
  private final ConsoleCommandSource console;
  private final BroadCast bc;
  private final DoServerOnline dso;
  private final MessageEditor discordME;
  private final Luckperms lp;
  private final Provider<SocketSwitch> sswProvider;
  private String currentServerName = null;

  @Inject
  public StopServer (ProxyServer server, Logger logger, Database db, ConsoleCommandSource console, MessageEditor discordME, BroadCast bc, DoServerOnline dso, Luckperms lp, Provider<SocketSwitch> sswProvider) {
    this.server = server;
    this.logger = logger;
    this.db = db;
    this.console = console;
    this.bc = bc;
    this.discordME = discordME;
    this.dso = dso;
    this.lp = lp;
    this.sswProvider = sswProvider;
  }

  public void execute(@NotNull CommandSource source, String[] args) {
    if (args.length < 2) {
      source.sendMessage(Component.text("引数が足りません。").color(NamedTextColor.RED));
      return;
    }

    String targetServerName = args[1];
    if (!(source instanceof Player)) {
      source.sendMessage(Component.text("このコマンドはプレイヤーのみが実行できます。").color(NamedTextColor.RED));
      return;
    }

    Player player = (Player) source;
    String playerName = player.getUsername(),
      playerUUID = player.getUniqueId().toString();

    if (args.length == 1 || targetServerName == null || targetServerName.isEmpty()) {
      player.sendMessage(Component.text("サーバー名を入力してください。").color(NamedTextColor.RED));
      return;
    }

    player.getCurrentServer().ifPresent(serverConnection -> {
      RegisteredServer registeredServer = serverConnection.getServer();
      currentServerName = registeredServer.getServerInfo().getName();
    });
    boolean containsServer = server.getAllServers().stream()
      .anyMatch(registeredServer -> registeredServer.getServerInfo().getName().equalsIgnoreCase(targetServerName));
    if (!containsServer) {
      player.sendMessage(Component.text("サーバー名が違います。").color(NamedTextColor.RED));
      return;
    }

    int permLevel = lp.getPermLevel(playerName);
    if (permLevel < 2) {
      player.sendMessage(Component.text("権限がありません。").color(NamedTextColor.RED));
      return;
    }

    try (Connection conn = db.getConnection()) {
      Map<String, Map<String, Object>> statusMap = dso.loadStatusTable(conn);
      statusMap.entrySet().stream()
        .filter(entry -> entry.getKey() instanceof String && entry.getKey().equals(targetServerName))
        .forEach(entry -> {
          Map<String, Object> serverInfo = entry.getValue();
          if (serverInfo.get("online") instanceof Boolean online && !online) {
            player.sendMessage(Component.text(targetServerName+"サーバーは停止しています。").color(NamedTextColor.RED));
            logger.info(targetServerName+"サーバーは停止しています。");
            return;
          }

          Message msg = new Message();
          msg.mc = new Message.Minecraft();
          msg.mc.server = new Message.Minecraft.Server();
          msg.mc.server.action = "ADMIN_STOP";
          msg.mc.server.name = targetServerName;

          SocketSwitch ssw = sswProvider.get();
          try (Connection connection = db.getConnection()) {
            if (ssw.sendSpecificServer(connection, msg)) {
              db.insertLog(connection, "INSERT INTO log (name, uuid, server, sss, status) VALUES (?, ?, ?, ?, ?);", new Object[] {playerName, playerUUID, currentServerName, true, "stop"});
              try {
                discordME.AddEmbedSomeMessage("Stop", player, targetServerName);
              } catch (Exception e) {
                logger.error("An exception occurred while executing the AddEmbedSomeMessage method: {}", e.getMessage());
                for (StackTraceElement ste : e.getStackTrace()) {
                  logger.error(ste.toString());
                }
              }

              TextComponent component = Component.text()
                .append(Component.text("WEB認証...PASS")
                .appendNewline()
                .append(Component.text("アドミン認証...PASS"))
                .appendNewline()
                .appendNewline()
                .append(Component.text("ALL CORRECT"))
                .appendNewline()
                .color(NamedTextColor.GREEN))
                .append(Component.text(targetServerName+"サーバーがまもなく停止します。").color(NamedTextColor.RED))
                .build();
              player.sendMessage(component);
              TextComponent notifyComponent = Component.text()
                .append(Component.text(player.getUsername() + "が" + targetServerName + "サーバーを停止させました。")
                .appendNewline()
                .append(Component.text("まもなく" + targetServerName + "サーバーが停止します。"))
                .color(NamedTextColor.RED))
                .build();
              bc.sendExceptPlayerMessage(notifyComponent, player.getUsername());
              console.sendMessage(Component.text(targetServerName+"サーバーがまもなく停止します。").color(NamedTextColor.RED));
            } else {
              player.sendMessage(Component.text(targetServerName + "サーバーとの通信に失敗しました。").color(NamedTextColor.RED));
              logger.error(targetServerName + "サーバーとの通信に失敗しました。");
            }
          } catch (SQLException | ClassNotFoundException e) {
            logger.error("A SQLException | ClassNotFoundException error occurred: {}", e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
              logger.error(element.toString());
            }
          }
        });
    } catch (SQLException | ClassNotFoundException e) {
      logger.error("An SQLException | ClassNotFoundException error occurred: " + e.getMessage());
      for (StackTraceElement element : e.getStackTrace()) {
        logger.error(element.toString());
      }
    }
  }
}
