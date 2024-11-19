package keyp.forev.fmc.cmd;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.google.inject.Inject;

import spigot.core.main.PortalsConfig;

public class ReloadConfig {

	private final keyp.forev.fmc.spigot.Main plugin;
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
