package keyp.forev.fmc.velocity.discord;

import java.io.IOException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.slf4j.Logger;

import com.google.inject.Provider;

import keyp.forev.fmc.common.database.Database;
import keyp.forev.fmc.common.settings.FMCSettings;
import keyp.forev.fmc.common.socket.SocketSwitch;
import keyp.forev.fmc.common.util.OTPGenerator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import keyp.forev.fmc.velocity.discord.interfaces.ReflectionHandler;
import keyp.forev.fmc.velocity.server.BroadCast;
import keyp.forev.fmc.velocity.server.cmd.sub.VelocityRequest;
import keyp.forev.fmc.velocity.server.cmd.sub.interfaces.Request;
import keyp.forev.fmc.velocity.util.config.VelocityConfig;
import com.google.inject.Inject;

public class DiscordEventListener {
	public static String playerChatMessageId = null;
	private final Logger logger;
	private final VelocityConfig config;
	private final Database db;
	private final BroadCast bc;
	private final MessageEditor discordME;
	private final Request req;
	private final Discord discord;
	private final Provider<SocketSwitch> sswProvider;
	@Inject
	public DiscordEventListener(Logger logger, VelocityConfig config, Database db, BroadCast bc, MessageEditor discordME, Request req, Discord discord, Provider<SocketSwitch> sswProvider) throws ClassNotFoundException {
		this.logger = logger;
		this.config = config;
		this.db = db;
		this.bc = bc;
		this.discordME = discordME;
		this.req = req;
		this.discord = discord;
		this.sswProvider = sswProvider;
	}

	@ReflectionHandler(event = "net.dv8tion.jda.api.events.message.MessageUpdateEvent")
	public void onMessageUpdate(@Nonnull Object event) {
        try {
            String ruleChannelId = Long.toString(config.getLong("Discord.Rule.ChannelId", 0));
            String ruleMessageId = Long.toString(config.getLong("Discord.Rule.MessageId", 0));

            Method getChannelMethod = event.getClass().getMethod("getChannel");
            Object channel = getChannelMethod.invoke(event);

            Method getIdMethod = channel.getClass().getMethod("getId");
            String channelId = (String) getIdMethod.invoke(channel);

            Method getMessageIdMethod = event.getClass().getMethod("getMessageId");
            String messageId = (String) getMessageIdMethod.invoke(event);

            if (channelId.equals(ruleChannelId) && messageId.equals(ruleMessageId)) {
                Method getMessageMethod = event.getClass().getMethod("getMessage");
                Object message = getMessageMethod.invoke(event);

                Method getContentDisplayMethod = message.getClass().getMethod("getContentDisplay");
                String newContent = (String) getContentDisplayMethod.invoke(message);

                try (Connection conn = db.getConnection()) {
                    db.updateLog(conn, "UPDATE settings SET value = ? WHERE name = ?;", new Object[]{newContent, FMCSettings.RULEBOOK_CONTENT.getColumnKey()});
                    SocketSwitch ssw = sswProvider.get();
                    ssw.sendSpigotServer(conn, "RulebookSync");
                    logger.info("detecting rulebook update.");
                    logger.info("updated rulebook content: {}", newContent);
                } catch (SQLException | ClassNotFoundException e) {
                    logger.error("An SQLException | ClassNotFoundException error occurred: " + e.getMessage());
                    for (StackTraceElement element : e.getStackTrace()) {
                        logger.error(element.toString());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("An error occurred: " + e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                logger.error(element.toString());
            }
        }
    }

	@ReflectionHandler(event = "net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent")
    public void onSlashCommandInteraction(@Nonnull Object event) throws Exception {
		Method getUserMethod = event.getClass().getMethod("getUser");
		Method getMemberMethod = event.getClass().getMethod("getMember");
		Method getGuildMethod = event.getClass().getMethod("getGuild");

		Object user = getUserMethod.invoke(event);
		Object member = getMemberMethod.invoke(event);
		Object guild = getGuildMethod.invoke(event);

		Method getAsMentationMethod = user.getClass().getMethod("getAsMention");
		String userMention = (String) getAsMentationMethod.invoke(user);

		if (member == null) return;
		if (guild == null) return;

		Method getIdMethod = member.getClass().getMethod("getId");
		Method getNicknameMethod = member.getClass().getMethod("getNickname");
		Method getNameMethod = user.getClass().getMethod("getName");

		String userId = (String) getIdMethod.invoke(member),
			userName = (String) getNicknameMethod.invoke(member),
			userName2 = (String) getNameMethod.invoke(user),
			userName12 = userName != null ? userName : userName2;

		Method getRolesMethod = member.getClass().getMethod("getRoles");

		List<?> roles = (List<?>) getRolesMethod.invoke(member);

		Method getcmdName = event.getClass().getMethod("getName");
		String cmdName = (String) getcmdName.invoke(event);

		if (cmdName.equals("fmc")) {
			Method getSubcommandName = event.getClass().getMethod("getSubcommandName");
			String subcommandName = (String) getSubcommandName.invoke(event);
			if (subcommandName == null) return;
			switch (subcommandName) {
				case "syncrulebook" -> {
					discord.getMessageContent().thenAccept(content -> {
						try {
							Method getRoleByIdMethod = guild.getClass().getMethod("getRoleById", long.class);
							Object adCraRole = getRoleByIdMethod.invoke(guild, config.getLong("Discord.AdCraRoleId"));
							if (!roles.contains(adCraRole)) {
								replyMessage(event, userMention + " あなたはこの操作を行う権限がありません。", true);
								return;
							}
							logger.info("メッセージの内容: {}", content);
							if (content != null) {
								try (Connection conn = db.getConnection()) {
									db.updateLog(conn, "UPDATE settings SET value = ? WHERE name = ?;", new Object[] {content, FMCSettings.RULEBOOK_CONTENT.getColumnKey()});
									SocketSwitch ssw = sswProvider.get();
									ssw.sendSpigotServer(conn, "RulebookSync");
									replyMessage(event, "ルールブックを更新しました。", true);
								} catch (SQLException | ClassNotFoundException e1) {
									replyMessage(event, "データベースに接続できませんでした。", true);
									logger.error("An SQLException | ClassNotFoundException error occurred: " + e1.getMessage());
									for (StackTraceElement element : e1.getStackTrace()) {
										logger.error(element.toString());
									}
								}
							} else {
								replyMessage(event, "メッセージの内容が取得できませんでした。", true);
							}
						} catch (Exception e1) {
							logger.error("An error occurred: " + e1.getMessage());
							for (StackTraceElement element : e1.getStackTrace()) {
								logger.error(element.toString());
							}
						}
					}).exceptionally(throwable -> {
						try {
							replyMessage(event, "メッセージの取得に失敗しました。", true);
						} catch (Exception e1) {
							logger.error("An error occurred: " + e1.getMessage());
							for (StackTraceElement element : e1.getStackTrace()) {
								logger.error(element.toString());
							}
						}
						return null;
					});
				}
				case "image_add_q" -> {
					try (Connection conn = db.getConnection()) {
						int limitUploadTimes = FMCSettings.DISCORD_IMAGE_LIMIT_TIMES.getIntValue(),
							userUploadTimes = getDiscordUserTodayRegisterImageMetaTimes(conn, userId),
							thisTimes = userUploadTimes + 1;
						if (thisTimes >= limitUploadTimes) {
							replyMessage(event, "1日の登録回数は"+limitUploadTimes+"回までです。", true);
							return;
						}

						Method getOptionMethod = event.getClass().getMethod("getOption", String.class);
						Object urlObj = getOptionMethod.invoke(event, "url"),
							titleObj = getOptionMethod.invoke(event, "title"),
							commentObj = getOptionMethod.invoke(event, "comment"),
							imageObj = getOptionMethod.invoke(event, "image");

						String url = urlObj != null ? (String) urlObj.getClass().getMethod("getAsString").invoke(urlObj) : null,
							title = titleObj != null ? (String) titleObj.getClass().getMethod("getAsString").invoke(titleObj) : "無名のタイトル",
							comment = commentObj != null ? (String) commentObj.getClass().getMethod("getAsString").invoke(commentObj) : "コメントなし";
						Object attachment = imageObj != null ? imageObj.getClass().getMethod("getAsAttachment").invoke(imageObj) : null;

						if (url == null && attachment == null) {
							replyMessage(event, "画像URLまたは画像を指定してください。", true);
							return;
						}
						if (url != null && attachment != null) {
							replyMessage(event, "画像URLと画像の両方を指定することはできません。", true);
							return;
						}
						if (attachment != null) {
							Method getUrlMethod = attachment.getClass().getMethod("getUrl");
							url = (String) getUrlMethod.invoke(attachment);
						}
						LocalDate now = LocalDate.now();
						String otp = OTPGenerator.generateOTP(6);
						db.insertLog(conn, "INSERT INTO images (name, title, url, comment, otp, d, dname, did, date, locked, locked_action) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);", new Object[] {userName12, title, url, comment, otp, true, userName12, userId, java.sql.Date.valueOf(now), true, false});

						replyMessage(event, "画像メタデータを登録しました。(" + thisTimes + "/" + limitUploadTimes + ")\nワンタイムパスワード: "+otp+"\nマイクラ画像マップ取得コマンド: ```/q "+otp+"```\nこのコマンドをFMCのマイクラサーバー上で実行してください。", true);

						logger.info("(Discord) 画像メタデータを登録しました。");
						logger.info("ユーザー: {}\n試行: {}", (userName != null ? userName : userName2),"("+thisTimes+"/10)");
					} catch (SQLException | ClassNotFoundException e1) {
						replyMessage(event, "データベースに接続できませんでした。", true);
						logger.error("An SQLException | ClassNotFoundException error occurred: " + e1.getMessage());
						for (StackTraceElement element : e1.getStackTrace()) {
							logger.error(element.toString());
						}
					}
				}
			}
		}
    }

	@ReflectionHandler(event = "net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent")
	public void onGuildVoiceUpdate(@Nonnull Object event) throws Exception {
		Method getMemberMethod = event.getClass().getMethod("getMember");
		Method getChannelJoinedMethod = event.getClass().getMethod("getChannelJoined");

		Object member = getMemberMethod.invoke(event);
		Method getUserMethod = member.getClass().getMethod("getUser");
		Object user = getUserMethod.invoke(member);
		Method getEffectiveNameMethod = member.getClass().getMethod("getEffectiveName");
		String memberName = (String) getEffectiveNameMethod.invoke(member);

		Object channel = getChannelJoinedMethod.invoke(event);
		if (channel == null) return;
		Method getNameMethod = channel.getClass().getMethod("getName");
		String channelName = (String) getNameMethod.invoke(channel);

		if (user != null) {
			Method isBotMethod = user.getClass().getMethod("isBot");
			if ((boolean) isBotMethod.invoke(user)) {
				return;
			}
			if (channel != null) {
				String message = "(discord) " + memberName + " がボイスチャットチャンネル " + channelName + " に参加しました。";
				TextComponent component = Component.text(message).color(NamedTextColor.GREEN);
				bc.broadCastMessage(component);
			}
		}
    }

	@ReflectionHandler(event = "net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent")
	public void onButtonInteraction(@Nonnull Object event) throws Exception {
		Method getComponentIdMethod = event.getClass().getMethod("getComponentId");
		Method getMessageMethod = event.getClass().getMethod("getMessage");
		Method getUserMethod = event.getClass().getMethod("getUser");
		Method getMemberMethod = event.getClass().getMethod("getMember");
		Method getGuildMethod = event.getClass().getMethod("getGuild");

		String buttonId = (String) getComponentIdMethod.invoke(event);
		Object message = getMessageMethod.invoke(event);
		String buttonMessage = (String) message.getClass().getMethod("getContentRaw").invoke(message);

        if (buttonMessage == null || buttonId == null) return;

		Object user = getUserMethod.invoke(event);
		Object member = getMemberMethod.invoke(event);
		Object guild = getGuildMethod.invoke(event);

        if (member == null) return;
        if (guild == null) return;

		Method getAsMentionMethod = user.getClass().getMethod("getAsMention");
		Method getRolesMethod = member.getClass().getMethod("getRoles");

		String userMention = (String) getAsMentionMethod.invoke(user);
		List<?> roles = (List<?>) getRolesMethod.invoke(member);

		Method getRoleByIdMethod = guild.getClass().getMethod("getRoleById", long.class);
		Object adCraRole = getRoleByIdMethod.invoke(guild, config.getLong("Discord.AdCraRoleId"));

		if (!roles.contains(adCraRole)) {
			replyMessage(event, userMention + " あなたはこの操作を行う権限がありません。", false);
			return;
		}
		String replyMessage = null;

        switch (buttonId) {
        	case "reqOK" -> {
				replyMessage = userMention + " リクエストを受諾しました。";
				Map<String, String> reqMap = req.paternFinderMapForReq(buttonMessage);
				if (!reqMap.isEmpty()) {
					String reqPlayerName = reqMap.get("playerName"),
						reqServerName = reqMap.get("serverName"),
						reqPlayerUUID = reqMap.get("playerUUID");
					try (Connection conn = db.getConnection()) {
						db.insertLog(conn, "INSERT INTO log (name, uuid, reqsul, reqserver, reqsulstatus) VALUES (?, ?, ?, ?, ?);", new Object[] {reqPlayerName, reqPlayerUUID, true, reqServerName, "ok"});
					} catch (SQLException | ClassNotFoundException e2) {
						logger.error("A SQLException | ClassNotFoundException error occurred: {}", e2.getMessage());
						for (StackTraceElement element : e2.getStackTrace()) {
							logger.error(element.toString());
						}
					}
					String execPath = req.getExecPath(reqServerName);
					ProcessBuilder processBuilder = new ProcessBuilder(execPath);
					try {
						processBuilder.start();
						discordME.AddEmbedSomeMessage("RequestOK", reqPlayerName);
						bc.broadCastMessage(Component.text("管理者がリクエストを受諾しました。"+reqServerName+"サーバーがまもなく起動します。").color(NamedTextColor.GREEN));
						VelocityRequest.PlayerReqFlags.remove(reqPlayerUUID);
					} catch (IOException e1) {
						replyMessage = "内部エラーが発生しました。\nサーバーが起動できません。";
						logger.error("An IOException error occurred: " + e1.getMessage());
						for (StackTraceElement element : e1.getStackTrace()) {
							logger.error(element.toString());
						}
					}
				} else {
					replyMessage = "エラーが発生しました。\npattern形式が無効です。";
				}
				if (replyMessage != null) {
					replyMessage(event, replyMessage, false);
				}
            }
        	case "reqCancel" -> {
				replyMessage = userMention + " リクエストを拒否しました。";
				Map<String, String> reqMap = req.paternFinderMapForReq(buttonMessage);
				if (!reqMap.isEmpty()) {
					String reqPlayerName = reqMap.get("playerName"),
					reqServerName = reqMap.get("serverName"),
					reqPlayerUUID = reqMap.get("playerUUID");
					try (Connection conn = db.getConnection()) {
						db.insertLog(conn, "INSERT INTO log (name, uuid, reqsul, reqserver, reqsulstatus) VALUES (?, ?, ?, ?, ?);", new Object[] {reqPlayerName, reqPlayerUUID, true, reqServerName, "cancel"});
					} catch (SQLException | ClassNotFoundException e2) {
						logger.error("A SQLException | ClassNotFoundException error occurred: {}", e2.getMessage());
						for (StackTraceElement element : e2.getStackTrace()) {
							logger.error(element.toString());
						}
					}
					discordME.AddEmbedSomeMessage("RequestCancel", reqPlayerName);
					bc.broadCastMessage(Component.text("管理者が"+reqPlayerName+"の"+reqServerName+"サーバーの起動リクエストをキャンセルしました。").color(NamedTextColor.RED));
					VelocityRequest.PlayerReqFlags.remove(reqPlayerUUID);
				} else {
					replyMessage = "エラーが発生しました。\npattern形式が無効です。";
				}
				if (replyMessage != null) {
					replyMessage(event, replyMessage, false);
				}
            }
        }
    }

	@ReflectionHandler(event = "net.dv8tion.jda.api.events.message.MessageReceivedEvent")
    public void onMessageReceived(@Nonnull Object event) throws Exception{
		Method getAuthorMethod = event.getClass().getMethod("getAuthor");
		Method getMemberMethod = event.getClass().getMethod("getMember");
		Method getChannelMethod = event.getClass().getMethod("getChannel");
		Method getMessageMethod = event.getClass().getMethod("getMessage");
		Method getAttachmentsMethod = getMessageMethod.getReturnType().getMethod("getAttachments");
		Method getContentRawMethod = getMessageMethod.getReturnType().getMethod("getContentRaw");

		Object channel = getChannelMethod.invoke(event);
		Method getChannelIdMethod = channel.getClass().getMethod("getId");
		String channelId = (String) getChannelIdMethod.invoke(channel);

		Object message = getMessageMethod.invoke(event);
		Method isWebhookMessageMethod = message.getClass().getMethod("isWebhookMessage");

		List<?> attachments = (List<?>) getAttachmentsMethod.invoke(message);
		String content = (String) getContentRawMethod.invoke(message);

		Object author = getAuthorMethod.invoke(event);
		Method authorIsBotMethod = author.getClass().getMethod("isBot");

		if ((boolean) authorIsBotMethod.invoke(author) || 
			(boolean) isWebhookMessageMethod.invoke(message) || 
			!channelId.equals(Long.toString(config.getLong("Discord.ChatChannelId")))) {
			return;
		}

		Object member = getMemberMethod.invoke(event);
		if (member == null) return;

		Method getEffectiveNameMethod = member.getClass().getMethod("getEffectiveName");
		String userName = (String) getEffectiveNameMethod.invoke(member);

		if (!content.isEmpty()) {
			content = "(discord) " + userName + " -> " + content;
			sendMixUrl(content);
		}

		int attachmentsSize = attachments.size();
		if (attachmentsSize > 0) {
			TextComponent component = Component.text()
					.append(Component.text("(discord) " + userName+" -> Discordで画像か動画を上げています！").color(NamedTextColor.AQUA))
					.build();
			TextComponent additionalComponent;
			int i=0;
		    for (Object attachment : attachments) {
				try {
					Method getUrlMethod = attachment.getClass().getMethod("getUrl");
					String url = (String) getUrlMethod.invoke(attachment);
					additionalComponent = Component.text()
						.appendNewline()
						.append(Component.text(url)
							.color(NamedTextColor.GRAY)
							.decorate(TextDecoration.UNDERLINED))
							.clickEvent(ClickEvent.openUrl(url))
							.hoverEvent(HoverEvent.showText(Component.text("添付ファイル"+(i+1))))
						.build();
					component = component.append(additionalComponent);
					i++;
					bc.broadCastMessage(component);
				} catch (Exception e) {
					logger.error("An error occurred: " + e.getMessage());
					for (StackTraceElement element : e.getStackTrace()) {
						logger.error(element.toString());
					}
				}
		    }
		}
	}

	private void replyMessage(Object event, String message, boolean isEphemeral) throws Exception {
		Method replyMethod = event.getClass().getMethod("reply", String.class);
		Object replyResult = replyMethod.invoke(event, message);
		Method setEphemeralMethod = replyResult.getClass().getMethod("setEphemeral", boolean.class);
		Object ephemeralReplyResult = setEphemeralMethod.invoke(replyResult, true);
		Method queueMethod = ephemeralReplyResult.getClass().getMethod("queue");
		queueMethod.invoke(ephemeralReplyResult);
	}

	private int getDiscordUserTodayRegisterImageMetaTimes(Connection conn, String userId) throws SQLException, ClassNotFoundException {
        String query = "SELECT COUNT(*) FROM images WHERE did = ? AND DATE(date) = ?";
		PreparedStatement ps = conn.prepareStatement(query);
		ps.setString(1, userId);
		ps.setDate(2, java.sql.Date.valueOf(LocalDate.now()));
		ResultSet rs = ps.executeQuery();
		if (rs.next()) {
			return rs.getInt(1);
		}
        return 0;
    }

	private void sendMixUrl(String string) {
        String urlRegex = "https?://\\S+";
        Pattern patternUrl = Pattern.compile(urlRegex);
        Matcher matcher = patternUrl.matcher(string);
        List<String> urls = new ArrayList<>();
        List<String> textParts = new ArrayList<>();
        int lastMatchEnd = 0;
        Boolean isUrl = false;
        while (matcher.find()) {
        	isUrl = true;
            urls.add(matcher.group());
            textParts.add(string.substring(lastMatchEnd, matcher.start()));
            lastMatchEnd = matcher.end();
        }
        if (!isUrl) {
        	bc.broadCastMessage(Component.text(string).color(NamedTextColor.AQUA));
        	return;
        }
        if (lastMatchEnd < string.length()) {
            textParts.add(string.substring(lastMatchEnd));
        }
        TextComponent component = Component.text().build();
        int textPartsSize = textParts.size();
        int urlsSize = urls.size();
        for (int i = 0; i < textPartsSize; i++) {
        	Boolean isText = false;
        	if (Objects.nonNull(textParts) && textPartsSize != 0) {
        		String text;
        		text = textParts.get(i);
        		TextComponent additionalComponent;
        		additionalComponent = Component.text()
					.append(Component.text(text))
					.color(NamedTextColor.AQUA)
					.build();
        		component = component.append(additionalComponent);
        	} else {
        		isText = true;
        	}
        	if (i < urlsSize) {
        		String getUrl;
        		if (isText) {
        			// textがなかったら、先頭の改行は無くす(=URLのみ)
        			getUrl = urls.get(i);
        		} else if (i != textPartsSize - 1) {
            		getUrl = "\n" + urls.get(i) + "\n";
            	} else {
            		getUrl = "\n" + urls.get(i);
            	}
        		TextComponent additionalComponent;
        		additionalComponent = Component.text()
            				.append(Component.text(getUrl)
    						.color(NamedTextColor.GRAY)
    						.decorate(TextDecoration.UNDERLINED))
    						.clickEvent(ClickEvent.openUrl(urls.get(i)))
    						.hoverEvent(HoverEvent.showText(Component.text("リンク"+(i+1))))
                            .build();
                component = component.append(additionalComponent);
        	}
        }
        bc.broadCastMessage(component);
    }
}
