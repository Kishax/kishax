package discord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;

import com.google.inject.Inject;

import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import velocity.BroadCast;
import velocity.Config;

public class DiscordEventListener extends ListenerAdapter
{
	private final Logger logger;
	private final Config config;
	private final BroadCast bc;
	private final DiscordListener discord;
	
	public static String PlayerChatMessageId = null;
	
	@Inject
	public DiscordEventListener
	(
		Logger logger, Config config, BroadCast bc,
		DiscordListener discord
	)
	{
		this.logger = logger;
		this.config = config;
		this.bc = bc;
		this.discord = discord;
	}
	
	public void onButtonInteraction(ButtonInteractionEvent event) {
        // ボタンIDを取得
        String buttonId = event.getComponentId();

        if (buttonId.equals("start_process")) {
            // ボタンを押したユーザーを取得
            User user = event.getUser();
            
            // 処理Aを開始
            discord.startProcessA(user);

            // ボタンを削除
            event.getMessage().editMessageComponents().queue();

            // ユーザーをメンションしてメッセージを送信
            event.reply(user.getAsMention() + " 処理Aが開始されました。").queue();
        }
    }

	@Override
    public void onMessageReceived(MessageReceivedEvent e) 
    {
        // DMやBot、Webhookのメッセージには反応しないようにする// e.isFromType(ChannelType.PRIVATE)
        if
        (
        	e.getAuthor().isBot() || 
        	e.getMessage().isWebhookMessage() || 
        	!e.getChannel().getId().equals(Long.valueOf(config.getLong("Discord.ChatChannelId")).toString())
        )
		{
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
        if(attachmentsSize > 0)
        {
        	TextComponent component = Component.text()
        			.append(Component.text(userName+" -> Discordで画像か動画を上げています！").color(NamedTextColor.AQUA))
        			.build();
        			
        	TextComponent additionalComponent = null;
        	int i=0;
        	// 添付ファイルを処理したい場合は、以下のようにできます
            for (Attachment attachment : attachments)
            {
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
        	bc.broadcastComponent(component, null, false);
        }
    }
	
	public void sendMixUrl(String string)
    {
    	// 正規表現パターンを定義（URLを見つけるための正規表現）
        String urlRegex = "https?://\\S+";
        Pattern pattern = Pattern.compile(urlRegex);
        Matcher matcher = pattern.matcher(string);

        // URLリストとテキストリストを作成
        List<String> urls = new ArrayList<>();
        List<String> textParts = new ArrayList<>();
        
        int lastMatchEnd = 0;
        
        Boolean isUrl = false;
        while (matcher.find())
        {
        	// URLが含まれていたら
        	isUrl = true;
        	
            // マッチしたURLをリストに追加
            urls.add(matcher.group());
            
            // URLの前のテキスト部分をリストに追加
            textParts.add(string.substring(lastMatchEnd, matcher.start()));
            lastMatchEnd = matcher.end();
        }
        
    	// URLが含まれてなかったら
        if(!isUrl)
        {
        	//if (string.contains("\\n")) string = string.replace("\\n", "\n");
        	bc.broadcastMessage(string, NamedTextColor.AQUA, null);
        	return;
        }
        
        // 最後のURLの後のテキスト部分を追加
        if (lastMatchEnd < string.length()) 
        {
            textParts.add(string.substring(lastMatchEnd));
        }
        

        // テキスト部分を結合
        TextComponent component = Component.text().build();
        
        int textPartsSize = textParts.size();
        int urlsSize = urls.size();
        
        for (int i = 0; i < textPartsSize; i++)
        {
        	Boolean isText = false;
        	if(Objects.nonNull(textParts) && textPartsSize != 0)
        	{
        		String text = null;
        		text = textParts.get(i);
        		
        		//if (text.contains("\\n")) text = text.replace("\\n", "\n");
        		TextComponent additionalComponent = null;
        		additionalComponent = Component.text()
        				.append(Component.text(text))
        				.color(NamedTextColor.AQUA)
        				.build();
        		component = component.append(additionalComponent);
        	}
        	else
        	{
        		isText = true;
        	}
        	
        	
        	// URLが1つなら、textPartsは2つになる。
        	// URLが2つなら、textPartsは3つになる。
        	//　ゆえ、最後の番号だけ考えなければ、
        	// 上で文字列にURLが含まれているかどうかを確認しているので、ぶっちゃけ以下のif文はいらないかも
        	//if(Objects.nonNull(urls) && urlsSize != 0)
        	if (i < urlsSize)
        	{
        		String getUrl = null;
        		if (isText)
        		{
        			// textがなかったら、先頭の改行は無くす(=URLのみ)
        			getUrl = urls.get(i);
        		}
        		else if (i != textPartsSize - 1)
            	{
            		getUrl = "\n"+urls.get(i)+"\n";
            	}
            	else
            	{
            		getUrl = "\n"+urls.get(i);
            	}
            	
        		TextComponent additionalComponent = null;
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
        
		bc.broadcastComponent(component,null,false);
		return;
    }
}
