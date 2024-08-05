package velocity_command;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;

import net.kyori.adventure.text.Component;
import velocity.Config;
import velocity.EmojiManager;
import velocity.Main;

public class Test
{
	@Inject
	public Test()
	{
		//
	}
	
	public void execute(CommandSource source, String[] args)
	{
		//Main.getInjector().getInstance(EmojiManager.class).checkAndAddEmojis();
		Main.getInjector().getInstance(EmojiManager.class).createEmoji("testemoji", "https://minotar.net/avatar/98242585b5ab492095540e4f3f899349");
		return;
	}
}
