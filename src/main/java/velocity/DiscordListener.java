package velocity;

import java.util.Objects;

import javax.security.auth.login.LoginException;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.proxy.ProxyServer;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import club.minnced.discord.webhook.send.WebhookMessage;

public class DiscordListener
{
	public static JDA jda = null;
	
	private final Main plugin;
	private final ProxyServer server;
    private final Logger logger;
    private final Config config;
    public static boolean isDiscord = false;
    
    @Inject
    public DiscordListener(Main plugin, ProxyServer server, Logger logger, Config config)
    {
    	this.plugin = plugin;
    	this.server = server;
    	this.logger = logger;
    	this.config = config;
    }
    
    //sendWebhookMessage("メンテナンスモードが無効になりました。\\nまだまだ遊べるドン！", "サーバー", "https://keypforev.ddns.net/assets/img/icon/donchan.png");
    public void loginDiscordBotAsync()
    {
    	if(config.getString("Discord.Token","").isEmpty()) return;
    	server.getScheduler().buildTask(plugin, () ->
    	{
			try
			{
				jda = JDABuilder.createDefault(config.getString("Discord.Token"))
						.addEventListeners(Main.getInjector().getInstance(DiscordEventListener.class))
						.build();
				jda.awaitReady();
				
				// ステータスメッセージを設定
	            jda.getPresence().setActivity(Activity.playing("FMCサーバー"));
	            
				isDiscord = true;
				logger.info("Discord-Botがログインしました。");
			}
			catch (LoginException | InterruptedException e)
			{
				// スタックトレースをログに出力
	            logger.error("An discord-bot-login error occurred: " + e.getMessage());
	            for (StackTraceElement element : e.getStackTrace()) 
	            {
	                logger.error(element.toString());
	            }
			}
    	}).schedule();
    }
    
    public void logoutDiscordBot()
    {
    	if (Objects.nonNull(jda))
    	{
            jda.shutdown();
            isDiscord = false;
            logger.info("Discord-Botがログアウトしました。");
        }
    }
    
    public void sendWebhookMessage(WebhookMessageBuilder builder)
    {
    	if(config.getString("Discord.Webhook_URL","").isEmpty()) return;
    		
        WebhookClient client = WebhookClient.withUrl(config.getString("Discord.Webhook_URL"));
        
        //.addField(new EmbedField(true, "フィールド1", "値1"))
        WebhookMessage message = builder.build();
        
        client.send(message).thenAccept(response ->
        {
        	logger.info("Message sent with ID: " + response.getId());
        }).exceptionally(throwable ->
        {
            throwable.printStackTrace();
            return null;
        });
    }
    
    public void editWebhookMessage(String messageId, String newContent)
    {
    	 if (config.getLong("Discord.ChannelId", 0)==0 || !isDiscord) return;
    	 
        // チャンネルIDは適切に設定してください
        String channelId = Long.valueOf(config.getLong("Discord.ChannelId")).toString();
        TextChannel channel = jda.getTextChannelById(channelId);
        
        if (Objects.nonNull(channel))
        {
            MessageAction messageAction = channel.editMessageById(messageId, newContent);
            messageAction.queue(
                success -> logger.info("Message edited successfully"),
                error -> error.printStackTrace()
            );
        }
        else
        {
        	logger.info("Channel not found!");
        }
    }
    
    public void sendBotMessageAsync(String content, MessageEmbed embed)
    {
        if (config.getLong("Discord.ChannelId", 0)==0 || !isDiscord) return;
        
        server.getScheduler().buildTask(plugin, () ->
        {
        	String channelId = Long.valueOf(config.getLong("Discord.ChannelId")).toString();
            TextChannel channel = jda.getTextChannelById(channelId);
            
            if (Objects.nonNull(channel))
            {
            	if (Objects.nonNull(embed))
                {
            		// 埋め込みメッセージを送信
                    MessageAction messageAction = channel.sendMessageEmbeds(embed);
                    messageAction.queue(response ->
                    {
                        // メッセージIDとチャンネルIDを取得
                        String messageId = response.getId();
                        logger.info("Message ID: " + messageId);
                        logger.info("Channel ID: " + channel.getId());
                    });
                }
            	
            	if(Objects.nonNull(content) && !content.isEmpty())
            	{
            		// テキストメッセージを送信
            		MessageAction messageAction = channel.sendMessage(content);
                    messageAction.queue(response ->
                    {
                        // メッセージIDとチャンネルIDを取得
                        String messageId = response.getId();
                        logger.info("Message ID: " + messageId);
                    	logger.info("Channel ID: " + channel.getId());
                    });
            	}
                
            }
            else
            {
                logger.error("Channel not found!");
            }
        }).schedule();
    }
    
    public void sendBotMessageAsync(String content)
    {
    	sendBotMessageAsync(content,null);
    }
    
    
    public void sendBotMessageAsync(MessageEmbed embed)
    {
    	sendBotMessageAsync(null, embed);
    }
    
    public MessageEmbed createEmbed(String description, int color)
    {
        return new MessageEmbed(
            null, // URL
            null, // Title
            description, // Description
            null, // Type
            null, // Timestamp
            color, // Color
            null, // Thumbnail
            null, // SiteProvider
            null, // Author
            null, // VideoInfo
            null, // Footer
            null, // Image(Example: new MessageEmbed.ImageInfo(imageUrl, null, 0, 0))
            null  // Fields
        );
    }
}
