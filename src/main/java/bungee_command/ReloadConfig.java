package bungee_command;

import bungee.Config;
import bungee.Main;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;

public class ReloadConfig
{
	public Main plugin;

	public ReloadConfig(CommandSender sender, String[] args)
	{
		this.plugin = Main.getInstance();
		
		new Config("config.yml",this.plugin);
		sender.sendMessage(new ComponentBuilder("コンフィグをリロードしました。").color(ChatColor.GREEN).create());
	}
}