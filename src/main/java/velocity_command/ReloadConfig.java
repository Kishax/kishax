package velocity_command;

import java.io.IOException;

import org.jetbrains.annotations.NotNull;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;

import net.kyori.adventure.text.Component;
import velocity.Config;

public class ReloadConfig
{
	private final Config config;
	
	@Inject
	public ReloadConfig(Config config)
	{
		this.config = config;
	}
	
    public void execute(@NotNull CommandSource source, String[] args)
    {
    	try
    	{
    		config.loadConfig();
			//Config.getInstance().loadConfig();
		}
    	catch (IOException e1)
    	{
			e1.printStackTrace();
		}
        source.sendMessage(Component.text("Plugin configuration reloaded."));
    }
}
