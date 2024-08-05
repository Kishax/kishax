package velocity_command;

import java.util.Objects;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;

import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import common.ColorUtil;
import net.kyori.adventure.text.Component;
import velocity.Config;
import velocity.DiscordListener;
import velocity.EmojiManager;
import velocity.Main;

public class Test
{
	private final Logger logger;
	private final DiscordListener discord;
	private WebhookMessageBuilder builder = null;
	private WebhookEmbed embed = null;
	
	@Inject
	public Test(Logger logger, DiscordListener discord)
	{
		this.logger = logger;
		this.discord = discord;
	}
	
	public void execute(CommandSource source, String[] args)
	{
		//Main.getInjector().getInstance(EmojiManager.class).checkAndAddEmojis();
		String emojiName = "testemoji";
		String emojiId = Main.getInjector().getInstance(EmojiManager.class)
			.createOrgetEmojiId(emojiName, "https://minotar.net/avatar/98242585b5ab492095540e4f3f899349");
		if(Objects.nonNull(emojiId))
		{
			logger.info("emojiId: " + emojiId);
			String playerEmoji = "<:" + emojiName + ":" + emojiId + ">";
			
			builder = new WebhookMessageBuilder();
	        builder.setUsername("test");
	        //builder.setAvatarUrl(avatarUrl);
	        embed = new WebhookEmbedBuilder()
	            .setColor(ColorUtil.ORANGE.getRGB())  // Embedの色
	            .setDescription("test-player"+playerEmoji+"がtestサーバーに初参加です！")
	            .build();
	        builder.addEmbeds(embed);
	        discord.sendWebhookMessage(builder);
		}
		else
		{
			logger.error("emojiIdがnullです。");
		}
	}
}
