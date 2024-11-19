package keyp.forev.fmc.spigot.cmd;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.google.inject.Inject;

import keyp.forev.fmc.spigot.util.PortalsConfig;
import org.bukkit.plugin.java.JavaPlugin;

public class ReloadConfig {

	private final JavaPlugin plugin;
	private final PortalsConfig psConfig;

	@Inject
	public ReloadConfig(JavaPlugin plugin, PortalsConfig psConfig) {
		this.plugin = plugin;
		this.psConfig = psConfig;
	}
	
	public void execute(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
		plugin.reloadConfig();
		psConfig.reloadPortalsConfig();
		sender.sendMessage(ChatColor.GREEN+"コンフィグをリロードしました。");
	}
}
