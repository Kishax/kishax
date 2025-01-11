package keyp.forev.fmc.velocity.discord;

import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;

import keyp.forev.fmc.common.database.Database;
import keyp.forev.fmc.common.util.ColorUtil;
import keyp.forev.fmc.common.util.JavaUtils;
import keyp.forev.fmc.common.util.PlayerUtils;
import keyp.forev.fmc.velocity.Main;
import keyp.forev.fmc.velocity.server.cmd.sub.Maintenance;
import keyp.forev.fmc.velocity.server.events.EventListener;
import keyp.forev.fmc.velocity.util.config.VelocityConfig;
import com.google.inject.Inject;

public class MessageEditor {
	public final Main plugin;
	private final ProxyServer server;
	private final Logger logger;
	private final VelocityConfig config;
	private final Database db;
	private final Discord discord;
	private final EmojiManager emoji;
	private final PlayerUtils pu;
	private final Webhooker webhooker;
	private String avatarUrl = null, addMessage = null, 
			Emoji = null, FaceEmoji = null, targetServerName = null,
			uuid = null, playerName = null, currentServerName = null;
	private CompletableFuture<String> EmojiFutureId = null, FaceEmojiFutureId = null;
	@Inject
	public MessageEditor (
		Main plugin, Logger logger, ProxyServer server,
		VelocityConfig config, Database db, Discord discord,
		EmojiManager emoji, PlayerUtils pu, Webhooker webhooker
	) {
		this.plugin = plugin;
		this.logger = logger;
		this.server = server;
		this.config = config;
		this.db = db;
		this.discord = discord;
		this.emoji = emoji;
		this.pu = pu;
		this.webhooker = webhooker;
	}
	
	public CompletableFuture<Void> AddEmbedSomeMessage(String type, Player player, String serverName) throws Exception {
		return AddEmbedSomeMessage(type, player, null, serverName, null, null, null);
	}
	
	public CompletableFuture<Void> AddEmbedSomeMessage(String type, Player player, ServerInfo serverInfo) throws Exception {
		return AddEmbedSomeMessage(type, player, serverInfo, null, null, null, null);
	}
	
	public CompletableFuture<Void> AddEmbedSomeMessage(String type, Player player) throws Exception {
		return AddEmbedSomeMessage(type, player, null, null, null, null, null);
	}
	
	public CompletableFuture<Void> AddEmbedSomeMessage(String type, String alternativePlayerName) throws Exception {
		return AddEmbedSomeMessage(type, null, null, null, alternativePlayerName, null, null);
	}
	
	public CompletableFuture<Void> AddEmbedSomeMessage(String type, String alternativePlayerName, String serverName) throws	Exception {
		return AddEmbedSomeMessage(type, null, null, serverName, alternativePlayerName, null, null);
	}
	
	public CompletableFuture<Void> AddEmbedSomeMessage(String type, Player player, ServerInfo serverInfo, String chatMessage) throws Exception {
		return AddEmbedSomeMessage(type, player, serverInfo, null, null, chatMessage, null);
	}
	
	public CompletableFuture<Void> AddEmbedSomeMessage(String type) throws Exception {
		return AddEmbedSomeMessage(type, null, null, null, null, null, null);
	}
	
	public CompletableFuture<Void> AddEmbedSomeMessage(String type, UUID playerUUID) throws Exception {
		return AddEmbedSomeMessage(type, null, null, null, null, null, playerUUID);
	}

	private CompletableFuture<Void> AddEmbedSomeMessage(String type, Player player, ServerInfo serverInfo, String serverName, String alternativePlayerName, String chatMessage, UUID playerUUID) throws Exception{
		boolean discordMessageType = config.getBoolean("Discord.MessageType", false);
		if (player == null) {
			if (Objects.nonNull(alternativePlayerName)) {
				uuid = pu.getPlayerUUIDByNameFromDB(alternativePlayerName);
				playerName = alternativePlayerName;
			} else if (Objects.nonNull(playerUUID)) {
				uuid = playerUUID.toString();
				playerName = pu.getPlayerNameByUUIDFromDB(uuid);
			}
		} else {
			uuid = player.getUniqueId().toString();
			playerName = player.getUsername();
		}

		// サイレント中のプレイヤーはDiscordにメッセージを送信しない
		try (Connection conn = db.getConnection();
			PreparedStatement ps = conn.prepareStatement("SELECT silent FROM members WHERE uuid=?;")) {
			ps.setString(1, uuid);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					if (rs.getBoolean("silent")) {
						return CompletableFuture.completedFuture(null);
					}
				}
			}
		} catch (SQLException | ClassNotFoundException e) {
			logger.error("An onConnection error occurred: " + e.getMessage());
			for (StackTraceElement element : e.getStackTrace()) {
				logger.error(element.toString());
			}
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
				.thenCompose((var _p) -> {
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
	            String chatMessageId = DiscordEventListener.playerChatMessageId;
	            addMessage = null;
	            switch (type) {
	            	case "End" -> {
						List<CompletableFuture<Void>> futures = new ArrayList<>();
						for (Player eachPlayer : server.getAllPlayers()) {
							CompletableFuture<Void> future = eachPlayer.getCurrentServer()
								.map(serverConnection -> {
									RegisteredServer registerServer = serverConnection.getServer();
									ServerInfo playerServerInfo = registerServer.getServerInfo();
									// AddEmbedSomeMessageがCompletableFuture<Void>を返すと仮定
									try {
										return AddEmbedSomeMessage("Exit", eachPlayer, playerServerInfo);
									} catch (Exception e) {
										logger.error("An onConnection error occurred: " + e.getMessage());
										for (StackTraceElement element : e.getStackTrace()) {
											logger.error(element.toString());
										}
										throw new RuntimeException(e);
									}
								}).orElse(CompletableFuture.completedFuture(null)); // サーバーが取得できない場合は即完了するFuture
							futures.add(future);
						}
						CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
							.thenRun(() -> {
								// 全てのAddEmbedSomeMessageの処理が完了した後に実行される
								discord.logoutDiscordBot().thenRun(() -> server.shutdown());
							});
						return CompletableFuture.completedFuture(null);
                    }
	            	case "Exit" -> {
						try (Connection conn = db.getConnection()) {
							if (Objects.nonNull(Emoji) && Objects.nonNull(FaceEmoji) && Objects.nonNull(messageId)) {
								int hubJoinLogId = EventListener.playerJoinHubIds.getOrDefault(player, 0);
								if (hubJoinLogId != 0) {
									int playTime = db.getPlayerTime(conn, uuid, hubJoinLogId);
									EventListener.playerJoinHubIds.remove(player);
									db.insertLog(conn, "INSERT INTO `log` (name, uuid, server, quit, playtime) VALUES (?,?,?,?,?);", new Object[] {playerName, playerUUID, currentServerName, true, playTime});
									String convStringTime = JavaUtils.secondsToStr(playTime);
									CompletableFuture<Void> editFuture = CompletableFuture.completedFuture(null);
									try {
										if (!Main.isVelocity) {
											String EndEmojiName = config.getString("Discord.EndEmojiName","");
											editFuture = emoji.createOrgetEmojiId(EndEmojiName)
												.thenAccept(EndEmojiId -> {
													if (Objects.nonNull(EndEmojiId)) {
														String EndEmoji = emoji.getEmojiString(EndEmojiName, EndEmojiId);
														addMessage = MessageFormat.format("\n\n{0}プロキシサーバーが停止しました。\n\n{1}{2}{3}が{4}サーバーから退出しました。\n\n:alarm_clock: プレイ時間: {5}", EndEmoji, Emoji, FaceEmoji, playerName, currentServerName, convStringTime);
													}
												});
											return editFuture.thenCompose(_pp -> {
												try {
													return discord.editBotEmbed(messageId, addMessage);
												} catch (Exception e) {
													logger.error("An Exception error occurred: " + e.getMessage());
													for (StackTraceElement element : e.getStackTrace()) {
														logger.error(element.toString());
													}
												}
												return CompletableFuture.completedFuture(null);
											});
										} else if (Maintenance.isMente) {
											Maintenance.isMente = false;
											addMessage = MessageFormat.format("\n\n:red_circle: メンテナンスモードが有効になりました。\n\n{0}{1}{2}が{3}サーバーから退出しました。\n\n:alarm_clock: プレイ時間: {4}", Emoji, FaceEmoji, playerName, currentServerName, convStringTime);
											if (player instanceof Player) {
												if (!player.hasPermission("group.super-admin")) {
													EventListener.PlayerMessageIds.remove(uuid);
													return editFuture.thenCompose(_pp -> {
														try {
															return discord.editBotEmbed(messageId, addMessage);
														} catch (Exception e) {
															logger.error("An Exception error occurred: " + e.getMessage());
															for (StackTraceElement element : e.getStackTrace()) {
																logger.error(element.toString());
															}
														}
														return CompletableFuture.completedFuture(null);
													});
												}
											}
										} else {
											addMessage = MessageFormat.format("\n\n{0}{1}{2}が{3}サーバーから退出しました。\n\n:alarm_clock: プレイ時間: {4}", Emoji, FaceEmoji, playerName, currentServerName, convStringTime);
											EventListener.PlayerMessageIds.remove(uuid);
											return editFuture.thenCompose(_pp -> {
												try {
													return discord.editBotEmbed(messageId, addMessage);
												} catch (Exception e) {
													logger.error("An Exception error occurred: " + e.getMessage());
													for (StackTraceElement element : e.getStackTrace()) {
														logger.error(element.toString());
													}
												}
												return CompletableFuture.completedFuture(null);
											});
										}
										return editFuture.thenCompose(_pp -> {
											try {
												return discord.editBotEmbed(messageId, addMessage);
											} catch (Exception e) {
												logger.error("An Exception error occurred: " + e.getMessage());
												for (StackTraceElement element : e.getStackTrace()) {
													logger.error(element.toString());
												}
											}
											return CompletableFuture.completedFuture(null);
										});
									} catch (Exception e) {
										logger.error("An Exception error occurred: " + e.getMessage());
										for (StackTraceElement element : e.getStackTrace()) {
											logger.error(element.toString());
										}
										return CompletableFuture.completedFuture(null);
									}
								}
							}
						} catch (SQLException | ClassNotFoundException e1) {
							logger.error("An onConnection error occurred: " + e1.getMessage());
							for (StackTraceElement element : e1.getStackTrace()) {
								logger.error(element.toString());
							}
						}
						return CompletableFuture.completedFuture(null);
                    }
	            	case "MenteOn" -> {
						if (Objects.nonNull(Emoji)) {
							addMessage = MessageFormat.format("{0}メンテナンスモードが有効になりました。\nいまは遊べないカッ...", Emoji);
						} else {
							addMessage = "メンテナンスモードが有効になりました。";
						}
						try {
							Object createEmbed = discord.createEmbed(addMessage, ColorUtil.RED.getRGB());
							discord.sendBotMessage(createEmbed);
							for (Player eachPlayer : server.getAllPlayers()) {
								// プレイヤーの現在のサーバーを取得
								Optional<ServerConnection> optionalServerConnection = eachPlayer.getCurrentServer();
								if (optionalServerConnection.isPresent()) {
									ServerConnection serverConnection = optionalServerConnection.get();
									RegisteredServer registerServer = serverConnection.getServer();
									ServerInfo playerServerInfo = registerServer.getServerInfo();
									// AddEmbedSomeMessageがCompletableFuture<Void>を返すと仮定
									try {
										AddEmbedSomeMessage("Exit", eachPlayer, playerServerInfo);
									} catch (Exception e) {
										logger.error("An onConnection error occurred: " + e.getMessage());
										for (StackTraceElement element : e.getStackTrace()) {
											logger.error(element.toString());
										}
									}
								}
							}
						} catch (Exception e) {
							logger.error("An Exception error occurred: " + e.getMessage());
							for (StackTraceElement element : e.getStackTrace()) {
								logger.error(element.toString());
							}
						}
						return CompletableFuture.completedFuture(null);
                    }
	            	case "MenteOff" -> {
						if (Objects.nonNull(Emoji)) {
							addMessage = MessageFormat.format("{0}メンテナンスモードが無効になりました。\nまだまだ遊べるドン！", Emoji);
						} else {
							addMessage = "メンテナンスモードが無効になりました。";
						}
						try {
							Object createEmbed = discord.createEmbed(addMessage, ColorUtil.GREEN.getRGB());
							discord.sendBotMessage(createEmbed);
						} catch (Exception e) {
							logger.error("An Exception error occurred: " + e.getMessage());
							for (StackTraceElement element : e.getStackTrace()) {
								logger.error(element.toString());
							}
						}
						return CompletableFuture.completedFuture(null);
                    }
	            	case "Invader" -> {
						// Invader専用の絵文字は追加する予定はないので、Emojiのnullチェックは不要
						if (Objects.nonNull(FaceEmoji)) {
							addMessage = MessageFormat.format("侵入者が現れました。\n\n:arrow_down: 侵入者情報:arrow_down:\nフェイスアイコン: {0}\n\nプレイヤーネーム: {1}\n\nプレイヤーUUID: {2}", FaceEmoji, playerName, uuid);
							try {
								Object createEmbed = discord.createEmbed(addMessage, ColorUtil.RED.getRGB());
								discord.sendBotMessage(createEmbed);
							} catch (Exception e) {
								logger.error("An Exception error occurred: " + e.getMessage());
								for (StackTraceElement element : e.getStackTrace()) {
									logger.error(element.toString());
								}
							}
						}
						return CompletableFuture.completedFuture(null);
                    }
	            	case "Chat" -> {
						// Chat専用の絵文字は追加する予定はないので、Emojiのnullチェックは不要
						if (Objects.nonNull(FaceEmoji)) {
							try {
								if (discordMessageType) {
									//logger.info("embed mode");
									// 編集embedによるChatメッセージ送信
									if (chatMessageId == null) {
										// 直前にEmbedによるChatメッセージを送信しなかった場合
										// EmbedChatMessageを送って、MessageIdを
										addMessage = MessageFormat.format("<{0}{1}> {2}", FaceEmoji, playerName, chatMessage);
										//logger.info("Creating embed...");
										Object createEmbed = discord.createEmbed(addMessage, ColorUtil.GREEN.getRGB());
										//logger.info("Sending message...");
										return discord.sendBotMessageAndgetMessageId(createEmbed, true).thenAccept(messageId2 -> {
											DiscordEventListener.playerChatMessageId = messageId2;
										}).exceptionally(e -> {
											logger.error("An Exception error occurred: " + e.getMessage());
											for (StackTraceElement element : e.getStackTrace()) {
												logger.error(element.toString());
											}
											return null;
										});
									} else {
										addMessage = MessageFormat.format("\n\n<{0}{1}> {2}", FaceEmoji, playerName, chatMessage);
										return discord.editBotEmbed(chatMessageId, addMessage, true)
											.exceptionally(e -> {
												logger.error("An Exception error occurred: " + e.getMessage());
												for (StackTraceElement element : e.getStackTrace()) {
													logger.error(element.toString());
												}
												return null;
											});
									}
								} else {
									logger.info("webhook mode");
									// Webhook経由で送信
									return CompletableFuture.runAsync(() -> {
										try {
											webhooker.sendWebhookMessage(playerName, avatarUrl, chatMessage);
										} catch (Exception e) {
											logger.error("Failed to send webhook message", e);
											throw new CompletionException(e);
										}
									}).exceptionally(e -> {
										logger.error("An Exception error occurred: " + e.getMessage());
										for (StackTraceElement element : e.getStackTrace()) {
											logger.error(element.toString());
										}
										return null;
									});
								}
							} catch (Exception e) {
								logger.error("An Exception error occurred: " + e.getMessage());
								for (StackTraceElement element : e.getStackTrace()) {
									logger.error(element.toString());
								}
							}
						}
						return CompletableFuture.completedFuture(null);
                    }
	            	case "AddMember" -> {
						if (Objects.nonNull(Emoji) && Objects.nonNull(FaceEmoji)) {
							addMessage = MessageFormat.format("{0}{1}{2}が新規FMCメンバーになりました！:congratulations:", Emoji, FaceEmoji, playerName);
						} else {
							addMessage = MessageFormat.format("{0}が新規FMCメンバーになりました！:congratulations:", playerName);
						}
						try {
							Object createEmbed = discord.createEmbed(addMessage, ColorUtil.PINK.getRGB());
							discord.sendBotMessage(createEmbed);
						} catch (Exception e) {
							logger.error("An Exception error occurred: " + e.getMessage());
							for (StackTraceElement element : e.getStackTrace()) {
								logger.error(element.toString());
							}
						}
						return CompletableFuture.completedFuture(null);
                    }
	            	case "Stop" -> {
						if (Objects.nonNull(Emoji) && Objects.nonNull(FaceEmoji) && Objects.nonNull(messageId)) {
							addMessage = MessageFormat.format("\n\n{0}{1}{2}が{3}サーバーを停止させました。", Emoji, FaceEmoji, playerName, targetServerName);
							try {
								discord.editBotEmbed(messageId, addMessage);
							} catch (Exception e) {
								logger.error("An Exception error occurred: " + e.getMessage());
								for (StackTraceElement element : e.getStackTrace()) {
									logger.error(element.toString());
								}
							}
						}
						return CompletableFuture.completedFuture(null);
					}
	                case "Start" -> {
						if (Objects.nonNull(Emoji) && Objects.nonNull(FaceEmoji)) {
							addMessage = MessageFormat.format("{0}{1}{2}が{3}サーバーを起動させました。", Emoji, FaceEmoji, playerName, targetServerName);
							try {
								Object createEmbed = discord.createEmbed(addMessage, ColorUtil.GREEN.getRGB());
								if (messageId != null) {
									addMessage = "\n\n" + addMessage;
									discord.editBotEmbed(messageId, addMessage);
								} else {
									discord.sendBotMessageAndgetMessageId(createEmbed).thenAccept(messageId2 -> {
										EventListener.PlayerMessageIds.put(uuid, messageId2);
									});
								}
							} catch (Exception e) {
								logger.error("An Exception error occurred: " + e.getMessage());
								for (StackTraceElement element : e.getStackTrace()) {
									logger.error(element.toString());
								}
							}
						}
						return CompletableFuture.completedFuture(null);
                    }
	                case "Move" -> {
						if (Objects.nonNull(Emoji) && Objects.nonNull(FaceEmoji)) {
							addMessage = MessageFormat.format("{0}{1}{2}が{3}サーバーへ移動しました。", Emoji, FaceEmoji, playerName, currentServerName);
							try {
								Object createEmbed = discord.createEmbed(addMessage, ColorUtil.GREEN.getRGB());
								if (messageId != null) {
									addMessage = "\n\n" + addMessage;
									discord.editBotEmbed(messageId, addMessage);
								} else {
									discord.sendBotMessageAndgetMessageId(createEmbed).thenAccept(messageId2 -> {
										EventListener.PlayerMessageIds.put(uuid, messageId2);
									});
								}
							} catch (Exception e) {
								logger.error("An Exception error occurred: " + e.getMessage());
								for (StackTraceElement element : e.getStackTrace()) {
									logger.error(element.toString());
								}
							}
						}
						return CompletableFuture.completedFuture(null);
                    }
	                case "Request" -> {
						if (Objects.nonNull(Emoji) && Objects.nonNull(FaceEmoji)) {
							addMessage = MessageFormat.format("{0}{1}{2}が{3}サーバーの起動リクエストを送りました。", Emoji, FaceEmoji, playerName, targetServerName);
							try {
								Object createEmbed = discord.createEmbed(addMessage, ColorUtil.GREEN.getRGB());
								if (messageId != null) {
									addMessage = "\n\n" + addMessage;
									discord.editBotEmbed(messageId, addMessage);
								} else {
									discord.sendBotMessageAndgetMessageId(createEmbed).thenAccept(messageId2 -> {
										EventListener.PlayerMessageIds.put(uuid, messageId2);
									});
								}
							} catch (Exception e) {
								logger.error("An Exception error occurred: " + e.getMessage());
								for (StackTraceElement element : e.getStackTrace()) {
									logger.error(element.toString());
								}
							}
						}
						return CompletableFuture.completedFuture(null);
                    }
	                case "Join" -> {
						String query = "UPDATE members SET emid=? WHERE uuid=?;";
						try (Connection conn = db.getConnection();
							 PreparedStatement ps = conn.prepareStatement(query)) {
							if (Objects.nonNull(Emoji) && Objects.nonNull(FaceEmoji)) {
								ps.setString(1, FaceEmojiId);
								addMessage = MessageFormat.format("{0}{1}{2}が{3}サーバーに参加しました。", Emoji, FaceEmoji, playerName, currentServerName);
							} else {
								ps.setString(1, null);
								addMessage = MessageFormat.format("{0}が{1}サーバーに参加しました。", playerName, currentServerName);
							}
							ps.setString(2, uuid);
							int rsAffected = ps.executeUpdate();
							if (rsAffected > 0) {
								try {
									Object createEmbed = discord.createEmbed(addMessage, ColorUtil.GREEN.getRGB());
									if (messageId != null) {
										addMessage = "\n\n" + addMessage;
										discord.editBotEmbed(messageId, addMessage);
									} else {
										discord.sendBotMessageAndgetMessageId(createEmbed).thenAccept(messageId2 -> {
											EventListener.PlayerMessageIds.put(uuid, messageId2);
										});
									}
								} catch (Exception e) {
									logger.error("An Exception error occurred: " + e.getMessage());
									for (StackTraceElement element : e.getStackTrace()) {
										logger.error(element.toString());
									}
								}
							}
						} catch (SQLException | ClassNotFoundException e1) {
							logger.error("An onConnection error occurred: " + e1.getMessage());
							for (StackTraceElement element : e1.getStackTrace()) {
								logger.error(element.toString());
							}
						}
						return CompletableFuture.completedFuture(null);
                    }
	                case "FirstJoin" -> {
						String query = "INSERT INTO members (name, uuid, server, emid) VALUES (?, ?, ?, ?);";
						try (Connection conn = db.getConnection();
							 PreparedStatement ps = conn.prepareStatement(query)) {
							ps.setString(1, playerName);
							ps.setString(2, uuid);
							ps.setString(3, currentServerName);
							if (Objects.nonNull(Emoji) && Objects.nonNull(FaceEmoji)) {
								ps.setString(4, FaceEmojiId);
								addMessage = MessageFormat.format("{0}{1}{2}が{3}サーバーに初参加です！", Emoji, FaceEmoji, playerName, currentServerName);
							} else {
								ps.setString(4, null);
								addMessage = MessageFormat.format("{0}が{1}サーバーに初参加です！", playerName, currentServerName);
							}
							int rsAffected = ps.executeUpdate();
							if (rsAffected > 0) {
								try {
									Object createEmbed = discord.createEmbed(addMessage, ColorUtil.ORANGE.getRGB());
									discord.sendBotMessageAndgetMessageId(createEmbed).thenAccept(messageId2 -> {
										EventListener.PlayerMessageIds.put(uuid, messageId2);
									});
								} catch (Exception e) {
									logger.error("An Exception error occurred: " + e.getMessage());
									for (StackTraceElement element : e.getStackTrace()) {
										logger.error(element.toString());
									}
								}
							}
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
							try {
								addMessage = MessageFormat.format("{0}管理者が{1}{2}の{3}サーバー起動リクエストを受諾しました。", Emoji, FaceEmoji, playerName, targetServerName);
								Object sendEmbed = discord.createEmbed(addMessage, ColorUtil.GREEN.getRGB());
								discord.sendBotMessage(sendEmbed);
							} catch (Exception e) {
								logger.error("An Exception error occurred: " + e.getMessage());
								for (StackTraceElement element : e.getStackTrace()) {
									logger.error(element.toString());
								}
							}
						}
						return CompletableFuture.completedFuture(null);
                    }
	                case "RequestCancel" -> {
						if (Objects.nonNull(Emoji) && Objects.nonNull(FaceEmoji)) {
							try {
								addMessage = MessageFormat.format("{0}管理者が{1}{2}の{3}サーバー起動リクエストをキャンセルしました。", Emoji, FaceEmoji, playerName, targetServerName);
								Object sendEmbed = discord.createEmbed(addMessage, ColorUtil.RED.getRGB());
								discord.sendBotMessage(sendEmbed);
							} catch (Exception e) {
								logger.error("An Exception error occurred: " + e.getMessage());
								for (StackTraceElement element : e.getStackTrace()) {
									logger.error(element.toString());
								}
							}
						}
						return CompletableFuture.completedFuture(null);
                    }
	                case "RequestNoRes" -> {
						if (Objects.nonNull(Emoji) && Objects.nonNull(FaceEmoji)) {
							try {
								addMessage = MessageFormat.format("{0}管理者が{1}{2}の{3}サーバー起動リクエストに対して、応答しませんでした。", Emoji, FaceEmoji, playerName, targetServerName);
								Object sendEmbed = discord.createEmbed(addMessage, ColorUtil.BLUE.getRGB());
								discord.sendBotMessage(sendEmbed);
							} catch (Exception e) {
								logger.error("An Exception error occurred: " + e.getMessage());
								for (StackTraceElement element : e.getStackTrace()) {
									logger.error(element.toString());
								}
							}
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
	    }).exceptionally(e -> {
	        logger.error("An Exception error occurred: " + e.getMessage());
			for (StackTraceElement element : e.getStackTrace()) {
				logger.error(element.toString());
			}
	        return null;
		});
	}
}
