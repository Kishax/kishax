package bungee_command;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;

import bungee.Config;
import bungee.Database;
import bungee.Main;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.chat.TextComponent;

public class Request
{
	public Main plugin;
	public Connection conn = null;
	public ResultSet minecrafts = null, reqstatus = null;
	public ResultSet[] resultsets = {minecrafts,reqstatus};
	public PreparedStatement ps = null;
	
	public Request(CommandSender sender, String[] args)
	{
		if (sender instanceof ProxiedPlayer)
		{
            // プレイヤーがコマンドを実行した場合の処理
			ProxiedPlayer player = (ProxiedPlayer) sender;
        
            if(args.length == 1 || Objects.isNull(args[1]) || args[1].isEmpty())
            {
            	player.sendMessage(new TextComponent(ChatColor.RED+"サーバー名を入力してください。"));
            	return;
            }
            boolean containsServer = false;
            for (ServerInfo serverInfo : ProxyServer.getInstance().getServers().values())
            {
            	if(serverInfo.getName().equalsIgnoreCase(args[1]))
            	{
            		containsServer = true;
            		break;
            	}
            }
            if(!containsServer)
            {
            	player.sendMessage(new TextComponent(ChatColor.RED+"サーバー名が違います。"));
            	return;
            }
            
            if(Config.getConfig().getString("Servers."+args[1]+".Bat_Path").isEmpty())
            {
            	player.sendMessage(new TextComponent(ChatColor.RED+"許可されていません。"));
            	return;
            }
            
            try
            {
            	conn = Database.getConnection();
            	String sql = "SELECT * FROM minecraft WHERE uuid=?;";
    			ps = conn.prepareStatement(sql);
    			ps.setString(1,player.getUniqueId().toString());
    			ResultSet minecrafts = ps.executeQuery();
    			if(minecrafts.next())
    			{
    				if(Objects.nonNull(minecrafts.getTimestamp("sst")) && Objects.nonNull(minecrafts.getTimestamp("req")))
    				{
        				long now_timestamp = Instant.now().getEpochSecond();
    					
    					// /ssで発行したセッションタイムをみる
    	                Timestamp sst_timeget = minecrafts.getTimestamp("sst");
    	                long sst_timestamp = sst_timeget.getTime() / 1000L;
    					
    					long ss_sa = now_timestamp-sst_timestamp;
    					long ss_sa_minute = ss_sa/60;
    					if(ss_sa_minute>Config.getConfig().getInt("Interval.Session",3))
    					{
    						player.sendMessage(new TextComponent(ChatColor.RED+"セッションが無効です。"));
    						return;
    					}
    					
        				// /reqを実行したときに発行したセッションタイムをみる
    	                Timestamp req_timeget = minecrafts.getTimestamp("req");
    	                long req_timestamp = req_timeget.getTime() / 1000L;
    					
    					long req_sa = now_timestamp-req_timestamp;
    					long req_sa_minute = req_sa/60;
        				
        		        if(req_sa_minute<=Config.getConfig().getInt("Interval.Request",0))
        		        {
        		        	player.sendMessage(new TextComponent(ChatColor.RED+"リクエストは"+Config.getConfig().getInt("Interval.Request",0)+"分に1回までです。"));
        		        	return;
        		        }
    				}
    			}
    			else
    			{
    				// MySQLサーバーにサーバーが登録されてなかった場合
    				this.plugin.getLogger().info(ChatColor.RED+"このサーバーは、データベースに登録されていません。");
    				player.sendMessage(new TextComponent(ChatColor.RED+"このサーバーは、データベースに登録されていません。"));
    				return;
    			}
    			
		        String req_type = "";
		        sql = "SELECT * from mine_sktoken WHERE id=1;";
		        ps = conn.prepareStatement(sql);
		        ResultSet reqstatus = ps.executeQuery();
		        if(reqstatus.next())
		        {
		        	int i = 0;
		        	while (i<=2)
		        	{
		        		if(!reqstatus.getBoolean("req"+i))
		        		{
		        			req_type = "req"+i; break;
		        		}
		        		i++;
		        	}
		        }
		        
		        if (req_type.isEmpty())
		        {
		        	player.sendMessage(new TextComponent(ChatColor.BLUE+"リクエストが集中しています！\\nしばらくしてやり直してください。"));
		        	return;
		        }
		        
		        // /reqを実行したので、セッションタイムをreqに入れる
				sql = "UPDATE minecraft SET req=CURRENT_TIMESTAMP WHERE uuid=?;";
				ps = conn.prepareStatement(sql);
				ps.setString(1,player.getUniqueId().toString());
				ps.executeUpdate();
		        
		        String pythonScriptPath = Config.getConfig().getString("Servers.Request_Path");
            	// ProcessBuilderを作成
		        ProcessBuilder pb = null;
		        if(Config.getConfig().getBoolean("Debug.Mode"))
		        {
		        	pb = new ProcessBuilder
	            			(
	            					"python",
	            					pythonScriptPath, 
	            					player.getName().toString(),
	            					player.getUniqueId().toString(),
	            					Config.getConfig().getString("Servers.Hub"),
	            					args[1].toString(),
	            					req_type,
	            					Config.getConfig().getString("Servers."+args[1]+".Bat_Path"),
	            					"test"
	            			);
		        }
		        else
		        {
		        	pb = new ProcessBuilder
	            			(
	            					"python",
	            					pythonScriptPath, 
	            					player.getName().toString(),
	            					player.getUniqueId().toString(),
	            					Config.getConfig().getString("Servers.Hub"),
	            					args[1].toString(),
	            					req_type,
	            					Config.getConfig().getString("Servers."+args[1]+".Bat_Path")
	            			);
		        }
            	pb.start();
            	player.sendMessage(new TextComponent(ChatColor.GREEN+"送信されました。"));
            	
            	// add log
            	sql = "INSERT INTO mine_log (name,uuid,server,req,reqserver) VALUES (?,?,?,?,?);";
    			ps = conn.prepareStatement(sql);
    			ps.setString(1, player.getName().toString());
    			ps.setString(2, player.getUniqueId().toString());
    			ps.setString(3, player.getServer().getInfo().getName());
    			ps.setBoolean(4, true);
    			ps.setString(5, args[1]);
    			ps.executeUpdate();
            }
            catch (IOException | SQLException | ClassNotFoundException e)
            {
            	e.printStackTrace();
            }
            finally
            {
            	Database.close_resorce(resultsets, conn, ps);
            }
        }
		else
		{
			sender.sendMessage(new TextComponent(ChatColor.RED+"このコマンドはプレイヤーのみが実行できます。"));
			return;
		}
        return;
	}
}