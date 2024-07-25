package bungee_command;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Random;

import bungee.Config;
import bungee.Database;
import bungee.Main;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

public class SetServer
{
	public Main plugin;
	public Connection conn = null;
	public ResultSet minecrafts = null, mine_status = null, issuperadmin = null, issubadmin = null;
	public ResultSet[] resultsets = {minecrafts,mine_status,issuperadmin,issubadmin};
	public PreparedStatement ps = null;
	
	public SetServer(CommandSender sender, String[] args)
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
            	this.plugin.getLogger().info(ChatColor.RED+"サーバー名が違います。");
            	return;
            }

            try
            {
            	conn = Database.getConnection();
    			String sql = "SELECT * FROM minecraft WHERE uuid=?;";
    			ps = conn.prepareStatement(sql);
    			ps.setString(1,player.getUniqueId().toString());
    			minecrafts = ps.executeQuery();
    			
    			sql = "SELECT * FROM mine_status WHERE id=1;";
    			ps = conn.prepareStatement(sql);
    			mine_status = ps.executeQuery();
    			
    			if(minecrafts.next())
    			{
    				if(mine_status.next())
        			{
        				if(mine_status.getBoolean(args[1].toString()))
        				{
        					// オンライン
        					if(minecrafts.getBoolean("confirm"))
        					{
        						// fmcアカウントを持っている /stpが使用可能
        						// /stpで用いるセッションタイム(現在時刻)(sst)をデータベースに
        						LocalDateTime now = LocalDateTime.now();
        				        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        				        String formattedDateTime = now.format(formatter);
        				        
        						sql = "UPDATE minecraft SET sst=? WHERE uuid=?;";
        						ps = conn.prepareStatement(sql);
        						ps.setString(1,formattedDateTime);
        		    			ps.setString(2,player.getUniqueId().toString());
        		    			ps.executeUpdate();
        		    			
        						ComponentBuilder component =
        			    			    new ComponentBuilder(ChatColor.WHITE+args[1].toString()+"サーバーは現在")
        			    			    	.append(ChatColor.AQUA+"オンライン")
        			    			    	.append(ChatColor.WHITE+"です。\nサーバーに入りますか？\n")
        			    			    	.append(ChatColor.GOLD+"YES")
        			    			    	.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/fmcp stp "+args[1].toString()))
        			    			        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("(クリックして)"+args[1].toString()+"サーバーに入ります。")))
        			    			    	.append(ChatColor.GOLD+" or ")
        			    			    	.append(ChatColor.GOLD+"NO")
        			    			    	.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/fmcp cancel"))
        			    			        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("(クリックして)キャンセルします。")));
        						
        						// BaseComponent[]に変換
        						BaseComponent[] messageComponents = component.create();
        						
        						player.sendMessage(messageComponents);
        					}
        					else
        					{
        						// fmcアカウントを持ってない
        						// 6桁の乱数を生成
        				        Random rnd = new Random();
        				        int ranum = 100000 + rnd.nextInt(900000);
        				        String ranumstr = Integer.toString(ranum);
        				        
        				        
        				        sql = "UPDATE minecraft SET secret2=? WHERE uuid=?;";
        				        ps = conn.prepareStatement(sql);
        				        ps.setInt(1,ranum);
        		    			ps.setString(2,player.getUniqueId().toString());
        		    			
        						ComponentBuilder component =
        			    			    new ComponentBuilder(ChatColor.WHITE+args[1].toString()+"サーバーは現在")
        			    			    	.append(ChatColor.AQUA+"オンライン")
        			    			    	.append(ChatColor.WHITE+"です。\nサーバーに参加するには、FMCアカウントとあなたのMinecraftでのUUIDをリンクさせる必要があります。以下、")
        			    			    	.append(ChatColor.LIGHT_PURPLE+"UUID認証").bold(true).underlined(true)
        			    			    	.append(ChatColor.WHITE+"より、手続きを進めてください。")
        			    			    	.append(ChatColor.LIGHT_PURPLE+"\nUUID認証\n\n").bold(true).underlined(true)
        			    			    	.append(ChatColor.GRAY+"https://keypforev.ddns.net/minecraft/uuid_check2.php").underlined(true)
        			    			    	.event(new ClickEvent(ClickEvent.Action.OPEN_URL,"https://keypforev.ddns.net/minecraft/uuid_check2.php?n="+minecrafts.getInt("id")))
        			    			    	.append(ChatColor.WHITE+"\n\n認証コードは ")
        			    			    	.append(ChatColor.BLUE+ranumstr)
        			    			    	.event(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, ranumstr))
        			    			    	.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("(クリックして)コピー")))
        			    			    	.append(ChatColor.WHITE+" です。")
        			    			    	.append(ChatColor.LIGHT_PURPLE+"\n\n認証コードの再生成").bold(true).underlined(true)
        			    			    	.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("(クリックして)認証コードを再生成します。")))
        			    			    	.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/fmcp retry"));
        						// BaseComponent[]に変換
        						BaseComponent[] messageComponents = component.create();
        						
        						player.sendMessage(messageComponents);
        					}
        				}
        				else
        				{
        					// オフライン
        					if(minecrafts.getBoolean("confirm"))
        					{
        						//メモリの確認
        						int sum_memory = 0;
        						//現在オンラインのサーバーのメモリ合計を取得
        						for (ServerInfo serverInfo: this.plugin.getProxy().getServers().values())
        						{
        							if(mine_status.getBoolean(serverInfo.getName().toString()))
        							{
        								sum_memory = sum_memory + Config.getConfig().getInt("Servers."+serverInfo.getName().toString()+".Memory",0);
        							}
        						}
        						// 起動・起動リクエストしたいサーバーのメモリも足す
        						sum_memory = sum_memory + Config.getConfig().getInt("Servers."+args[1].toString()+".Memory",0);
        						
        						// BungeeCordのメモリも足す
        						sum_memory = sum_memory + Config.getConfig().getInt("Servers.BungeeCord.Memory",0);
        								
    							if(!(sum_memory<=Config.getConfig().getInt("Servers.Memory_Limit",0)))
    							{
    								ComponentBuilder component =
            			    			    new ComponentBuilder(ChatColor.WHITE+args[1].toString()+"サーバーは現在")
            			    			    	.append(ChatColor.BLUE+"オフライン")
            			    			    	.append(ChatColor.WHITE+"です。")
            			    			    	.append(ChatColor.RED+"\nメモリ超過のため、サーバーを起動できません。("+sum_memory+"GB/"+Config.getConfig().getInt("Servers.Memory_Limit",0)+"GB)");
    								
            						// BaseComponent[]に変換
            						BaseComponent[] messageComponents = component.create();
            						
            						player.sendMessage(messageComponents);
    								return;
    							}
    							
    							sql = "SELECT * FROM lp_user_permissions WHERE uuid=? AND permission=?;";
    							ps = conn.prepareStatement(sql);
    							ps.setString(1, player.getUniqueId().toString());
    							ps.setString(2, "group.super-admin");
    							issuperadmin = ps.executeQuery();
    							
    							sql = "SELECT * FROM lp_user_permissions WHERE uuid=? AND permission=?;";
    							ps = conn.prepareStatement(sql);
    							ps.setString(1, player.getUniqueId().toString());
    							ps.setString(2, "group.sub-admin");
    							issubadmin = ps.executeQuery();
    							
        						// fmcアカウントを持っている
        						if(issuperadmin.next() || issubadmin.next())
        						{
        							// adminである /startが使用可能
        							// /startで用いるセッションタイム(現在時刻)(sst)をデータベースに
            						LocalDateTime now = LocalDateTime.now();
            				        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            				        String formattedDateTime = now.format(formatter);
            				        
            						sql = "UPDATE minecraft SET sst=? WHERE uuid=?;";
            						ps = conn.prepareStatement(sql);
            						ps.setString(1,formattedDateTime);
            		    			ps.setString(2,player.getUniqueId().toString());
            		    			ps.executeUpdate();
            		    			
            						ComponentBuilder component =
            			    			    new ComponentBuilder(ChatColor.WHITE+args[1].toString()+"サーバーは現在")
            			    			    	.append(ChatColor.BLUE+"オフライン")
            			    			    	.append(ChatColor.WHITE+"です。\nサーバーを起動しますか？")
            			    			    	.append(ChatColor.GOLD+"\nYES")
            			    			    	.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/fmcp start "+args[1].toString()))
            			    			        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("(クリックして)"+args[1].toString()+"サーバーを起動します。")))
            			    			    	.append(ChatColor.GOLD+" or ")
            			    			    	.append(ChatColor.GOLD+"NO")
            			    			    	.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/fmcp cancel"))
            			    			        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("(クリックして)キャンセルします。")));
            						
            						// BaseComponent[]に変換
            						BaseComponent[] messageComponents = component.create();
            						
            						player.sendMessage(messageComponents);
        						}
        						else
        						{
        							// adminでない /reqが使用可能
        							// /reqで用いるセッションタイム(現在時刻)(sst)をデータベースに
            						LocalDateTime now = LocalDateTime.now();
            				        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            				        String formattedDateTime = now.format(formatter);
            				        
            						sql = "UPDATE minecraft SET sst=? WHERE uuid=?;";
            						ps = conn.prepareStatement(sql);
            						ps.setString(1,formattedDateTime);
            		    			ps.setString(2,player.getUniqueId().toString());
            		    			ps.executeUpdate();
            		    			
            						ComponentBuilder component =
            			    			    new ComponentBuilder(ChatColor.WHITE+args[1].toString()+"サーバーは現在")
            			    			    	.append(ChatColor.BLUE+"オフライン")
            			    			    	.append(ChatColor.WHITE+"です。\n管理者に、サーバー起動のリクエストを送信できます。\n結果は3分以内に返ってきます。\n送信しますか？")
            			    			    	.append(ChatColor.GOLD+"\nYES")
            			    			    	.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/fmcp req "+args[1].toString()))
            			    			        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("(クリックして)"+args[1].toString()+"サーバー起動リクエストを送信する。")))
            			    			    	.append(ChatColor.GOLD+" or ")
            			    			    	.append(ChatColor.GOLD+"NO")
            			    			    	.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/fmcp cancel"))
            			    			        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("(クリックして)キャンセルします。")));
            						
            						// BaseComponent[]に変換
            						BaseComponent[] messageComponents = component.create();
            						
            						player.sendMessage(messageComponents);
        						}
        					}
        					else
        					{
        						// fmcアカウントを持ってない
        						// 6桁の乱数を生成
        				        Random rnd = new Random();
        				        int ranum = 100000 + rnd.nextInt(900000);
        				        String ranumstr = Integer.toString(ranum);
        				        
        				        sql = "UPDATE minecraft SET secret2=? WHERE uuid=?;";
        				        ps = conn.prepareStatement(sql);
        				        ps.setInt(1,ranum);
        		    			ps.setString(2,player.getUniqueId().toString());
        		    			ps.executeUpdate();
        		    			
        						ComponentBuilder component =
        			    			    new ComponentBuilder(ChatColor.WHITE+args[1].toString()+"サーバーは現在")
        			    			    	.append(ChatColor.BLUE+"オフライン")
        			    			    	.append(ChatColor.WHITE+"です。\nサーバーを起動して、参加するには、FMCアカウントとあなたのMinecraftでのUUIDをリンクさせる必要があります。以下、")
        			    			    	.append(ChatColor.LIGHT_PURPLE+"UUID認証").bold(true).underlined(true)
        			    			    	.append(ChatColor.WHITE+"より、手続きを進めてください。")
        			    			    	.append(ChatColor.LIGHT_PURPLE+"\nUUID認証\n\n").bold(true).underlined(true)
        			    			    	.append(ChatColor.GRAY+"https://keypforev.ddns.net/minecraft/uuid_check2.php").underlined(true)
        			    			    	.event(new ClickEvent(ClickEvent.Action.OPEN_URL,"https://keypforev.ddns.net/minecraft/uuid_check2.php?n="+minecrafts.getInt("id")))
        			    			    	.append(ChatColor.WHITE+"\n\n認証コードは ")
        			    			    	.append(ChatColor.BLUE+ranumstr)
        			    			    	.event(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, ranumstr))
        			    			    	.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("(クリックして)コピー")))
        			    			    	.append(ChatColor.WHITE+" です。")
        			    			    	.append(ChatColor.LIGHT_PURPLE+"\n\n認証コードの再生成").bold(true).underlined(true)
        			    			    	.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("(クリックして)認証コードを再生成します。")))
        			    			    	.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/fmcp retry"));
        						// BaseComponent[]に変換
        						BaseComponent[] messageComponents = component.create();
        						
        						player.sendMessage(messageComponents);
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
    			}
    			else
    			{
    				// MySQLサーバーにプレイヤー情報が登録されてなかった場合
    				this.plugin.getLogger().info(ChatColor.RED+"あなたのプレイヤー情報がデータベースに登録されていません。");
    				player.sendMessage(new TextComponent(ChatColor.RED+player.getName().toString()+"のプレイヤー情報がデータベースに登録されていません。"));
    			}
            }
            catch (SQLException | ClassNotFoundException e)
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