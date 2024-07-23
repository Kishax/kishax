package velocity_command;

import java.io.IOException;

import com.velocitypowered.api.command.CommandSource;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import velocity.Config;
import velocity.Main;

public class Debug
{
	public Main plugin;
	public String value1 = null, value2 = null;
	
	public Debug(CommandSource source,String[] args)
	{
		this.plugin = Main.getInstance();
		
		if
		(
			!(((String) Config.getConfig().getOrDefault("Debug.Webhook_URL","")).isEmpty()) && 
			!(((String) Config.getConfig().getOrDefault("Discord.Webhook_URL","")).isEmpty())
		)
		{
			value1 = (String) Config.getConfig().get("Debug.Webhook_URL");
			value2 = (String) Config.getConfig().get("Discord.Webhook_URL");
			
			Config.getConfig().put("Discord.Webhook_URL", value1);
			Config.getConfig().put("Debug.Webhook_URL", value2);
			
			
			
			if(((boolean) Config.getConfig().get("Debug.Mode")))
			{
				source.sendMessage(Component.text("デバッグモードがOFFになりました。").color(NamedTextColor.GREEN));
				Config.getConfig().put("Debug.Mode", false);
				try
				{
					Config.getInstance().saveConfig();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
			else
			{
				source.sendMessage(Component.text("デバッグモードがONになりました。").color(NamedTextColor.GREEN));
				Config.getConfig().put("Debug.Mode", true);
				try
				{
					Config.getInstance().saveConfig();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}
		else
		{
			source.sendMessage(Component.text("コンフィグの設定が不十分です。").color(NamedTextColor.RED));
		}
	}
}
