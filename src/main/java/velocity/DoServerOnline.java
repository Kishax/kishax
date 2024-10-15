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
    private final Map<String, Integer> serverConfigInfo = new HashMap<>();
    private final Map<String, Integer> serverDBInfo = new HashMap<>();
	private final Map<String, String> serverDBTypeInfo = new HashMap<>();
    
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
		// Configから、サーバー名とポートを取得してMySQLに反映
		// テーブルにないものは追加、Configになくて、テーブルにあるものは削除
		//　VelocityネットワークにつながらないMODサーバーなどの例外があるため、そういうのはconfig.ymlに記述
		// ポートが重複していたらエラーを出す。
		// 同じサーバー名がテーブルにあって、ポート番号が違っていたら、ポート番号を更新する。
		// まず、サーバー名とポートを取得する。
		// 以下非同期処理
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
                	serverConfigInfo.put(serverConfigName, serverConfigPort);
                }

				
				insql = "SELECT * FROM status WHERE exception!=? AND exception2!=?;";
				ps = conn.prepareStatement(insql);
				ps.setBoolean(1, true);
				ps.setBoolean(2, true);
				mine_status = ps.executeQuery();
				
				while (mine_status.next()) {
					String serverDBName = mine_status.getString("name");
					int serverDBPort = mine_status.getInt("port");
					serverDBTypeInfo.put(serverDBName, mine_status.getString("type"));
					serverDBInfo.put(serverDBName,serverDBPort);
				}
				
				// サーバー情報の追加、削除、更新を行う
			    // DBの情報を回して、Configの情報と比較
				for (Map.Entry<String, Integer> dbEntry : serverDBInfo.entrySet()) {
			        String serverDBName = dbEntry.getKey();
			        int serverDBPort = dbEntry.getValue();

			        // Configに存在しないサーバーは削除
			        if (!serverConfigInfo.containsKey(serverDBName)) {
			            insql = "DELETE FROM status WHERE name = ?;";
			            ps = conn.prepareStatement(insql);
			            ps.setString(1, serverDBName);
			            ps.executeUpdate();
			            console.sendMessage(Component.text(serverDBName+"サーバーはConfigに記載されていないため、データベースから削除しました。").color(NamedTextColor.GREEN));
			        } else {
			            // Configに存在するサーバーがポート番号が異なる場合、ポート番号を更新
			            int serverConfigPort = serverConfigInfo.get(serverDBName);
			            if (serverDBPort != serverConfigPort) {
			                insql = "UPDATE status SET port=? WHERE name=?;";
			                ps = conn.prepareStatement(insql);
		                    ps.setInt(1, serverConfigPort);
		                    ps.setString(2, serverDBName);
		                    ps.executeUpdate();
		                    
		                    console.sendMessage(Component.text(serverDBName+"サーバー(ポート:"+serverDBPort+")のポートを "+serverConfigPort+" に更新しました。").color(NamedTextColor.GREEN));
			            }
			        }

					String serverConfigType = config.getString("Servers." + serverDBName + ".Type", "");
						// サーバータイプがconfigとdatabaseで異なる場合、databaseの情報をconfigに合わせる
						if (serverDBTypeInfo.containsKey(serverDBName)) {
							String serverDBType = serverDBTypeInfo.get(serverDBName);
							if (!serverConfigType.equalsIgnoreCase(serverDBType)) {
								String insql2 = "UPDATE status SET type=? WHERE name=?;";
								try (PreparedStatement ps2 = conn.prepareStatement(insql2)) {
									ps2.setString(1, serverConfigType.toLowerCase());
									ps2.setString(2, serverDBName);
									ps2.executeUpdate();
									console.sendMessage(Component.text(serverDBName+"サーバーのタイプを"+serverConfigType+"に更新しました。").color(NamedTextColor.GREEN));	
								} catch (SQLException e) {
									logger.error("An error occurred while updating the server type: " + e.getMessage());
									for (StackTraceElement element : e.getStackTrace()) {
										logger.error(element.toString());
									}
								}
							}
						}
			    }
				
				boolean secondCheck = false;
				// Configに存在するがDBに存在しないサーバーを追加
			    for (Map.Entry<String, Integer> configEntry : serverConfigInfo.entrySet()) {
			        String serverConfigName = configEntry.getKey();
			        int serverConfigPort = configEntry.getValue();
			        
			        // DBに存在しないサーバーを追加
			        if (!serverDBInfo.containsKey(serverConfigName)) {
			        	// 2回目以降、ここを通る場合はserverDBInfoを更新する。
			        	if (secondCheck) {
			        		// serverDBInfo初期化
			        		serverDBInfo.clear();
			        		insql = "SELECT * FROM status;";
							ps = conn.prepareStatement(insql);
							mine_status = ps.executeQuery();
							while (mine_status.next()) {
								String serverDBName = mine_status.getString("name");
								int serverDBPort = mine_status.getInt("port");
								serverDBTypeInfo.put(serverDBName, mine_status.getString("type"));
								serverDBInfo.put(serverDBName,serverDBPort);
							}
			        	}

			        	secondCheck = true;
			            // ポートの重複をチェック
			            if (serverDBInfo.containsValue(serverConfigPort)) {
			                throw new SQLException("ポート番号が重複しています: " + serverConfigPort);
			            }
			            
			            // サーバー名の重複をチェック
			            if (serverDBInfo.containsKey(serverConfigName)) {
			            	throw new SQLException("サーバー名が重複しています: " + serverConfigName);
			            }
						
			            insql = "INSERT INTO status (name,port,live,mod,distributed) VALUES (?,?,?,?,?);";
                        ps = conn.prepareStatement(insql);
                        ps.setString(1, serverConfigName);
                        ps.setInt(2, serverConfigPort);

                        // サーバーのタイプを取得して設定
                        String serverConfigType = config.getString("Servers." + serverConfigName + ".Type", "");
						// サーバータイプがconfigとdatabaseで異なる場合、databaseの情報をconfigに合わせる
						if (serverDBTypeInfo.containsKey(serverConfigName)) {
							String serverDBType = serverDBTypeInfo.get(serverConfigName);
							if (!serverConfigType.equalsIgnoreCase(serverDBType)) {
								String insql2 = "UPDATE status SET type=? WHERE name=?;";
								try (PreparedStatement ps2 = conn.prepareStatement(insql2)) {
									ps2.setString(1, serverConfigType.toLowerCase());
									ps2.setString(2, serverConfigName);
									ps2.executeUpdate();
									console.sendMessage(Component.text(serverConfigName+"サーバーのタイプを"+serverConfigType+"に更新しました。").color(NamedTextColor.GREEN));	
								} catch (SQLException e) {
									logger.error("An error occurred while updating the server type: " + e.getMessage());
									for (StackTraceElement element : e.getStackTrace()) {
										logger.error(element.toString());
									}
								}
							}
						} else if ((!serverConfigType.isEmpty()) && (serverConfigType.equalsIgnoreCase("live") || serverConfigType.equalsIgnoreCase("mod") || serverConfigType.equalsIgnoreCase("distributed"))) {
							ps.setString(3, serverConfigType.toLowerCase());
						}

                        //ps.setBoolean(3, serverConfigType.equalsIgnoreCase("live"));

                        ps.executeUpdate();
		                
		                console.sendMessage(Component.text(serverConfigName+"サーバー(ポート:"+serverConfigPort+")をデータベースに追加しました。").color(NamedTextColor.GREEN));
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
