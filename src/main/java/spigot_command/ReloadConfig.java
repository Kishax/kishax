package spigot_command;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.google.inject.Inject;

import spigot.PortalsConfig;

public class ReloadConfig {

	private final common.Main plugin;
	private final PortalsConfig psConfig;

	@Inject
	public ReloadConfig(common.Main plugin, PortalsConfig psConfig) {
		this.plugin = plugin;
		this.psConfig = psConfig;
	}
	
	public void execute(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
		plugin.reloadConfig();
		psConfig.reloadPortalsConfig();
		sender.sendMessage(ChatColor.GREEN+"コンフィグをリロードしました。");
	}
}
