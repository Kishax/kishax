package velocity_command;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;

import net.kyori.adventure.text.Component;
import velocity.Config;

public class Test
{
	private final Config config;
	
	@Inject
	public Test(Config config)
	{
		this.config = config;
	}
	
	public void execute(CommandSource source, String[] args)
	{
		if(config.getString("Debug.Test","").isEmpty())
		{
			source.sendMessage(Component.text("Config not given"));
			return;
		}
		
		source.sendMessage(Component.text(config.getString("Debug.Test")));
		return;
	}
}
