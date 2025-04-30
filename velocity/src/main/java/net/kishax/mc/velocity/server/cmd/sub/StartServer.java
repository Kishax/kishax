package net.kishax.mc.velocity.server.cmd.sub;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import net.kishax.mc.common.database.Database;
import net.kishax.mc.common.server.Luckperms;
import net.kishax.mc.velocity.discord.MessageEditor;
import net.kishax.mc.velocity.server.BroadCast;
import net.kishax.mc.velocity.server.DoServerOnline;
import net.kishax.mc.velocity.server.PlayerDisconnect;
import net.kishax.mc.velocity.util.config.VelocityConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

public class StartServer {
  private final ProxyServer server;
  private final VelocityConfig config;
  private final Logger logger;
  private final Database db;
  private final ConsoleCommandSource console;
  private final BroadCast bc;
  private final DoServerOnline dso;
  private final MessageEditor discordME;
  private final Luckperms lp;
  private final PlayerDisconnect pd;
  private String currentServerName = null;

  @Inject
  public StartServer (ProxyServer server, Logger logger, VelocityConfig config, Database db, ConsoleCommandSource console, MessageEditor discordME, BroadCast bc, DoServerOnline dso, Luckperms lp, PlayerDisconnect pd) {
    this.server = server;
    this.logger = logger;
    this.config = config;
    this.db = db;
    this.console = console;
    this.bc = bc;
    this.discordME = discordME;
    this.dso = dso;
    this.lp = lp;
    this.pd = pd;
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

    String query = "SELECT * FROM members WHERE uuid=?;";
    try (Connection conn = db.getConnection();
        PreparedStatement ps = conn.prepareStatement(query)) {
      ps.setString(1, player.getUniqueId().toString());
      try (ResultSet minecrafts = ps.executeQuery()) {
        if (minecrafts.next()) {
          Timestamp beforeStartTime = minecrafts.getTimestamp("st");
          if (beforeStartTime != null) {
            long now_timestamp = Instant.now().getEpochSecond();
            long st_timestamp = beforeStartTime.getTime() / 1000L;
            long sa = now_timestamp-st_timestamp;
            long sa_minute = sa/60;
            if (sa_minute <= config.getInt("Interval.Start_Server",0)) {
              player.sendMessage(Component.text("サーバーの起動間隔は"+config.getInt("Interval.Start_Server",0)+"分以上は空けてください。").color(NamedTextColor.RED));
              return;
            }
          }
          Map<String, Map<String, Object>> statusMap = dso.loadStatusTable(conn);
          statusMap.entrySet().stream()
            .filter(entry -> entry.getKey() instanceof String && entry.getKey().equals(targetServerName))
            .forEach(entry -> {
              Map<String, Object> serverInfo = entry.getValue();
              if (serverInfo.get("online") instanceof Boolean online && online) {
                player.sendMessage(Component.text(targetServerName+"サーバーは起動中です。").color(NamedTextColor.RED));
                logger.info(targetServerName+"サーバーは起動中です。");
                return;
              }
              if (serverInfo.get("exec") instanceof String execPath) {
                if (permLevel < 3 && serverInfo.get("enter") instanceof Boolean enter && !enter) {
                  player.sendMessage(Component.text("許可されていません。").color(NamedTextColor.RED));
                  return;
                }
                if (serverInfo.get("memory") instanceof Integer memory) {
                  int currentUsedMemory = dso.getCurrentUsedMemory(statusMap),
                      maxMemory = config.getInt("MaxMemory", 0);
                  int futureMemory = currentUsedMemory + memory;
                  if (maxMemory < futureMemory) {
                    String message = "メモリ超過のため、サーバーを起動できません。(" + futureMemory + "GB/" + maxMemory + "GB)";
                    player.sendMessage(Component.text(message).color(NamedTextColor.RED));
                    logger.info(message);
                    return;
                  }
                  String query3 = "UPDATE members SET st=CURRENT_TIMESTAMP WHERE uuid=?;";
                  try (PreparedStatement ps3 = conn.prepareStatement(query3)) {
                    ps3.setString(1, player.getUniqueId().toString());
                    int rsAffected3 = ps3.executeUpdate();
                    if (rsAffected3 > 0) {
                      try {
                        ProcessBuilder processBuilder = new ProcessBuilder(execPath);
                        processBuilder.start();
                        String query4 = "UPDATE status SET online=? WHERE name=?;";
                        try (PreparedStatement ps4 = conn.prepareStatement(query4)) {
                          ps4.setBoolean(1, true);
                          ps4.setString(2, targetServerName);
                          int rsAffected4 = ps4.executeUpdate();
                          if (rsAffected4 > 0) {
                            try (Connection connection = db.getConnection()) {
                              db.insertLog(connection, "INSERT INTO log (name, uuid, server, sss, status) VALUES (?, ?, ?, ?, ?);", new Object[] {playerName, playerUUID, currentServerName, true, "start"});
                            } catch (SQLException | ClassNotFoundException e) {
                              logger.error("A SQLException | ClassNotFoundException error occurred: {}", e.getMessage());
                              for (StackTraceElement element : e.getStackTrace()) {
                                logger.error(element.toString());
                              }
                            }
                            try {
                              discordME.AddEmbedSomeMessage("Start", player, targetServerName);
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
                              .append(Component.text(targetServerName+"サーバーがまもなく起動します。").color(NamedTextColor.GREEN))
                              .build();
                            player.sendMessage(component);
                            TextComponent notifyComponent = Component.text()
                              .append(Component.text(player.getUsername() + "が" + targetServerName + "サーバーを起動しました。")
                              .appendNewline()
                              .append(Component.text("まもなく" + targetServerName + "サーバーが起動します。"))
                              .color(NamedTextColor.AQUA))
                              .build();
                            bc.sendExceptPlayerMessage(notifyComponent, player.getUsername());
                            console.sendMessage(Component.text(targetServerName+"サーバーがまもなく起動します。").color(NamedTextColor.GREEN));
                          }
                        }
                      } catch (IOException e) {
                        player.sendMessage(Component.text("実行時にエラーが発生しました。").color(NamedTextColor.RED));
                        logger.error(NamedTextColor.RED+"実行時にエラーが発生しました。");
                      }
                    } else {
                      Component errorMessage = Component.text("内部エラーが発生しました。")
                        .appendNewline()
                        .append(Component.text("サーバーを起動できません。"))
                        .color(NamedTextColor.RED);
                      player.sendMessage(errorMessage);
                      logger.error(NamedTextColor.RED + "内部エラーが発生しました。\nサーバーを起動できません。");
                    }
                  } catch (SQLException e) {
                    logger.error("An SQLException error occurred: " + e.getMessage());
                    for (StackTraceElement element : e.getStackTrace()) {
                      logger.error(element.toString());
                    }
                  }
                } else {
                  player.sendMessage(Component.text("メモリが設定されていないため、サーバーを起動できません。").color(NamedTextColor.RED));
                }
              } else {
                player.sendMessage(Component.text("実行パスが設定されていないため、サーバーを起動できません。").color(NamedTextColor.RED));
              }
            });
        } else {
          // MySQLサーバーにプレイヤー情報が登録されてなかった場合
          logger.info(playerName + "のプレイヤー情報がデータベースに登録されていません。");
          player.sendMessage(Component.text(playerName + "のプレイヤー情報がデータベースに登録されていません。").color(NamedTextColor.RED));
        }
      }
    } catch (SQLException | ClassNotFoundException e) {
      logger.error("An SQLException | ClassNotFoundException error occurred: " + e.getMessage());
      for (StackTraceElement element : e.getStackTrace()) {
        logger.error(element.toString());
      }
    }
  }

  // this is called from EventListener#onServerPreConnectEvent
  public void execute2(Player player, String targetServerName) {
    String playerName = player.getUsername(),
      playerUUID = player.getUniqueId().toString();
    int permLevel = lp.getPermLevel(playerName);
    String query = "SELECT * FROM members WHERE uuid=?;";

    try (Connection conn = db.getConnection();
        PreparedStatement ps = conn.prepareStatement(query)) {
      ps.setString(1, player.getUniqueId().toString());
      try (ResultSet minecrafts = ps.executeQuery()) {
        if (minecrafts.next()) {
          Timestamp beforeStartTime = minecrafts.getTimestamp("st");
          if (beforeStartTime != null) {
            long now_timestamp = Instant.now().getEpochSecond();
            long st_timestamp = beforeStartTime.getTime() / 1000L;
            long sa = now_timestamp-st_timestamp;
            long sa_minute = sa/60;
            if (sa_minute <= config.getInt("Interval.Start_Server",0)) {
              pd.playerDisconnect (
                  false,
                  player,
                  Component.text("サーバーの起動間隔は"+config.getInt("Interval.Start_Server",0)+"分以上は空けてください。").color(NamedTextColor.RED)
                  );
            }
          }
          Map<String, Map<String, Object>> statusMap = dso.loadStatusTable(conn);
          statusMap.entrySet().stream()
            .filter(entry -> entry.getKey() instanceof String && entry.getKey().equals(targetServerName))
            .forEach(entry -> {
              Map<String, Object> serverInfo = entry.getValue();
              if (serverInfo.get("exec") instanceof String execPath) {
                if (permLevel < 3 && serverInfo.get("enter") instanceof Boolean enter && !enter) {
                  pd.playerDisconnect (
                    false,
                    player,
                    Component.text("許可されていません。").color(NamedTextColor.RED)
                  );
                  return;
                }
                if (serverInfo.get("memory") instanceof Integer memory) {
                  int currentUsedMemory = dso.getCurrentUsedMemory(statusMap),
                    maxMemory = config.getInt("MaxMemory", 0);
                  int futureMemory = currentUsedMemory + memory;
                  if (maxMemory < futureMemory) {
                    String message = "メモリ超過のため、サーバーを起動できません。(" + futureMemory + "GB/" + maxMemory + "GB)";
                    pd.playerDisconnect (
                      false,
                      player,
                      Component.text(message).color(NamedTextColor.RED)
                    );
                    logger.info(message);
                    return;
                  }

                  String query3 = "UPDATE members SET st=CURRENT_TIMESTAMP WHERE uuid=?;";
                  try (PreparedStatement ps3 = conn.prepareStatement(query3)) {
                    ps3.setString(1, player.getUniqueId().toString());
                    int rsAffected3 = ps3.executeUpdate();
                    if (rsAffected3 > 0) {
                      try {
                        ProcessBuilder processBuilder = new ProcessBuilder(execPath);
                        processBuilder.start();
                        String query4 = "UPDATE status SET online=? WHERE name=?;";
                        try (PreparedStatement ps4 = conn.prepareStatement(query4)) {
                          ps4.setBoolean(1, true);
                          ps4.setString(2, targetServerName);
                          int rsAffected4 = ps4.executeUpdate();
                          if (rsAffected4 > 0) {
                            try (Connection connection = db.getConnection()) {
                              db.insertLog(connection, "INSERT INTO log (name, uuid, server, sss, status) VALUES (?, ?, ?, ?, ?);", new Object[] {playerName, playerUUID, currentServerName, true, "start"});
                            } catch (SQLException | ClassNotFoundException e) {
                              logger.error("A SQLException | ClassNotFoundException error occurred: {}", e.getMessage());
                              for (StackTraceElement element : e.getStackTrace()) {
                                logger.error(element.toString());
                              }
                            }
                            try {
                              discordME.AddEmbedSomeMessage("Start", player, targetServerName);
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
                              .append(Component.text(targetServerName+"サーバーがまもなく起動します。").color(NamedTextColor.GREEN))
                              .build();
                            pd.playerDisconnect (
                              false,
                              player,
                              component
                            );
                            console.sendMessage(Component.text(targetServerName+"サーバーがまもなく起動します。").color(NamedTextColor.GREEN));
                          }
                        }
                      } catch (IOException e) {
                        pd.playerDisconnect (
                          false,
                          player,
                          Component.text("実行時にエラーが発生しました。").color(NamedTextColor.RED)
                        );
                        logger.error(NamedTextColor.RED+"実行時にエラーが発生しました。");
                      }
                    } else {
                      pd.playerDisconnect (
                        false,
                        player,
                        Component.text()
                          .append(Component.text("内部エラーが発生しました。"))
                          .appendNewline()
                          .append(Component.text("サーバーを起動できません。"))
                          .color(NamedTextColor.RED)
                          .build()
                        );
                      logger.error(NamedTextColor.RED + "内部エラーが発生しました。\nサーバーを起動できません。");
                    }
                  } catch (SQLException e) {
                    logger.error("An SQLException error occurred: " + e.getMessage());
                    for (StackTraceElement element : e.getStackTrace()) {
                      logger.error(element.toString());
                    }
                  }
                } else {
                  pd.playerDisconnect (
                    false,
                    player,
                    Component.text("メモリが設定されていないため、サーバーを起動できません。").color(NamedTextColor.RED)
                  );
                }
              } else {
                pd.playerDisconnect (
                  false,
                  player,
                  Component.text("実行パスが設定されていないため、サーバーを起動できません。").color(NamedTextColor.RED)
                );
              }
            });
        } else {
          // MySQLサーバーにプレイヤー情報が登録されてなかった場合
          logger.info(playerName + "のプレイヤー情報がデータベースに登録されていません。");
          pd.playerDisconnect (
            false,
            player,
            Component.text(playerName + "のプレイヤー情報がデータベースに登録されていません。").color(NamedTextColor.RED)
          );
        }
      }
    } catch (SQLException | ClassNotFoundException e) {
      logger.error("An SQLException | ClassNotFoundException error occurred: " + e.getMessage());
      for (StackTraceElement element : e.getStackTrace()) {
        logger.error(element.toString());
      }
    }
  }
}
