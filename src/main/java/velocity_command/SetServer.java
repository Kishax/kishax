package velocity_command;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Random;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import velocity.Config;
import velocity.Database;
import velocity.DatabaseInterface;
import velocity.Main;

public class SetServer
{
	private final Main plugin;
	private final ProxyServer server;
	private final Config config;
	private final Logger logger;
	private final DatabaseInterface db;
	
	public Connection conn = null;
	public ResultSet minecrafts = null, mine_status = null, issuperadmin = null, issubadmin = null;
	public ResultSet[] resultsets = {minecrafts,mine_status,issuperadmin,issubadmin};
	public PreparedStatement ps = null;
	
	@Inject
	public SetServer(Main plugin,ProxyServer server, Logger logger, Config config, DatabaseInterface db)
	{
		this.plugin = plugin;
		this.server = server;
		this.logger = logger;
		this.config = config;
		this.db = db;
	}
	
	public void execute(CommandSource source,String[] args)
	{
		if (source instanceof Player)
		{
            // プレイヤーがコマンドを実行した場合の処理
			Player player = (Player) source;
        
            if(args.length == 1 || Objects.isNull(args[1]) || args[1].isEmpty())
            {
            	player.sendMessage(Component.text(NamedTextColor.RED+"サーバー名を入力してください。"));
            	return;
            }
            
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
            	player.sendMessage(Component.text(NamedTextColor.RED+"サーバー名が違います。"));
            	logger.info(NamedTextColor.RED+"サーバー名が違います。");
            	return;
            }

            try
            {
            	conn = db.getConnection();
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
        				if(mine_status.getBoolean(args[1]))
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
        		    			
        		    			TextComponent component = Component.text()
        		    						.append(Component.text(args[1]+"サーバーは現在").color(NamedTextColor.WHITE))
        			    			    	.append(Component.text("オンライン").color(NamedTextColor.AQUA))
        			    			    	.append(Component.text("です。\nサーバーに入りますか？\n").color(NamedTextColor.WHITE))
        			    			    	.append(Component.text("YES")
        			    			    			.color(NamedTextColor.GOLD)
        			    			    			.clickEvent(ClickEvent.runCommand("/fmcp stp "+args[1]))
        			                                .hoverEvent(HoverEvent.showText(Component.text("(クリックして)"+args[1]+"サーバーに入ります。"))))
        			    			    	.append(Component.text(" or ").color(NamedTextColor.GOLD))
        			    			    	.append(Component.text("NO").color(NamedTextColor.GOLD)
        			    			    			.clickEvent(ClickEvent.runCommand("/fmcp cancel"))
        			                                .hoverEvent(HoverEvent.showText(Component.text("(クリックして)キャンセルします。"))))
        			    			    	.build();
        						
        						player.sendMessage(component);
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
        		    			
        		    			TextComponent component = Component.text()
        			    			    	.append(Component.text(args[1]+"サーバーは現在").color(NamedTextColor.WHITE))
        			    			    	.append(Component.text("オンライン").color(NamedTextColor.AQUA))
        			    			    	.append(Component.text("です。\nサーバーに参加するには、FMCアカウントとあなたのMinecraftでのUUIDをリンクさせる必要があります。以下、").color(NamedTextColor.WHITE))
        			    			    	.append(Component.text("UUID認証").color(NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD, TextDecoration.UNDERLINED))
        			    			    	.append(Component.text("より、手続きを進めてください。").color(NamedTextColor.WHITE))
        			    			    	.append(Component.text("\nUUID認証\n\n").color(NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD, TextDecoration.UNDERLINED))
        			    			    	.append(Component.text("https://keypforev.ddns.net/minecraft/uuid_check2.php").color(NamedTextColor.GRAY).decorate(TextDecoration.UNDERLINED))
        			    			    		.clickEvent(ClickEvent.openUrl("https://keypforev.ddns.net/minecraft/uuid_check2.php?n="+minecrafts.getInt("id")))
        			    			    	.append(Component.text("\n\n認証コードは ").color(NamedTextColor.WHITE))
        			    			    	.append(Component.text(ranumstr).color(NamedTextColor.BLUE)
        			    			    		.clickEvent(ClickEvent.copyToClipboard(ranumstr))
        			    			    		.hoverEvent(HoverEvent.showText(Component.text("(クリックして)コピー"))))
        			    			    	.append(Component.text(" です。").color(NamedTextColor.WHITE)
        			    			    	.append(Component.text("\n\n認証コードの再生成").color(NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD, TextDecoration.UNDERLINED))
	        			    			    	.clickEvent(ClickEvent.runCommand("/fmcp retry"))
	        	                                .hoverEvent(HoverEvent.showText(Component.text("(クリックして)認証コードを再生成します。"))))
        			    			    	.build();
        						player.sendMessage(component);
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
        						for (RegisteredServer server : server.getAllServers())
        						{
        							if(mine_status.getBoolean(server.getServerInfo().getName()))
        							{
        								sum_memory = sum_memory + config.getInt("Servers."+server.getServerInfo().getName()+".Memory",0);
        							}
        						}
        						// 起動・起動リクエストしたいサーバーのメモリも足す
        						sum_memory = sum_memory + config.getInt("Servers."+args[1]+".Memory",0);
        						
        						// BungeeCordのメモリも足す
        						sum_memory = sum_memory + config.getInt("Servers.BungeeCord.Memory",0);
        								
    							if(!(sum_memory<=config.getInt("Servers.Memory_Limit",0)))
    							{
    								TextComponent component = Component.text()
            			    			    	.append(Component.text(args[1]+"サーバーは現在").color(NamedTextColor.WHITE))
            			    			    	.append(Component.text("オフライン").color(NamedTextColor.BLUE))
            			    			    	.append(Component.text("です。").color(NamedTextColor.WHITE))
            			    			    	.append(Component.text("\nメモリ超過のため、サーバーを起動できません。("+sum_memory+"GB/"+config.getInt("Servers.Memory_Limit",0)+"GB)").color(NamedTextColor.RED))
            			    			    	.build();
            						
            						player.sendMessage(component);
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
            		    			
            		    			TextComponent component = Component.text()
            			    			    	.append(Component.text(args[1]+"サーバーは現在").color(NamedTextColor.WHITE))
            			    			    	.append(Component.text("オフライン").color(NamedTextColor.BLUE))
            			    			    	.append(Component.text("です。\nサーバーを起動しますか？").color(NamedTextColor.WHITE))
            			    			    	.append(Component.text("\nYES").color(NamedTextColor.GOLD))
	            			    			    	.clickEvent(ClickEvent.runCommand("/fmcp start "+args[1]))
		        	                                .hoverEvent(HoverEvent.showText(Component.text("(クリックして)"+args[1]+"サーバーを起動します。")))
            			    			    	.append(Component.text(" or ").color(NamedTextColor.GOLD))
            			    			    	.append(Component.text("NO").color(NamedTextColor.GOLD)
	            			    			    	.clickEvent(ClickEvent.runCommand("/fmcp cancel"))
		        	                                .hoverEvent(HoverEvent.showText(Component.text("(クリックして)キャンセルします。"))))
		        	                            .build();
            						
            						player.sendMessage(component);
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
            		    			
            		    			TextComponent component = Component.text()
            			    			    	.append(Component.text(args[1]+"サーバーは現在").color(NamedTextColor.WHITE))
            			    			    	.append(Component.text("オフライン").color(NamedTextColor.BLUE))
            			    			    	.append(Component.text("です。\n管理者に、サーバー起動のリクエストを送信できます。\n結果は3分以内に返ってきます。\n送信しますか？").color(NamedTextColor.WHITE))
            			    			    	.append(Component.text("\nYES").color(NamedTextColor.GOLD))
            			    			    		.clickEvent(ClickEvent.runCommand("/fmcp req "+args[1]))
            			    			    		.hoverEvent(HoverEvent.showText(Component.text("(クリックして)"+args[1]+"サーバー起動リクエストを送信する。")))
            			    			    	.append(Component.text(" or ").color(NamedTextColor.GOLD))
            			    			    	.append(Component.text("NO").color(NamedTextColor.GOLD))
            			    			    		.clickEvent(ClickEvent.runCommand("/fmcp cancel"))
            			    			    		.hoverEvent(HoverEvent.showText(Component.text("(クリックして)キャンセルします。")))
            			    			    	.build();
            						player.sendMessage(component);
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
        		    			
        		    			TextComponent component = Component.text()
    			    			    	.append(Component.text(args[1]+"サーバーは現在").color(NamedTextColor.WHITE))
    			    			    	.append(Component.text("オフライン").color(NamedTextColor.BLUE))
    			    			    	.append(Component.text("です。\nサーバーを起動して、参加するには、FMCアカウントとあなたのMinecraftでのUUIDをリンクさせる必要があります。以下、").color(NamedTextColor.WHITE))
    			    			    	.append(Component.text("UUID認証").color(NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD, TextDecoration.UNDERLINED))
    			    			    	.append(Component.text("より、手続きを進めてください。").color(NamedTextColor.WHITE))
    			    			    	.append(Component.text("\nUUID認証\n\n").color(NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD, TextDecoration.UNDERLINED))
    			    			    	.append(Component.text("https://keypforev.ddns.net/minecraft/uuid_check2.php").color(NamedTextColor.GRAY).decorate(TextDecoration.UNDERLINED))
    			    			    		.clickEvent(ClickEvent.openUrl("https://keypforev.ddns.net/minecraft/uuid_check2.php?n="+minecrafts.getInt("id")))
    			    			    	.append(Component.text("\n\n認証コードは ").color(NamedTextColor.WHITE))
    			    			    	.append(Component.text(ranumstr).color(NamedTextColor.BLUE)
    			    			    		.clickEvent(ClickEvent.copyToClipboard(ranumstr))
    			    			    		.hoverEvent(HoverEvent.showText(Component.text("(クリックして)コピー"))))
    			    			    	.append(Component.text(" です。").color(NamedTextColor.WHITE)
    			    			    	.append(Component.text("\n\n認証コードの再生成").color(NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD, TextDecoration.UNDERLINED))
        			    			    	.clickEvent(ClickEvent.runCommand("/fmcp retry"))
        	                                .hoverEvent(HoverEvent.showText(Component.text("(クリックして)認証コードを再生成します。"))))
    			    			    	.build();
        						
        						player.sendMessage(component);
        					}
        				}
        			}
        			else
        			{
        				// MySQLサーバーにサーバーが登録されてなかった場合
        				logger.info(NamedTextColor.RED+"このサーバーは、データベースに登録されていません。");
        				player.sendMessage(Component.text(NamedTextColor.RED+"このサーバーは、データベースに登録されていません。"));
        				return;
        			}
    			}
    			else
    			{
    				// MySQLサーバーにプレイヤー情報が登録されてなかった場合
    				logger.info(NamedTextColor.RED+"あなたのプレイヤー情報がデータベースに登録されていません。");
    				player.sendMessage(Component.text(NamedTextColor.RED+player.getUsername()+"のプレイヤー情報がデータベースに登録されていません。"));
    			}
            }
            catch (SQLException | ClassNotFoundException e)
            {
            	e.printStackTrace();
            }
            finally
            {
            	db.close_resorce(resultsets, conn, ps);
            }
        }
		else
		{
			source.sendMessage(Component.text(NamedTextColor.RED+"このコマンドはプレイヤーのみが実行できます。"));
			return;
		}
        return;
	}
}