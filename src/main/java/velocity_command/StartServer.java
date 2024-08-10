package velocity_command;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import discord.MessageEditorInterface;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import velocity.BroadCast;
import velocity.Config;
import velocity.DatabaseInterface;

public class StartServer
{
	private final ProxyServer server;
	private final Config config;
	private final Logger logger;
	private final DatabaseInterface db;
	private final ConsoleCommandSource console;
	private final BroadCast bc;
	private final MessageEditorInterface discordME;
	private String currentServerName = null;
	
	public Connection conn = null;
	public ResultSet minecrafts = null, mine_status = null;
	public ResultSet[] resultsets = {minecrafts,mine_status};
	public PreparedStatement ps = null;
	
	@Inject
	public StartServer
	(
		ProxyServer server, Logger logger, Config config,
		DatabaseInterface db, ConsoleCommandSource console, MessageEditorInterface discordME,
		BroadCast bc
	)
	{
		this.server = server;
		this.logger = logger;
		this.config = config;
		this.db = db;
		this.console = console;
		this.bc = bc;
		this.discordME = discordME;
	}
	
	public void execute(CommandSource source,String[] args)
	{
		if (source instanceof Player)
		{
            // プレイヤーがコマンドを実行した場合の処理
			Player player = (Player) source;
			if(args.length == 1 || Objects.isNull(args[1]) || args[1].isEmpty())
			{
				player.sendMessage(Component.text("サーバー名を入力してください。").color(NamedTextColor.RED));
				return;
			}
			
			// プレイヤーの現在のサーバーを取得
	        player.getCurrentServer().ifPresent(serverConnection ->
	        {
	            RegisteredServer server = serverConnection.getServer();
	            currentServerName = server.getServerInfo().getName();
	        });
			
			String targetServerName = args[1];
			boolean containsServer = false;
			for (RegisteredServer server : server.getAllServers())
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
					conn = db.getConnection();
					String sql = "SELECT * FROM minecraft WHERE uuid=?;";
	    			ps = conn.prepareStatement(sql);
	    			ps.setString(1,player.getUniqueId().toString());
	    			ResultSet minecrafts = ps.executeQuery();
	    			
					sql = "SELECT * FROM mine_status WHERE name=?;";
					ps = conn.prepareStatement(sql);
					ps.setString(1,args[1]);
					ResultSet mine_status = ps.executeQuery();
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
							if(ss_sa_minute>=config.getInt("Interval.Session",3))
							{
								player.sendMessage(Component.text("セッションが無効です。").color(NamedTextColor.RED));
								return;
							}
							
							// /startを実行したときに発行したセッションタイムをみる
							Timestamp st_timeget = minecrafts.getTimestamp("st");
			                long st_timestamp = st_timeget.getTime() / 1000L;
			                
					        long sa = now_timestamp-st_timestamp;
					        long sa_minute = sa/60;
					        
					        if(sa_minute<=config.getInt("Interval.Start_Server",0))
					        {
					        	player.sendMessage(Component.text("サーバーの起動間隔は"+config.getInt("Interval.Start_Server",0)+"分以上は空けてください。").color(NamedTextColor.RED));
					        	return;
					        }
						}
	    		        
						if(mine_status.next())
						{
							if(mine_status.getBoolean("online"))
							{
								player.sendMessage(Component.text(args[1]+"サーバーは起動中です。").color(NamedTextColor.RED));
								logger.info(NamedTextColor.RED+args[1]+"サーバーは起動中です。");
							}
							else
							{
								if(config.getString("Servers."+args[1]+".Bat_Path","").isEmpty())
								{
									player.sendMessage(Component.text("許可されていません。").color(NamedTextColor.RED));
									return;
								}
								
								// 全サーバーにプレイヤーがサーバーを起動したことを通知
								TextComponent notifyComponent = Component.text()
										.append(Component.text(player.getUsername()+"が"+args[1]+"サーバーを起動しました。\nまもなく"+args[1]+"サーバーが起動します。").color(NamedTextColor.AQUA))
										.build();
								bc.sendExceptPlayerMessage(notifyComponent, player.getUsername());
								
								// stにセッションタイムを入れる
								sql = "UPDATE minecraft SET st=CURRENT_TIMESTAMP WHERE uuid=?;";
								ps = conn.prepareStatement(sql);
								ps.setString(1,player.getUniqueId().toString());
								ps.executeUpdate();
								
					            // バッチファイルのパスを指定
					            String batchFilePath = config.getString("Servers."+args[1]+".Bat_Path");
					            ProcessBuilder processBuilder = new ProcessBuilder(batchFilePath);
					            processBuilder.start();
					            
					            discordME.AddEmbedSomeMessage("Start", player, args[1]);
					            
					            TextComponent component = Component.text()
        			    			    	.append(Component.text("UUID認証...PASS\n\nアドミン認証...PASS\n\nALL CORRECT\n\n").color(NamedTextColor.GREEN))
        			    			    	.append(Component.text(args[1]+"サーバーがまもなく起動します。").color(NamedTextColor.GREEN))
        									.build();
        						
        						player.sendMessage(component);
					            console.sendMessage(Component.text(args[1]+"サーバーがまもなく起動します。").color(NamedTextColor.GREEN));
					            
					            sql = "UPDATE mine_status SET online=? WHERE name=?;";
					            ps = conn.prepareStatement(sql);
				            	ps.setBoolean(1, true);
				            	ps.setString(2, args[1]);
				            	ps.executeUpdate();
				            	
				            	// add log
				            	sql = "INSERT INTO mine_log (name,uuid,server,sss,status) VALUES (?,?,?,?,?);";
		            			ps = conn.prepareStatement(sql);
		            			ps.setString(1, player.getUsername());
		            			ps.setString(2, player.getUniqueId().toString());
		            			ps.setString(3, currentServerName);
		            			ps.setBoolean(4, true);
		            			ps.setString(5, "start");
		            			ps.executeUpdate();
							}
						}
					}
					else
					{
						// MySQLサーバーにプレイヤー情報が登録されてなかった場合
	    				logger.info(NamedTextColor.RED+"あなたのプレイヤー情報がデータベースに登録されていません。");
	    				player.sendMessage(Component.text(player.getUsername()+"のプレイヤー情報がデータベースに登録されていません。").color(NamedTextColor.RED));
					}
					
		        }
				catch (IOException | SQLException | ClassNotFoundException e)
				{
		            e.printStackTrace();
		        }
				finally
				{
					db.close_resorce(resultsets, conn, ps);
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