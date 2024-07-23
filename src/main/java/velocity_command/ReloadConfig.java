package velocity_command;

import java.io.IOException;

import com.velocitypowered.api.command.CommandSource;

import net.kyori.adventure.text.Component;
import velocity.Config;

public class ReloadConfig
{
    public ReloadConfig(CommandSource source,String[] args)
    {
    	try
    	{
			Config.getInstance().loadConfig();
		}
    	catch (IOException e1)
    	{
			e1.printStackTrace();
		}
        source.sendMessage(Component.text("Plugin configuration reloaded."));
    }
}
