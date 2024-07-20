package bungee_command;

import bungee.Config;
import bungee.Main;
import bungee.PlayerList;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;

public class ReloadConfig
{
	public Main plugin;

	public ReloadConfig(CommandSender sender, String[] args)
	{
		this.plugin = Main.getInstance();
		
		new Config("bungee-config.yml",this.plugin);
		PlayerList.updatePlayers(); // プレイヤーリストをアップデート
		sender.sendMessage(new ComponentBuilder("コンフィグをリロードしました。").color(ChatColor.GREEN).create());
	}
}