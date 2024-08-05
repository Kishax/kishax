package velocity;

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

import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;

import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import common.ColorUtil;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class EventListener
{
	public final Main plugin;
	private final ProxyServer server;
	private final Config config;
	private final Logger logger;
	private final DatabaseInterface db;
	private final BroadCast bc;
	private final ConsoleCommandSource console;
	private final RomaToKanji conv;
	private String chatserverName = "";
	private final PlayerList pl;
	private final PlayerDisconnect pd;
	private final RomajiConversion rc;
	private final DiscordListener discord;
	private final EmojiManager emoji;
	
	private WebhookMessageBuilder builder = null;
	public Connection conn = null;
	public ResultSet yuyu = null, yu = null, logs = null, rs = null, bj_logs = null, ismente = null;
	public ResultSet[] resultsets = {yuyu, yu, logs, rs, bj_logs, ismente};
	public PreparedStatement ps = null;
	private String avatarUrl = null;
	private String emojiId = null;
	private MessageEmbed joinEmbed = null;
	
	@Inject
	public EventListener
	(
		Main plugin, Logger logger, ProxyServer server,
		Config config, DatabaseInterface db, BroadCast bc,
		ConsoleCommandSource console, RomaToKanji conv, PlayerList pl,
		PlayerDisconnect pd, RomajiConversion rc, DiscordListener discord,
		EmojiManager emoji
	)
	{
		this.plugin = plugin;
		this.logger = logger;
		this.server = server;
		this.config = config;
		this.db = db;
		this.bc = bc;
		this.console = console;
		this.conv = conv;
		this.pl = pl;
		this.pd = pd;
		this.rc = rc;
		this.discord = discord;
		this.emoji = emoji;
	}
	
	@Subscribe
	public void onChat(PlayerChatEvent e)
	{
		if (e.getMessage().startsWith("/")) return;
	    if (config.getString("Discord.Webhook_URL", "").isEmpty()) return;

	    Player player = e.getPlayer();
	    String originalMessage = e.getMessage();
	    
	    
	    // プレイヤーの現在のサーバーを取得
        player.getCurrentServer().ifPresent(serverConnection ->
        {
            RegisteredServer server = serverConnection.getServer();
            chatserverName = server.getServerInfo().getName();
        });
        
        // マルチバイト文字の長さを取得
        int NameCount = player.getUsername().length();
        
        // スペースを生成
        StringBuilder space = new StringBuilder();
        for (int i = 0; i <= NameCount; i++)
        {
            space.append('\u0020');  // Unicodeのスペースを追加
        }
        
        server.getScheduler().buildTask(plugin, () ->
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
    		    while (matcher.find())
    		    {
    		        // URLが含まれていたら
    		        isUrl = true;

    		        // マッチしたURLをリストに追加
    		        urls.add(matcher.group());

    		        // URLの前のテキスト部分をリストに追加
    		        textParts.add(originalMessage.substring(lastMatchEnd, matcher.start()));
    		        lastMatchEnd = matcher.end();
    		    }

    		    Component component = Component.text(space+"(").color(NamedTextColor.GOLD);
    		    // URLが含まれてなかったら
    		    if (!isUrl)
    		    {
    		    	// 漢字の検出
    		        String kanjiPattern = "[\\u4E00-\\u9FFF]+";
    		        
    		        // ひらがなの検出
    		        String hiraganaPattern = "[\\u3040-\\u309F]+";
    		        
    		        // カタカナの検出
    		        String katakanaPattern = "[\\u30A0-\\u30FF]+";
    		        
    		        if
    		        (
    		        	!detectMatches(originalMessage, kanjiPattern) &&
    		        	!detectMatches(originalMessage, hiraganaPattern) &&
    		        	!detectMatches(originalMessage, katakanaPattern)
    		        )
    		        {
    		        	// 日本語でなかったら
    		        	if(config.getBoolean("Conv.Mode"))
    		        	{
    		        		// Map方式
    		        		String kanaMessage = conv.ConvRomaToKana(originalMessage);
            		        String kanjiMessage = conv.ConvRomaToKanji(kanaMessage);
            		        
            		        builder = new WebhookMessageBuilder();
        			        builder.setUsername(player.getUsername());
        			        builder.setAvatarUrl("https://minotar.net/avatar/"+player.getUniqueId().toString());
        			        builder.setContent(kanjiMessage);
        			        discord.sendWebhookMessage(builder);
        			        
            		        component = component.append(Component.text(kanjiMessage + ")").color(NamedTextColor.GOLD));
            		        bc.broadcastComponent(component, chatserverName, true);
    		        	}
    		        	else
    		        	{
    		        		// pde方式
    		        		String kanaMessage = rc.Romaji(originalMessage);
    		        		String kanjiMessage = conv.ConvRomaToKanji(kanaMessage);
    		        		
    		        		builder = new WebhookMessageBuilder();
        			        builder.setUsername(player.getUsername());
        			        builder.setAvatarUrl("https://minotar.net/avatar/"+player.getUniqueId().toString());
        			        builder.setContent(kanjiMessage);
        			        discord.sendWebhookMessage(builder);
        			        
            		        component = component.append(Component.text(kanjiMessage + ")").color(NamedTextColor.GOLD));
            		        bc.broadcastComponent(component, chatserverName, true);
    		        	}
        		        return;
    		        }
    		        else
    		        {
    		        	builder = new WebhookMessageBuilder();
    			        builder.setUsername(player.getUsername());
    			        builder.setAvatarUrl("https://minotar.net/avatar/"+player.getUniqueId().toString());
    			        builder.setContent(originalMessage);
    			        discord.sendWebhookMessage(builder);
    		        	return;
    		        }
    		    }

    		    // 最後のURLの後のテキスト部分を追加
    		    if (lastMatchEnd < originalMessage.length())
    		    {
    		        textParts.add(originalMessage.substring(lastMatchEnd));
    		    }

    		    // テキスト部分を結合
    		    int textPartsSize = textParts.size();
    		    int urlsSize = urls.size();
    		    //boolean isUrlLineBreak = false;
    		    
    		    for (int i = 0; i < textPartsSize; i++)
    		    {
    		        if (Objects.nonNull(textParts) && textPartsSize != 0)
    		        {
    		            String text = textParts.get(i);
    		            String kanaMessage = null;
    		            String kanjiMessage = null;
    		            if(config.getBoolean("Conv.Mode"))
    		        	{
    		        		// Map方式
    		            	kanaMessage = conv.ConvRomaToKana(text);
        		            kanjiMessage = conv.ConvRomaToKanji(kanaMessage);
    		        	}
    		            else
    		            {
    		            	// pde方式
    		            	kanaMessage = rc.Romaji(text);
        		            kanjiMessage = conv.ConvRomaToKanji(kanaMessage);
    		            }
    		            mixtext += kanjiMessage;
    		            component = component.append(Component.text(kanjiMessage).color(NamedTextColor.GOLD));
    		        }

    		        if (i < urlsSize)
    		        {
    		            String getUrl;
    		            String getUrl2;
    		            if (textParts.get(i).isEmpty())
    		            {
    		                // textがなかったら、先頭の改行は無くす(=URLのみ)
    		                getUrl = urls.get(i);
    		                getUrl2 = urls.get(i);
    		            }
    		            else if (i != textPartsSize - 1)
    		            {
    		                getUrl = "\n" + urls.get(i) + "\n";
    		                getUrl2 = "\n" + space + urls.get(i);
    		                
    		                //if(i = urlsSize)
    		                //isUrlLineBreak = true;
    		            }
    		            else
    		            {
    		                getUrl = "\n" + urls.get(i);
    		                getUrl2 = "\n" + space + urls.get(i);
    		            }
    		            mixtext += getUrl;
    		            component = component.append(Component.text(getUrl2).color(NamedTextColor.GRAY).clickEvent(ClickEvent.openUrl(urls.get(i))).hoverEvent(HoverEvent.showText(Component.text("リンク"+(i+1)))));
    		        }
    		    }
    		    
    		    component = component.append(Component.text(")").color(NamedTextColor.GOLD));
    		    bc.broadcastComponent(component, chatserverName, true);
    		    
    		    builder = new WebhookMessageBuilder();
		        builder.setUsername(player.getUsername());
		        builder.setAvatarUrl("https://minotar.net/avatar/"+player.getUniqueId().toString());
		        builder.setContent(mixtext);
		        discord.sendWebhookMessage(builder);
    		    return;
    		}
    		catch (Exception ex) {
    			// スタックトレースをログに出力
	            logger.error("An onChat error occurred: " + ex.getMessage());
	            for (StackTraceElement element : ex.getStackTrace()) 
	            {
	                logger.error(element.toString());
	            }
    		}
        }).schedule();
	}
	
	public boolean detectMatches(String input, String pattern)
	{
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(input);

        while (matcher.find())
        {
            //String found = matcher.group();
            //System.out.println(message + ": " + found);
        	return true;
        }
        return false;
    }
	
	@Subscribe
	public void onServerSwitch(ServerConnectedEvent e)
	{
		Player player = e.getPlayer();
		RegisteredServer serverConnection = e.getServer();
        ServerInfo serverInfo = serverConnection.getServerInfo();
        if(Objects.isNull(serverInfo))
        {
        	pd.playerDisconnect
        	(
        		false,
        		player,
        		Component.text("コネクションエラー: 接続サーバー名が不明です。").color(NamedTextColor.RED)
        	);
        	logger.error("コネクションエラー: 接続サーバー名が不明です。");
        	return;
        }
        
		server.getScheduler().buildTask(plugin, () ->
		{
			try
			{
				conn = db.getConnection();
				if(Objects.isNull(conn))
				{
					pd.playerDisconnect
					(
						false,
						player,
						Component.text("Database Server is closed now!!").color(NamedTextColor.BLUE)
					);
				}
				
				String sql = "SELECT online FROM mine_status WHERE name=?;";
				ps = conn.prepareStatement(sql);
				ps.setString(1, "Maintenance");
				ismente = ps.executeQuery();
				
				if(ismente.next())
				{
					if(ismente.getBoolean("online"))
					{
						if(!player.hasPermission("group.super-admin"))
						{
							pd.playerDisconnect
							(
								false,
								player,
								Component.text("現在メンテナンス中です。").color(NamedTextColor.BLUE)
							);
							return;
						}
						player.sendMessage(Component.text("スーパーアドミン認証...PASS\n\nALL CORRECT\n\nメンテナンスモードが有効です。").color(NamedTextColor.GREEN));
					}
				}
				
				sql = "SELECT * FROM minecraft WHERE uuid=? ORDER BY id DESC LIMIT 1;";
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
	            	ps.setString(1, config.getString("Servers.Hub"));
	            	ps.setString(2, player.getUniqueId().toString());
	            	ps.executeUpdate();
	            	
	            	if(yuyu.getBoolean("ban"))
	            	{
	            		pd.playerDisconnect
	            		(
	            			true,
	            			player,
	            			Component.text("You are banned from this server.").color(NamedTextColor.RED)
	            		);
	    				return;
	            	}
	            	else
	            	{
	            		if(player.getUsername().equals(yuyu.getString("name")))
	            		{
	            			if(!config.getString("Discord.Webhook_URL","").isEmpty())
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
	        	    				
	        	    				if(beforejoin_sa_minute>=config.getInt("Interval.Login",0))
	        	    				{
	        	    					avatarUrl = "https://minotar.net/avatar/"+player.getUniqueId().toString();
	        	    	    			emojiId = null;
	        	    	    			joinEmbed = null;
	        	    	    			// Discord絵文字を取得する
	        	    	    			// 追加される絵文字は参加通知、データベース保存で使うので、ここだけ同期処理にする
	        	    	    			synchronized (this)
	        	    	    			{
	        	    	    				emojiId = emoji.createOrgetEmojiId(player.getUsername(), avatarUrl);
	        	    	    				
	        	    				        if(Objects.nonNull(emojiId))
	        	    		    			{
	        	    				        	joinEmbed = discord.createEmbed
	    	        	    							(
	    	        	    								emoji.getEmojiString(player.getUsername(), emojiId)+
	    	        	    								player.getUsername()+"が"+config.getString("Servers.Hub","")+
	    	        	    								"サーバーに参加したぜよ！",
	    	        	    								ColorUtil.GREEN.getRGB()
	    	        	    							);
	    	        	    					discord.sendBotMessageAsync(joinEmbed);
	        	    		    			}
	        	    				        else
	        	    				        {
	        	    				        	joinEmbed = discord.createEmbed
	    	        	    							(
	    	        	    								player.getUsername()+"が"+config.getString("Servers.Hub","")+
	    	        	    								"サーバーに参加したぜよ！",
	    	        	    								ColorUtil.GREEN.getRGB()
	    	        	    							);
	    	        	    					discord.sendBotMessageAsync(joinEmbed);
	        	    				        }
	        	    	    			}
	        	    				}
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
	            				pd.playerDisconnect
	            				(
	            					true,
	            					player,
	            					Component.text("You are banned from this server.").color(NamedTextColor.RED)
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
	            			
	            			avatarUrl = "https://minotar.net/avatar/"+player.getUniqueId().toString();
	    	    			emojiId = null;
	    	    			joinEmbed = null;
	    	    			// Discord絵文字を取得する
	    	    			// 追加される絵文字は参加通知、データベース保存で使うので、ここだけ同期処理にする
	    	    			synchronized (this)
	    	    			{
	    	    				emojiId = emoji.createOrgetEmojiId(player.getUsername(), avatarUrl);
	    	    				
	    				        if(Objects.nonNull(emojiId))
	    		    			{
	    				        	joinEmbed = discord.createEmbed
        	    							(
        	    								emoji.getEmojiString(player.getUsername(), emojiId)+
        	    								player.getUsername()+"が"+config.getString("Servers.Hub","")+
        	    								"サーバーに参加したぜよ！",
        	    								ColorUtil.GREEN.getRGB()
        	    							);
        	    					discord.sendBotMessageAsync(joinEmbed);
	    		    			}
	    				        else
	    				        {
	    				        	joinEmbed = discord.createEmbed
        	    							(
        	    								player.getUsername()+"が"+config.getString("Servers.Hub","")+
        	    								"サーバーに参加したぜよ！",
        	    								ColorUtil.GREEN.getRGB()
        	    							);
        	    					discord.sendBotMessageAsync(joinEmbed);
	    				        }
	    	    			}
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
	        			
	        			pd.playerDisconnect
	    				(
	    					true,
	    					player,
	    					Component.text("You are banned from this server.").color(NamedTextColor.RED)
	    				);
	    				return;
	    			}
	    			
	    			avatarUrl = "https://minotar.net/avatar/"+player.getUniqueId().toString();
	    			emojiId = null;
	    			joinEmbed = null;
	    			// Discord絵文字を追加する
	    			// 追加される絵文字は参加通知、データベース保存で使うので、ここだけ同期処理にする
	    			synchronized (this)
	    			{
	    				emojiId = emoji.createOrgetEmojiId(player.getUsername(), avatarUrl);
	    				
				        if(Objects.nonNull(emojiId))
		    			{
		    				// 絵文字が正常に追加され、emidを返した場合
		    				sql="INSERT INTO minecraft (name,uuid,server, emid) VALUES (?,?,?,?);";
			    			ps = conn.prepareStatement(sql);
			    			ps.setString(1, player.getUsername());
			    			ps.setString(2, player.getUniqueId().toString());
			    			ps.setString(3, serverInfo.getName());
			    			ps.setString(4, emojiId);
			    			ps.executeUpdate();
			    			
			    			joinEmbed = discord.createEmbed
	    							(
	    								emoji.getEmojiString(player.getUsername(), emojiId)+
	    								player.getUsername()+"が"+config.getString("Servers.Hub","")+
	    								"サーバーに初参加です！",
	    								ColorUtil.ORANGE.getRGB()
	    							);
	    					discord.sendBotMessageAsync(joinEmbed);
		    			}
		    			else
		    			{
		    				// 絵文字が正常に追加されなかった場合
			    			sql="INSERT INTO minecraft (name,uuid,server) VALUES (?,?,?);";
			    			ps = conn.prepareStatement(sql);
			    			ps.setString(1, player.getUsername());
			    			ps.setString(2, player.getUniqueId().toString());
			    			ps.setString(3, serverInfo.getName());
			    			ps.executeUpdate();
			    			
					        joinEmbed = discord.createEmbed
	    							(
	    								player.getUsername()+"が"+serverInfo.getName()+
	    								"サーバーに初参加です！",
	    								ColorUtil.ORANGE.getRGB()
	    							);
	    					discord.sendBotMessageAsync(joinEmbed);
		    			}
	    			}
	            }
	            
				// サーバー移動通知
				logger.info("Player connected to server: " + serverInfo.getName());
				if(serverInfo.getName().equalsIgnoreCase(config.getString("hub")))
				{
					bc.broadcastMessage(player.getUsername()+"が"+config.getString("Servers.Hub")+"サーバーにやってきました！", NamedTextColor.AQUA, serverInfo.getName());
				}
				else
				{
					bc.broadcastMessage("サーバー移動通知: "+player.getUsername()+" -> "+serverInfo.getName(), NamedTextColor.AQUA, serverInfo.getName());
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
				
				// Amabassadorプラグインと競合している可能性あり
				// Main.getInjector().getInstance(velocity.PlayerList.class).updatePlayers();
				pl.updatePlayers();
			}
			catch (Exception e1)// SQLException | IOException | ClassNotFoundException
			{
				// スタックトレースをログに出力
	            logger.error("An onConnection error occurred: " + e1.getMessage());
	            for (StackTraceElement element : e1.getStackTrace()) 
	            {
	                logger.error(element.toString());
	            }
	        }
			finally
			{
				db.close_resorce(resultsets,conn,ps);
			}
		}).schedule();
	}
	
	@Subscribe
    public void onPlayerDisconnect(DisconnectEvent e)
    {
    	Player player = e.getPlayer();
    	
        server.getScheduler().buildTask(plugin, () ->
    	{
			// プレイヤーが最後にいたサーバーを取得
	        player.getCurrentServer().ifPresent(currentServer ->
	        {
	            RegisteredServer server = currentServer.getServer();
	            String serverName = server.getServerInfo().getName();
	            console.sendMessage(Component.text("Player " + player.getUsername() + " disconnected from server: " + serverName).color(NamedTextColor.GREEN));
	            try
	        	{
	            	conn = db.getConnection();
	        		
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
	            		ps.setString(3, serverName);
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
	        		db.close_resorce(resultsets, conn, ps);
	        	}
	        });
    	}).schedule();
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
            	logger.error("GETリクエストに失敗しました。HTTPエラーコード: " + response.statusCode());
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