package velocity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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

    private Connection conn = null;
    private PreparedStatement ps = null;
    public ResultSet mine_status = null;
	public ResultSet[] resultsets = {mine_status};
    private final Main plugin;
	private final Config config;
    private final Logger logger;
    private final ProxyServer server;
    private final ConsoleCommandSource console;
    private final Database db;
    private final Map<String, Integer> velocityToml = new ConcurrentHashMap<>();
	private Map<String, Map<String, String>> dbStatusMap = new ConcurrentHashMap<>();
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
	
	public void UpdateDatabase() {
		// Tomlのサーバー優先
		// Tomlのサーバー名から得られるconfig情報をDBに反映
		server.getScheduler().buildTask(plugin, () -> {
        	try {
        		conn = db.getConnection();
        		if (Objects.nonNull(conn)) {
        			logger.info("MySQL Server is connected!");
    			} else {
        			logger.info("MySQL Server is canceled for config value not given");
        			return;
    			}
        		
				String insql = "UPDATE status SET online=? WHERE name=?;";
				ps = conn.prepareStatement(insql);
				ps.setBoolean(1, true);
				ps.setString(2, "Proxy");
				ps.executeUpdate();

        		for (RegisteredServer registeredServer : server.getAllServers()) {
					ServerInfo serverInfo = registeredServer.getServerInfo();
					String TomlServerName = serverInfo.getName();
                	int TomlServerPort = serverInfo.getAddress().getPort();
                	velocityToml.put(TomlServerName, TomlServerPort);
                }
				
				insql = "SELECT * FROM status WHERE exception!=? AND exception2!=?;";//SELECT * FROM status WHERE exception!=? AND exception2!=?;
				ps = conn.prepareStatement(insql);
				ps.setBoolean(1, true);
				ps.setBoolean(2, true);
				mine_status = ps.executeQuery();
				
				// プロキシだけ特別
				String insql2 = "SELECT * FROM status WHERE name=?;";
				try (PreparedStatement ps2 = conn.prepareStatement(insql2)) {
					ps2.setString(1, "proxy");
					ResultSet proxySet = ps2.executeQuery();
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
						try (PreparedStatement ps3 = conn.prepareStatement("UPDATE status SET socketport=?,platform=? WHERE name=?;")) {
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
							ps3.executeUpdate();
						} catch (SQLException e) {
							logger.error("An error occurred while updating the socket port: " + e.getMessage());
							for (StackTraceElement element : e.getStackTrace()) {
								logger.error(element.toString());
							}
						}
					}
				} catch (SQLException e) {
					logger.error("An error occurred while updating the socket port: " + e.getMessage());
					for (StackTraceElement element : e.getStackTrace()) {
						logger.error(element.toString());
					}
				}

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
						insql = "DELETE FROM status WHERE name = ?;";
						ps = conn.prepareStatement(insql);
						ps.setString(1, serverDBName);
						ps.executeUpdate();
						console.sendMessage(Component.text(serverDBName+"サーバーはConfigに記載されていないため、データベースから削除しました。").color(NamedTextColor.GREEN));
					} else {
						// 以下、〇〇がTomlから得られるconfigとdatabaseで異なる場合、config優先
						// ポート番号
						insql = "UPDATE status SET port=?,type=?,platform=? WHERE name=?;";
						ps = conn.prepareStatement(insql);
						if (serverDBPort != serverDBTomlPort) {
							dbStatusMap.get(serverDBName).put("port", String.valueOf(serverDBTomlPort));
							ps.setInt(1, serverDBTomlPort);
							console.sendMessage(Component.text(serverDBName+"サーバー(ポート:"+serverDBPort+")のポートを "+serverDBTomlPort+" に更新しました。").color(NamedTextColor.GREEN));
						} else {
							ps.setInt(1, serverDBPort);
						}
						// サーバータイプ
						if (!serverDBConfigType.equalsIgnoreCase(serverDBType)) {
							dbStatusMap.get(serverDBName).put("type", serverDBConfigType);
							ps.setString(2, serverDBConfigType.toLowerCase());
							console.sendMessage(Component.text(serverDBName+"サーバーのタイプを"+serverDBConfigType+"に更新しました。").color(NamedTextColor.GREEN));	
						} else {
							ps.setString(2, serverDBType);
						}
						// サーバープラットフォーム
						if (!serverDBConfigPlatform.equalsIgnoreCase(serverDBPlatform)) {
							dbStatusMap.get(serverDBName).put("platform", serverDBConfigPlatform);
							ps.setString(3, serverDBConfigPlatform);
							console.sendMessage(Component.text(serverDBName+"サーバーのプラットフォームを"+serverDBConfigPlatform+"に更新しました。").color(NamedTextColor.GREEN));	
						} else {
							ps.setString(3, serverDBPlatform);
						}
						ps.setString(4, serverDBName);
						ps.executeUpdate();
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

							insql = "INSERT INTO status (name,port,type,platform) VALUES (?,?,?,?);";
							ps = conn.prepareStatement(insql);
							ps.setString(1, serverTomlName);
							ps.setInt(2, serverTomlPort);
							ps.setString(3, serverTomlConfigType.toLowerCase());
							ps.setString(4, serverTomlConfigPlatform);
							ps.executeUpdate();
							console.sendMessage(Component.text(serverTomlName+"サーバー(ポート:"+serverTomlPort+")をデータベースに追加しました。").color(NamedTextColor.GREEN));
						}
					}
			    }
        	} catch (ClassNotFoundException | SQLException e1) {
    			logger.error("A ClassNotFoundException | SQLException error occurred: " + e1.getMessage());
				for (StackTraceElement element : e1.getStackTrace()) {
					logger.error(element.toString());
				}
    		} finally {
    			db.close_resource(resultsets,conn,ps);
    		}
        }).schedule();
	}
}
