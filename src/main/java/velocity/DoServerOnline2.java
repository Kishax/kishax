package velocity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

public class DoServerOnline2 {
    private final Main plugin;
	private final Config config;
	private final ConfigUtils cutils;
    private final Logger logger;
    private final ProxyServer server;
    private final ConsoleCommandSource console;
    private final Database db;
    //private final Map<String, Integer> serverDBInfo = new HashMap<>();
	//private final Map<String, String> serverDBTypeInfo = new HashMap<>();
    
    @Inject
    public DoServerOnline2 (Main plugin, Config config, ConfigUtils cutils, ProxyServer server, Logger logger, Database db, ConsoleCommandSource console) {
    	this.plugin = plugin;
		this.config = config;
		this.cutils = cutils;
    	this.logger = logger;
    	this.db = db;
    	this.server = server;
    	this.console = console;
    }
	
	public Map<String, Map<String, Object>> loadStatusTable(Connection conn) throws SQLException {
        Map<String, Map<String, Object>> resultMap = new HashMap<>();
        String query = "SELECT * FROM status exception!=? AND exception2!=?;;";
        try (PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
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
        }
        return resultMap;
    }

	private Map<String, Integer> getServerFromToml () {
		Map<String, Integer> velocityToml = new ConcurrentHashMap<>();
		for (RegisteredServer registeredServer : server.getAllServers()) {
			ServerInfo serverInfo = registeredServer.getServerInfo();
			String TomlServerName = serverInfo.getName();
			int TomlServerPort = serverInfo.getAddress().getPort();
			velocityToml.put(TomlServerName, TomlServerPort);
		}
		return velocityToml;
	}

	private void resetDBPlayerList(Connection conn) throws SQLException {
		String query = "UPDATE status SET player_list=?, current_players=?;";
		try (PreparedStatement ps0 = conn.prepareStatement(query)) {
			ps0.setString(1, null);
			ps0.setInt(2, 0);
			int rsAffected0 = ps0.executeUpdate();
			if (rsAffected0 > 0) {
				console.sendMessage(Component.text("プレイヤーリストを初期化しました。").color(NamedTextColor.GREEN));
			}
		}
	}

	private void makeProxyOnline(Connection conn) throws SQLException {
		String query = "UPDATE status SET online=? WHERE name=?;";
		try (PreparedStatement ps = conn.prepareStatement(query)) {
			ps.setBoolean(1, true);
			ps.setString(2, "proxy");
		}
	}

	private void deleteDBServer(Connection conn, String serverName) throws SQLException {
		String query = "DELETE FROM status WHERE name = ?;";
		try (PreparedStatement ps1 = conn.prepareStatement(query)) {
			ps1.setString(1, serverName);
			int rsAffected1 = ps1.executeUpdate();
			if (rsAffected1 > 0) {
				console.sendMessage(Component.text(serverName+"サーバーはTomlに記載されていないため、データベースから削除しました。").color(NamedTextColor.GREEN));
			}
		}
	}

	private void addDBServer(Connection conn, String serverName, int serverPort) throws SQLException {
		String query = "INSERT INTO status (name, port) VALUES (?, ?);";
		try (PreparedStatement ps = conn.prepareStatement(query)) {
			ps.setString(1, serverName);
			ps.setInt(2, serverPort);
			int rsAffected = ps.executeUpdate();
			if (rsAffected > 0) {
				console.sendMessage(Component.text(serverName+"サーバーはデータベースに存在していないため、追加しました。").color(NamedTextColor.GREEN));
			}
		}
	}
	public void updateDatabase() {
		server.getScheduler().buildTask(plugin, () -> {
			try (Connection conn = db.getConnection()) {
				resetDBPlayerList(conn);
				makeProxyOnline(conn);
				// Toml, Config情報, DB情報をすべて同期 (Tomlを主軸に)
				Map<String, Map<String, Object>> configMap = cutils.getConfigMap("Servers");
				Map<String, Integer> velocityToml = getServerFromToml();
				Map<String, Map<String, Object>> dbStatusMap = loadStatusTable(conn);

				Set<String> configEachServerKeySet = cutils.getKeySet(configMap); // Configサーバーの各情報のキーセット

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
				velocityToml.keySet().stream().filter(e -> !dbStatusMap.keySet().contains(e)).forEach(entry -> {
					int velocityTomlPort = velocityToml.get(entry);
					boolean isDupPort = velocityToml.containsValue(velocityTomlPort);
					if (!isDupPort) {
						try {
							addDBServer(conn, entry, velocityTomlPort);
							dbStatusMap.put(entry, Map.of("port", velocityTomlPort));
							configMap.put(entry, Map.of("port", velocityTomlPort));
						} catch (SQLException e) {
							logger.error("A SQLException error occurred: " + e.getMessage());
							for (StackTraceElement element : e.getStackTrace()) {
								logger.error(element.toString());
							}
						}
					} else {
						console.sendMessage(Component.text(entry+"サーバーのポート番号が重複しているため、データベースに追加できません。").color(NamedTextColor.RED));
					}
				});
				// toml, db, configのキーが同期した
				// ここから、configMapとDBの情報を比較
				// 更新 (ポートの更新も含む)
				dbStatusMap.keySet().stream().forEach(entry -> {
					Set<String> diffKeySet = new HashSet<>();
					configMap.keySet().stream().filter(entry2 -> entry2.equals(entry)).forEach(entry3 -> {
						Map<String, Object> configServerInfo = configMap.get(entry);
						Map<String, Object> dbServerInfo = dbStatusMap.get(entry);
						// 下の処理で、すべての同期は完了されているが、どの情報が更新されたかはわからない
						// そのため、上のconfigServerInfoとdbServerInfoの比較が必要
						// ここで、configServerInfoとdbServerInfoの比較を行う
						configServerInfo.keySet().stream().forEach(entry4 -> {
							Object configServerValue = configServerInfo.get(entry4);
							if (dbServerInfo.keySet().contains(entry4)) {
								Object dbServerValue = dbServerInfo.get(entry4);
								if (!configServerValue.equals(dbServerValue)) {
									diffKeySet.add(entry4);
								}
							} else {
								diffKeySet.add(entry4);
								// DBに存在していないキーがある場合は、追加
								// configServerValueの型によって、DBのカラムの型を変更する必要がある
								String columnType, defaultValue = null;
								if (configServerValue instanceof Integer) {
									columnType = "INT";
									defaultValue = "0";
								} else if (configServerValue instanceof String) {
									columnType = "VARCHAR(255)";
								} else if (configServerValue instanceof Boolean) {
									columnType = "BOOLEAN";
									defaultValue = "FALSE";
								} else {
									columnType = "VARCHAR(255)";
								}
								String query = "ALTER TABLE status ADD COLUMN " + entry4 + " " + columnType;
								if (defaultValue != null) {
									query += " DEFAULT ";
									query += defaultValue;
								}
								query += ";";
								try (PreparedStatement ps = conn.prepareStatement(query)) {
									int rsAffected = ps.executeUpdate();
									if (rsAffected > 0) {
										console.sendMessage(Component.text(entry4 + "カラムを追加しました。").color(NamedTextColor.GREEN));
									}
								} catch (SQLException e) {
									logger.error("A SQLException error occurred: " + e.getMessage());
									for (StackTraceElement element : e.getStackTrace()) {
										logger.error(element.toString());
									}
								}
							}
						});
						
						if (!diffKeySet.isEmpty()) {
							String queryPart = db.createQueryPart(diffKeySet);
							String query2 = "UPDATE status SET "+queryPart+" WHERE name=?;";
							try (PreparedStatement ps2 = conn.prepareStatement(query2)) {
								configServerInfo.keySet().stream().filter(f -> diffKeySet.contains(f)).forEach(entry5 -> {
									try {
										db.setPreparedStatementValue(ps2, diffKeySet.stream().toList().indexOf(entry5)+1, configServerInfo.get(entry5));
									} catch (SQLException e2) {
										logger.error("A SQLException error occurred: " + e2.getMessage());
										for (StackTraceElement element : e2.getStackTrace()) {
											logger.error(element.toString());
										}
									}
								});
								ps2.setString(diffKeySet.size()+1, entry);
								int rsAffected2 = ps2.executeUpdate();
								if (rsAffected2 > 0) {
									TextComponent logMessage = Component.text(entry+"サーバーの情報を更新しました。")
															.color(NamedTextColor.GREEN)
															.append(Component.text(" 更新項目: "));
									
									int index = 0, size = diffKeySet.size();
									for (String diffKey : diffKeySet) {
										logMessage = logMessage.append(Component.text(diffKey + ": " + dbServerInfo.get(diffKey) + " -> " + configServerInfo.get(diffKey)));
										if (index != size-1) {
											logMessage = logMessage.append(Component.text(", "));
										}
										index++;
									}
									console.sendMessage(logMessage);
								}
							} catch (SQLException e2) {
								logger.error("A SQLException error occurred: " + e2.getMessage());
								for (StackTraceElement element : e2.getStackTrace()) {
									logger.error(element.toString());
								}
							}
						}
						configServerInfo.keySet().stream().forEach(entry4 -> {
							String queryPart = db.createQueryPart(configServerInfo.keySet());
							String query2 = "UPDATE status SET "+queryPart+" WHERE name=?;";
							try (PreparedStatement ps2 = conn.prepareStatement(query2)) {
								configServerInfo.keySet().stream().forEach(entry5 -> {
									try {
										db.setPreparedStatementValue(ps2, configServerInfo.keySet().stream().toList().indexOf(entry5)+1, configServerInfo.get(entry5));
									} catch (SQLException e2) {
										logger.error("A SQLException error occurred: " + e2.getMessage());
										for (StackTraceElement element : e2.getStackTrace()) {
											logger.error(element.toString());
										}
									}
								});
								ps2.setString(configServerInfo.keySet().size()+1, entry);
								int rsAffected2 = ps2.executeUpdate();
								if (rsAffected2 > 0) {
									console.sendMessage(Component.text(entry+"サーバーの情報を更新しました。").color(NamedTextColor.GREEN));
								}
							} catch (SQLException e2) {
								logger.error("A SQLException error occurred: " + e2.getMessage());
								for (StackTraceElement element : e2.getStackTrace()) {
									logger.error(element.toString());
								}
							}
						});
					});
				});
				/*if (configMap.containsKey(TomlServerName)) {
						// のちに、configMapを回すときに、DBの情報と比較する?
						Map<String, Object> configServerInfo = configMap.get(TomlServerName);
						configServerInfo.put("port", String.valueOf(TomlServerPort));
					} else {
						configMap.put(TomlServerName, Map.of("port", String.valueOf(TomlServerPort))); // TomlServerNameは絶対
						// configMapのキーがemptyの場合は、configにそのサーバー情報がないので、DBにはポート、サーバー名以外はnullで追加する
					}*/
				// 最新のToml情報がconfigServerInfoに入った。
				// configMapの値であるMap<String, Object>の重複のないキーセットを取得
				String query3 = "SELECT * FROM status;";
				try (PreparedStatement ps3 = conn.prepareStatement(query3)) {
					try (ResultSet status = ps3.executeQuery()) {
						Set<String> dbServers = new HashSet<>();
						while (status.next()) {
							String serverDBName = status.getString("name");
							if (!velocityToml.containsKey(serverDBName)) {
								// DBに存在して、Tomlに存在しないサーバー(削除対象)
								String query4 = "DELETE FROM status WHERE name = ?;";
								try (PreparedStatement ps4 = conn.prepareStatement(query4)) {
									ps4.setString(1, serverDBName);
									int rsAffected4 = ps4.executeUpdate();
									if (rsAffected4 > 0) {
										configMap.remove(serverDBName); // ConfigUtils.getConfigMap()より、サーバー情報を削除
										console.sendMessage(Component.text(serverDBName+"サーバーはTomlに記載されていないため、データベースから削除しました。").color(NamedTextColor.GREEN));
									}
								}
							} else {
								dbServers.add(serverDBName); // DBに存在してるサーバーを入れておく
								// Tomlに存在するサーバー(追加・更新対象)
								// DBに存在しているかはわからないため、DBに存在していない場合は追加、存在している場合は更新
								configMap.keySet().stream().forEach(entry -> {
									String queryPart = db.createQueryPart(configEachServerKeySet);
									String query2 = "UPDATE status SET "+queryPart+" WHERE name=?;";
									try (PreparedStatement ps2 = conn.prepareStatement(query2)) {
										Map<String, Object> configServerInfo = configMap.get(entry);
										configServerInfo.keySet().stream().forEach(entry2 -> {
											try {
												db.setPreparedStatementValue(ps2, configEachServerKeySet.stream().toList().indexOf(entry2)+1, configServerInfo.get(entry2));
											} catch (SQLException e2) {
												logger.error("A SQLException error occurred: " + e2.getMessage());
												for (StackTraceElement element : e2.getStackTrace()) {
													logger.error(element.toString());
												}
											}
										});
										ps2.setString(configEachServerKeySet.size()+1, entry);
										int rsAffected2 = ps2.executeUpdate();
										if (rsAffected2 > 0) {
											console.sendMessage(Component.text(entry+"サーバーの情報を更新しました。").color(NamedTextColor.GREEN));
										}
									} catch (SQLException e2) {
										logger.error("A SQLException error occurred: " + e2.getMessage());
										for (StackTraceElement element : e2.getStackTrace()) {
											logger.error(element.toString());
										}
									}
								});
							}
						}
						// Tomlに存在して、DBに存在していないサーバー(追加対象)
						if (configServersKeySet.removeAll(dbServers)) {
							configServersKeySet.stream().forEach(entry -> {
								configMap.entrySet().stream().filter(entry2 -> entry2.getKey().equals(entry)).forEach(entry3 -> {
									Map<String, Object> configServerInfo = configMap.get(entry);
									configServerInfo.keySet().stream().forEach(entry4 -> {
										String queryPart = String.join(", ", configEachServerKeySet);
										String placeholders = db.createPlaceholders(configEachServerKeySet.size());
										String query5 = "INSERT INTO status ("+queryPart+") VALUES (" + placeholders + ");";
										try (PreparedStatement ps5 = conn.prepareStatement(query5)) {
											configServerInfo.keySet().stream().forEach(entry5 -> {
												try {
													db.setPreparedStatementValue(ps5, configEachServerKeySet.stream().toList().indexOf(entry5)+1, configServerInfo.get(entry5));
												} catch (SQLException e5) {
													logger.error("A SQLException error occurred: " + e5.getMessage());
													for (StackTraceElement element : e5.getStackTrace()) {
														logger.error(element.toString());
													}
												}
											});
											int rsAffected5 = ps5.executeUpdate();
											if (rsAffected5 > 0) {
												console.sendMessage(Component.text(entry+"サーバーはデータベースに存在していないため、追加しました。").color(NamedTextColor.GREEN));
											}
										} catch (SQLException e5) {
											logger.error("A SQLException error occurred: " + e5.getMessage());
											for (StackTraceElement element : e5.getStackTrace()) {
												logger.error(element.toString());
											}
										}
									});
								});
							});
						}
					}
				}
			} catch (ClassNotFoundException | SQLException e1) {
				logger.error("A ClassNotFoundException | SQLException error occurred: " + e1.getMessage());
				for (StackTraceElement element : e1.getStackTrace()) {
					logger.error(element.toString());
				}
			}
        }).schedule();
	}
}
