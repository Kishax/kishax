package velocity_command;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import velocity.Config;
import velocity.Database;
import velocity.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class Request
{
	private final Main plugin;
	private final ProxyServer server;
	public Connection conn = null;
	public ResultSet minecrafts = null, reqstatus = null;
	public ResultSet[] resultsets = {minecrafts,reqstatus};
	public PreparedStatement ps = null;
	
	public Request(CommandSource source,String[] args)
	{
		this.plugin = Main.getInstance();
		this.server = this.plugin.getServer();
		
		if (source instanceof Player)
		{
            // プレイヤーがコマンドを実行した場合の処理
			Player player = (Player) source;
        
            if(args.length == 1 || Objects.isNull(args[1]) || args[1].isEmpty())
            {
            	player.sendMessage(Component.text("サーバー名を入力してください。").color(NamedTextColor.RED));
            	return;
            }
            
            String targetServerName = args[0];
            boolean containsServer = false;
            for (RegisteredServer server : this.server.getAllServers())
            {
                if (server.getServerInfo().getName().equalsIgnoreCase(targetServerName))
                {
                    containsServer = true;
                    break;
                }
            }

            if (!containsServer)
            {
                player.sendMessage(Component.text("サーバー名が違います。").color(NamedTextColor.RED));
                return;
            }
            
            if(((String) Config.getConfig().get("Servers."+args[1]+".Bat_Path")).isEmpty())
            {
            	player.sendMessage(Component.text("許可されていません。").color(NamedTextColor.RED));
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
    					if(ss_sa_minute>((int) Config.getConfig().getOrDefault("Interval.Session",3)))
    					{
    						player.sendMessage(Component.text("セッションが無効です。").color(NamedTextColor.RED));
    						return;
    					}
    					
        				// /reqを実行したときに発行したセッションタイムをみる
    	                Timestamp req_timeget = minecrafts.getTimestamp("req");
    	                long req_timestamp = req_timeget.getTime() / 1000L;
    					
    					long req_sa = now_timestamp-req_timestamp;
    					long req_sa_minute = req_sa/60;
        				
        		        if(req_sa_minute<=((int) Config.getConfig().getOrDefault("Interval.Request",0)))
        		        {
        		        	player.sendMessage(Component.text("リクエストは"+((int) Config.getConfig().getOrDefault("Interval.Request",0))+"分に1回までです。").color(NamedTextColor.RED));
        		        	return;
        		        }
    				}
    			}
    			else
    			{
    				// MySQLサーバーにサーバーが登録されてなかった場合
    				this.plugin.getLogger().info(NamedTextColor.RED+"このサーバーは、データベースに登録されていません。");
    				player.sendMessage(Component.text("このサーバーは、データベースに登録されていません。").color(NamedTextColor.RED));
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
		        	player.sendMessage(Component.text("リクエストが集中しています！\\nしばらくしてやり直してください。").color(NamedTextColor.BLUE));
		        	return;
		        }
		        
		        // /reqを実行したので、セッションタイムをreqに入れる
				sql = "UPDATE minecraft SET req=CURRENT_TIMESTAMP WHERE uuid=?;";
				ps = conn.prepareStatement(sql);
				ps.setString(1,player.getUniqueId().toString());
				ps.executeUpdate();
		        
		        String pythonScriptPath = ((String) Config.getConfig().get("Servers.Request_Path"));
            	// ProcessBuilderを作成
		        ProcessBuilder pb = null;
		        if(((boolean) Config.getConfig().get("Debug.Mode")))
		        {
		        	pb = new ProcessBuilder
	            			(
	            					"python",
	            					pythonScriptPath, 
	            					player.getUsername(),
	            					player.getUniqueId().toString(),
	            					((String) Config.getConfig().get("Servers.Hub")),
	            					args[1].toString(),
	            					req_type,
	            					((String) Config.getConfig().get("Servers."+args[1]+".Bat_Path")),
	            					"test"
	            			);
		        }
		        else
		        {
		        	pb = new ProcessBuilder
	            			(
	            					"python",
	            					pythonScriptPath, 
	            					player.getUsername(),
	            					player.getUniqueId().toString(),
	            					((String) Config.getConfig().get("Servers.Hub")),
	            					args[1].toString(),
	            					req_type,
	            					((String) Config.getConfig().get("Servers."+args[1]+".Bat_Path"))
	            			);
		        }
            	pb.start();
            	player.sendMessage(Component.text("送信されました。").color(NamedTextColor.GREEN));
            	
            	// add log
            	sql = "INSERT INTO mine_log (name,uuid,server,req,reqserver) VALUES (?,?,?,?,?);";
    			ps = conn.prepareStatement(sql);
    			ps.setString(1, player.getUsername());
    			ps.setString(2, player.getUniqueId().toString());
    			ps.setString(3, player.getCurrentServer().toString());
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
			source.sendMessage(Component.text("このコマンドはプレイヤーのみが実行できます。").color(NamedTextColor.RED));
			return;
		}
        return;
	}
}