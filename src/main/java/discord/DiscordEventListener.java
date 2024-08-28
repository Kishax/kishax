package discord;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;

import com.google.inject.Inject;

import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import velocity.BroadCast;
import velocity.Config;
import velocity.Database;
import velocity.PlayerUtil;
import velocity_command.Request;

public class DiscordEventListener extends ListenerAdapter {

	public static String PlayerChatMessageId = null;
	
	private final Logger logger;
	private final Config config;
	private final Database db;
	private final BroadCast bc;
	private final PlayerUtil pu;
	private final MessageEditorInterface discordME;
	private Connection conn = null;
	private PreparedStatement ps = null;
	private String pattern = null, sql = null, replyMessage = null, 
        		reqServerName = null, reqPlayerName = null, batchFilePath = null, reqPlayerUUID = null;
	private Pattern compiledPattern = null;
	private Matcher matcher = null;
	private ProcessBuilder processBuilder = null;

	@Inject
	public DiscordEventListener (
		Logger logger, Config config, Database db, 
		BroadCast bc, PlayerUtil pu, MessageEditorInterface discordME
	) {
		this.logger = logger;
		this.config = config;
		this.db = db;
		this.bc = bc;
		this.pu = pu;
		this.discordME = discordME;
	}
	
	@SuppressWarnings("null")
	@Override
	public void onButtonInteraction(ButtonInteractionEvent e) {
        // ボタンIDを取得
        String buttonId = e.getComponentId();
        String buttonMessage = e.getMessage().getContentRaw();
        
        if (Objects.isNull(buttonMessage) || Objects.isNull(buttonId)) return;
        
        // ボタンを押したユーザーを取得
        User user = e.getUser();
        
        switch (buttonId) {
        	case "reqOK" -> {
				replyMessage = user.getAsMention() + "startが押されました。";
				// プレイヤー名・サーバー名、取得
				pattern = "(.*?)が(.*?)サーバーの起動リクエストを送信しました。\n起動しますか？";
				compiledPattern = Pattern.compile(pattern);
				matcher = compiledPattern.matcher(buttonMessage);
				if (matcher.find()) {
					reqPlayerName = matcher.group(1);
					reqServerName = matcher.group(2);
					reqPlayerUUID = pu.getPlayerUUIDByNameFromDB(reqPlayerName);
					
					try {
						conn = db.getConnection();
						sql = "INSERT INTO mine_log (name, uuid, reqsul, reqserver, reqsulstatus) VALUES (?, ?, ?, ?, ?);";
						ps = conn.prepareStatement(sql);
						ps.setString(1, reqPlayerName);
						ps.setString(2, reqPlayerUUID);
						ps.setBoolean(3, true);
						ps.setString(4, reqServerName);
						ps.setString(5, "ok");
						ps.executeUpdate();
					} catch (SQLException | ClassNotFoundException e1) {
						logger.error("A SQLException error occurred: " + e1.getMessage());
						for (StackTraceElement element : e1.getStackTrace()) {
							logger.error(element.toString());
						}
					}
					
					// サーバー起動メソッド開始
					// バッチファイルのパスを指定
					batchFilePath = config.getString("Servers."+reqServerName+".Bat_Path");
					processBuilder = new ProcessBuilder(batchFilePath);
					try {
						processBuilder.start();
					} catch (IOException e1) {
						for (StackTraceElement element : e1.getStackTrace()) {
							logger.error(element.toString());
						}
					}
					
					// Discord通知
					discordME.AddEmbedSomeMessage("RequestOK", reqPlayerName);
					
					// マイクラサーバーへ通知
					bc.broadCastMessage(Component.text("管理者がリクエストを受諾しました。"+reqServerName+"サーバーがまもなく起動します。").color(NamedTextColor.GREEN));
					
					// フラグから削除
					String playerUUID = pu.getPlayerUUIDByNameFromDB(reqPlayerName);
					Request.PlayerReqFlags.remove(playerUUID); // フラグから除去
				} else {
					replyMessage = "エラーが発生しました。\npattern形式が無効です。";
				}
				
				e.reply(replyMessage).queue();
				
				// ボタンを削除
				e.getMessage().editMessageComponents().queue();
            }
        		
        	case "reqCancel" -> {
				replyMessage = user.getAsMention() + "stopが押されました。";
				// プレイヤー名・サーバー名、取得
				pattern = "(.*?)が(.*?)サーバーの起動リクエストを送信しました。\n起動しますか？";
				compiledPattern = Pattern.compile(pattern);
				matcher = compiledPattern.matcher(buttonMessage);
				if (matcher.find()) {
					reqPlayerName = matcher.group(1);
					reqServerName = matcher.group(2);
					reqPlayerUUID = pu.getPlayerUUIDByNameFromDB(reqPlayerName);
					
					try {
						conn = db.getConnection();
						sql = "INSERT INTO mine_log (name, uuid, reqsul, reqserver, reqsulstatus) VALUES (?, ?, ?, ?, ?);";
						ps = conn.prepareStatement(sql);
						ps.setString(1, reqPlayerName);
						ps.setString(2, reqPlayerUUID);
						ps.setBoolean(3, true);
						ps.setString(4, reqServerName);
						ps.setString(5, "cancel");
						ps.executeUpdate();
					}
					catch (SQLException | ClassNotFoundException e1) {
						logger.error("A SQLException error occurred: " + e1.getMessage());
						for (StackTraceElement element : e1.getStackTrace()) {
							logger.error(element.toString());
						}
					}
					
					// Discord通知
					discordME.AddEmbedSomeMessage("RequestCancel", reqPlayerName);
					
					// マイクラサーバーへ通知
					bc.broadCastMessage(Component.text("管理者が"+reqPlayerName+"の"+reqServerName+"サーバーの起動リクエストをキャンセルしました。").color(NamedTextColor.RED));
					
					// フラグから削除
					String playerUUID = pu.getPlayerUUIDByNameFromDB(reqPlayerName);
					Request.PlayerReqFlags.remove(playerUUID); // フラグから除去
				} else {
					replyMessage = "エラーが発生しました。\npattern形式が無効です。";
				}
				
				e.reply(replyMessage).queue();
				
				// ボタンを削除
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
        String userName = e.getAuthor().getName();
        
        // メッセージが空でないことを確認
        if (!message.isEmpty())
        {
        	message = userName + " -> " + message;
        	sendMixUrl(message);
        }
        
        DiscordEventListener.PlayerChatMessageId = null;
        
        // チャンネルIDやユーザーIDも取得可能
        //String channelId = e.getChannel().getId();
        
        List <Attachment> attachments = e.getMessage().getAttachments();
        int attachmentsSize = attachments.size();
        if (attachmentsSize > 0) {
        	TextComponent component = Component.text()
        			.append(Component.text(userName+" -> Discordで画像か動画を上げています！").color(NamedTextColor.AQUA))
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
	
	public void sendMixUrl(String string) {
    	// 正規表現パターンを定義（URLを見つけるための正規表現）
        String urlRegex = "https?://\\S+";
        Pattern patternUrl = Pattern.compile(urlRegex);
        matcher = patternUrl.matcher(string);

        // URLリストとテキストリストを作成
        List<String> urls = new ArrayList<>();
        List<String> textParts = new ArrayList<>();
        
        int lastMatchEnd = 0;
        
        Boolean isUrl = false;
        while (matcher.find()) {
        	// URLが含まれていたら
        	isUrl = true;
        	
            // マッチしたURLをリストに追加
            urls.add(matcher.group());
            
            // URLの前のテキスト部分をリストに追加
            textParts.add(string.substring(lastMatchEnd, matcher.start()));
            lastMatchEnd = matcher.end();
        }
        
    	// URLが含まれてなかったら
        if (!isUrl) {
        	//if (string.contains("\\n")) string = string.replace("\\n", "\n");
        	bc.broadCastMessage(Component.text(string).color(NamedTextColor.AQUA));
        	return;
        }
        
        // 最後のURLの後のテキスト部分を追加
        if (lastMatchEnd < string.length()) {
            textParts.add(string.substring(lastMatchEnd));
        }
        

        // テキスト部分を結合
        TextComponent component = Component.text().build();
        
        int textPartsSize = textParts.size();
        int urlsSize = urls.size();
        
        for (int i = 0; i < textPartsSize; i++) {
        	Boolean isText = false;
        	if (Objects.nonNull(textParts) && textPartsSize != 0) {
        		String text;
        		text = textParts.get(i);
        		
        		//if (text.contains("\\n")) text = text.replace("\\n", "\n");
        		TextComponent additionalComponent;
        		additionalComponent = Component.text()
        				.append(Component.text(text))
        				.color(NamedTextColor.AQUA)
        				.build();
        		component = component.append(additionalComponent);
        	} else {
        		isText = true;
        	}
        	
        	
        	// URLが1つなら、textPartsは2つになる。
        	// URLが2つなら、textPartsは3つになる。
        	//　ゆえ、最後の番号だけ考えなければ、
        	// 上で文字列にURLが含まれているかどうかを確認しているので、ぶっちゃけ以下のif文はいらないかも
        	//if(Objects.nonNull(urls) && urlsSize != 0)
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
