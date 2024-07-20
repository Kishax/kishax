package bungee;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.ucchyocean.lc3.LunaChatAPI;
import com.github.ucchyocean.lc3.LunaChatBungee;
import com.github.ucchyocean.lc3.japanize.IMEConverter;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import common.DiscordWebhook;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
/*
import my.shaded.ucchyocean.lc3.LunaChatBungee;
import my.shaded.ucchyocean.lc3.japanize.IMEConverter;
import my.shaded.ucchyocean.lc3.LunaChatAPI;
*/

public class EventListener implements Listener
{
	public Main plugin;
	public SocketSwitch socket;
	public LunaChatAPI lunaChatAPI;
	public LunaChatBungee lunaChatBungee;
	
	public Connection conn = null;
	public ResultSet yuyu = null, yu = null, logs = null, rs = null, bj_logs = null;
	public ResultSet[] resultsets = {yuyu, yu, logs, rs, bj_logs};
	public PreparedStatement ps = null;
	
	public EventListener(Main plugin,SocketSwitch socket)
	{
		this.plugin = plugin;
		this.socket = socket;
		this.lunaChatBungee = LunaChatBungee.getInstance();
		this.lunaChatAPI = lunaChatBungee.getLunaChatAPI();
	}
	
	@net.md_5.bungee.event.EventHandler
	public void onChat(ChatEvent e)
	{
		//my.shaded.ucchyocean.lc3.LunaChatBungee lunaChatBungee = my.shaded.ucchyocean.lc3.LunaChatBungee.getInstance();
		//my.shaded.ucchyocean.lc3.LunaChatAPI lunaChatAPI = lunaChatBungee.getLunaChatAPI();
		
		if (e.isCommand() || e.isCancelled()) return;
	    if (Config.getConfig().getString("Discord.Webhook_URL", "").isEmpty()) return;

	    ProxiedPlayer player = (ProxiedPlayer) e.getSender();
	    String originalMessage = e.getMessage();
	    
	    ProxyServer.getInstance().getScheduler().runAsync(plugin, () ->
	    {
	    	try
			{
	    		// 正規表現パターンを定義（URLを見つけるための正規表現）
			    String urlRegex = "https?://\\S+";
			    Pattern pattern = Pattern.compile(urlRegex);
			    Matcher matcher = pattern.matcher(originalMessage);

			    // URLリストを作成
			    List<String> urls = new ArrayList<>();
			    List<String> textParts = new ArrayList<>();

			    int lastMatchEnd = 0;
			    boolean isUrl = false;
			    String mixtext = "";

			    // マッチするものをリストに追加
			    while (matcher.find()) {
			        // URLが含まれていたら
			        isUrl = true;

			        // マッチしたURLをリストに追加
			        urls.add(matcher.group());

			        // URLの前のテキスト部分をリストに追加
			        textParts.add(originalMessage.substring(lastMatchEnd, matcher.start()));
			        lastMatchEnd = matcher.end();
			    }

			    // URLが含まれてなかったら
			    if (!isUrl) {
			        String kanaMessage = this.lunaChatAPI.japanize(originalMessage, null);
			        String kanjiMessage = IMEConverter.convByGoogleIME(kanaMessage);
			        sendChatToDiscord(player, kanjiMessage);
			        return;
			    }

			    // 最後のURLの後のテキスト部分を追加
			    if (lastMatchEnd < originalMessage.length()) {
			        textParts.add(originalMessage.substring(lastMatchEnd));
			    }

			    // テキスト部分を結合
			    int textPartsSize = textParts.size();
			    int urlsSize = urls.size();

			    for (int i = 0; i < textPartsSize; i++) {
			        if (Objects.nonNull(textParts) && textPartsSize != 0) {
			            String text = textParts.get(i);
			            String kanaMessage = this.lunaChatAPI.japanize(text, null);
			            String kanjiMessage = IMEConverter.convByGoogleIME(kanaMessage);
			            mixtext += kanjiMessage;
			        }

			        if (i < urlsSize) {
			            String getUrl;
			            if (textParts.get(i).isEmpty()) {
			                // textがなかったら、先頭の改行は無くす(=URLのみ)
			                getUrl = urls.get(i);
			            } else if (i != textPartsSize - 1) {
			                getUrl = "\\n" + urls.get(i) + "\\n";
			            } else {
			                getUrl = "\\n" + urls.get(i);
			            }

			            mixtext += getUrl;
			        }
			    }

			    // 完全なメッセージを送信
			    sendChatToDiscord(player, mixtext);
			}
			catch (Exception ex) {
	            ex.printStackTrace();
			}
	    });
	}

	
	public void sendChatToDiscord(ProxiedPlayer player,String message)
	{
		DiscordWebhook webhook = new DiscordWebhook(Config.getConfig().getString("Discord.Webhook_URL"));
        webhook.setUsername(player.getName().toString());
        webhook.setAvatarUrl("https://minotar.net/avatar/"+player.getUniqueId().toString());
	    webhook.setContent(message);
	    try
	    {
	    	webhook.execute();
	    }
	    catch (java.io.IOException e1)
	    {
	    	this.plugin.getLogger().severe(e1.getStackTrace().toString());
	    }
	}
	
	@net.md_5.bungee.event.EventHandler
	public void onServerSwitch(ServerSwitchEvent e)
	{
		ProxiedPlayer player = e.getPlayer();
		ProxyServer.getInstance().getScheduler().runAsync(plugin, () ->
		{
			try
			{
				conn = Database.getConnection();
				if(Objects.isNull(conn))
				{
					player_disconnect(false,player,ChatColor.BLUE+"Database Server is closed now!!");
				}
				
				String sql = "SELECT * FROM minecraft WHERE uuid=? ORDER BY id DESC LIMIT 1;";
	            ps = conn.prepareStatement(sql);
	            ps.setString(1, player.getUniqueId().toString());
	            yuyu = ps.executeQuery();
	            
	            sql = "SELECT * FROM minecraft WHERE name=? ORDER BY id DESC LIMIT 1;";
	            ps = conn.prepareStatement(sql);
	            ps.setString(1, player.getName());
	            yu = ps.executeQuery();
	            
				//一番最初に登録された名前と一致したら
	            if(yuyu.next())// 結果セットに少なくとも1行が存在するかどうかをチェック
	            {
	            	sql = "UPDATE minecraft SET server=? WHERE uuid=?;";
	            	ps = conn.prepareStatement(sql);
	            	ps.setString(1, Config.getConfig().getString("Servers.Hub"));
	            	ps.setString(2, player.getUniqueId().toString());
	            	ps.executeUpdate();
	            	
	            	if(yuyu.getBoolean("ban"))
	            	{
	            		player_disconnect(true,player,ChatColor.RED+"You are banned from this server.");
	    				return;
	            	}
	            	else
	            	{
	            		if(player.getName().equals(yuyu.getString("name")))
	            		{
	            			if(!Config.getConfig().getString("Discord.Webhook_URL","").isEmpty())
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
	        	    				
	        	    				if(beforejoin_sa_minute>=Config.getConfig().getInt("Interval.Login",0)) discord_join_notify(player,Color.GREEN,player.getName()+"が"+Config.getConfig().getString("Servers.Hub","")+"サーバーに参加したぜよ！");
	            				}
	            			}
	            			
	            			//　add log
	            			sql = "INSERT INTO mine_log (name,uuid,server,`join`) VALUES (?,?,?,?);";
	            			ps = conn.prepareStatement(sql);
	            			ps.setString(1, player.getName().toString());
	            			ps.setString(2, player.getUniqueId().toString());
	            			ps.setString(3, player.getServer().getInfo().getName());
	            			ps.setBoolean(4, true);
	            			ps.executeUpdate();
	            		}
	            		else
	            		{
	            			// 一番最初に登録した名前と一致しなかったら
	            			// MOJANG-APIからとってきた名前でレコードを更新させる
	            			String current_name = getPlayerNameFromUUID(player.getUniqueId());
	            			if(Objects.isNull(current_name) || !(current_name.equals(player.getName())))
	            			{
	            				player_disconnect(true,player,ChatColor.RED+"You are banned from this server.");
	            				return;
	            			}
	            			
	            			sql = "UPDATE minecraft SET name=?,server=?,old_name=? WHERE uuid=?;";
	            			ps = conn.prepareStatement(sql);
	            			ps.setString(1, current_name);
	            			ps.setString(2, player.getServer().getInfo().getName());
	            			ps.setString(3, yuyu.getString("name"));
	            			ps.setString(4, player.getUniqueId().toString());
	            			ps.executeUpdate();
	            			
	            			player.sendMessage(new TextComponent(ChatColor.GREEN+"MCIDの変更が検出されたため、データベースを更新しました。"));
	            			
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
	            			discord_join_notify(player,Color.GREEN,player.getName()+"が"+Config.getConfig().getString("Servers.Hub","")+"サーバーに参加したぜよ！");
	            		}
	            	}
	            }
	            else
	            {
	            	// DBにデータがなかったら (初参加)
	            	// MojangAPIによるUUID-MCIDチェックも行う
	            	// データベースに同一の名前がないか確認
	            	
	            	String current_name = getPlayerNameFromUUID(player.getUniqueId());
	    			if(Objects.isNull(current_name) || !(current_name.equals(player.getName())) || yu.next())
	    			{
	    				sql="INSERT INTO minecraft (name,uuid,server,ban) VALUES (?,?,?,?);";
	        			ps = conn.prepareStatement(sql);
	        			ps.setString(1, player.getName());
	        			ps.setString(2, player.getUniqueId().toString());
	        			ps.setString(3, player.getServer().getInfo().getName());
	        			ps.setBoolean(4, true);
	        			ps.executeUpdate();
	        			
	    				player_disconnect(true,player,ChatColor.RED+"You are banned from this server.");
	    				return;
	    			}
	    			
	    			sql="INSERT INTO minecraft (name,uuid,server) VALUES (?,?,?);";
	    			ps = conn.prepareStatement(sql);
	    			ps.setString(1, player.getName());
	    			ps.setString(2, player.getUniqueId().toString());
	    			ps.setString(3, player.getServer().getInfo().getName());
	    			ps.executeUpdate();
	    			
	    			discord_join_notify(player,Color.orange,player.getName()+"が"+player.getServer().getInfo().getName()+"サーバーに初参加です！");
	    			
	    			// Discord絵文字を追加する
	    			String pythonScriptPath = Config.getConfig().getString("Discord.Emoji_Add_Path");
	            	// ProcessBuilderを作成
	            	ProcessBuilder pb = new ProcessBuilder
	            			(
	            					"python",
	            					pythonScriptPath
	            			);
	            	pb.start();
	            }
	            
				// サーバー移動通知
				ServerInfo serverInfo = player.getServer().getInfo();
				if (serverInfo != null)
				{
					this.plugin.getLogger().info("Player connected to server: " + serverInfo.getName());
					if(serverInfo.getName().equalsIgnoreCase(Config.getConfig().getString("hub")))
					{
						this.plugin.broadcastMessage(ChatColor.AQUA+player.getName()+"が"+Config.getConfig().getString("Servers.Hub")+"サーバーにやってきました！",serverInfo.getName());
					}
					else
					{
						this.plugin.broadcastMessage(ChatColor.AQUA+"サーバー移動通知: "+player.getName()+" -> "+serverInfo.getName(), serverInfo.getName());
					}
					
					
					player.sendMessage(new TextComponent(ChatColor.YELLOW+player.getName()+"が"+serverInfo.getName()+"サーバーに参加しました。"));
					
					if(serverInfo.getName().equals("Latest"))
					{
						ComponentBuilder component =
			    			    new ComponentBuilder("BLUE MAP").underlined(false).bold(true).color(ChatColor.BLUE)
			    			    	.append("\nhttps://keypforev.ddns.net/bluemap/")
			    			    	.underlined(true).bold(false).color(ChatColor.GRAY)
			    			    	.event(new ClickEvent(ClickEvent.Action.OPEN_URL,"https://keypforev.ddns.net/bluemap/"))
			    			    	.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("(クリックして)地図を見る")));
						// BaseComponent[]に変換
						BaseComponent[] messageComponents = component.create();
						
						player.sendMessage(messageComponents);
					}
				}
				
				PlayerList.updatePlayers(); // プレイヤーリストをアップデート
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

	public void discord_join_notify(ProxiedPlayer player,Color color,String msg)
	{
		DiscordWebhook webhook = new DiscordWebhook(Config.getConfig().getString("Discord.Webhook_URL"));
		webhook.setUsername(player.getName().toString());
        webhook.setAvatarUrl("https://minotar.net/avatar/"+player.getUniqueId().toString());
	    webhook.addEmbed(new DiscordWebhook.EmbedObject().setColor(color).setDescription(msg));
	    try
		{
		    webhook.execute();
		}    		    
		catch (java.io.IOException e1)
		{
			this.plugin.getLogger().severe(e1.getStackTrace().toString());
		}
	}
	
	@SuppressWarnings("deprecation")
	public void player_disconnect(Boolean bool,ProxiedPlayer player,String msg)
	{
		player.disconnect(msg);
		
		if(!(bool))
		{
			return;
		}
		
		try
		{
			conn = Database.getConnection();
			String sql="UPDATE minecraft SET ban=? WHERE uuid=?;";
			ps = conn.prepareStatement(sql);
			ps.setBoolean(1, true);
			ps.setString(2, player.getUniqueId().toString());
			ps.executeUpdate();
			
			DiscordWebhook webhook = new DiscordWebhook(Config.getConfig().getString("Discord.Webhook_URL"));
	        webhook.setUsername("サーバー");
	        webhook.setAvatarUrl("https://www.illust-box.jp/db_img/sozai/00021/213610/watermark.jpg");
		    webhook.addEmbed(new DiscordWebhook.EmbedObject().setColor(Color.RED).setDescription("侵入者が現れました。"));
		    webhook.execute();
		}
		catch (SQLException | IOException | ClassNotFoundException e)
		{
			this.plugin.getLogger().severe(e.getStackTrace().toString());
		}
		finally
		{
			Database.close_resorce(null, conn, ps);
		}
	}
	
    @net.md_5.bungee.event.EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent e)
    {
    	ProxiedPlayer player = e.getPlayer();
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
        		ps.setString(1, player.getName().toString());
        		ps.setString(2, player.getUniqueId().toString());
        		ps.setString(3, player.getServer().getInfo().getName());
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
            	this.plugin.getLogger().severe("GETリクエストに失敗しました。HTTPエラーコード: " + response.statusCode());
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