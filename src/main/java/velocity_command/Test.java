package velocity_command;


import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;

import discord.DiscordInterface;
import net.kyori.adventure.text.Component;

public class Test
{
	private final Logger logger;
	private final DiscordInterface discord;
	
	@Inject
	public Test(Logger logger, DiscordInterface discord)
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
				/*for(Map.Entry<String, String> entry : EventListener.PlayerMessageIds.entrySet())
				{
					logger.info("PlayerMessageIdkey: " + entry.getKey());
					logger.info("PlayerMessageIdvalue: " + entry.getValue());
				}
				
				logger.info("PlayerChatMessageId: "+DiscordEventListener.PlayerChatMessageId);*/
				for(Entry<String, Boolean> entry : Request.PlayerReqFlags.entrySet())
				{
					source.sendMessage(Component.text("PlayerMessageIdkey: " + entry.getKey()));
					source.sendMessage(Component.text("PlayerMessageIdvalue: " + entry.getValue()));
				}
				
				break;
				
			case 2:
				//discord.editBotEmbed(args[1], "\n追加メッセージです。");
				break;
				
			default:
				break;
		}
	}
}
