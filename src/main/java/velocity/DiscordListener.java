package velocity;

import java.util.Objects;

import javax.security.auth.login.LoginException;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.ProxyServer;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.kyori.adventure.text.Component;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import club.minnced.discord.webhook.send.WebhookMessage;

public class DiscordListener
{
	private static JDA jda = null;
	private final Main plugin;
	private final ProxyServer server;
    private final Logger logger;
    private final Config config;
    private final ConsoleCommandSource console;
    public static boolean isDiscord = false;
    
    @Inject
    public DiscordListener(Main plugin, ProxyServer server, Logger logger, Config config, ConsoleCommandSource console)
    {
    	this.plugin = plugin;
    	this.server = server;
    	this.logger = logger;
    	this.config = config;
    	this.console = console;
    }
    
    //sendWebhookMessage("メンテナンスモードが無効になりました。\\nまだまだ遊べるドン！", "サーバー", "https://keypforev.ddns.net/assets/img/icon/donchan.png");
    public void loginDiscordBotAsync()
    {
    	if(config.getString("Discord.Token","").isEmpty()) return;
    	server.getScheduler().buildTask(plugin, () ->
    	{
			try
			{
				jda = JDABuilder.createDefault(config.getString("Discord.Token")).build();
				jda.awaitReady();
				isDiscord = true;
				logger.info("Discord-Botがログインしました。");
				sendBotMessageAsync("テスト");
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
    
    public void sendWebhookMessage(String content, String username, String avatarUrl)
    {
    	if(config.getString("Discord.Webhook_URL","").isEmpty()) return;
    		
        WebhookClient client = WebhookClient.withUrl(config.getString("Discord.Webhook_URL"));
        WebhookMessageBuilder builder = new WebhookMessageBuilder();
        builder.setContent(content);
        builder.setUsername(username);
        builder.setAvatarUrl(avatarUrl);
        
        WebhookEmbed embed = new WebhookEmbedBuilder()
            .setColor(0xFF0000)  // Embedの色
            .setDescription("このメッセージは埋め込みです。")
            //.addField(new EmbedField(true, "フィールド1", "値1"))
            .build();

        builder.addEmbeds(embed);
        
        WebhookMessage message = builder.build();
        client.send(message).thenAccept(response ->
        {
        	console.sendMessage(Component.text("Message sent with ID: " + response.getId()));
        }).exceptionally(throwable ->
        {
            throwable.printStackTrace();
            return null;
        });
    }
    
    public void sendBotMessageAsync(String content)
    {
        if (config.getLong("Discord.ChannelId", 0)==0 || !isDiscord) return;
        
        server.getScheduler().buildTask(plugin, () ->
        {
            TextChannel channel = jda.getTextChannelById(Long.valueOf(config.getLong("Discord.ChannelId")).toString());
            
            if (Objects.nonNull(channel))
            {
                MessageAction messageAction = channel.sendMessage(content);
                messageAction.queue(response ->
                {
                    logger.info("Message sent: " + response.getId());
                    // メッセージIDとチャンネルIDを取得
                    String messageId = response.getId();
                    logger.info("Message ID: " + messageId);
                	logger.info("Channel ID: " + channel.getId());
                });
            }
            else
            {
                logger.error("Channel not found!");
            }
        }).schedule();
    }
}
