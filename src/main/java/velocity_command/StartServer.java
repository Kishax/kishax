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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import velocity.Config;
import velocity.Database;
import velocity.Main;

public class StartServer
{
	private final Main plugin;
	private final ProxyServer server;
	public Connection conn = null;
	public ResultSet minecrafts = null, status = null;
	public ResultSet[] resultsets = {minecrafts,status};
	public PreparedStatement ps = null;
	
	public StartServer(CommandSource source,String[] args)
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
				if(server.getServerInfo().getName().equalsIgnoreCase(targetServerName))
				{
					containsServer = true;
					break;
				}
			}
			if(!containsServer)
			{
		        player.sendMessage(Component.text("サーバー名が違います。").color(NamedTextColor.RED));
		        return;
			}
			else
			{
				try
				{
					conn = Database.getConnection();
					String sql = "SELECT * FROM minecraft WHERE uuid=?;";
	    			ps = conn.prepareStatement(sql);
	    			ps.setString(1,player.getUniqueId().toString());
	    			ResultSet minecrafts = ps.executeQuery();
	    			
					sql = "SELECT "+args[1]+" FROM mine_status WHERE id=1;";
					ps = conn.prepareStatement(sql);
					ResultSet status = ps.executeQuery();
					if(minecrafts.next())
					{
						
						
						//初参加のプレイヤーのsst,req,stカラムはnull値を返すので
						if(Objects.nonNull(minecrafts.getTimestamp("sst")) && Objects.nonNull(minecrafts.getTimestamp("st")))
						{
							long now_timestamp = Instant.now().getEpochSecond();
							
							// /ssで発行したセッションタイムをみる
			                Timestamp sst_timeget = minecrafts.getTimestamp("sst");
			                long sst_timestamp = sst_timeget.getTime() / 1000L;
							
							long ss_sa = now_timestamp-sst_timestamp;
							long ss_sa_minute = ss_sa/60;
							if(ss_sa_minute>=(int) Config.getConfig().getOrDefault("Interval.Session",3))
							{
								player.sendMessage(Component.text("セッションが無効です。").color(NamedTextColor.RED));
								return;
							}
							
							// /startを実行したときに発行したセッションタイムをみる
							Timestamp st_timeget = minecrafts.getTimestamp("st");
			                long st_timestamp = st_timeget.getTime() / 1000L;
			                
					        long sa = now_timestamp-st_timestamp;
					        long sa_minute = sa/60;
					        
					        if(sa_minute<=(int) Config.getConfig().getOrDefault("Interval.Start_Server",0))
					        {
					        	player.sendMessage(Component.text("サーバーの起動間隔は"+(int) Config.getConfig().getOrDefault("Interval.Start_Server",0)+"分以上は空けてください。").color(NamedTextColor.RED));
					        	return;
					        }
						}
	    		        
						if(status.next())
						{
							if(status.getBoolean(args[1]))
							{
								player.sendMessage(Component.text(args[1]+"サーバーは起動中です。").color(NamedTextColor.RED));
								this.plugin.getLogger().info(NamedTextColor.RED+args[1]+"サーバーは起動中です。");
							}
							else
							{
								if(((String) Config.getConfig().getOrDefault("Servers."+args[1]+".Bat_Path","")).isEmpty())
								{
									player.sendMessage(Component.text("許可されていません。").color(NamedTextColor.RED));
									return;
								}
								// /startでサーバー起動
								// stにセッションタイムを入れる
								sql = "UPDATE minecraft SET st=CURRENT_TIMESTAMP WHERE uuid=?;";
								ps = conn.prepareStatement(sql);
								ps.setString(1,player.getUniqueId().toString());
								ps.executeUpdate();
								
					            // バッチファイルのパスを指定
					            String batchFilePath = (String) Config.getConfig().get("Servers."+args[1]+".Bat_Path");

					            // ProcessBuilderを作成
					            ProcessBuilder processBuilder = new ProcessBuilder(batchFilePath);

					            // プロセスを開始
					            processBuilder.start();
					            
					            TextComponent component = Component.text()
        			    			    	.append(Component.text("UUID認証...PASS\n\nアドミン認証...PASS\n\nALL CORRECT\n\n").color(NamedTextColor.GREEN))
        			    			    	.append(Component.text(args[1]+"サーバーがまもなく起動します。").color(NamedTextColor.GREEN))
        									.build();
        						
        						player.sendMessage(component);
					            this.plugin.getLogger().info(NamedTextColor.GREEN+args[1]+"サーバーがまもなく起動します。");
					            
					            sql = "UPDATE mine_status SET "+args[1]+"=? WHERE id=1;";
					            ps = conn.prepareStatement(sql);
				            	ps.setBoolean(1, true);
				            	ps.executeUpdate();
				            	
				            	// add log
				            	sql = "INSERT INTO mine_log (name,uuid,server,sss,status) VALUES (?,?,?,?,?);";
		            			ps = conn.prepareStatement(sql);
		            			ps.setString(1, player.getUsername());
		            			ps.setString(2, player.getUniqueId().toString());
		            			ps.setString(3, player.getCurrentServer().toString());
		            			ps.setBoolean(4, true);
		            			ps.setString(5, "start");
		            			ps.executeUpdate();
							}
						}
					}
					else
					{
						// MySQLサーバーにプレイヤー情報が登録されてなかった場合
	    				this.plugin.getLogger().info(NamedTextColor.RED+"あなたのプレイヤー情報がデータベースに登録されていません。");
	    				player.sendMessage(Component.text(player.getUsername()+"のプレイヤー情報がデータベースに登録されていません。").color(NamedTextColor.RED));
					}
					
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
		}
		else
		{
			source.sendMessage(Component.text("このコマンドはプレイヤーのみが実行できます。").color(NamedTextColor.RED));
			return;
		}
		return;
	}
}