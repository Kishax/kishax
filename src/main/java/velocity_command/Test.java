package velocity_command;

import java.util.Map;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;

import discord.DiscordEventListener;
import discord.DiscordListener;
import velocity.EventListener;

public class Test
{
	private final Logger logger;
	private final DiscordListener discord;
	
	@Inject
	public Test(Logger logger, DiscordListener discord)
	{
		this.logger = logger;
		this.discord = discord;
	}
	
	public void execute(CommandSource source, String[] args)
	{
		//Main.getInjector().getInstance(EmojiManager.class).checkAndAddEmojis();
		switch(args.length)
		{
			case 1:
				//logger.info(args[0]);
				for(Map.Entry<String, String> entry : EventListener.PlayerMessageIds.entrySet())
				{
					logger.info("PlayerMessageIdkey: " + entry.getKey());
					logger.info("PlayerMessageIdvalue: " + entry.getValue());
				}
				
				logger.info("PlayerChatMessageId: "+DiscordEventListener.PlayerChatMessageId);
				break;
				
			case 2:
				logger.info(args[1]);
				/*MessageEmbed joinEmbed = discord.createEmbed
				(
					"編集に成功しました。",
					ColorUtil.GREEN.getRGB()
				);*/
				discord.editBotEmbed(args[1], "\n追加メッセージです。");
				break;
				
			default:
				break;
		}
	}
}
