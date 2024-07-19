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
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;

public class StartServer
{
	public Main plugin;
	public Connection conn = null;
	public ResultSet minecrafts = null, status = null;
	public ResultSet[] resultsets = {minecrafts,status};
	public PreparedStatement ps = null;
	
	public StartServer(CommandSender sender, String[] args)
	{
		this.plugin = Main.getInstance();
		
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
			else
			{
				try
				{
					conn = Database.getConnection();
					String sql = "SELECT * FROM minecraft WHERE uuid=?;";
	    			ps = conn.prepareStatement(sql);
	    			ps.setString(1,player.getUniqueId().toString());
	    			ResultSet minecrafts = ps.executeQuery();
	    			
					sql = "SELECT "+args[1].toString()+" FROM mine_status WHERE id=1;";
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
							if(ss_sa_minute>=Config.getConfig().getInt("Interval.Session",3))
							{
								player.sendMessage(new TextComponent(ChatColor.RED+"セッションが無効です。"));
								return;
							}
							
							// /startを実行したときに発行したセッションタイムをみる
							Timestamp st_timeget = minecrafts.getTimestamp("st");
			                long st_timestamp = st_timeget.getTime() / 1000L;
			                
					        long sa = now_timestamp-st_timestamp;
					        long sa_minute = sa/60;
					        
					        if(sa_minute<=Config.getConfig().getInt("Interval.Start_Server",0))
					        {
					        	player.sendMessage(new TextComponent(ChatColor.RED+"サーバーの起動間隔は"+Config.getConfig().getInt("Interval.Start_Server",0)+"分以上は空けてください。"));
					        	return;
					        }
						}
	    		        
						if(status.next())
						{
							if(status.getBoolean(args[1].toString()))
							{
								player.sendMessage(new TextComponent(ChatColor.RED+args[1].toString()+"サーバーは起動中です。"));
								this.plugin.getLogger().info(ChatColor.RED+args[1].toString()+"サーバーは起動中です。");
							}
							else
							{
								if(Config.getConfig().getString("Servers."+args[1].toString()+".Bat_Path").isEmpty())
								{
									player.sendMessage(new TextComponent(ChatColor.RED+"許可されていません。"));
									return;
								}
								// /startでサーバー起動
								// stにセッションタイムを入れる
								sql = "UPDATE minecraft SET st=CURRENT_TIMESTAMP WHERE uuid=?;";
								ps = conn.prepareStatement(sql);
								ps.setString(1,player.getUniqueId().toString());
								ps.executeUpdate();
								
					            // バッチファイルのパスを指定
					            String batchFilePath = Config.getConfig().getString("Servers."+args[1].toString()+".Bat_Path");

					            // ProcessBuilderを作成
					            ProcessBuilder processBuilder = new ProcessBuilder(batchFilePath);

					            // プロセスを開始
					            processBuilder.start();
					            
        						ComponentBuilder component =
        			    			    new ComponentBuilder(ChatColor.GREEN+"UUID認証...PASS\n\nアドミン認証...PASS\n\nALL CORRECT\n\n")
        			    			    	.append(ChatColor.GREEN+args[1].toString()+"サーバーがまもなく起動します。");
        						
        						// BaseComponent[]に変換
        						BaseComponent[] messageComponents = component.create();
        						
        						player.sendMessage(messageComponents);
					            this.plugin.getLogger().info(ChatColor.GREEN+args[1].toString()+"サーバーがまもなく起動します。");
					            
					            sql = "UPDATE mine_status SET "+args[1].toString()+"=? WHERE id=1;";
					            ps = conn.prepareStatement(sql);
				            	ps.setBoolean(1, true);
				            	ps.executeUpdate();
				            	
				            	// add log
				            	sql = "INSERT INTO mine_log (name,uuid,server,sss,status) VALUES (?,?,?,?,?);";
		            			ps = conn.prepareStatement(sql);
		            			ps.setString(1, player.getName().toString());
		            			ps.setString(2, player.getUniqueId().toString());
		            			ps.setString(3, player.getServer().getInfo().getName());
		            			ps.setBoolean(4, true);
		            			ps.setString(5, "start");
		            			ps.executeUpdate();
							}
						}
					}
					else
					{
						// MySQLサーバーにプレイヤー情報が登録されてなかった場合
	    				this.plugin.getLogger().info(ChatColor.RED+"あなたのプレイヤー情報がデータベースに登録されていません。");
	    				player.sendMessage(new TextComponent(ChatColor.RED+player.getName().toString()+"のプレイヤー情報がデータベースに登録されていません。"));
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
			sender.sendMessage(new TextComponent(ChatColor.RED+"このコマンドはプレイヤーのみが実行できます。"));
			return;
		}
		return;
	}
}