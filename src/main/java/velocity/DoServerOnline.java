package velocity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
    private final Map<String, Integer> velocityToml = new HashMap<>();
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
					String serverConfigName = serverInfo.getName();
                	int serverConfigPort = serverInfo.getAddress().getPort();
                	velocityToml.put(serverConfigName, serverConfigPort);
                }
				
				insql = "SELECT * FROM status WHERE exception!=? AND exception2!=?;";
				ps = conn.prepareStatement(insql);
				ps.setBoolean(1, true);
				ps.setBoolean(2, true);
				mine_status = ps.executeQuery();
				
				int velocitySocketServerPort = config.getInt("Socket.Server_Port", 0);
				Map<String, Map<String, String>> dbStatusMap = new HashMap<>();
				while (mine_status.next()) {
					Map<String, String> serverInfo = new HashMap<>();
					String serverDBName = mine_status.getString("name"),
							serverType = mine_status.getString("type");
					int serverDBPort = mine_status.getInt("port"),
							serverSocketPort = mine_status.getInt("socketport");
					
					serverInfo.put("port", String.valueOf(serverDBPort));
					serverInfo.put("socketport", String.valueOf(serverSocketPort));
					serverInfo.put("type", serverType);
					dbStatusMap.put(serverDBName, serverInfo);

					// proxyのみ、ソケットポートが固定なので更新をチェック
					if (serverDBName.equalsIgnoreCase("proxy")) {
						if (velocitySocketServerPort != serverSocketPort) {
							try (PreparedStatement ps2 = conn.prepareStatement("UPDATE status SET socketport=? WHERE name=?;")) {
								ps2.setInt(1, velocitySocketServerPort);
								ps2.setString(2, serverDBName);
								ps2.executeUpdate();
								console.sendMessage(Component.text("Proxyサーバーのソケットポートを"+velocitySocketServerPort+"に更新しました。").color(NamedTextColor.GREEN));	
							} catch (SQLException e) {
								logger.error("An error occurred while updating the socket port: " + e.getMessage());
								for (StackTraceElement element : e.getStackTrace()) {
									logger.error(element.toString());
								}
							}
						}
					}
				}
				
				// サーバー情報の追加、削除、更新を行う
			    // DBの情報を回して、Tomlの情報と比較
				for (Map.Entry<String, Map<String, String>> dbEntry : dbStatusMap.entrySet()) {
			        String serverDBName = dbEntry.getKey();
					Map<String, String> eachServerStatus = dbEntry.getValue();
					for (Map.Entry<String, String> dbServerStatusEntry : eachServerStatus.entrySet()) {
						// データベースへの更新、削除を行う
						// ( 特別、追加は行わない )
						// 以下、dbの配列を回して得られるサーバーネームで得たconfig情報
						int serverDBPort = Integer.parseInt(dbServerStatusEntry.getValue()),
							serverTomlPort = velocityToml.get(serverDBName);
						String serverTomlType = config.getString("Servers." + serverDBName + ".Type", ""),
								serverDBType = eachServerStatus.get("type");

						// Tomlに存在しないサーバーは削除
						if (!velocityToml.containsKey(serverDBName)) {
							dbStatusMap.remove(serverDBName);
							insql = "DELETE FROM status WHERE name = ?;";
							ps = conn.prepareStatement(insql);
							ps.setString(1, serverDBName);
							ps.executeUpdate();
							console.sendMessage(Component.text(serverDBName+"サーバーはConfigに記載されていないため、データベースから削除しました。").color(NamedTextColor.GREEN));
						} else {
							// Tomlに存在するサーバーがデータベースのポート番号が異なる場合
							// Tomlのポート番号に更新
							if (serverDBPort != serverTomlPort) {
								dbStatusMap.get(serverDBName).put("port", String.valueOf(serverTomlPort));
								insql = "UPDATE status SET port=? WHERE name=?;";
								ps = conn.prepareStatement(insql);
								ps.setInt(1, serverTomlPort);
								ps.setString(2, serverDBName);
								ps.executeUpdate();
								console.sendMessage(Component.text(serverDBName+"サーバー(ポート:"+serverDBPort+")のポートを "+serverTomlPort+" に更新しました。").color(NamedTextColor.GREEN));
							}
						}

						// サーバータイプがTomlから得られるconfigとdatabaseで異なる場合、databaseの情報をconfigに合わせる
						if (!serverTomlType.equalsIgnoreCase(serverDBType)) {
							dbStatusMap.get(serverDBName).put("type", serverTomlType);
							String insql2 = "UPDATE status SET type=? WHERE name=?;";
							try (PreparedStatement ps2 = conn.prepareStatement(insql2)) {
								ps2.setString(1, serverTomlType.toLowerCase());
								ps2.setString(2, serverDBName);
								ps2.executeUpdate();
								console.sendMessage(Component.text(serverDBName+"サーバーのタイプを"+serverTomlType+"に更新しました。").color(NamedTextColor.GREEN));	
							} catch (SQLException e) {
								logger.error("An error occurred while updating the server type: " + e.getMessage());
								for (StackTraceElement element : e.getStackTrace()) {
									logger.error(element.toString());
								}
							}
						}

						// データベースへの削除を行う
						// ( 特別、追加を行う )
						// Tomlにあるが、DBにないサーバーを追加
						for (Map.Entry<String, Integer> configEntry : velocityToml.entrySet()) {
							// 以下、Tomlの配列を回して得られるサーバーネームで得たconfig情報
							String serverConfigName = configEntry.getKey(),
							 		serverConfigType = config.getString("Servers." + serverConfigName + ".Type", null);
							int serverConfigPort = configEntry.getValue();
							if (!dbStatusMap.containsKey(serverConfigName)) {
								// ポートの重複をチェック
								if (dbStatusMap.values().stream().anyMatch(map -> map.containsValue(String.valueOf(serverConfigPort)))) {
									throw new SQLException("ポート番号が重複しています: " + serverConfigPort);
								}
								// サーバー名の重複をチェック
								if (dbStatusMap.containsKey(serverConfigName)) {
									throw new SQLException("サーバー名が重複しています: " + serverConfigName);
								}

								// マップ更新 (追加)
								Map<String, String> newServerInfo = new HashMap<>();
								newServerInfo.put("port", String.valueOf(serverConfigPort));
								newServerInfo.put("type", serverConfigType);
								dbStatusMap.put(serverConfigName, newServerInfo);

								insql = "INSERT INTO status (name,port,type) VALUES (?,?,?);";
								ps = conn.prepareStatement(insql);
								ps.setString(1, serverConfigName);
								ps.setInt(2, serverConfigPort);
								ps.setString(3, serverConfigType.toLowerCase());
								ps.executeUpdate();
								console.sendMessage(Component.text(serverConfigName+"サーバー(ポート:"+serverConfigPort+")をデータベースに追加しました。").color(NamedTextColor.GREEN));
							}
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
