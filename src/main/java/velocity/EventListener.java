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
	public final Main plugin;
	private final ProxyServer server;
	private final Config config;
	private final Logger logger;
	private final DatabaseInterface db;
	private final BroadCast bc;
	private final ConsoleCommandSource console;
	private final RomaToKanji conv;
	private String chatServerName = null, originalMessage = null, joinMessage = null;
	private Component component = null;
	private final PlayerUtils pu;
	private final PlayerDisconnect pd;
	private final RomajiConversion rc;
	private final MessageEditorInterface discordME;
	private final MineStatus ms;
	private final DatabaseLog dbLog;
	private final GeyserMC gm;
	private ServerInfo serverInfo = null;
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	@Inject
	public EventListener (Main plugin, Logger logger, ProxyServer server, Config config, DatabaseInterface db, BroadCast bc, ConsoleCommandSource console, RomaToKanji conv, PlayerUtils pu, PlayerDisconnect pd, RomajiConversion rc, MessageEditorInterface discordME, MineStatus ms, DatabaseLog dbLog, GeyserMC gm) {
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
		this.ms = ms;
		this.dbLog = dbLog;
		this.gm = gm;
	}
	
	@Subscribe
	public void onChat(PlayerChatEvent e) {
		if (e.getMessage().startsWith("/")) return;
	    Player player = e.getPlayer();
		String playerName = player.getUsername();
	    originalMessage = e.getMessage();
	    // プレイヤーの現在のサーバーを取得
        player.getCurrentServer().ifPresent(serverConnection -> {
            RegisteredServer registeredServer = serverConnection.getServer();
            serverInfo = registeredServer.getServerInfo();
            chatServerName = serverInfo.getName();
        });
        // マルチバイト文字の長さを取得
        int NameCount = playerName.length();
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
    		        if (detectMatches(originalMessage, kanjiPattern) || detectMatches(originalMessage, hiraganaPattern) || detectMatches(originalMessage, katakanaPattern) || isEnglish) {
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
		String playerName = player.getUsername(),
			playerUUID = player.getUniqueId().toString();
		if (!disconnectTasks.isEmpty()) {
			for (Player disconnectPlayer : disconnectTasks.keySet()) {
				if (disconnectPlayer.getUniqueId().equals(player.getUniqueId())) {
					disconnectTasks.remove(disconnectPlayer);
				}
			}
		}
		RegisteredServer serverConnection = e.getServer();
        serverInfo = serverConnection.getServerInfo();
		String currentServerName = serverInfo.getName();
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
		boolean isBedrock = false;
		if (gm.isGeyserPlayer(player)) {
			logger.info("GeyserMC player connected: " + playerName);
			String playerXuid = gm.getGeyserPlayerXuid(player);
			logger.info("Player XUID: " + playerXuid);
			isBedrock = true;
			//discordME.AddEmbedSomeMessage("Join", player, serverInfo);
			//return;
		} else {
			logger.info("Java player connected: " + playerName);
		}
		server.getScheduler().buildTask(plugin, () -> {
			try (Connection conn = db.getConnection()) {
				if (db.isMaintenance(conn)) {
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
				String query2 = "SELECT * FROM members WHERE uuid=? ORDER BY id DESC LIMIT 1;";
				try (PreparedStatement ps2 = conn.prepareStatement(query2)) {
					ps2.setString(1, playerUUID);
					try (ResultSet yuyu = ps2.executeQuery();) {
						if (yuyu.next()) {
							if (yuyu.getBoolean("ban")) {
								pd.playerDisconnect (
									true,
									player,
									Component.text("You are banned from this server.").color(NamedTextColor.RED)
								);
								return;
							} else {
								// メッセージ送信
								component = Component.text(playerName+"が"+currentServerName+"サーバーに参加しました。").color(NamedTextColor.YELLOW);
								bc.sendSpecificServerMessage(component, currentServerName);
								joinMessage = config.getString("EventMessage.Join","");
								if (!joinMessage.isEmpty()) {
									// \\n を \n に変換
									joinMessage = joinMessage.replace("\\n", "\n");
									
									player.sendMessage(Component.text(joinMessage).color(NamedTextColor.AQUA));
								}
								// 2時間経ってたら
								String query4 = "SELECT * FROM log WHERE uuid=? AND `join`=? ORDER BY id DESC LIMIT 1;";
								try (PreparedStatement ps4 = conn.prepareStatement(query4)) {
									ps4.setString(1, playerUUID);
									ps4.setBoolean(2, true);
									try (ResultSet logs = ps4.executeQuery()) {
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
										if (playerName.equals(yuyu.getString("name"))) {
											dbLog.insertLog(conn, "INSERT INTO `log` (name, uuid, server, `join`) VALUES (?, ?, ?, ?);", new Object[] {playerName, playerUUID, currentServerName, true});
										} else {
											// 一番最初に登録した名前と一致しなかったら
											// MOJANG-APIからとってきた名前でレコードを更新させる
											String current_name = pu.getPlayerNameFromUUID(player.getUniqueId());
											if (Objects.isNull(current_name) || !(current_name.equals(playerName))) {
												pd.playerDisconnect (
													true,
													player,
													Component.text("You are banned from this server.").color(NamedTextColor.RED)
												);
												return;
											}
											String query5 = "UPDATE members SET name=?, old_name=? WHERE uuid=?;";
											try (PreparedStatement ps5 = conn.prepareStatement(query5)) {
												ps5.setString(1, current_name);
												ps5.setString(2, yuyu.getString("name"));
												ps5.setString(3, playerUUID);
												int rsAffected5 = ps5.executeUpdate();
												if (rsAffected5 > 0) {
													player.sendMessage(Component.text("MCIDの変更が検出されたため、データベースを更新しました。").color(NamedTextColor.GREEN));
													// 過去の名前を解放するため、過去の名前のレコードがほかにもあったらそれをinvalid_loginへ移動
													String query6 = "SELECT COUNT(*) FROM members WHERE name=?;";
													try (PreparedStatement ps6 = conn.prepareStatement(query6)) {
														ps6.setString(1, yuyu.getString("name"));
														try (ResultSet rs6 = ps6.executeQuery()) {
															if (rs6.next()) {
																int count = rs6.getInt(1);
																if (count >= 1) {
																	String query7 = "INSERT INTO invalid_login SELECT * FROM members WHERE name=?;";
																	try (PreparedStatement ps7 = conn.prepareStatement(query7)) {
																		ps7.setString(1, yuyu.getString("name"));
																		int rsAffected7 = ps7.executeUpdate();
																		if (rsAffected7 > 0) {
																			String query8 = "DELETE from members WHERE name=?;";
																			try (PreparedStatement ps8 = conn.prepareStatement(query8)) {
																				ps8.setString(1, yuyu.getString("name"));
																				int rsAffected8 = ps8.executeUpdate();
																				if (rsAffected8 > 0) {
																					console.sendMessage(Component.text("過去の名前のレコードをinvalid_loginへ移動しました。").color(NamedTextColor.GREEN));
																				}
																			}
																		} else {
																			console.sendMessage(Component.text("過去の名前のレコードをinvalid_loginへ移動できませんでした。").color(NamedTextColor.RED));
																		}
																	}
																}
															}
														}
													}
												}
											}
										}
										// AmabassadorプラグインによるReconnectの場合 Or リログして〇秒以内の場合
										if (EventListener.PlayerMessageIds.containsKey(playerUUID)) {
											// どこからか移動してきたとき
											//ms.updateJoinPlayers(playerName, currentServerName);
											if (previousServerInfo.isPresent()) {
												RegisteredServer previousServer = previousServerInfo.get();
												ServerInfo beforeServerInfo = previousServer.getServerInfo();
												String beforeServerName = beforeServerInfo.getName();
												//logger.info("Player connected to server: " + beforeServerName);
												//logger.info("Player connected to server: " + currentServerName);
												ms.updateMovePlayers(playerName, beforeServerName, currentServerName);
												discordME.AddEmbedSomeMessage("Move", player, serverInfo);
											}
										} else {
											if (beforejoin_sa_minute>=config.getInt("Interval.Login",0)) {
												if (previousServerInfo.isPresent()) {
													// どこからか移動してきたとき
													RegisteredServer previousServer = previousServerInfo.get();
													ServerInfo beforeServerInfo = previousServer.getServerInfo();
													String beforeServerName = beforeServerInfo.getName();
													//logger.info("Player connected to server: " + beforeServerName);
													//logger.info("Player connected to server: " + currentServerName);
													ms.updateMovePlayers(playerName, beforeServerName, currentServerName);
													discordME.AddEmbedSomeMessage("Move", player, currentServerName);
												} else {
													// 1回目のどこかのサーバーに上陸したとき
													ms.updateJoinPlayers(playerName, currentServerName);
													discordME.AddEmbedSomeMessage("Join", player, serverInfo);
												}
											}
										}
									}
								}
							}
						} else {
							// DBにデータがなかったら (初参加)
							// MojangAPIによるUUID-MCIDチェックも行う
							// データベースに同一の名前がないか確認
							String current_name = pu.getPlayerNameFromUUID(player.getUniqueId());
							String query3 = "SELECT * FROM members WHERE name=? ORDER BY id DESC LIMIT 1;";
							try (PreparedStatement ps3 = conn.prepareStatement(query3)) {
								ps3.setString(1, playerName);
								try (ResultSet yu = ps3.executeQuery()) {
									if (yu.next() || Objects.isNull(current_name) || !(current_name.equals(playerName))) {
										// クエリ実行より優先してキック
										pd.playerDisconnect (
											true,
											player,
											Component.text("You are banned from this server.").color(NamedTextColor.RED)
										);
										String query4 = "INSERT INTO members (name, uuid, ban) VALUES (?, ?, ?);";
										try (PreparedStatement ps4 = conn.prepareStatement(query4)) {
											ps4.setString(1, playerName);
											ps4.setString(2, playerUUID);
											ps4.setBoolean(3, true);
											ps4.executeUpdate();
										}
										return;
									}
									String DiscordInviteUrl = config.getString("Discord.InviteUrl","");
									if (!DiscordInviteUrl.isEmpty()) {
										component = Component.text(playerName+"が"+currentServerName+"サーバーに初参加しました。").color(NamedTextColor.YELLOW)
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
										component = Component.text(playerName+"が"+currentServerName+"サーバーに初参加しました。").color(NamedTextColor.YELLOW)
												.append(Component.text("\nFMCサーバー").color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD, TextDecoration.UNDERLINED))
												.append(Component.text("へようこそ！\n当サーバーでは、サーバーへ参加するにあたって、FMCアカウント作成と、それをマイクラアカウントと紐づける").color(NamedTextColor.AQUA))
												.append(Component.text("UUID認証").color(NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD, TextDecoration.UNDERLINED))
												.append(Component.text("を必須としています。\n").color(NamedTextColor.AQUA))
												.append(Component.text("FMCユーザーは、サーバーを起動するためのリクエストを管理者へ送ることができます。今後色々なコンテンツを追加していく予定です！").color(NamedTextColor.AQUA))
												.append(Component.text("\n正面にいるNPCをクリックして、UUID認証手続きへ移ります。").color(NamedTextColor.AQUA));
										player.sendMessage(component);
									}
									ms.updateJoinPlayers(playerName, currentServerName);
									discordME.AddEmbedSomeMessage("FirstJoin", player, serverInfo);
									if (config.getBoolean("Servers." + currentServerName + ".hub")) {
										component = Component.text(playerName+"がhomeサーバーに初めてやってきました！").color(NamedTextColor.AQUA);
										bc.sendExceptServerMessage(component, currentServerName);
									}
								}
							}
						}
						// サーバー移動通知
						component = Component.text("サーバー移動通知: "+playerName+" -> "+currentServerName).color(NamedTextColor.AQUA);
						bc.sendExceptServerMessage(component, currentServerName);
						if (currentServerName.equals("latest")) {
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
					}
				}
			} catch (ClassNotFoundException | SQLException e1) {
				pd.playerDisconnect (
						false,
						player,
						Component.text("Database Server is closed now!!").color(NamedTextColor.BLUE)
					);
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
		String playerName = player.getUsername();
		if (gm.isGeyserPlayer(player)) {
			logger.info("GeyserMC player connected: " + playerName);
			return;
		} else {
			logger.info("Java player connected: " + playerName);
		}
		String playerUUID = player.getUniqueId().toString();
		player.getCurrentServer().ifPresent(serverConnection -> {
			RegisteredServer registeredServer = serverConnection.getServer();
			serverInfo = registeredServer.getServerInfo();
			ms.updateQuitPlayers(playerName, serverInfo.getName());
		});
    	Runnable task = () -> {
            // プレイヤーがReconnectしなかった場合に実行する処理
    		server.getScheduler().buildTask(plugin, () -> {
    			// プレイヤーが最後にいたサーバーを取得
    	        player.getCurrentServer().ifPresent(currentServer -> {
    	            RegisteredServer registeredServer = currentServer.getServer();
    	            serverInfo = registeredServer.getServerInfo();
    	            console.sendMessage(Component.text("Player " + playerName + " disconnected from server: " + serverInfo.getName()).color(NamedTextColor.GREEN));
    	            int playTime = pu.getPlayerTime(player, serverInfo);
    	            discordME.AddEmbedSomeMessage("Exit", player, serverInfo, playTime);
					dbLog.insertLog("INSERT INTO `log` (name, uuid, server, quit, playtime) VALUES (?,?,?,?,?);", new Object[] {playerName, playerUUID, serverInfo.getName(), true, playTime});
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