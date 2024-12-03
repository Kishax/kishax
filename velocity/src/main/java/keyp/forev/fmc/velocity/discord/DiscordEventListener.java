package keyp.forev.fmc.velocity.discord;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
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
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import keyp.forev.fmc.velocity.cmd.sub.VelocityRequest;
import keyp.forev.fmc.velocity.cmd.sub.interfaces.Request;
import keyp.forev.fmc.velocity.server.BroadCast;
import keyp.forev.fmc.velocity.util.config.VelocityConfig;

public class DiscordEventListener extends ListenerAdapter {
	public static String PlayerChatMessageId = null;
	private final Logger logger;
	private final VelocityConfig config;
	private final Database db;
	private final BroadCast bc;
	private final MessageEditor discordME;
	private final Request req;
	private final Discord discord;
	private final Provider<SocketSwitch> sswProvider;
	private String replyMessage = null;

	public DiscordEventListener(Logger logger, VelocityConfig config, Database db, BroadCast bc, MessageEditor discordME, Request req, Discord discord, Provider<SocketSwitch> sswProvider) {
		this.logger = logger;
		this.config = config;
		this.db = db;
		this.bc = bc;
		this.discordME = discordME;
		this.req = req;
		this.discord = discord;
		this.sswProvider = sswProvider;
	}
	
	@Override
	public void onMessageUpdate(@Nonnull MessageUpdateEvent event) {
        String ruleChannelId = Long.toString(config.getLong("Discord.Rule.ChannelId", 0));
        String ruleMessageId = Long.toString(config.getLong("Discord.Rule.MessageId", 0));
        if (event.getChannel().getId().equals(ruleChannelId) && event.getMessageId().equals(ruleMessageId)) {
            String newContent = event.getMessage().getContentDisplay();
			try (Connection conn = db.getConnection()) {
				db.updateLog(conn, "UPDATE settings SET value = ? WHERE name = ?;", new Object[] {newContent, FMCSettings.RULEBOOK_CONTENT.getColumnKey()});
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
    }
	
	@SuppressWarnings("null")
	@Override
    public void onSlashCommandInteraction(@Nonnull SlashCommandInteractionEvent e) {
		User user = e.getUser();
		Member member = e.getMember();
		String userMention = user.getAsMention();
		Guild guild = e.getGuild();
		MessageChannel channel = e.getChannel();
		if (member == null) return;
		if (guild == null) return;
		String userId = member.getId(),
			userName = member.getNickname(),
			userName2 = user.getName(),
			channelId = channel.getId(),
			guildId = guild.getId(),
			channelLink = String.format("https://discord.com/channels/%s/%s", guildId, teraChannelId),
			userName12 = userName != null ? userName : userName2;
		List<Role> roles = member.getRoles();
		if (e.getName().equals("fmc")) {
			String cmd = e.getSubcommandName();
			if (cmd == null) return;
			switch (cmd) {
				case "syncrulebook" -> {
					discord.getMessageContent().thenAccept(content -> {
						if (!roles.contains(guild.getRoleById(config.getLong("Discord.AdCraRoleId")))) {
							replyMessage = user.getAsMention() + " あなたはこの操作を行う権限がありません。";
							e.reply(replyMessage).setEphemeral(true).queue();
							return;
						}
						logger.info("メッセージの内容: {}", content);
						if (content != null) {
							try (Connection conn = db.getConnection()) {
								db.updateLog(conn, "UPDATE settings SET value = ? WHERE name = ?;", new Object[] {content, FMCSettings.RULEBOOK_CONTENT.getColumnKey()});
								e.reply("ルールブックを更新しました。").setEphemeral(true).queue();
								SocketSwitch ssw = sswProvider.get();
								ssw.sendSpigotServer(conn, "RulebookSync");
							} catch (SQLException | ClassNotFoundException e1) {
								e.reply("データベースに接続できませんでした。").setEphemeral(true).queue();
								logger.error("An SQLException | ClassNotFoundException error occurred: " + e1.getMessage());
								for (StackTraceElement element : e1.getStackTrace()) {
									logger.error(element.toString());
								}
							}
						} else {
							e.reply("メッセージの内容が取得できませんでした。").setEphemeral(true).queue();
						}
					}).exceptionally(throwable -> {
						e.reply("エラーが発生しました。").setEphemeral(true).queue();
						logger.error("エラーが発生しました: {}", throwable.getMessage());
						return null;
					});
				}
				case "image_add_q" -> {
					try (Connection conn = db.getConnection()) {
						int limitUploadTimes = FMCSettings.DISCORD_IMAGE_LIMIT_TIMES.getIntValue(),
							userUploadTimes = getDiscordUserTodayRegisterImageMetaTimes(conn, userId),
							thisTimes = userUploadTimes + 1;
						if (thisTimes >= limitUploadTimes) {
							e.reply("1日の登録回数は"+limitUploadTimes+"回までです。").setEphemeral(true).queue();
							return;
						}
						String url = e.getOption("url") != null ? e.getOption("url").getAsString() : null,
							title = (e.getOption("title") != null) ? e.getOption("title").getAsString() : "無名のタイトル",
							comment = (e.getOption("comment") != null) ? e.getOption("comment").getAsString() : "コメントなし";
						Attachment attachment = e.getOption("image") != null ? e.getOption("image").getAsAttachment() : null;
						if (url == null && attachment == null) {
							e.reply("画像URLまたは画像を指定してください。").setEphemeral(true).queue();
							return;
						}
						if (url != null && attachment != null) {
							e.reply("画像URLと画像の両方を指定することはできません。").setEphemeral(true).queue();
							return;
						}
						if (attachment != null) {
							url = attachment.getUrl();
						}
						LocalDate now = LocalDate.now();
						String otp = OTPGenerator.generateOTP(6);
						db.insertLog(conn, "INSERT INTO images (name, title, url, comment, otp, d, dname, did, date, locked, locked_action) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);", new Object[] {userName12, title, url, comment, otp, true, userName12, userId, java.sql.Date.valueOf(now), true, false});
						e.reply("画像メタデータを登録しました。(" + thisTimes + "/" + limitUploadTimes + ")\nワンタイムパスワード: "+otp+"\nマイクラ画像マップ取得コマンド: ```/q "+otp+"```").setEphemeral(true).queue();
						logger.info("(Discord) 画像メタデータを登録しました。");
						logger.info("ユーザー: {}\n試行: {}", (userName != null ? userName : userName2),"("+thisTimes+"/10)");
					} catch (SQLException | ClassNotFoundException e1) {
						e.reply("データベースに接続できませんでした。").setEphemeral(true).queue();
						logger.error("An SQLException | ClassNotFoundException error occurred: " + e1.getMessage());
						for (StackTraceElement element : e1.getStackTrace()) {
							logger.error(element.toString());
						}
					}
					
				}
			}
		}
    }
	
	@Override
	public void onGuildVoiceUpdate(@Nonnull GuildVoiceUpdateEvent e) {
		Member member = e.getMember();
		User user = member.getUser();
		if (user != null ) {
			if (user.isBot()) {
				return;
			}
			AudioChannel channel = e.getChannelJoined();
			if (channel != null) {
				String message = "(discord) " + member.getEffectiveName() + " がボイスチャットチャンネル " + channel.getName() + " に参加しました。";
				TextComponent component = Component.text(message).color(NamedTextColor.GREEN);
				bc.broadCastMessage(component);
			}
		}
    }

	@Override
	public void onButtonInteraction(@Nonnull ButtonInteractionEvent e) {
        String buttonId = e.getComponentId();
        String buttonMessage = e.getMessage().getContentRaw();
        if (Objects.isNull(buttonMessage) || Objects.isNull(buttonId)) return;
        User user = e.getUser();
        Member member = e.getMember();
        Guild guild = e.getGuild();
        if (member == null) return;
        if (guild == null) return;
		List<Role> roles = member.getRoles();
		if (!roles.contains(guild.getRoleById(config.getLong("Discord.AdCraRoleId")))) {
			replyMessage = user.getAsMention() + " あなたはこの操作を行う権限がありません。";
			e.reply(replyMessage).setEphemeral(true).queue();
			return;
		}
        switch (buttonId) {
        	case "reqOK" -> {
				replyMessage = user.getAsMention() + " リクエストを受諾しました。";
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
					e.reply(replyMessage).queue();
				}
				if (replyMessage != null) {
					e.reply(replyMessage).queue();
				}
				e.getMessage().editMessageComponents().queue();
            }
        	case "reqCancel" -> {
				replyMessage = user.getAsMention() + " リクエストを拒否しました。";
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
					e.reply(replyMessage).queue();
				}
				e.getMessage().editMessageComponents().queue();
            }
        }
    }

	@Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent e) {
		if (
			e.getAuthor().isBot() || 
			e.getMessage().isWebhookMessage() || 
			!e.getChannel().getId().equals(Long.toString(config.getLong("Discord.ChatChannelId")))
		) {
			return;
		}
		Member member = e.getMember();
		if (member == null) return;
		String message = e.getMessage().getContentRaw();
		String userName = member.getEffectiveName();
		if (!message.isEmpty()) {
			message = "(discord) " + userName + " -> " + message;
			sendMixUrl(message);
		}
		DiscordEventListener.PlayerChatMessageId = null;
		List <Attachment> attachments = e.getMessage().getAttachments();
		int attachmentsSize = attachments.size();
		if (attachmentsSize > 0) {
			TextComponent component = Component.text()
					.append(Component.text("(discord) " + userName+" -> Discordで画像か動画を上げています！").color(NamedTextColor.AQUA))
					.build();
			TextComponent additionalComponent;
			int i=0;
		    for (Attachment attachment : attachments) {
		    	additionalComponent = Component.text()
		    			.append(Component.text("\n"+attachment.getUrl())
								.color(NamedTextColor.GRAY)
								.decorate(TextDecoration.UNDERLINED))
								.clickEvent(ClickEvent.openUrl(attachment.getUrl()))
								.hoverEvent(HoverEvent.showText(Component.text("添付ファイル"+(i+1))))
		                            .build();
		        	component = component.append(additionalComponent);
		            i++;
		        }
		        bc.broadCastMessage(component);
		    }
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
        	//if (string.contains("\\n")) string = string.replace("\\n", "\n");
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
            		getUrl = "\n"+urls.get(i)+"\n";
            	} else {
            		getUrl = "\n"+urls.get(i);
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
