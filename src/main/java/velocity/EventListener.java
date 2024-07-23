package velocity;

import java.awt.Color;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.ServerInfo;

import common.DiscordWebhook;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatColor;


public class EventListener
{
	public final Main plugin;
	private final ProxyServer server;
	
	public Connection conn = null;
	public ResultSet yuyu = null, yu = null, logs = null, rs = null, bj_logs = null;
	public ResultSet[] resultsets = {yuyu, yu, logs, rs, bj_logs};
	public PreparedStatement ps = null;
	
	public EventListener()
	{
		this.plugin = Main.getInstance();
		this.server = this.plugin.getServer();
	}
	
	@Subscribe
	public void onChat(PlayerChatEvent e)
	{
		if (e.getMessage().startsWith("/")) return;
	    if (((String) Config.getConfig().getOrDefault("Discord.Webhook_URL", "")).isEmpty()) return;

	    Player player = e.getPlayer();
	    String originalMessage = e.getMessage();
	    
	    this.server.getScheduler().buildTask(plugin, () ->
	    {
	    	// Chatをローマ字→かな→漢字にできる"何か"を探すまで待機
	    });
	}
	
	@Subscribe
	public void onServerSwitch(ServerConnectedEvent e)
	{
		Player player = e.getPlayer();
		ServerConnection serverConnection = (ServerConnection) e.getServer();
        ServerInfo serverInfo = serverConnection.getServerInfo();
        if(Objects.isNull(serverInfo))
        {
        	player_disconnect
        	(
        		false,
        		player,
        		Component.text(ChatColor.RED+"コネクションエラー: 接続サーバー名が不明です。")
        	);
        	this.plugin.getLogger().error("コネクションエラー: 接続サーバー名が不明です。");
        	return;
        }
        
		this.server.getScheduler().buildTask(plugin, () ->
		{
			try
			{
				conn = Database.getConnection();
				if(Objects.isNull(conn))
				{
					player_disconnect
					(
						false,
						player,
						Component.text(NamedTextColor.BLUE+"Database Server is closed now!!")
					);
				}
				
				String sql = "SELECT * FROM minecraft WHERE uuid=? ORDER BY id DESC LIMIT 1;";
	            ps = conn.prepareStatement(sql);
	            ps.setString(1, player.getUniqueId().toString());
	            yuyu = ps.executeQuery();
	            
	            sql = "SELECT * FROM minecraft WHERE name=? ORDER BY id DESC LIMIT 1;";
	            ps = conn.prepareStatement(sql);
	            ps.setString(1, player.getUsername());
	            yu = ps.executeQuery();
	            
				//一番最初に登録された名前と一致したら
	            if(yuyu.next())// 結果セットに少なくとも1行が存在するかどうかをチェック
	            {
	            	sql = "UPDATE minecraft SET server=? WHERE uuid=?;";
	            	ps = conn.prepareStatement(sql);
	            	ps.setString(1, (String) Config.getConfig().get("Servers.Hub"));
	            	ps.setString(2, player.getUniqueId().toString());
	            	ps.executeUpdate();
	            	
	            	if(yuyu.getBoolean("ban"))
	            	{
	            		player_disconnect
	            		(
	            			true,
	            			player,
	            			Component.text(NamedTextColor.RED+"You are banned from this server.")
	            		);
	    				return;
	            	}
	            	else
	            	{
	            		if(player.getUsername().equals(yuyu.getString("name")))
	            		{
	            			if(!((String) Config.getConfig().getOrDefault("Discord.Webhook_URL","")).isEmpty())
	            			{
	            				// 2時間経ってたら
	            				sql = "SELECT * FROM mine_log WHERE uuid=? AND `join`=? ORDER BY id DESC LIMIT 1;";
	            				ps = conn.prepareStatement(sql);
	            				ps.setString(1, player.getUniqueId().toString());
	            				ps.setBoolean(2, true);
	            				logs = ps.executeQuery();
	            				
	            				if(logs.next())
	            				{
	        	    				long now_timestamp = Instant.now().getEpochSecond();
	        	    				
	            					// TIMESTAMP型のカラムを取得
	            	                Timestamp beforejoin_timeget = logs.getTimestamp("time");

	            	                // Unixタイムスタンプに変換
	            	                long beforejoin_timestamp = beforejoin_timeget.getTime() / 1000L;
	            	                
	        	    				long beforejoin_sa = now_timestamp-beforejoin_timestamp;
	        	    				long beforejoin_sa_minute = beforejoin_sa/60;
	        	    				
	        	    				if(beforejoin_sa_minute>=(int) Config.getConfig().getOrDefault("Interval.Login",0)) discord_join_notify(player,Color.GREEN,player.getUsername()+"が"+(String) Config.getConfig().getOrDefault("Servers.Hub","")+"サーバーに参加したぜよ！");
	            				}
	            			}
	            			
	            			//　add log
	            			sql = "INSERT INTO mine_log (name,uuid,server,`join`) VALUES (?,?,?,?);";
	            			ps = conn.prepareStatement(sql);
	            			ps.setString(1, player.getUsername());
	            			ps.setString(2, player.getUniqueId().toString());
	            			ps.setString(3, serverInfo.getName());
	            			ps.setBoolean(4, true);
	            			ps.executeUpdate();
	            		}
	            		else
	            		{
	            			// 一番最初に登録した名前と一致しなかったら
	            			// MOJANG-APIからとってきた名前でレコードを更新させる
	            			String current_name = getPlayerNameFromUUID(player.getUniqueId());
	            			if(Objects.isNull(current_name) || !(current_name.equals(player.getUsername())))
	            			{
	            				player_disconnect
	            				(
	            					true,
	            					player,
	            					Component.text(NamedTextColor.RED+"You are banned from this server.")
	            				);
	            				return;
	            			}
	            			
	            			sql = "UPDATE minecraft SET name=?,server=?,old_name=? WHERE uuid=?;";
	            			ps = conn.prepareStatement(sql);
	            			ps.setString(1, current_name);
	            			ps.setString(2, serverInfo.getName());
	            			ps.setString(3, yuyu.getString("name"));
	            			ps.setString(4, player.getUniqueId().toString());
	            			ps.executeUpdate();
	            			
	            			player.sendMessage(Component.text(NamedTextColor.GREEN+"MCIDの変更が検出されたため、データベースを更新しました。"));
	            			
	            			// 過去の名前を解放するため、過去の名前のレコードがほかにもあったらそれをinvalid_loginへ移動
	            			sql="SELECT COUNT(*) FROM minecraft WHERE name=?;";
	            			ps = conn.prepareStatement(sql);
	            			ps.setString(1, yuyu.getString("name"));
	            			rs = ps.executeQuery();
	            			if(rs.next())
	            			{
	            				int count = rs.getInt(1);
	            				if (count>=1)
	            				{
	            					sql="INSERT INTO minevalid_login SELECT * FROM minecraft WHERE name=?;";
	            					ps = conn.prepareStatement(sql);
	            					ps.setString(1, yuyu.getString("name"));
	            					ps.executeUpdate();
	            					
	            					sql="DELETE from minecraft WHERE name=?;";
	            					ps.setString(1, yuyu.getString("name"));
	            					ps.executeUpdate();
	            				}
	            			}
	            			discord_join_notify(player,Color.GREEN,player.getUsername()+"が"+(String) Config.getConfig().getOrDefault("Servers.Hub","")+"サーバーに参加したぜよ！");
	            		}
	            	}
	            }
	            else
	            {
	            	// DBにデータがなかったら (初参加)
	            	// MojangAPIによるUUID-MCIDチェックも行う
	            	// データベースに同一の名前がないか確認
	            	
	            	String current_name = getPlayerNameFromUUID(player.getUniqueId());
	    			if(Objects.isNull(current_name) || !(current_name.equals(player.getUsername())) || yu.next())
	    			{
	    				sql="INSERT INTO minecraft (name,uuid,server,ban) VALUES (?,?,?,?);";
	        			ps = conn.prepareStatement(sql);
	        			ps.setString(1, player.getUsername());
	        			ps.setString(2, player.getUniqueId().toString());
	        			ps.setString(3, serverInfo.getName());
	        			ps.setBoolean(4, true);
	        			ps.executeUpdate();
	        			
	    				player_disconnect
	    				(
	    					true,
	    					player,
	    					Component.text(NamedTextColor.RED+"You are banned from this server.")
	    				);
	    				return;
	    			}
	    			
	    			sql="INSERT INTO minecraft (name,uuid,server) VALUES (?,?,?);";
	    			ps = conn.prepareStatement(sql);
	    			ps.setString(1, player.getUsername());
	    			ps.setString(2, player.getUniqueId().toString());
	    			ps.setString(3, serverInfo.getName());
	    			ps.executeUpdate();
	    			
	    			discord_join_notify
	    			(
	    				player,
	    				Color.orange,
	    				player.getUsername()+"が"+serverInfo.getName()+"サーバーに初参加です！"
	    			);
	    			
	    			// Discord絵文字を追加する
	    			String pythonScriptPath = (String) Config.getConfig().get("Discord.Emoji_Add_Path");
	            	// ProcessBuilderを作成
	            	ProcessBuilder pb = new ProcessBuilder
	            			(
	            					"python",
	            					pythonScriptPath
	            			);
	            	pb.start();
	            }
	            
	            
				// サーバー移動通知
				this.plugin.getLogger().info("Player connected to server: " + serverInfo.getName());
				if(serverInfo.getName().equalsIgnoreCase((String) Config.getConfig().get("hub")))
				{
					this.plugin.broadcastMessage(NamedTextColor.AQUA+player.getUsername()+"が"+(String) Config.getConfig().get("Servers.Hub")+"サーバーにやってきました！",serverInfo.getName());
				}
				else
				{
					this.plugin.broadcastMessage(NamedTextColor.AQUA+"サーバー移動通知: "+player.getUsername()+" -> "+serverInfo.getName(), serverInfo.getName());
				}
				
				
				player.sendMessage(Component.text(player.getUsername()+"が"+serverInfo.getName()+"サーバーに参加しました。").color(NamedTextColor.YELLOW));
				
				if(serverInfo.getName().equals("Latest"))
				{
					TextComponent component = Component.text()
		    			    	.append(Component.text("BLUE MAP").decorate(TextDecoration.BOLD).color(NamedTextColor.BLUE))
		    			    	.append(Component.text("\nhttps://keypforev.ddns.net/bluemap/").color(NamedTextColor.GRAY).decorate(TextDecoration.UNDERLINED)
	    			    			.clickEvent(ClickEvent.openUrl("https://keypforev.ddns.net/bluemap/"))
	                                .hoverEvent(HoverEvent.showText(Component.text("(クリックして)地図を見る"))))
		    			    	.build();
					player.sendMessage(component);
				}
				PlayerList.updatePlayers();
			}
			catch (SQLException | IOException | ClassNotFoundException e1)
			{
	            e1.printStackTrace();
	        }
			finally
			{
				Database.close_resorce(resultsets,conn,ps);
			}
		});
	}
	
	public void sendChatToDiscord(Player player,String message)
	{
		DiscordWebhook webhook = new DiscordWebhook((String) Config.getConfig().get("Discord.Webhook_URL"));
        webhook.setUsername(player.getUsername());
        webhook.setAvatarUrl("https://minotar.net/avatar/"+player.getUniqueId().toString());
	    webhook.setContent(message);
	    try
	    {
	    	webhook.execute();
	    }
	    catch (java.io.IOException e1)
	    {
	    	this.plugin.getLogger().error(e1.getStackTrace().toString());
	    }
	}
	
	public void discord_join_notify(Player player,Color color,String msg)
	{
		DiscordWebhook webhook = new DiscordWebhook((String) Config.getConfig().get("Discord.Webhook_URL"));
		webhook.setUsername(player.getUsername());
        webhook.setAvatarUrl("https://minotar.net/avatar/"+player.getUniqueId().toString());
	    webhook.addEmbed(new DiscordWebhook.EmbedObject().setColor(color).setDescription(msg));
	    try
		{
		    webhook.execute();
		}    		    
		catch (java.io.IOException e1)
		{
			this.plugin.getLogger().error(e1.getStackTrace().toString());
		}
	}
	
	public void player_disconnect(Boolean bool,Player player,TextComponent component)
	{
		player.disconnect(component);
		
		if(!(bool))	return;
		
		try
		{
			conn = Database.getConnection();
			String sql="UPDATE minecraft SET ban=? WHERE uuid=?;";
			ps = conn.prepareStatement(sql);
			ps.setBoolean(1, true);
			ps.setString(2, player.getUniqueId().toString());
			ps.executeUpdate();
			
			DiscordWebhook webhook = new DiscordWebhook((String) Config.getConfig().get("Discord.Webhook_URL"));
	        webhook.setUsername("サーバー");
	        webhook.setAvatarUrl("https://www.illust-box.jp/db_img/sozai/00021/213610/watermark.jpg");
		    webhook.addEmbed(new DiscordWebhook.EmbedObject().setColor(Color.RED).setDescription("侵入者が現れました。"));
		    webhook.execute();
		}
		catch (SQLException | IOException | ClassNotFoundException e)
		{
			this.plugin.getLogger().error(e.getStackTrace().toString());
		}
		finally
		{
			Database.close_resorce(null, conn, ps);
		}
	}
	
	@Subscribe
    public void onPlayerDisconnect(DisconnectEvent e)
    {
    	Player player = e.getPlayer();
    	try
    	{
    		conn = Database.getConnection();
    		
    		// calc playtime
    		String sql = "SELECT * FROM mine_log WHERE uuid=? AND `join`=? ORDER BY id DESC LIMIT 1;";
    		ps = conn.prepareStatement(sql);
    		ps.setString(1, player.getUniqueId().toString());
    		ps.setBoolean(2, true);
    		bj_logs = ps.executeQuery();
    		
    		if(bj_logs.next())
    		{
    			long now_timestamp = Instant.now().getEpochSecond();
                Timestamp bj_time = bj_logs.getTimestamp("time");
                long bj_timestamp = bj_time.getTime() / 1000L;
    			
    			long bj_sa = now_timestamp-bj_timestamp;
        		
    			int int_bj_sa = (int) bj_sa;
    					
        		// add log
        		sql = "INSERT INTO mine_log (name,uuid,server,quit,playtime) VALUES (?,?,?,?,?);";
        		ps = conn.prepareStatement(sql);
        		ps.setString(1, player.getUsername());
        		ps.setString(2, player.getUniqueId().toString());
        		ps.setString(3, player.getCurrentServer().toString());
        		ps.setBoolean(4, true);
        		ps.setInt(5, int_bj_sa);
        		ps.executeUpdate();
    		}
    	}
    	catch (SQLException | ClassNotFoundException e1)
		{
            e1.printStackTrace();
        }
    	finally
    	{
    		Database.close_resorce(resultsets, conn, ps);
    	}
    }
    
    public String getPlayerNameFromUUID(UUID uuid)
    {
        String uuidString = uuid.toString().replace("-", "");
        String urlString = "https://sessionserver.mojang.com/session/minecraft/profile/" + uuidString;
        
        try
        {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(urlString))
                    .header("User-Agent", "Mozilla/5.0")
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200)
            {
                // JSONレスポンスを解析
                Gson gson = new Gson();
                JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
                return jsonResponse.get("name").getAsString();
            }
            else
            {
            	this.plugin.getLogger().error("GETリクエストに失敗しました。HTTPエラーコード: " + response.statusCode());
                return null;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }
}