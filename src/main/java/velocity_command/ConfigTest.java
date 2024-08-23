package velocity_command;

import org.jetbrains.annotations.NotNull;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;

import net.kyori.adventure.text.Component;
import velocity.Config;

public class ConfigTest
{
	private final Config config;
	
	@Inject
	public ConfigTest(Config config)
	{
		this.config = config;
	}
	
	public void execute(@NotNull CommandSource source, String[] args)
	{
		if(config.getString("Debug.Test","").isEmpty())
		{
			source.sendMessage(Component.text("Config not given"));
			return;
		}
		
		source.sendMessage(Component.text(config.getString("Debug.Test")));
	}
}
