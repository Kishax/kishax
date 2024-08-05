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
		Main.getInjector().getInstance(EmojiManager.class).checkAndAddEmojis();
	}
}
