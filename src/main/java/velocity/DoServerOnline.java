package velocity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class DoServerOnline {
    private final Main plugin;
	private final Config config;
    private final Logger logger;
    private final ProxyServer server;
    private final ConsoleCommandSource console;
    private final Database db;
    private final Map<String, Integer> velocityToml = new ConcurrentHashMap<>();
	private final Map<String, Map<String, String>> dbStatusMap = new ConcurrentHashMap<>();
    //private final Map<String, Integer> serverDBInfo = new HashMap<>();
	//private final Map<String, String> serverDBTypeInfo = new HashMap<>();
    
    @Inject
    public DoServerOnline (Main plugin, Config config, ProxyServer server, Logger logger, Database db, ConsoleCommandSource console) {
    	this.plugin = plugin;
		this.config = config;
    	this.logger = logger;
    	this.db = db;
    	this.server = server;
    	this.console = console;
    }
	
	public void updateDatabase() {
		// Tomlのサーバー優先
		// Tomlのサーバー名から得られるconfig情報をDBに反映
		server.getScheduler().buildTask(plugin, () -> {
			String query0 = "UPDATE status SET player_list=?, current_players=?;";
			try (Connection conn = db.getConnection();
				PreparedStatement ps0 = conn.prepareStatement(query0)) {
				ps0.setString(1, null);
				ps0.setInt(2, 0);
				int rsAffected0 = ps0.executeUpdate();
				if (rsAffected0 > 0) {
					console.sendMessage(Component.text("プレイヤーリストを初期化しました。").color(NamedTextColor.GREEN));
					String query = "UPDATE status SET online=? WHERE name=?;";
					try (PreparedStatement ps = conn.prepareStatement(query)) {
						ps.setBoolean(1, true);
						ps.setString(2, "proxy");
						int rsAffected = ps.executeUpdate();
						if (rsAffected > 0) {
							for (RegisteredServer registeredServer : server.getAllServers()) {
								ServerInfo serverInfo = registeredServer.getServerInfo();
								String TomlServerName = serverInfo.getName();
								int TomlServerPort = serverInfo.getAddress().getPort();
								velocityToml.put(TomlServerName, TomlServerPort);
							}
							
							// プロキシだけ特別
							String query2 = "SELECT * FROM status WHERE name=?;";
							try (PreparedStatement ps2 = conn.prepareStatement(query2)) {
								ps2.setString(1, "proxy");
								try (ResultSet proxySet = ps2.executeQuery()) {
									List<Integer> rsAffecteds3 = new ArrayList<>();
									while (proxySet.next()) {
										String proxyDBPlatform = proxySet.getString("platform");
										int proxyDBSocketPort;
										try {
											proxyDBSocketPort = proxySet.getInt("socketport");
										} catch (SQLException e) {
											proxyDBSocketPort = 0;
										}
										
										// proxyのみ、特別、〇〇が固定なので更新をチェック
										String proxyConfigPlatform = config.getString("Servers.proxy.Platform", null);
										int proxyConfigSocketServerPort = config.getInt("Socket.Server_Port", 0);
										String query3 = "UPDATE status SET socketport=?,platform=? WHERE name=?;";
										try (PreparedStatement ps3 = conn.prepareStatement(query3)) {
											// ソケットポート
											if (proxyConfigSocketServerPort != proxyDBSocketPort) {
												ps3.setInt(1, proxyConfigSocketServerPort);
												console.sendMessage(Component.text("Proxyサーバーのソケットポートを"+proxyConfigSocketServerPort+"に更新しました。").color(NamedTextColor.GREEN));	
											} else {
												ps3.setInt(1, proxyDBSocketPort);
											}
											// プラットフォーム
											if (!proxyConfigPlatform.equalsIgnoreCase(proxyDBPlatform)) {
												ps3.setString(2, proxyConfigPlatform);
												console.sendMessage(Component.text("Proxyサーバーのプラットフォームを"+proxyConfigPlatform+"に更新しました。").color(NamedTextColor.GREEN));	
											} else {
												ps3.setString(2, proxyDBPlatform);
											}
											ps3.setString(3, "proxy");
											int rsAffected3 = ps3.executeUpdate();
											rsAffecteds3.add(rsAffected3);
										}
									}
									if (rsAffecteds3.stream().anyMatch(rs -> rs > 0)) {
										String query4 = "SELECT * FROM status WHERE exception!=? AND exception2!=?;"; //SELECT * FROM status WHERE exception!=? AND exception2!=?;
										try (PreparedStatement ps4 = conn.prepareStatement(query4)) {
											ps4.setBoolean(1, true);
											ps4.setBoolean(2, true);
											try (ResultSet mine_status = ps4.executeQuery()) {
												while (mine_status.next()) {
													Map<String, String> serverInfo = new HashMap<>();
													String serverDBName = mine_status.getString("name"),
															serverType = mine_status.getString("type"),
															serverPlatform = mine_status.getString("platform");
													int serverDBPort = mine_status.getInt("port"),
															serverSocketPort = mine_status.getInt("socketport");
													
													serverInfo.put("port", String.valueOf(serverDBPort));
													serverInfo.put("socketport", String.valueOf(serverSocketPort));
													serverInfo.put("type", serverType);
													serverInfo.put("platform", serverPlatform);
													dbStatusMap.put(serverDBName, serverInfo);
												}
												
												// サーバー情報の追加、削除、更新を行う
												// DBの情報を回して、Tomlの情報と比較
												for (Map.Entry<String, Map<String, String>> dbEntry : dbStatusMap.entrySet()) {
													String serverDBName = dbEntry.getKey();
													Map<String, String> eachServerStatus = dbEntry.getValue();
													// データベースへの更新、削除を行う
													// ( 特別、追加は行わない )
													// 以下、dbの配列を回して得られるサーバーネームで得たconfig情報
													int serverDBPort = eachServerStatus.get("port") != null ? Integer.parseInt(eachServerStatus.get("port")) : 0;
													Integer serverDBTomlPort = velocityToml.get(serverDBName);
													if (serverDBTomlPort == null) {
														// 適切なデフォルト値を設定するか、エラーハンドリングを行います
														serverDBTomlPort = 0; // 例: デフォルト値を0に設定
													}
													String serverDBConfigType = config.getString("Servers." + serverDBName + ".Type", ""),
															serverDBType = eachServerStatus.get("type"),
															serverDBConfigPlatform = config.getString("Servers." + serverDBName + ".Platform", ""),
															serverDBPlatform = eachServerStatus.get("platform");
								
													// Tomlに存在しないサーバーは削除
													if (!velocityToml.containsKey(serverDBName)) {
														dbStatusMap.remove(serverDBName);
														String query5 = "DELETE FROM status WHERE name = ?;";
														try (PreparedStatement ps5 = conn.prepareStatement(query5)) {
															ps5.setString(1, serverDBName);
															int rsAffected5 = ps5.executeUpdate();
															if (rsAffected5 > 0) {
																console.sendMessage(Component.text(serverDBName+"サーバーはConfigに記載されていないため、データベースから削除しました。").color(NamedTextColor.GREEN));
															}
														}
													} else {
														// 以下、〇〇がTomlから得られるconfigとdatabaseで異なる場合、config優先
														// ポート番号
														String query5 = "UPDATE status SET port=?, type=?, platform=? WHERE name=?;";
														try (PreparedStatement ps5 = conn.prepareStatement(query5)) {
															if (serverDBPort != serverDBTomlPort) {
																dbStatusMap.get(serverDBName).put("port", String.valueOf(serverDBTomlPort));
																ps5.setInt(1, serverDBTomlPort);
																console.sendMessage(Component.text(serverDBName+"サーバー(ポート:"+serverDBPort+")のポートを "+serverDBTomlPort+" に更新しました。").color(NamedTextColor.GREEN));
															} else {
																ps5.setInt(1, serverDBPort);
															}
															// サーバータイプ
															if (!serverDBConfigType.equalsIgnoreCase(serverDBType)) {
																dbStatusMap.get(serverDBName).put("type", serverDBConfigType);
																ps5.setString(2, serverDBConfigType.toLowerCase());
																console.sendMessage(Component.text(serverDBName+"サーバーのタイプを"+serverDBConfigType+"に更新しました。").color(NamedTextColor.GREEN));	
															} else {
																ps5.setString(2, serverDBType);
															}
															// サーバープラットフォーム
															if (!serverDBConfigPlatform.equalsIgnoreCase(serverDBPlatform)) {
																dbStatusMap.get(serverDBName).put("platform", serverDBConfigPlatform);
																ps5.setString(3, serverDBConfigPlatform);
																console.sendMessage(Component.text(serverDBName+"サーバーのプラットフォームを"+serverDBConfigPlatform+"に更新しました。").color(NamedTextColor.GREEN));	
															} else {
																ps5.setString(3, serverDBPlatform);
															}
															ps5.setString(4, serverDBName);
															ps5.executeUpdate();
														}
													}
								
													// データベースへの削除を行う
													// ( 特別、追加を行う )
													// Tomlにあるが、DBにないサーバーを追加
													for (Map.Entry<String, Integer> configEntry : velocityToml.entrySet()) {
														// 以下、Tomlの配列を回して得られるサーバーネームで得たconfig情報
														String serverTomlName = configEntry.getKey(),
																serverTomlConfigType = config.getString("Servers." + serverTomlName + ".Type", null),
																serverTomlConfigPlatform = config.getString("Servers." + serverTomlName + ".Platform", null);
														int serverTomlPort = configEntry.getValue();
														if (!dbStatusMap.containsKey(serverTomlName)) {
															// ポートの重複をチェック
															if (dbStatusMap.values().stream().anyMatch(map -> map.containsValue(String.valueOf(serverTomlPort)))) {
																throw new SQLException("ポート番号が重複しています: " + serverTomlPort);
															}
															// サーバー名の重複をチェック
															if (dbStatusMap.containsKey(serverTomlName)) {
																throw new SQLException("サーバー名が重複しています: " + serverTomlName);
															}
															// マップ更新 (追加)
															Map<String, String> newServerInfo = new HashMap<>();
															newServerInfo.put("port", String.valueOf(serverTomlPort));
															newServerInfo.put("type", serverTomlConfigType);
															newServerInfo.put("platform", serverTomlConfigPlatform);
															dbStatusMap.put(serverTomlName, newServerInfo);
								
															String query6 = "INSERT INTO status (name,port,type,platform) VALUES (?,?,?,?);";
															try (PreparedStatement ps6 = conn.prepareStatement(query6)) {
																ps6.setString(1, serverTomlName);
																ps6.setInt(2, serverTomlPort);
																ps6.setString(3, serverTomlConfigType.toLowerCase());
																ps6.setString(4, serverTomlConfigPlatform);
																int rsAffected6 = ps6.executeUpdate();
																if (rsAffected6 > 0) {
																	console.sendMessage(Component.text(serverTomlName+"サーバー(ポート:"+serverTomlPort+")をデータベースに追加しました。").color(NamedTextColor.GREEN));
																}
															}
														}
													}
												}
											}
										}
									}
								}
							}
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
