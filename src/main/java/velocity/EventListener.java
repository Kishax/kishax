package velocity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;

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

import discord.DiscordEventListener;
import discord.MessageEditorInterface;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class EventListener {
	public static Map<String, String> PlayerMessageIds = new HashMap<>();
	public static final Map<Player, Runnable> disconnectTasks = new HashMap<>();
	public static final Map<Player, String> playersServerMap = new ConcurrentHashMap<>();
	public final Main plugin;
	public Connection conn = null;
	public ResultSet yuyu = null, yu = null, logs = null, rs = null, bj_logs = null, ismente = null;
	public ResultSet[] resultsets = {yuyu, yu, logs, rs, bj_logs, ismente};
	public PreparedStatement ps = null;
	private final ProxyServer server;
	private final Config config;
	private final Logger logger;
	private final DatabaseInterface db;
	private final BroadCast bc;
	private final ConsoleCommandSource console;
	private final RomaToKanji conv;
	private String chatServerName = null, originalMessage = null, joinMessage = null;
	private Component component = null;
	private final PlayerUtil pu;
	private final PlayerDisconnect pd;
	private final RomajiConversion rc;
	private final MessageEditorInterface discordME;
	private ServerInfo serverInfo = null;
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	@Inject
	public EventListener (
		Main plugin, Logger logger, ProxyServer server,
		Config config, DatabaseInterface db, BroadCast bc,
		ConsoleCommandSource console, RomaToKanji conv, PlayerUtil pu,
		PlayerDisconnect pd, RomajiConversion rc, MessageEditorInterface discordME
	) {
		this.plugin = plugin;
		this.logger = logger;
		this.server = server;
		this.config = config;
		this.db = db;
		this.bc = bc;
		this.console = console;
		this.conv = conv;
		this.pu = pu;
		this.pd = pd;
		this.rc = rc;
		this.discordME = discordME;
	}
	
	@Subscribe
	public void onChat(PlayerChatEvent e) {
		if (e.getMessage().startsWith("/")) return;

	    Player player = e.getPlayer();
	    originalMessage = e.getMessage();
	    
	    // プレイヤーの現在のサーバーを取得
        player.getCurrentServer().ifPresent(serverConnection -> {
            RegisteredServer registeredServer = serverConnection.getServer();
            serverInfo = registeredServer.getServerInfo();
            chatServerName = serverInfo.getName();
        });
        
        // マルチバイト文字の長さを取得
        int NameCount = player.getUsername().length();
        
        // スペースを生成
        StringBuilder space = new StringBuilder();
        for (int i = 0; i <= NameCount; i++) {
            space.append('\u0020');  // Unicodeのスペースを追加
        }
        
        server.getScheduler().buildTask(plugin, () -> {
        	try {
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

    		    component = Component.text(space+"(").color(NamedTextColor.GOLD);
    		    
    	        boolean isEnglish = false;
    	        if (originalMessage.length() >= 1) {
    	        	String firstOneChars = originalMessage.substring(0, 1);
    	        	if(".".equalsIgnoreCase(firstOneChars)) {
        	        	isEnglish = true;
        	        	originalMessage = originalMessage.substring(1);
        	        }
    	        }
    	        
    	        if (originalMessage.length() >= 2) {
    	        	String firstTwoChars = originalMessage.substring(0, 2);
    	        	if("@n".equalsIgnoreCase(firstTwoChars)) {
        	        	// 新しいEmbedをDiscordに送る(通知を鳴らす)
        	        	DiscordEventListener.PlayerChatMessageId = null;
        	        	originalMessage = originalMessage.substring(2);
        	        }
    	        }
    	        
    	        if (originalMessage.length() >= 3) {
    	        	String firstThreeChars = originalMessage.substring(0, 3);
    	        	if ("@en".equalsIgnoreCase(firstThreeChars)) {
        	        	isEnglish = true;
        	        	originalMessage = originalMessage.substring(3);
        	        }
    	        }
    	        
    		    // URLが含まれてなかったら
    		    if (!isUrl) {
    		    	// 漢字の検出
    		        String kanjiPattern = "[\\u4E00-\\u9FFF]+";
    		        
    		        // ひらがなの検出
    		        String hiraganaPattern = "[\\u3040-\\u309F]+";
    		        
    		        // カタカナの検出
    		        String katakanaPattern = "[\\u30A0-\\u30FF]+";
    		        
    		        if (
    		        	detectMatches(originalMessage, kanjiPattern) ||
    		        	detectMatches(originalMessage, hiraganaPattern) ||
    		        	detectMatches(originalMessage, katakanaPattern) ||
    		        	isEnglish
    		        ) {
    		        	// 日本語であったら
    			        discordME.AddEmbedSomeMessage("Chat", player, serverInfo, originalMessage);
    		        	return;
    		        }
    		        
    		        if (config.getBoolean("Conv.Mode")) {
		        		// Map方式
		        		String kanaMessage = conv.ConvRomaToKana(originalMessage);
        		        String kanjiMessage = conv.ConvRomaToKanji(kanaMessage);
        		        
        		        discordME.AddEmbedSomeMessage("Chat", player, serverInfo, kanjiMessage);
    			        
        		        component = component.append(Component.text(kanjiMessage + ")").color(NamedTextColor.GOLD));
        		        bc.sendSpecificServerMessage(component, chatServerName);
		        	} else {
		        		// pde方式
		        		String kanaMessage = rc.Romaji(originalMessage);
		        		String kanjiMessage = conv.ConvRomaToKanji(kanaMessage);
		        		
		        		discordME.AddEmbedSomeMessage("Chat", player, serverInfo, kanjiMessage);
    			        
        		        component = component.append(Component.text(kanjiMessage + ")").color(NamedTextColor.GOLD));
        		        bc.sendSpecificServerMessage(component, chatServerName);
		        	}

    		        return;
    		    }

		    	// 最後のURLの後のテキスト部分を追加
    		    if (lastMatchEnd < originalMessage.length()) {
    		        textParts.add(originalMessage.substring(lastMatchEnd));
    		    }

    		    // テキスト部分を結合
    		    int textPartsSize = textParts.size();
    		    int urlsSize = urls.size();
    		    //boolean isUrlLineBreak = false;
    		    
    		    for (int i = 0; i < textPartsSize; i++) {
    		        if (Objects.nonNull(textParts) && textPartsSize != 0) {
    		            String text = textParts.get(i);
    		            String kanaMessage;
    		            String kanjiMessage;
    		            if (isEnglish) {
    		            	// 英語
    		            	mixtext += text;
    		            } else {
    		            	// 日本語
    		            	if (config.getBoolean("Conv.Mode")) {
        		        		// Map方式
        		            	kanaMessage = conv.ConvRomaToKana(text);
            		            kanjiMessage = conv.ConvRomaToKanji(kanaMessage);
        		        	} else {
        		            	// pde方式
        		            	kanaMessage = rc.Romaji(text);
            		            kanjiMessage = conv.ConvRomaToKanji(kanaMessage);
        		            }

        		            mixtext += kanjiMessage;
        		            component = component.append(Component.text(kanjiMessage).color(NamedTextColor.GOLD));
    		            }
    		        }

    		        if (i < urlsSize) {
    		            String getUrl;
    		            String getUrl2;
    		            if (textParts.get(i).isEmpty()) {
    		                // textがなかったら、先頭の改行は無くす(=URLのみ)
    		                getUrl = urls.get(i);
    		                getUrl2 = urls.get(i);
    		            } else if (i != textPartsSize - 1) {
    		                getUrl = "\n" + urls.get(i) + "\n";
    		                getUrl2 = "\n" + space + urls.get(i);
    		                
    		                //if(i = urlsSize)
    		                //isUrlLineBreak = true;
    		            } else {
    		                getUrl = "\n" + urls.get(i);
    		                getUrl2 = "\n" + space + urls.get(i);
    		            }

    		            mixtext += getUrl;
    		            component = component.append(Component.text(getUrl2).color(NamedTextColor.GRAY).clickEvent(ClickEvent.openUrl(urls.get(i))).hoverEvent(HoverEvent.showText(Component.text("リンク"+(i+1)))));
    		        }
    		    }
    		    
    		    if (!isEnglish) {
    		    	component = component.append(Component.text(")").color(NamedTextColor.GOLD));
        		    bc.sendSpecificServerMessage(component, chatServerName);
    		    }
    		    
    		    discordME.AddEmbedSomeMessage("Chat", player, serverInfo, mixtext);
    		} catch (Exception ex) {
    			// スタックトレースをログに出力
	            logger.error("An onChat error occurred: " + ex.getMessage());
	            for (StackTraceElement element : ex.getStackTrace()) {
	                logger.error(element.toString());
	            }
    		}
        }).schedule();
	}
	
	public boolean detectMatches(String input, String pattern) {
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(input);

        while (matcher.find()) {
            //String found = matcher.group();
            //System.out.println(message + ": " + found);
        	return true;
        }

        return false;
    }
	
	@Subscribe
	public void onServerSwitch(ServerConnectedEvent e) {
		Player player = e.getPlayer();
		if (!disconnectTasks.isEmpty()) {
			for (Player disconnectPlayer : disconnectTasks.keySet()) {
				if (disconnectPlayer.getUniqueId().equals(player.getUniqueId())) {
					disconnectTasks.remove(disconnectPlayer);
				}
			}
		}
		
		/*if (disconnectTasks.containsKey(player)) 
        {
			disconnectTasks.remove(player);
        }*/
		
		RegisteredServer serverConnection = e.getServer();
        serverInfo = serverConnection.getServerInfo();
        Optional <RegisteredServer> previousServerInfo = e.getPreviousServer();
        
        if (Objects.isNull(serverInfo)) {
        	pd.playerDisconnect (
        		false,
        		player,
        		Component.text("コネクションエラー: 接続サーバー名が不明です。").color(NamedTextColor.RED)
        	);

        	logger.error("コネクションエラー: 接続サーバー名が不明です。");
        	return;
        }
        
		server.getScheduler().buildTask(plugin, () -> {
			try {
				conn = db.getConnection();
				if (Objects.isNull(conn)) {
					pd.playerDisconnect (
						false,
						player,
						Component.text("Database Server is closed now!!").color(NamedTextColor.BLUE)
					);
				}
				
				String sql = "SELECT online FROM status WHERE name=?;";
				ps = conn.prepareStatement(sql);
				ps.setString(1, "maintenance");
				ismente = ps.executeQuery();
				
				if (ismente.next()) {
					if (ismente.getBoolean("online")) {
						if (!player.hasPermission("group.super-admin")) {
							pd.playerDisconnect (
								false,
								player,
								Component.text("現在メンテナンス中です。").color(NamedTextColor.BLUE)
							);
							                
							return;
						}

						player.sendMessage(Component.text("スーパーアドミン認証...PASS\n\nALL CORRECT\n\nメンテナンスモードが有効です。").color(NamedTextColor.GREEN));
					}
				}
				
				sql = "SELECT * FROM members WHERE uuid=? ORDER BY id DESC LIMIT 1;";
	            ps = conn.prepareStatement(sql);
	            ps.setString(1, player.getUniqueId().toString());
	            yuyu = ps.executeQuery();
	            
	            sql = "SELECT * FROM members WHERE name=? ORDER BY id DESC LIMIT 1;";
	            ps = conn.prepareStatement(sql);
	            ps.setString(1, player.getUsername());
	            yu = ps.executeQuery();
	            
				//一番最初に登録された名前と一致したら
	            if (yuyu.next()) {
					// 結果セットに少なくとも1行が存在するかどうかをチェック 
	            	sql = "UPDATE members SET server=? WHERE uuid=?;";
	            	ps = conn.prepareStatement(sql);
	            	ps.setString(1, config.getString("Servers.Hub"));
	            	ps.setString(2, player.getUniqueId().toString());
	            	ps.executeUpdate();
	            	
	            	if (yuyu.getBoolean("ban")) {
	            		pd.playerDisconnect (
	            			true,
	            			player,
	            			Component.text("You are banned from this server.").color(NamedTextColor.RED)
	            		);

	    				return;
	            	} else {
	            		// メッセージ送信
	            		component = Component.text(player.getUsername()+"が"+serverInfo.getName()+"サーバーに参加しました。").color(NamedTextColor.YELLOW);
	            		bc.sendSpecificServerMessage(component, serverInfo.getName());
	            		joinMessage = config.getString("EventMessage.Join","");
						if (!joinMessage.isEmpty()) {
							// \\n を \n に変換
					        joinMessage = joinMessage.replace("\\n", "\n");
					        
							player.sendMessage(Component.text(joinMessage).color(NamedTextColor.AQUA));
						}
						
	            		// 2時間経ってたら
        				sql = "SELECT * FROM log WHERE uuid=? AND `join`=? ORDER BY id DESC LIMIT 1;";
        				ps = conn.prepareStatement(sql);
        				ps.setString(1, player.getUniqueId().toString());
        				ps.setBoolean(2, true);
        				logs = ps.executeQuery();
        				
						// タイムゾーンをAsia/Tokyoに設定
        				long beforejoin_sa_minute = 0;
        				if (logs.next()) {
    	    				// Asia/Tokyoのタイムゾーンで現在の時刻を取得
							ZonedDateTime nowTokyo = ZonedDateTime.now(ZoneId.of("Asia/Tokyo"));
							long now_timestamp = nowTokyo.toEpochSecond();
    
							// TIMESTAMP型のカラムを取得
							Timestamp beforejoin_timeget = logs.getTimestamp("time");

							// Unixタイムスタンプに変換
        	                long beforejoin_timestamp = beforejoin_timeget.getTime() / 1000L;
							long beforejoin_sa = now_timestamp - beforejoin_timestamp;
							if (beforejoin_sa < 0) {
								logger.error("beforejoin_sa is less than 0.");
							}
							beforejoin_sa_minute = Math.max(beforejoin_sa / 60, 0); // マイナス値を防ぐためにMath.maxを使用
        				}
        				
	            		if (player.getUsername().equals(yuyu.getString("name"))) {
	            			//　add log
	            			sql = "INSERT INTO log (name,uuid,server,`join`) VALUES (?,?,?,?);";
	            			ps = conn.prepareStatement(sql);
	            			ps.setString(1, player.getUsername());
	            			ps.setString(2, player.getUniqueId().toString());
	            			ps.setString(3, serverInfo.getName());
	            			ps.setBoolean(4, true);
	            			ps.executeUpdate();
	            		} else {
	            			// 一番最初に登録した名前と一致しなかったら
	            			// MOJANG-APIからとってきた名前でレコードを更新させる
	            			String current_name = pu.getPlayerNameFromUUID(player.getUniqueId());
	            			if (Objects.isNull(current_name) || !(current_name.equals(player.getUsername()))) {
	            				pd.playerDisconnect (
	            					true,
	            					player,
	            					Component.text("You are banned from this server.").color(NamedTextColor.RED)
	            				);

	            				return;
	            			}
	            			
	            			sql = "UPDATE members SET name=?,server=?,old_name=? WHERE uuid=?;";
	            			ps = conn.prepareStatement(sql);
	            			ps.setString(1, current_name);
	            			ps.setString(2, serverInfo.getName());
	            			ps.setString(3, yuyu.getString("name"));
	            			ps.setString(4, player.getUniqueId().toString());
	            			ps.executeUpdate();
	            			
	            			player.sendMessage(Component.text("MCIDの変更が検出されたため、データベースを更新しました。").color(NamedTextColor.GREEN));
	            			
	            			// 過去の名前を解放するため、過去の名前のレコードがほかにもあったらそれをinvalid_loginへ移動
	            			sql="SELECT COUNT(*) FROM members WHERE name=?;";
	            			ps = conn.prepareStatement(sql);
	            			ps.setString(1, yuyu.getString("name"));
	            			rs = ps.executeQuery();
	            			if (rs.next()) {
	            				int count = rs.getInt(1);
	            				if (count>=1) {
	            					sql="INSERT INTO invalid_login SELECT * FROM members WHERE name=?;";
	            					ps = conn.prepareStatement(sql);
	            					ps.setString(1, yuyu.getString("name"));
	            					ps.executeUpdate();
	            					
	            					sql="DELETE from members WHERE name=?;";
									ps = conn.prepareStatement(sql);
	            					ps.setString(1, yuyu.getString("name"));
	            					ps.executeUpdate();
	            				}
	            			}
	            		}
	            		
	            		// AmabassadorプラグインによるReconnectの場合 Or リログして〇秒以内の場合
	            		if (EventListener.PlayerMessageIds.containsKey(player.getUniqueId().toString())) {
	            			// どこからか移動してきたとき
    						discordME.AddEmbedSomeMessage("Move", player, serverInfo);
	            		} else {
	            			if (beforejoin_sa_minute>=config.getInt("Interval.Login",0)) {
								EventListener.playersServerMap.put(player, serverInfo.getName());
		    					if (previousServerInfo.isPresent()) {
		    						// どこからか移動してきたとき
		    						discordME.AddEmbedSomeMessage("Move", player, serverInfo);
		    			        } else {
		    			        	// 1回目のどこかのサーバーに上陸したとき
		    						discordME.AddEmbedSomeMessage("Join", player, serverInfo);
		    			        }
		    				}
	            		}
	            	}
	            } else {
	            	// DBにデータがなかったら (初参加)
	            	// MojangAPIによるUUID-MCIDチェックも行う
	            	// データベースに同一の名前がないか確認
	            	String current_name = pu.getPlayerNameFromUUID(player.getUniqueId());
	    			if (Objects.isNull(current_name) || !(current_name.equals(player.getUsername())) || yu.next()) {
	    				sql="INSERT INTO members (name,uuid,server,ban) VALUES (?,?,?,?);";
	        			ps = conn.prepareStatement(sql);
	        			ps.setString(1, player.getUsername());
	        			ps.setString(2, player.getUniqueId().toString());
	        			ps.setString(3, serverInfo.getName());
	        			ps.setBoolean(4, true);
	        			ps.executeUpdate();
	        			
	        			pd.playerDisconnect (
	    					true,
	    					player,
	    					Component.text("You are banned from this server.").color(NamedTextColor.RED)
	    				);

	    				return;
	    			}
	    			
                	String DiscordInviteUrl = config.getString("Discord.InviteUrl","");
                	if (!DiscordInviteUrl.isEmpty()) {
                		component = Component.text(player.getUsername()+"が"+serverInfo.getName()+"サーバーに初参加しました。").color(NamedTextColor.YELLOW)
    							.append(Component.text("\nFMCサーバー").color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD, TextDecoration.UNDERLINED))
    							.append(Component.text("へようこそ！\n当サーバーでは、サーバーへ参加するにあたって、FMCアカウント作成と、それをマイクラアカウントと紐づける").color(NamedTextColor.AQUA))
    							.append(Component.text("UUID認証").color(NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD, TextDecoration.UNDERLINED))
    							.append(Component.text("を必須としています。\n").color(NamedTextColor.AQUA))
    							.append(Component.text("FMCユーザーは、サーバーを起動するためのリクエストを管理者へ送ることができます。今後色々なコンテンツを追加していく予定です！").color(NamedTextColor.AQUA))
    							.append(Component.text("\n正面にいるNPCをクリックして、UUID認証手続きへ移ります。").color(NamedTextColor.AQUA))
    							.append(Component.text("\nなにかわからないことがあったら、当サーバーの").color(NamedTextColor.AQUA))
    							.append(Component.text("Discord").color(NamedTextColor.BLUE).decorate(TextDecoration.BOLD, TextDecoration.UNDERLINED)
    									.clickEvent(ClickEvent.openUrl(DiscordInviteUrl))
        			    				.hoverEvent(HoverEvent.showText(Component.text("FMCサーバーのDiscordへいこう！"))))
    							.append(Component.text("にて質問してください！参加するには、上の「Discord」をクリックしてね。").color(NamedTextColor.AQUA));
                		player.sendMessage(component);
                	} else {
                		component = Component.text(player.getUsername()+"が"+serverInfo.getName()+"サーバーに初参加しました。").color(NamedTextColor.YELLOW)
    							.append(Component.text("\nFMCサーバー").color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD, TextDecoration.UNDERLINED))
    							.append(Component.text("へようこそ！\n当サーバーでは、サーバーへ参加するにあたって、FMCアカウント作成と、それをマイクラアカウントと紐づける").color(NamedTextColor.AQUA))
    							.append(Component.text("UUID認証").color(NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD, TextDecoration.UNDERLINED))
    							.append(Component.text("を必須としています。\n").color(NamedTextColor.AQUA))
    							.append(Component.text("FMCユーザーは、サーバーを起動するためのリクエストを管理者へ送ることができます。今後色々なコンテンツを追加していく予定です！").color(NamedTextColor.AQUA))
    							.append(Component.text("\n正面にいるNPCをクリックして、UUID認証手続きへ移ります。").color(NamedTextColor.AQUA));
                		player.sendMessage(component);
                	}
                	
                	discordME.AddEmbedSomeMessage("FirstJoin", player, serverInfo);
	            }
	            
				// サーバー移動通知
				//logger.info("Player connected to server: " + serverInfo.getName());
				if (serverInfo.getName().equalsIgnoreCase(config.getString("hub"))) {
					component = Component.text(player.getUsername()+"が"+config.getString("Servers.Hub")+"サーバーに初めてやってきました！").color(NamedTextColor.AQUA);
					bc.sendExceptServerMessage(component, serverInfo.getName());
				} else {
					component = Component.text("サーバー移動通知: "+player.getUsername()+" -> "+serverInfo.getName()).color(NamedTextColor.AQUA);
					bc.sendExceptServerMessage(component, serverInfo.getName());
				}
				
				if (serverInfo.getName().equals("latest")) {
					component = Component.text()
		    			    	.append(Component.text("dynmap").decorate(TextDecoration.BOLD).color(NamedTextColor.GOLD))
		    			    	.append(Component.text("\nhttps://keypforev.ddns.net/dynmap/").color(NamedTextColor.GRAY).decorate(TextDecoration.UNDERLINED)
	    			    			.clickEvent(ClickEvent.openUrl("https://keypforev.ddns.net/dynmap/"))
	                                .hoverEvent(HoverEvent.showText(Component.text("(クリックして)地図を見る"))))
		    			    	.build();
					player.sendMessage(component);
				}
				
				// Amabassadorプラグインと競合している可能性あり
				// Main.getInjector().getInstance(velocity.PlayerUtil.class).updatePlayers();
				pu.updatePlayers();
			} catch (ClassNotFoundException | SQLException e1) {
				// スタックトレースをログに出力
	            logger.error("An onConnection error occurred: " + e1.getMessage());
	            for (StackTraceElement element : e1.getStackTrace()) {
	                logger.error(element.toString());
	            }
	        }
		}).schedule();
	}
	
	@Subscribe
    public void onPlayerDisconnect(DisconnectEvent e) {
    	Player player = e.getPlayer();
		EventListener.playersServerMap.remove(player);
		logger.info("Player disconnected: " + player.getUsername());
    	Runnable task = () -> {
            // プレイヤーがReconnectしなかった場合に実行する処理
    		server.getScheduler().buildTask(plugin, () -> {
    			// プレイヤーが最後にいたサーバーを取得
    	        player.getCurrentServer().ifPresent(currentServer -> {
    	            RegisteredServer registeredServer = currentServer.getServer();
    	            serverInfo = registeredServer.getServerInfo();
    	            console.sendMessage(Component.text("Player " + player.getUsername() + " disconnected from server: " + serverInfo.getName()).color(NamedTextColor.GREEN));
    	            
    	            int playTime = pu.getPlayerTime(player, serverInfo);
    	            discordME.AddEmbedSomeMessage("Exit", player, serverInfo, playTime);
    	            
    	            try {
    	            	conn = db.getConnection();
                		// add log
                		String sql = "INSERT INTO log (name,uuid,server,quit,playtime) VALUES (?,?,?,?,?);";
                		ps = conn.prepareStatement(sql);
                		ps.setString(1, player.getUsername());
                		ps.setString(2, player.getUniqueId().toString());
                		ps.setString(3, serverInfo.getName());
                		ps.setBoolean(4, true);
                		ps.setInt(5, playTime);
                		ps.executeUpdate();
    	        	} catch (SQLException | ClassNotFoundException e1) {
    	                logger.error("A SQLException | ClassNotFoundException error occurred: " + e1.getMessage());
						for (StackTraceElement element : e1.getStackTrace()) {
							logger.error(element.toString());
						}
    	            } finally {
    	        		db.close_resource(resultsets, conn, ps);
    	        	}
    	        });
        	}).schedule();
        };
        
        // タイマーを設定し、一定時間後に処理Aを実行
        disconnectTasks.put(player, task);
        scheduler.schedule(() -> {
            if (disconnectTasks.containsKey(player)) {
                task.run();
                disconnectTasks.remove(player);
            }
        }, 10, TimeUnit.SECONDS);  // 10秒の遅延
    }
}