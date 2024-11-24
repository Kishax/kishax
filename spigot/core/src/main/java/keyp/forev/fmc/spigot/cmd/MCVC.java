package keyp.forev.fmc.spigot.cmd;

import java.util.Objects;

import org.bukkit.command.CommandSender;

import com.google.inject.Inject;

import net.md_5.bungee.api.ChatColor;

import org.bukkit.plugin.java.JavaPlugin;

public class MCVC {
	private final JavaPlugin plugin;
	
	@Inject
	public MCVC(JavaPlugin plugin) {
		this.plugin = plugin;
	}
	
	public void execute(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
		if (Objects.isNull(plugin.getConfig().getBoolean("MCVC.Mode"))) {
			sender.sendMessage(ChatColor.RED+"コンフィグの設定が不十分です。");
			return;
		}
		if (plugin.getConfig().getBoolean("MCVC.Mode")) {
			sender.sendMessage(ChatColor.GREEN+"MCVCモードがOFFになりました。");
			plugin.getConfig().set("MCVC.Mode", false);
			plugin.reloadConfig();
		} else {
			sender.sendMessage("MCVCモードがONになりました。");
			plugin.getConfig().set("MCVC.Mode", true);
			plugin.reloadConfig();
		}
	}
}
