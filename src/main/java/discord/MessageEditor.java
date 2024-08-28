package discord;

import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;

import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import common.ColorUtil;
import net.dv8tion.jda.api.entities.MessageEmbed;
import velocity.Config;
import velocity.Database;
import velocity.EventListener;
import velocity.Main;
import velocity.PlayerUtil;
import velocity_command.Maintenance;

public class MessageEditor implements MessageEditorInterface {

	private PreparedStatement ps = null;
	public Connection conn = null;
	public final Main plugin;
	private final ProxyServer server;
	private final Logger logger;
	private final Config config;
	private final Database db;
	private final DiscordInterface discord;
	private final EmojiManager emoji;
	private final PlayerUtil pu;
	private String avatarUrl = null, addMessage = null, 
			Emoji = null, FaceEmoji = null, targetServerName = null,
			uuid = null, playerName = null, currentServerName = null;
	private MessageEmbed sendEmbed = null, createEmbed = null;
	private WebhookMessageBuilder builder = null;
	private CompletableFuture<String> EmojiFutureId = null, FaceEmojiFutureId = null;

	@Inject
	public MessageEditor (
		Main plugin, Logger logger, ProxyServer server,
		Config config, Database db, DiscordInterface discord,
		EmojiManager emoji, PlayerUtil pu
	) {
		this.plugin = plugin;
		this.logger = logger;
		this.server = server;
		this.config = config;
		this.db = db;
		this.discord = discord;
		this.emoji = emoji;
		this.pu = pu;
	}
	
	@SuppressWarnings("null")
	@Override
	public CompletableFuture<Void> AddEmbedSomeMessage (
		String type, Player player, ServerInfo serverInfo, 
		String serverName, String alternativePlayerName, int playTime,
		String chatMessage, UUID playerUUID
	) {
		if (Objects.isNull(player)) {
			// player変数がnullかつalternativePlayerNameが与えられていたとき
			if (Objects.nonNull(alternativePlayerName)) {
				// データベースからuuidを取ってくる
				uuid = pu.getPlayerUUIDByNameFromDB(alternativePlayerName);
				playerName = alternativePlayerName;
			} else if(Objects.nonNull(playerUUID)) {
				// プレイヤー変数がnullかつalternativePlayerNameがnullかつplayerUUIDが与えられていた時
				// データベースからnameを取ってくる
				uuid = playerUUID.toString();
				playerName = pu.getPlayerNameByUUIDFromDB(playerUUID);
			}
		} else {
			uuid = player.getUniqueId().toString();
			playerName = player.getUsername();
		}
		
	    avatarUrl = "https://minotar.net/avatar/" + uuid;
	    
	    String EmojiName = config.getString("Discord." + type + "EmojiName", "");

	    // 第二引数に画像URLが入っていないため、もし、EmojiNameという絵文字がなかったら、追加せずにnullで返る
	    // createOrgetEmojiIdの第一引数がnull Or Emptyであった場合、nullで返るので、DiscordBotへのリクエスト回数を減らせる
	    EmojiFutureId = emoji.createOrgetEmojiId(EmojiName);
	    FaceEmojiFutureId = null;
		try {
			FaceEmojiFutureId = emoji.createOrgetEmojiId(playerName, avatarUrl);
		} catch (URISyntaxException e) {
			logger.error("A URISyntaxException error occurred: " + e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                logger.error(element.toString());
            }
		}

	    return CompletableFuture.allOf(EmojiFutureId, FaceEmojiFutureId)
	            .thenCompose((var v) -> {
	        try {
	            if (Objects.nonNull(serverInfo)) {
	                currentServerName = serverInfo.getName();
	            } else {
	                currentServerName = "";
	            }

	            if (Objects.isNull(serverName)) {
	                targetServerName = "";
	            } else {
	            	targetServerName = serverName;
	            }

	            String EmojiId = EmojiFutureId.get(); // プラスとかマイナスとかの絵文字ID取得
	            String FaceEmojiId = FaceEmojiFutureId.get(); // minecraftのアバターの顔の絵文字Id取得
	            Emoji = emoji.getEmojiString(EmojiName, EmojiId);
	            FaceEmoji = emoji.getEmojiString(playerName, FaceEmojiId);
	            
	            String messageId = EventListener.PlayerMessageIds.getOrDefault(uuid, null);
	            String chatMessageId = DiscordEventListener.PlayerChatMessageId;
	            
	            addMessage = null;
	            switch (type) {
	            	case "End" -> {
						List<CompletableFuture<Void>> futures = new ArrayList<>();
						for (Player eachPlayer : server.getAllPlayers()) {
							// プレイヤーの現在のサーバーを取得
							CompletableFuture<Void> future = eachPlayer.getCurrentServer()
									.map(serverConnection -> {
										RegisteredServer registerServer = serverConnection.getServer();
										ServerInfo playerServerInfo = registerServer.getServerInfo();
										int playTime2 = pu.getPlayerTime(eachPlayer, playerServerInfo);
										// AddEmbedSomeMessageがCompletableFuture<Void>を返すと仮定
										return AddEmbedSomeMessage("Exit", eachPlayer, playerServerInfo, playTime2);
									}).orElse(CompletableFuture.completedFuture(null)); // サーバーが取得できない場合は即完了するFuture
							
							futures.add(future);
						}
						
						// 全ての非同期処理が完了するのを待つ
						CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
								.thenRun(() -> {
									// 全てのAddEmbedSomeMessageの処理が完了した後に実行される
									discord.logoutDiscordBot().thenRun(() -> server.shutdown());
								});
						
						return CompletableFuture.completedFuture(null);
                    }
	            		
	            	case "Exit" -> {
						if (Objects.nonNull(Emoji) && Objects.nonNull(FaceEmoji) && Objects.nonNull(messageId)) {
							String convStringTime = pu.secondsToStr(playTime);
							// すべての処理が完了するまで待つ
							CompletableFuture<Void> editFuture = CompletableFuture.completedFuture(null);
							
							// Velocityサーバー停止のフラグが立っていたら
							if(!Main.isVelocity) {
								// poweroffの絵文字を取りに行く
								String EndEmojiName = config.getString("Discord.EndEmojiName","");
								editFuture = emoji.createOrgetEmojiId(EndEmojiName).thenAccept(EndEmojiId -> {
									if (Objects.nonNull(EndEmojiId)) {
										String EndEmoji = emoji.getEmojiString(EndEmojiName, EndEmojiId);
										addMessage = "\n\n" + EndEmoji + "プロキシサーバーが停止しました。" + "\n\n" + Emoji + FaceEmoji + playerName + "が" +
												currentServerName + "サーバーから退出しました。\n\n:alarm_clock: プレイ時間: " + convStringTime;
									}
								});
								
								// 編集を行う
								return editFuture.thenCompose(v1 -> discord.editBotEmbed(messageId, addMessage));
							} else if (Maintenance.isMente) {
								// メンテのフラグが立っていたら
								Maintenance.isMente = false;
								addMessage = String.format("""
             
								:red_circle: メンテナンスモードになりました。
								
								%s%s%sが%sサーバーから退出しました。
								
								:alarm_clock: プレイ時間: %s
								""", Emoji, FaceEmoji, playerName, currentServerName, convStringTime);

								if (!player.hasPermission("group.super-admin")) {
									EventListener.PlayerMessageIds.remove(uuid);
									// 編集を行う
									return editFuture.thenCompose(v1 -> discord.editBotEmbed(messageId, addMessage));
								}
							} else {
								addMessage = String.format("""
									
								
									%s%s%sが%sサーバーから退出しました。
								
									:alarm_clock: プレイ時間: %s
								""",Emoji, FaceEmoji, playerName, currentServerName, convStringTime);

								EventListener.PlayerMessageIds.remove(uuid);
								// 編集を行う
								return editFuture.thenCompose(v1 -> discord.editBotEmbed(messageId, addMessage));
							}
							
							// 編集を行う
							return editFuture.thenCompose(v1 -> discord.editBotEmbed(messageId, addMessage));
						}

						return CompletableFuture.completedFuture(null);
                    }
	                	
	            	case "MenteOn" -> {
						if (Objects.nonNull(Emoji)) {
							addMessage = Emoji + "メンテナンスモードが有効になりました。\nいまは遊べないカッ...";
						} else {
							addMessage = "メンテナンスモードが有効になりました。";
						}
						
						createEmbed = discord.createEmbed (
										addMessage,
										ColorUtil.AQUA.getRGB()
									);
						discord.sendBotMessage(createEmbed);
						
						for (Player eachPlayer : server.getAllPlayers()) {
							// プレイヤーの現在のサーバーを取得
							Optional<ServerConnection> optionalServerConnection = eachPlayer.getCurrentServer();
							if (optionalServerConnection.isPresent()) {
								ServerConnection serverConnection = optionalServerConnection.get();
								RegisteredServer registerServer = serverConnection.getServer();
								ServerInfo playerServerInfo = registerServer.getServerInfo();
								int playTime2 = pu.getPlayerTime(eachPlayer, playerServerInfo);
								// AddEmbedSomeMessageがCompletableFuture<Void>を返すと仮定
								AddEmbedSomeMessage("Exit", eachPlayer, playerServerInfo, playTime2);
							}
						}
						
						return CompletableFuture.completedFuture(null);
                    }
	            	
	            	case "MenteOff" -> {
						if (Objects.nonNull(Emoji)) {
							addMessage = Emoji + "メンテナンスモードが無効になりました。\nまだまだ遊べるドン！";
						} else {
							addMessage = "メンテナンスモードが無効になりました。";
						}
						
						createEmbed = discord.createEmbed (
										addMessage,
										ColorUtil.RED.getRGB()
									);
						discord.sendBotMessage(createEmbed);
						return CompletableFuture.completedFuture(null);
                    }
	            		
	            	case "Invader" -> {
						// Invader専用の絵文字は追加する予定はないので、Emojiのnullチェックは不要
						if (Objects.nonNull(FaceEmoji)) {
							addMessage = "侵入者が現れました。\n\n:arrow_down: 侵入者情報:arrow_down:\nスキン: " + FaceEmoji
									+ "\n\nプレイヤーネーム: " + playerName + "\n\nプレイヤーUUID: " + uuid;
							
							createEmbed = discord.createEmbed (
												addMessage,
												ColorUtil.RED.getRGB()
											);
							
							discord.sendBotMessage(createEmbed);
						}

						return CompletableFuture.completedFuture(null);
                    }
	            		
	            	case "Chat" -> {
						// Chat専用の絵文字は追加する予定はないので、Emojiのnullチェックは不要
						if (Objects.nonNull(FaceEmoji)) {
							if (config.getBoolean("Discord.ChatType", false)) {
								// 編集embedによるChatメッセージ送信
								if (Objects.isNull(chatMessageId)) {
									// 直前にEmbedによるChatメッセージを送信しなかった場合
									// EmbedChatMessageを送って、MessageIdを
									addMessage = String.format("""
										<%s%s> %s
									""",FaceEmoji, playerName, chatMessage);
									
									createEmbed = discord.createEmbed (
														addMessage,
														ColorUtil.GREEN.getRGB()
													);
									
									discord.sendBotMessageAndgetMessageId(createEmbed, true).thenAccept(messageId2 -> {
										//logger.info("Message sent with ID: " + messageId2);
										DiscordEventListener.PlayerChatMessageId = messageId2;
									});
								} else {
									//logger.info("chatMessageId: "+ chatMessageId);
									addMessage = String.format("""

									
										<%s%s> %s
									""",FaceEmoji, playerName, chatMessage);
									discord.editBotEmbed(chatMessageId, addMessage, true);
								}
							} else {
								// デフォルトのChatメッセージ送信(Webhook送信)
								builder = new WebhookMessageBuilder();
								builder.setUsername(playerName);
								builder.setAvatarUrl(avatarUrl);
								builder.setContent(chatMessage);
								
								discord.sendWebhookMessage(builder);
							}
						}

						return CompletableFuture.completedFuture(null);
                    }
	            		
	            	case "AddMember" -> {
						if (Objects.nonNull(Emoji) && Objects.nonNull(FaceEmoji)) {
							addMessage = Emoji + FaceEmoji +
									playerName + "が新規FMCメンバーになりました！:congratulations: ";
						} else {
							addMessage = playerName + "が新規FMCメンバーになりました！:congratulations: ";
						}
						
						createEmbed = discord.createEmbed (
											addMessage,
											ColorUtil.PINK.getRGB()
									);
						discord.sendBotMessage(createEmbed);
						return CompletableFuture.completedFuture(null);
                    }
	            		
	                case "Start" -> {
						if (Objects.nonNull(Emoji) && Objects.nonNull(FaceEmoji) && Objects.nonNull(messageId)) {
							addMessage = "\n\n" + Emoji + FaceEmoji + playerName + "が" +
									targetServerName+ "サーバーを起動させました。";
							discord.editBotEmbed(messageId, addMessage);
						}

						return CompletableFuture.completedFuture(null);
                    }
	
	                case "Move" -> {
						//int playTime3 = pu.getPlayerTime(player, serverInfo);
						//String convStringTime = pu.secondsToStr(playTime3);
						
						if (Objects.nonNull(Emoji) && Objects.nonNull(FaceEmoji) && Objects.nonNull(messageId)) {
							addMessage = "\n\n" + Emoji + FaceEmoji + playerName + "が" +
									currentServerName + "サーバーへ移動しました。";
							discord.editBotEmbed(messageId, addMessage);
						}

						return CompletableFuture.completedFuture(null);
                    }
	
	                case "Request" -> {
						if (Objects.nonNull(Emoji) && Objects.nonNull(FaceEmoji) && Objects.nonNull(messageId)) {
							addMessage = "\n\n" + Emoji + FaceEmoji + playerName + "が" +
									targetServerName + "サーバーの起動リクエストを送りました。";
							discord.editBotEmbed(messageId, addMessage);
						}

						return CompletableFuture.completedFuture(null);
                    }
	                    
	                case "Join" -> {
						try {
							conn = db.getConnection();
							if (Objects.nonNull(Emoji) && Objects.nonNull(FaceEmoji)) {
								// 絵文字IDをアップデートしておく
								if(Objects.nonNull(conn)) {
									ps = conn.prepareStatement("UPDATE minecraft SET emid=? WHERE uuid=?;");
									ps.setString(1, FaceEmojiId);
									ps.setString(2, uuid);
									ps.executeUpdate();
								}
								
								addMessage = Emoji + FaceEmoji +
										playerName + "が" + serverInfo.getName() +
										"サーバーに参加しました。";
							} else {
								//logger.info("Emoji ID is null");
								if(Objects.nonNull(conn)) {
									// 絵文字IDをアップデートしておく
									ps = conn.prepareStatement("UPDATE minecraft SET emid=? WHERE uuid=?;");
									ps.setString(1, null);
									ps.setString(2, uuid);
									ps.executeUpdate();
								}
								
								addMessage = playerName + "が" + serverInfo.getName() +
										"サーバーに参加しました。";
							}
							
							createEmbed = discord.createEmbed (
													addMessage,
													ColorUtil.GREEN.getRGB()
											);
							discord.sendBotMessageAndgetMessageId(createEmbed).thenAccept(messageId2 -> {
								//logger.info("Message sent with ID: " + messageId2);
								EventListener.PlayerMessageIds.put(uuid, messageId2);
							});
						} catch (SQLException | ClassNotFoundException e1) {
							logger.error("An onConnection error occurred: " + e1.getMessage());
							for (StackTraceElement element : e1.getStackTrace()) {
								logger.error(element.toString());
							}
						}

						return CompletableFuture.completedFuture(null);
                    }
                        
	                case "FirstJoin" -> {
						try {
							conn = db.getConnection();
							if (Objects.nonNull(Emoji) && Objects.nonNull(FaceEmoji)) {
								//logger.info("Emoji ID retrieved: " + emojiId);
								ps = conn.prepareStatement("INSERT INTO minecraft (name,uuid,server, emid) VALUES (?,?,?,?);");
								ps.setString(1, playerName);
								ps.setString(2, uuid);
								ps.setString(3, serverInfo.getName());
								ps.setString(4, FaceEmojiId);
								ps.executeUpdate();
								
								addMessage = Emoji + FaceEmoji +
										playerName + "が" + serverInfo.getName() +
										"サーバーに初参加です！";
							} else {
								//logger.info("Emoji ID is null");
								ps = conn.prepareStatement("INSERT INTO minecraft (name,uuid,server) VALUES (?,?,?);");
								ps.setString(1, playerName);
								ps.setString(2, uuid);
								ps.setString(3, serverInfo.getName());
								ps.executeUpdate();
								
								addMessage = playerName + "が" + serverInfo.getName() +
										"サーバーに初参加です！";
							}
							
							createEmbed = discord.createEmbed (
											addMessage,
											ColorUtil.ORANGE.getRGB()
										);
							discord.sendBotMessageAndgetMessageId(createEmbed).thenAccept(messageId2 -> {
								//logger.info("Message sent with ID: " + messageId2);
								EventListener.PlayerMessageIds.put(uuid, messageId2);
							});
						} catch (SQLException | ClassNotFoundException e1) {
							logger.error("An onConnection error occurred: " + e1.getMessage());
							for (StackTraceElement element : e1.getStackTrace()) {
								logger.error(element.toString());
							}
						}

						return CompletableFuture.completedFuture(null);
                    }
	
	                case "RequestOK" -> {
						if (Objects.nonNull(Emoji) && Objects.nonNull(FaceEmoji)) {
							addMessage = Emoji + "管理者が" + FaceEmoji + playerName + "の" +
									targetServerName + "サーバー起動リクエストを受諾しました。";
							sendEmbed = discord.createEmbed (
												addMessage,
												ColorUtil.GREEN.getRGB()
											);
							discord.sendBotMessage(sendEmbed);
						}

						return CompletableFuture.completedFuture(null);
                    }
	
	                case "RequestCancel" -> {
						if (Objects.nonNull(Emoji) && Objects.nonNull(FaceEmoji)) {
							addMessage = Emoji + "管理者が" + FaceEmoji + playerName + "の" +
									targetServerName + "サーバー起動リクエストをキャンセルしました。";
							sendEmbed = discord.createEmbed (
												addMessage,
												ColorUtil.RED.getRGB()
											);
							discord.sendBotMessage(sendEmbed);
						}

						return CompletableFuture.completedFuture(null);
                    }
	
	                case "RequestNoRes" -> {
						if (Objects.nonNull(Emoji) && Objects.nonNull(FaceEmoji)) {
							addMessage = Emoji + "管理者が" + FaceEmoji + playerName + "の" +
									targetServerName + "サーバー起動リクエストに対して、応答しませんでした。";
							sendEmbed = discord.createEmbed (
												addMessage,
												ColorUtil.BLUE.getRGB()
											);
							discord.sendBotMessage(sendEmbed);
						}

						return CompletableFuture.completedFuture(null);
					}
	
	                default -> {
						return CompletableFuture.completedFuture(null);
					}
	            }
	        } catch (InterruptedException | ExecutionException e1) {
	            logger.error("A InterruptedException | ExecutionException error occurred: " + e1.getMessage());
				for (StackTraceElement element : e1.getStackTrace()) {
					logger.error(element.toString());
				}

	            return CompletableFuture.completedFuture(null);
	        }
	    });
	}
	
	@Override
	public CompletableFuture<Void> AddEmbedSomeMessage(String type, Player player, String serverName) {
		return AddEmbedSomeMessage(type, player, null, serverName, null, 0, null, null);
	}
	
	@Override
	public CompletableFuture<Void> AddEmbedSomeMessage(String type, Player player, ServerInfo serverInfo) {
		return AddEmbedSomeMessage(type, player, serverInfo, null, null, 0, null, null);
	}
	
	@Override
	public CompletableFuture<Void> AddEmbedSomeMessage(String type, Player player) {
		return AddEmbedSomeMessage(type, player, null, null, null, 0, null, null);
	}
	
	@Override
	public CompletableFuture<Void> AddEmbedSomeMessage(String type, String alternativePlayerName) {
		return AddEmbedSomeMessage(type, null, null, null, alternativePlayerName, 0, null, null);
	}
	
	@Override
	public CompletableFuture<Void> AddEmbedSomeMessage(String type, String alternativePlayerName, String serverName) {
		return AddEmbedSomeMessage(type, null, null, serverName, alternativePlayerName, 0, null, null);
	}
	
	@Override
	public CompletableFuture<Void> AddEmbedSomeMessage(String type, Player player, ServerInfo serverInfo, int playTime) {
		return AddEmbedSomeMessage(type, player, serverInfo, null, null, playTime, null, null);
	}
	
	@Override
	public CompletableFuture<Void> AddEmbedSomeMessage(String type, Player player, ServerInfo serverInfo, String chatMessage) {
		return AddEmbedSomeMessage(type, player, serverInfo, null, null, 0, chatMessage, null);
	}
	
	@Override
	public CompletableFuture<Void> AddEmbedSomeMessage(String type) {
		return AddEmbedSomeMessage(type, null, null, null, null, 0, null, null);
	}
	
	@Override
	public CompletableFuture<Void> AddEmbedSomeMessage(String type, UUID playerUUID) {
		return AddEmbedSomeMessage(type, null, null, null, null, 0, null, playerUUID);
	}
}
