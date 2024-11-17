package discord;

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
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;

import common.Database;
import common.FMCSettings;
import common.OTPGenerator;
import common.SocketSwitch;
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
import velocity.BroadCast;
import velocity.Config;
import velocity_command.Request;
import velocity_command.RequestInterface;

public class DiscordEventListener extends ListenerAdapter {
	public static String PlayerChatMessageId = null;
	private final Logger logger;
	private final Config config;
	private final Database db;
	private final BroadCast bc;
	private final MessageEditorInterface discordME;
	private final RequestInterface req;
	private final DiscordInterface discord;
	private final Provider<SocketSwitch> sswProvider;
	private final String teraToken, teraExecFilePath;
	private final Long teraChannelId;
	private final boolean require;
	private String replyMessage = null, restAPIUrl = null;

	@Inject
	public DiscordEventListener(Logger logger, Config config, Database db, BroadCast bc, MessageEditorInterface discordME, RequestInterface req, DiscordInterface discord, Provider<SocketSwitch> sswProvider) {
		this.logger = logger;
		this.config = config;
		this.db = db;
		this.bc = bc;
		this.discordME = discordME;
		this.req = req;
		this.discord = discord;
		this.sswProvider = sswProvider;
		this.teraToken = config.getString("Terraria.Token", "");
		this.teraExecFilePath = config.getString("Terraria.Exec_Path", "");
		this.teraChannelId = config.getLong("Terraria.ChannelId", 0);
		this.restAPIUrl = config.getString("Terraria.RestApiUrl");
		this.require = !restAPIUrl.isEmpty() && 
			!teraToken.isEmpty() && 
			!teraExecFilePath.isEmpty() && 
			teraChannelId != 0;
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
    public void onSlashCommandInteraction(SlashCommandInteractionEvent e) {
		User user = e.getUser();
		Member member = e.getMember();
		String userMention = user.getAsMention();
		MessageChannel channel = e.getChannel();
		String userId = e.getMember().getId(),
			userName = e.getMember().getNickname(),
			userName2 = user.getName(),
			channelId = channel.getId(),
			guildId = e.getGuild().getId(),
			channelLink = String.format("https://discord.com/channels/%s/%s", guildId, teraChannelId);
		List<Role> roles = member.getRoles();
		if (e.getName().equals("fmc")) {
			switch (e.getSubcommandName()) {
				case "syncrulebook" -> {
					discord.getMessageContent().thenAccept(content -> {
						if (!roles.contains(e.getGuild().getRoleById(config.getLong("Discord.AdCraRoleId")))) {
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
							userUploadTimes = getUserTodayTimes(conn, userId),
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
						LocalDate now = LocalDate.now();
						String imageUUID = UUID.randomUUID().toString(),
							otp = OTPGenerator.generateOTP(6);
						db.insertLog(conn, "INSERT INTO images (name, uuid, server, mapid, title, imuuid, ext, url, comment, isqr, otp, d, dname, did, date, locked, locked_action) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);", new Object[] {userName != null ? userName : userName2, null, null, null, title, imageUUID, null, url, comment, null, otp, true, userName != null ? userName : userName2, userId, java.sql.Date.valueOf(now), true, false});
						e.reply("画像メタデータを登録しました。("+thisTimes+"/10)\nワンタイムパスワード: "+otp+"\nマイクラ画像マップ取得コマンド: ```/q "+otp+"```").setEphemeral(true).queue();
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
				case "tera" -> {
					String teraType = e.getOption("action").getAsString();
					if (!require) {
						e.reply("コンフィグの設定が不十分なため、コマンドを実行できません。").setEphemeral(true).queue();
						return;
					}
					String teraChannelId2 = Long.toString(teraChannelId);
					if (!channelId.equals(teraChannelId2)) {
						e.reply("テラリアのコマンドは " + channelLink + " で実行してください。").setEphemeral(true).queue();
						return;
					}
					switch (teraType.toLowerCase()) {
						case "start" -> {
							if (isTera()) {
								e.reply("Terrariaサーバーは既にオンラインです！").setEphemeral(true).queue();
							} else {
								try {
									ProcessBuilder teraprocessBuilder = new ProcessBuilder(teraExecFilePath);
									teraprocessBuilder.start();
									e.reply(userMention + " Terrariaサーバーを起動させました。\nまもなく起動します。").setEphemeral(false).queue();
								} catch (IOException e1) {
									e.reply(userMention + " 内部エラーが発生しました。\nサーバーが起動できません。").setEphemeral(false).queue();
									logger.error("An IOException error occurred: " + e1.getMessage());
									for (StackTraceElement element : e1.getStackTrace()) {
										logger.error(element.toString());
									}
								}
							}
						}
						case "stop" -> {
							if (isTera()) {
								try {
									String urlString = restAPIUrl + "/v2/server/off?token=" + teraToken + "&confirm=true&nosave=false";
									URI uri = new URI(urlString);
									URL url = uri.toURL();
									HttpURLConnection con = (HttpURLConnection) url.openConnection();
									con.setRequestMethod("GET");
									con.setRequestProperty("Content-Type", "application/json; utf-8");
									int code = con.getResponseCode();
									switch (code) {
										case 200 -> {
											e.reply(userMention + " Terrariaサーバーを正常に停止させました。").setEphemeral(false).queue();
										}
										default -> {
											e.reply(userMention + " 内部エラーが発生しました。\nサーバーが正常に停止できなかった可能性があります。").setEphemeral(false).queue();
										}
									}
								} catch (IOException | URISyntaxException e2) {
									logger.error("An IOException | URISyntaxException error occurred: " + e2.getMessage());
									for (StackTraceElement element : e2.getStackTrace()) {
										logger.error(element.toString());
									}
									e.reply(userMention + " 内部エラーが発生しました。\nサーバーが正常に停止できなかった可能性があります。").setEphemeral(false).queue();
								}
							} else {
								e.reply("Terrariaサーバーは現在オフラインです！").setEphemeral(true).queue();
							}
						}
						case "status" -> {
							if (isTera()) {
								e.reply("Terrariaサーバーは現在オンラインです。").setEphemeral(true).queue();
							} else {
								e.reply("Terrariaサーバーは現在オフラインです。").setEphemeral(true).queue();
							}
						}
						default -> throw new AssertionError();
					}
				}
			}
		}
    }

	@SuppressWarnings("null")
	@Override
	public void onGuildVoiceUpdate(GuildVoiceUpdateEvent e) {
        Member member = e.getMember();
		if (member.getUser().isBot()) {
			return;
		}
		AudioChannel channel = e.getChannelJoined();
		if (channel == null) {
            logger.error("chennel is null");
            return;
        }
        String message = "(discord) " + member.getEffectiveName() + " がボイスチャットチャンネル " + channel.getName() + " に参加しました。";
        TextComponent component = Component.text(message).color(NamedTextColor.GREEN);
        bc.broadCastMessage(component);
    }

	@SuppressWarnings("null")
	@Override
	public void onButtonInteraction(ButtonInteractionEvent e) {
        String buttonId = e.getComponentId();
        String buttonMessage = e.getMessage().getContentRaw();
        if (Objects.isNull(buttonMessage) || Objects.isNull(buttonId)) return;
        User user = e.getUser();
        Member member = e.getMember();
		List<Role> roles = member.getRoles();
		if (!roles.contains(e.getGuild().getRoleById(config.getLong("Discord.AdCraRoleId")))) {
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
						Request.PlayerReqFlags.remove(reqPlayerUUID);
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
					Request.PlayerReqFlags.remove(reqPlayerUUID);
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

	@SuppressWarnings("null")
	@Override
    public void onMessageReceived(MessageReceivedEvent e) {
        // DMやBot、Webhookのメッセージには反応しないようにする// e.isFromType(ChannelType.PRIVATE)
        if (
        	e.getAuthor().isBot() || 
        	e.getMessage().isWebhookMessage() || 
        	!e.getChannel().getId().equals(Long.toString(config.getLong("Discord.ChatChannelId")))
        ) {
			return;
		}
        
        // メッセージ内容を取得
        String message = e.getMessage().getContentRaw();
        String userName = e.getMember().getEffectiveName();
        
        // メッセージが空でないことを確認
        if (!message.isEmpty()) {
        	message = "(discord) " + userName + " -> " + message;
        	sendMixUrl(message);
        }
        
        DiscordEventListener.PlayerChatMessageId = null;
        
        // チャンネルIDやユーザーIDも取得可能
        //String channelId = e.getChannel().getId();
        
        List <Attachment> attachments = e.getMessage().getAttachments();
        int attachmentsSize = attachments.size();
        if (attachmentsSize > 0) {
        	TextComponent component = Component.text()
        			.append(Component.text("(discord) " + userName+" -> Discordで画像か動画を上げています！").color(NamedTextColor.AQUA))
        			.build();
        			
        	TextComponent additionalComponent;
        	int i=0;
        	// 添付ファイルを処理したい場合は、以下のようにできます
            for (Attachment attachment : attachments) {
            	additionalComponent = Component.text()
            			.append(Component.text("\n"+attachment.getUrl())
        						.color(NamedTextColor.GRAY)
        						.decorate(TextDecoration.UNDERLINED))
        						.clickEvent(ClickEvent.openUrl(attachment.getUrl()))
        						.hoverEvent(HoverEvent.showText(Component.text("添付ファイル"+(i+1))))
                                .build();
            	
                // ここで各添付ファイルに対する処理を実装できます
            	component = component.append(additionalComponent);
                i++;
            }
            
            bc.broadCastMessage(component);
        }
    }

	private int getUserTodayTimes(Connection conn, String userId) throws SQLException, ClassNotFoundException {
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

	private boolean isTera() {
        try {
            String urlString = restAPIUrl + "/status?token=" + teraToken;
            URI uri = new URI(urlString);
            URL url = uri.toURL();
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Content-Type", "application/json; utf-8");
            int code = con.getResponseCode();
            switch (code) {
                case 200 -> {
                    return true;
                }
                default -> {
                    return false;
                }
            }
        } catch (IOException | URISyntaxException e) {
            return false;
		}
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
