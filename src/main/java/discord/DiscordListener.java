package discord;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import javax.security.auth.login.LoginException;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.proxy.ProxyServer;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import velocity.Config;
import velocity.Main;
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
    
    public CompletableFuture<Void> logoutDiscordBot()
    {
    	return CompletableFuture.runAsync(() ->
    	{
	    	if (Objects.nonNull(jda))
	    	{
	            jda.shutdown();
	            isDiscord = false;
	            logger.info("Discord-Botがログアウトしました。");
	        }
    	});
    }
    
    public void sendWebhookMessage(WebhookMessageBuilder builder)
    {
    	if(config.getString("Discord.Webhook_URL","").isEmpty()) return;
    		
        WebhookClient client = WebhookClient.withUrl(config.getString("Discord.Webhook_URL"));
        
        //.addField(new EmbedField(true, "フィールド1", "値1"))
        WebhookMessage message = builder.build();
        
        client.send(message).thenAccept(response ->
        {
        	//logger.info("Message sent with ID: " + response.getId());
        }).exceptionally(throwable ->
        {
            throwable.printStackTrace();
            return null;
        });
    }
    
    public CompletableFuture<Void> editBotEmbed(String messageId, String additionalDescription, boolean isChat) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        getBotMessage(messageId, currentEmbed -> {
            if (Objects.isNull(currentEmbed)) {
                future.completeExceptionally(new RuntimeException("No embed found to edit."));
                return;
            }

            String channelId = null;
            if (isChat) {
                if (config.getLong("Discord.ChatChannelId", 0) == 0 || !isDiscord) {
                    future.completeExceptionally(new RuntimeException("Chat channel ID is invalid or Discord is not enabled."));
                    return;
                }
                channelId = Long.valueOf(config.getLong("Discord.ChatChannelId")).toString();
            } else {
                if (config.getLong("Discord.ChannelId", 0) == 0 || !isDiscord) {
                    future.completeExceptionally(new RuntimeException("Channel ID is invalid or Discord is not enabled."));
                    return;
                }
                channelId = Long.valueOf(config.getLong("Discord.ChannelId")).toString();
            }

            TextChannel channel = jda.getTextChannelById(channelId);
            if (Objects.isNull(channel)) {
                future.completeExceptionally(new RuntimeException("Channel not found!"));
                return;
            }

            // 現在のEmbedに新しい説明を追加
            MessageEmbed newEmbed = addDescriptionToEmbed(currentEmbed, additionalDescription);
            MessageAction messageAction = channel.editMessageEmbedsById(messageId, newEmbed);

            messageAction.queue(
                success -> future.complete(null),
                error -> {
                    future.completeExceptionally(error);
                    logger.info("Failed to edit message with ID: " + messageId);
                }
            );
        }, isChat);

        return future;
    }


    
    public CompletableFuture<Void> editBotEmbed(String messageId, String additionalDescription)
    {
    	return editBotEmbed(messageId, additionalDescription, false);
    }
    
    public void getBotMessage(String messageId, Consumer<MessageEmbed> embedConsumer, boolean isChat)
    {
    	String channelId = null;
    	if(isChat)
    	{
    		if (config.getLong("Discord.ChatChannelId", 0) == 0 || !isDiscord) return;
    		channelId = Long.valueOf(config.getLong("Discord.ChatChannelId")).toString();
    	}
    	else
    	{
    		if (config.getLong("Discord.ChannelId", 0) == 0 || !isDiscord) return;
    		channelId = Long.valueOf(config.getLong("Discord.ChannelId")).toString();
    	}
        
        TextChannel channel = jda.getTextChannelById(channelId);
        
        if (Objects.isNull(channel))
        {
            //logger.info("Channel not found!");
            return;
        }
        
        channel.retrieveMessageById(messageId).queue(
            message ->
            {
                List<MessageEmbed> embeds = message.getEmbeds();
                //logger.info("Message retrieved with " + embeds.size() + " embeds.");
                //logger.info("Message Id: "+messageId);
                if (!embeds.isEmpty()) 
                {
                    // 最初のEmbedを取得して消費
                    embedConsumer.accept(embeds.get(0));
                } 
                else 
                {
                    logger.info("No embeds found in the message.");
                    embedConsumer.accept(null);
                }
            },
            error ->
            {
                error.printStackTrace();
                embedConsumer.accept(null);
            }
        );
    }
    
    public MessageEmbed addDescriptionToEmbed(MessageEmbed embed, String additionalDescription)
    {
        EmbedBuilder builder = new EmbedBuilder(embed);
        
        String existingDescription = embed.getDescription();
        String newDescription = (existingDescription != null ? existingDescription : "") + additionalDescription;
        
        builder.setDescription(newDescription);
        
        return builder.build();
    }
    
    public void editBotEmbedReplacedAll(String messageId, MessageEmbed newEmbed)
    {
    	 if (config.getLong("Discord.ChannelId", 0)==0 || !isDiscord) return;
    	 
        // チャンネルIDは適切に設定してください
        String channelId = Long.valueOf(config.getLong("Discord.ChannelId")).toString();
        TextChannel channel = jda.getTextChannelById(channelId);
        
        if(Objects.isNull(channel))
        {
        	//logger.info("Channel not found!");
        	return;
        }
        
        MessageAction messageAction = channel.editMessageEmbedsById(messageId, newEmbed);
        messageAction.queue(
            success -> {	},
            error -> error.printStackTrace()
        );
    }
    
    public CompletableFuture<String> sendBotMessageAndgetMessageId(String content, MessageEmbed embed, boolean isChat)
    {
    	CompletableFuture<String> future = new CompletableFuture<>();
    	
    	String channelId = null;
    	if(isChat)
    	{
    		if (config.getLong("Discord.ChatChannelId", 0) == 0 || !isDiscord)
    		{
    			future.complete(null);
                return future;
    		}
    		
    		channelId = Long.valueOf(config.getLong("Discord.ChatChannelId")).toString();
    	}
    	else
    	{
    		if (config.getLong("Discord.ChannelId", 0)==0 || !isDiscord)
            {
            	future.complete(null);
                return future;
            }
    		
    		channelId = Long.valueOf(config.getLong("Discord.ChannelId")).toString();
    	}
        
        TextChannel channel = jda.getTextChannelById(channelId);
        
        if (Objects.isNull(channel))
        {
        	logger.error("Channel not found!");
        	future.complete(null);
            return future;
        }
        
    	if (Objects.nonNull(embed))
        {
    		// 埋め込みメッセージを送信
            MessageAction messageAction = channel.sendMessageEmbeds(embed);
            messageAction.queue(response ->
            {
                // メッセージIDとチャンネルIDを取得
                String messageId = response.getId();
                future.complete(messageId);
                //logger.info("Message ID: " + messageId);
                //logger.info("Channel ID: " + channel.getId());
            }, failure -> 
            {
            	logger.error("Failed to send embedded message: " + failure.getMessage());
                future.complete(null);
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
                //logger.info("Message ID: " + messageId);
            	//logger.info("Channel ID: " + channel.getId());
            	future.complete(messageId);
            }, failure ->
            {
            	logger.error("Failed to send text message: " + failure.getMessage());
                future.complete(null);
            }
            );
    	}
    	
    	return future;
    }
    
    public CompletableFuture<String> sendBotMessageAndgetMessageId(String content)
    {
    	return sendBotMessageAndgetMessageId(content, null, false);
    }
    
    
    public CompletableFuture<String> sendBotMessageAndgetMessageId(MessageEmbed embed)
    {
    	return sendBotMessageAndgetMessageId(null, embed, false);
    }
    
    public CompletableFuture<String> sendBotMessageAndgetMessageId(String content, boolean isChat)
    {
    	return sendBotMessageAndgetMessageId(content, null, isChat);
    }
    
    public CompletableFuture<String> sendBotMessageAndgetMessageId(MessageEmbed embed, boolean isChat)
    {
    	return sendBotMessageAndgetMessageId(null, embed, isChat);
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
    
    public void sendBotMessage(String content, MessageEmbed embed)
    {
    	CompletableFuture<String> future = new CompletableFuture<>();
    	
        if (config.getLong("Discord.ChannelId", 0)==0 || !isDiscord)
        {
        	future.complete(null);
            return;
        }
        
    	String channelId = Long.valueOf(config.getLong("Discord.ChannelId")).toString();
        TextChannel channel = jda.getTextChannelById(channelId);
        
        if (Objects.isNull(channel))
        {
        	//logger.error("Channel not found!");
        	future.complete(null);
            return;
        }
        
    	if (Objects.nonNull(embed))
        {
    		// 埋め込みメッセージを送信
            MessageAction messageAction = channel.sendMessageEmbeds(embed);
            messageAction.queue(response -> {	},
            		failure -> logger.error("Failed to send embedded message: " + failure.getMessage())
            );
        }
    	
    	if(Objects.nonNull(content) && !content.isEmpty())
    	{
    		// テキストメッセージを送信
    		MessageAction messageAction = channel.sendMessage(content);
            messageAction.queue(response -> {	},
            		failure -> logger.error("Failed to send text message: " + failure.getMessage())
            );
    	}
    }
    
    public void sendBotMessage(String content)
    {
    	sendBotMessage(content, null);
    }
    
    public void sendBotMessage(MessageEmbed embed)
    {
    	sendBotMessage(null, embed);
    }
}
