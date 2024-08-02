package spigot_command;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.google.inject.Inject;

public class ReloadConfig
{
	private final common.Main plugin;
	
	@Inject
	public ReloadConfig(common.Main plugin)
	{
		this.plugin = plugin;
	}
	
	public void execute(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args)
	{
		plugin.reloadConfig();
		sender.sendMessage(ChatColor.GREEN+"コンフィグをリロードしました。");
	}
}
