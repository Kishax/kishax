package bungee_command;

import bungee.Config;
import bungee.Main;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;

public class Debug
{
	public Main plugin;
	public String value1 = null, value2 = null;
	
	public Debug(CommandSender sender, String[] args)
	{
		this.plugin = Main.getInstance();
		
		if
		(
			!(Config.getConfig().getString("Debug.Webhook_URL","").isEmpty()) && 
			!(Config.getConfig().getString("Discord.Webhook_URL","").isEmpty())
		)
		{
			value1 = Config.getConfig().getString("Debug.Webhook_URL");
			value2 = Config.getConfig().getString("Discord.Webhook_URL");
			
			Config.getConfig().set("Discord.Webhook_URL", value1);
			Config.getConfig().set("Debug.Webhook_URL", value2);
			
			
			
			if(Config.getConfig().getBoolean("Debug.Mode"))
			{
				sender.sendMessage(new TextComponent(ChatColor.GREEN+"デバッグモードがOFFになりました。"));
				Config.getConfig().set("Debug.Mode", false);
				Config.save();
			}
			else
			{
				sender.sendMessage(new TextComponent(ChatColor.GREEN+"デバッグモードがONになりました。"));
				Config.getConfig().set("Debug.Mode", true);
				Config.save();
			}
			
			new Config("bungee-config.yml",this.plugin);
		}
		else
		{
			sender.sendMessage(new TextComponent(ChatColor.RED+"コンフィグの設定が不十分です。"));
		}
	}
}
