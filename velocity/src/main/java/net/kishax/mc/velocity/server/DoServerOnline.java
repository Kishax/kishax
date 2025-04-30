package net.kishax.mc.velocity.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;

import net.kishax.mc.common.database.Database;
import net.kishax.mc.common.socket.SocketSwitch;
import net.kishax.mc.common.socket.message.Message;
import net.kishax.mc.velocity.Main;
import net.kishax.mc.velocity.util.config.ConfigUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class DoServerOnline {
  private final Main plugin;
  private final ConfigUtils cutils;
  private final Logger logger;
  private final ProxyServer server;
  private final ConsoleCommandSource console;
  private final Database db;
  private final Provider<SocketSwitch> sswProvider;
  private final Set<String> addedColumnSet = new HashSet<>();

  @Inject
  public DoServerOnline(Main plugin, ConfigUtils cutils, ProxyServer server, Logger logger, Database db,
      ConsoleCommandSource console, Provider<SocketSwitch> sswProvider) {
    this.plugin = plugin;
    this.cutils = cutils;
    this.logger = logger;
    this.db = db;
    this.server = server;
    this.console = console;
    this.sswProvider = sswProvider;
  }

  public Map<String, Map<String, Object>> loadStatusTable(Connection conn) throws SQLException {
    Map<String, Map<String, Object>> resultMap = new ConcurrentHashMap<>();
    String query = "SELECT * FROM status WHERE exception2!=?;";// SELECT * FROM status WHERE exception!=? AND
                                                               // exception2!=?;
    PreparedStatement ps = conn.prepareStatement(query);
    ps.setBoolean(1, true);
    // ps.setBoolean(2, true);
    ResultSet rs = ps.executeQuery();
    while (rs.next()) {
      String name = rs.getString("name");
      Map<String, Object> columnMap = new HashMap<>();
      int columnCount = rs.getMetaData().getColumnCount();
      for (int i = 1; i <= columnCount; i++) {
        String columnName = rs.getMetaData().getColumnName(i);
        if (!"name".equals(columnName)) {
          columnMap.put(columnName, rs.getObject(columnName));
        }
      }
      resultMap.put(name, columnMap);
    }
    return resultMap;
  }

  public Map<String, Object> loadStatusTable(Connection conn, String serverName) throws SQLException {
    Map<String, Object> resultMap = new HashMap<>();
    String query = "SELECT * FROM status WHERE name=? AND exception2!=?;";// SELECT * FROM status WHERE name=? AND
                                                                          // exception!=? AND exception2!=?;
    PreparedStatement ps = conn.prepareStatement(query);
    ps.setString(1, serverName);
    ps.setBoolean(2, true);
    // ps.setBoolean(3, true);
    ResultSet rs = ps.executeQuery();
    if (rs.next()) {
      int columnCount = rs.getMetaData().getColumnCount();
      for (int i = 1; i <= columnCount; i++) {
        String columnName = rs.getMetaData().getColumnName(i);
        if (!columnName.equals("name")) {
          resultMap.put(columnName, rs.getObject(columnName));
        }
      }
    }
    return resultMap;
  }

  public int getCurrentUsedMemory(Map<String, Map<String, Object>> statusMap) {
    return statusMap.values().stream()
        .filter(entry -> entry.containsKey("online") && entry.get("online") instanceof Boolean online && online)
        .filter(entry -> entry.containsKey("memory") && entry.get("memory") instanceof Integer)
        .mapToInt(entry -> (Integer) entry.get("memory"))
        .sum();
  }

  private Map<String, Integer> getServerFromToml() {
    Map<String, Integer> velocityToml = new ConcurrentHashMap<>();
    for (RegisteredServer registeredServer : server.getAllServers()) {
      ServerInfo serverInfo = registeredServer.getServerInfo();
      String TomlServerName = serverInfo.getName();
      int TomlServerPort = serverInfo.getAddress().getPort();
      velocityToml.put(TomlServerName, TomlServerPort);
    }
    velocityToml.put("proxy", 25565); // Add the missing server name and port
    return velocityToml;
  }

  private void resetDBPlayerList(Connection conn) throws SQLException {
    String query = "UPDATE status SET player_list=?, current_players=?;";
    PreparedStatement ps0 = conn.prepareStatement(query);
    ps0.setString(1, null);
    ps0.setInt(2, 0);
    int rsAffected0 = ps0.executeUpdate();
    if (rsAffected0 > 0) {
      console.sendMessage(Component.text("プレイヤーリストを初期化しました。").color(NamedTextColor.GREEN));
    }
  }

  private void makeProxyOnline(Connection conn) throws SQLException {
    String query = "UPDATE status SET online=? WHERE name=?;";
    PreparedStatement ps = conn.prepareStatement(query);
    ps.setBoolean(1, true);
    ps.setString(2, "proxy");
    ps.executeUpdate();
  }

  private void deleteDBServer(Connection conn, String serverName) throws SQLException {
    String query = "DELETE FROM status WHERE name = ?;";
    PreparedStatement ps1 = conn.prepareStatement(query);
    ps1.setString(1, serverName);
    ps1.executeUpdate();
    console
        .sendMessage(Component.text(serverName + "サーバーはTomlに記載されていないため、データベースから削除しました。").color(NamedTextColor.GREEN));
  }

  private void addDBServer(Connection conn, String serverName, int serverPort) throws SQLException {
    String query = "INSERT INTO status (name, port) VALUES (?, ?);";
    PreparedStatement ps = conn.prepareStatement(query);
    ps.setString(1, serverName);
    ps.setInt(2, serverPort);
    int rsAffected = ps.executeUpdate();
    if (rsAffected > 0) {
      console.sendMessage(Component.text(serverName + "サーバーはデータベースに存在していないため、追加しました。").color(NamedTextColor.GREEN));
    }
  }

  public void updateAndSyncDatabase(Boolean fromReloadCmd) throws SQLException, ClassNotFoundException {
    updateDatabase(fromReloadCmd);

    try (Connection conn2 = db.getConnection()) {
      Message msg = new Message();
      msg.mc = new Message.Minecraft();
      msg.mc.sync = new Message.Minecraft.Sync();
      msg.mc.sync.content = "STATUS";

      SocketSwitch ssw = sswProvider.get();
      ssw.sendSpigotServer(conn2, msg);
      logger.info("データベースとの同期を完了しました。");
    }
  }

  private void updateDatabase(Boolean fromReloadCmd) throws SQLException, ClassNotFoundException {
    server.getScheduler().buildTask(plugin, () -> {
      try (Connection conn = db.getConnection()) {
        // コマンドから実行していなければ
        if (!fromReloadCmd) {
          resetDBPlayerList(conn);
          makeProxyOnline(conn);
        }
        // Toml, Config情報, DB情報をすべて同期 (Tomlを主軸に)
        Map<String, Map<String, Object>> configMap = cutils.getConfigMap("Servers");
        Map<String, Integer> velocityToml = getServerFromToml();
        Map<String, Map<String, Object>> dbStatusMap = new ConcurrentHashMap<>(loadStatusTable(conn));
        // dbにあってTomlにないサーバーは削除対象
        // Set(dbServersKeySet \ velocityTomlKeySet)
        dbStatusMap.keySet().stream().filter(e -> !velocityToml.keySet().contains(e)).forEach(entry -> {
          try {
            deleteDBServer(conn, entry);
            dbStatusMap.remove(entry);
            configMap.remove(entry);
          } catch (SQLException e) {
            logger.error("A SQLException error occurred: " + e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
              logger.error(element.toString());
            }
          }
        });
        // Tomlにあってdbにないサーバーは追加・更新対象
        // Set(velocityTomlKeySet \ dbServersKeySet)
        // 追加
        AtomicBoolean isAdded = new AtomicBoolean(false);
        velocityToml.keySet().stream().filter(e -> !dbStatusMap.keySet().contains(e)).forEach(entry -> {
          int velocityTomlPort = velocityToml.get(entry);
          boolean isDupPort = dbStatusMap.values().stream()
              .anyMatch(map -> map.containsKey("port") && velocityTomlPort == (int) map.get("port"));
          if (!isDupPort) {
            try {
              isAdded.set(true);
              addDBServer(conn, entry, velocityTomlPort);
              // ここで、追加されたサーバーの情報を取得して、configMapとdbStatusMapに追加する
              dbStatusMap.put(entry, loadStatusTable(conn, entry));
              configMap.put(entry, Map.of("port", velocityTomlPort));
            } catch (SQLException e) {
              logger.error("A SQLException error occurred: " + e.getMessage());
              for (StackTraceElement element : e.getStackTrace()) {
                logger.error(element.toString());
              }
            }
          } else {
            console
                .sendMessage(Component.text(entry + "サーバーのポート番号が重複しているため、データベースに追加できません。").color(NamedTextColor.RED));
          }
        });
        if (isAdded.get()) {
          // 追加されたサーバーがある場合は、再度同期を行う
          logger.info("追加されたサーバーがあるため、再度同期を実行します。");
          updateDatabase(fromReloadCmd);
        }
        // toml, db, configのキーが同期した
        // ここから、configMapとDBの情報を比較
        // 更新 (ポートの更新も含む)
        AtomicBoolean isChanged = new AtomicBoolean(false);
        dbStatusMap.keySet().stream().forEach(entry -> {
          Set<String> diffKeySet = new HashSet<>();
          if (configMap.containsKey(entry)) {
            Map<String, Object> configServerInfo = configMap.get(entry);
            Map<String, Object> dbServerInfo = dbStatusMap.get(entry);
            // ここで、configServerInfoとdbServerInfoの比較を行う
            configServerInfo.keySet().stream().forEach(entry4 -> {
              Object configServerValue = configServerInfo.get(entry4);
              if (dbServerInfo.keySet().contains(entry4)) {
                Object dbServerValue = dbServerInfo.get(entry4);
                if (!Objects.equals(configServerValue, dbServerValue)) {
                  diffKeySet.add(entry4);
                }
              } else if (addedColumnSet.contains(entry4)) {
                // すでに追加されたカラムは、変更とみなす
                diffKeySet.add(entry4);
              } else {
                addedColumnSet.add(entry4);
                diffKeySet.add(entry4);
                // DBに存在していないキーがある場合は、追加
                // configServerValueの型によって、DBのカラムの型を変更する必要がある
                String columnType, defaultValue = null;
                if (configServerValue instanceof Integer) {
                  columnType = "INT";
                  defaultValue = "0";
                  dbServerInfo.put(entry4, 0);
                } else if (configServerValue instanceof String) {
                  columnType = "VARCHAR(255)";
                  dbServerInfo.put(entry4, null);
                } else if (configServerValue instanceof Boolean) {
                  columnType = "BOOLEAN";
                  defaultValue = "FALSE";
                  dbServerInfo.put(entry4, false);
                } else {
                  columnType = "VARCHAR(255)";
                  dbServerInfo.put(entry4, null);
                }
                String query = "ALTER TABLE status ADD COLUMN " + entry4 + " " + columnType;
                if (defaultValue != null) {
                  query += " DEFAULT ";
                  query += defaultValue;
                }
                query += ";";
                try (PreparedStatement ps = conn.prepareStatement(query)) {
                  ps.executeUpdate();
                  logger.info(entry4 + " カラムを追加しました。");
                } catch (SQLException e) {
                  logger.error("A SQLException error occurred: " + e.getMessage());
                  for (StackTraceElement element : e.getStackTrace()) {
                    logger.error(element.toString());
                  }
                }
              }
            });
            if (!diffKeySet.isEmpty()) {
              if (isChanged.compareAndSet(false, true)) {
                logger.info("ConfigとDBの情報が一部異なるため、同期を実行します。");
              }
              String queryPart = db.createQueryPart(diffKeySet);
              String query2 = "UPDATE status SET " + queryPart + " WHERE name=?;";
              try (PreparedStatement ps2 = conn.prepareStatement(query2)) {
                configServerInfo.keySet().stream().filter(f -> diffKeySet.contains(f)).forEach(entry5 -> {
                  try {
                    db.setPreparedStatementValue(ps2, diffKeySet.stream().toList().indexOf(entry5) + 1,
                        configServerInfo.get(entry5));
                  } catch (SQLException e2) {
                    logger.error("A SQLException error occurred: " + e2.getMessage());
                    for (StackTraceElement element : e2.getStackTrace()) {
                      logger.error(element.toString());
                    }
                  }
                });
                ps2.setString(diffKeySet.size() + 1, entry);
                int rsAffected2 = ps2.executeUpdate();
                if (rsAffected2 > 0) {
                  logger.info(entry + ":");
                  for (String diffKey : diffKeySet) {
                    logger.info(
                        "  " + diffKey + ": " + dbServerInfo.get(diffKey) + " -> " + configServerInfo.get(diffKey));
                  }
                }
              } catch (SQLException e2) {
                logger.error("A SQLException error occurred: " + e2.getMessage());
                for (StackTraceElement element : e2.getStackTrace()) {
                  logger.error(element.toString());
                }
              }
            }
          }
        });
      } catch (ClassNotFoundException | SQLException e1) {
        logger.error("A ClassNotFoundException | SQLException error occurred: " + e1.getMessage());
        for (StackTraceElement element : e1.getStackTrace()) {
          logger.error(element.toString());
        }
      }
    }).schedule();
  }
}
