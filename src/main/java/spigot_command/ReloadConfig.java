package spigot_command;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import spigot.Config;

public class ReloadConfig
{
	public common.Main plugin;
	
	public ReloadConfig(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args)
	{
		this.plugin = common.Main.getInstance();
		
		this.plugin.reloadConfig();
		FileConfiguration config = this.plugin.getConfig();
		new Config(config);
		sender.sendMessage(ChatColor.GREEN+"コンフィグをリロードしました。");
	}
}
