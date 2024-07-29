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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class DoServerOnline
{
	private final Config config;
    private Connection conn = null;
    private PreparedStatement ps = null;
    public ResultSet mine_status = null;
	public ResultSet[] resultsets = {mine_status};
    private final Main plugin;
    private final Logger logger;
    private final ProxyServer server;
    private final ConsoleCommandSource console;
    private final Database db;
    private Map<String, Integer> serverConfigInfo = new HashMap<>();
    private Map<String, Integer> serverDBInfo = new HashMap<>();
    
    @Inject
    public DoServerOnline(Main plugin, ProxyServer server, Logger logger, Config config, Database db, ConsoleCommandSource console)
    {
    	this.plugin = plugin;
    	this.logger = logger;
    	this.config = config;
    	this.db = db;
    	this.server = server;
    	this.console = console;
    }
	
	public void UpdateDatabase()
	{
		// Configから、サーバー名とポートを取得してMySQLに反映
		// テーブルにないものは追加、Configになくて、テーブルにあるものは削除
		//　VelocityネットワークにつながらないMODサーバーなどの例外があるため、そういうのはconfig.ymlに記述
		// ポートが重複していたらエラーを出す。
		// 同じサーバー名がテーブルにあって、ポート番号が違っていたら、ポート番号を更新する。
		// まず、サーバー名とポートを取得する。
		// 以下非同期処理
		server.getScheduler().buildTask(plugin, () ->
        {
        	try
        	{
        		conn = db.getConnection();
        		if(Objects.nonNull(conn))
    			{
        			logger.info("MySQL Server is connected!");
    			}
        		else
    			{
        			logger.info("MySQL Server is canceled for config value not given");
        			return;
    			}
        		
				String insql = "UPDATE mine_status SET online=? WHERE name=?;";
				ps = conn.prepareStatement(insql);
				ps.setBoolean(1, true);
				ps.setString(2, "Proxy");
				ps.executeUpdate();
				
        		for (RegisteredServer server : server.getAllServers())
                {
					String serverConfigName = server.getServerInfo().getName();
                	int serverConfigPort = server.getServerInfo().getAddress().getPort();
                	serverConfigInfo.put(serverConfigName, serverConfigPort);
                }
				
				insql = "SELECT * FROM mine_status WHERE exception!=? AND exception2!=?;";
				ps = conn.prepareStatement(insql);
				ps.setBoolean(1, true);
				ps.setBoolean(2, true);
				mine_status = ps.executeQuery();
				
				while(mine_status.next())
				{
					String serverDBName = mine_status.getString("name");
					int serverDBPort = mine_status.getInt("port");
					serverDBInfo.put(serverDBName,serverDBPort);
				}
				
				// サーバー情報の追加、削除、更新を行う
			    // DBの情報を回して、Configの情報と比較
				for (Map.Entry<String, Integer> dbEntry : serverDBInfo.entrySet())
				{
			        String serverDBName = dbEntry.getKey();
			        int serverDBPort = dbEntry.getValue();

			        // Configに存在しないサーバーは削除
			        if (!serverConfigInfo.containsKey(serverDBName))
			        {
			            insql = "DELETE FROM mine_status WHERE name = ?;";
			            ps = conn.prepareStatement(insql);
			            ps.setString(1, serverDBName);
			            ps.executeUpdate();
			            console.sendMessage(Component.text(serverDBName+"サーバーはConfigに記載されていないため、データベースから削除しました。").color(NamedTextColor.GREEN));
			        }
			        else
			        {
			            // Configに存在するサーバーがポート番号が異なる場合、ポート番号を更新
			            int serverConfigPort = serverConfigInfo.get(serverDBName);
			            if (serverDBPort != serverConfigPort)
			            {
			                insql = "UPDATE mine_status SET port=? WHERE name=?;";
			                ps = conn.prepareStatement(insql);
		                    ps.setInt(1, serverConfigPort);
		                    ps.setString(2, serverDBName);
		                    ps.executeUpdate();
		                    
		                    console.sendMessage(Component.text(serverDBName+"サーバー(ポート:"+serverDBPort+")のポートを "+serverConfigPort+" に更新しました。").color(NamedTextColor.GREEN));
			            }
			        }
			    }
				
				boolean secondCheck = false;
				// Configに存在するがDBに存在しないサーバーを追加
			    for (Map.Entry<String, Integer> configEntry : serverConfigInfo.entrySet())
			    {
			        String serverConfigName = configEntry.getKey();
			        int serverConfigPort = configEntry.getValue();
			        
			        // DBに存在しないサーバーを追加
			        if (!serverDBInfo.containsKey(serverConfigName))
			        {
			        	// 2回目以降、ここを通る場合はserverDBInfoを更新する。
			        	if(secondCheck)
			        	{
			        		// serverDBInfo初期化
			        		serverDBInfo.clear();
			        		insql = "SELECT * FROM mine_status;";
							ps = conn.prepareStatement(insql);
							mine_status = ps.executeQuery();
							while(mine_status.next())
							{
								String serverDBName = mine_status.getString("name");
								int serverDBPort = mine_status.getInt("port");
								serverDBInfo.put(serverDBName,serverDBPort);
							}
			        	}
			        	secondCheck =true;
			            // ポートの重複をチェック
			            if (serverDBInfo.containsValue(serverConfigPort))
			            {
			                throw new SQLException("ポート番号が重複しています: " + serverConfigPort);
			            }
			            
			            // サーバー名の重複をチェック
			            if (serverDBInfo.containsKey(serverConfigName))
			            {
			            	throw new SQLException("サーバー名が重複しています: " + serverConfigName);
			            }
			            
			            insql = "INSERT INTO mine_status (name,port) VALUES (?,?);";
			            ps = conn.prepareStatement(insql);
		                ps.setString(1, serverConfigName);
		                ps.setInt(2, serverConfigPort);
		                ps.executeUpdate();
		                
		                console.sendMessage(Component.text(serverConfigName+"サーバー(ポート:"+serverConfigPort+")をデータベースに追加しました。").color(NamedTextColor.GREEN));
			        }
			    }
        	}
        	catch (ClassNotFoundException | SQLException e1)
    		{
    			e1.printStackTrace();
    		}
    		finally
    		{
    			db.close_resorce(resultsets,conn,ps);
    		}
        	
        }).schedule();
	}
}
