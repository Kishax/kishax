package keyp.forev.fmc.cmd;

import java.util.Objects;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.google.inject.Inject;

public class MCVC {
	private final keyp.forev.fmc.spigot.Main plugin;
	
	@Inject
	public MCVC(common.Main plugin) {
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
