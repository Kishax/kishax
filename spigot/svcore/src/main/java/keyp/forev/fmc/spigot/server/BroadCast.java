package keyp.forev.fmc.spigot.server;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.inject.Inject;

import net.md_5.bungee.api.ChatColor;
import net.kyori.adventure.text.Component;

public class BroadCast {
	private final JavaPlugin plugin;
	@Inject
	public BroadCast(JavaPlugin plugin) {
		this.plugin = plugin;
	}
	
	public void broadCastMessage(String message) {
		for (Player player : plugin.getServer().getOnlinePlayers()) {
		    player.sendMessage(ChatColor.RED + message);
		}
	}

	public void broadCastMessage(Component component) {
		for (Player player : plugin.getServer().getOnlinePlayers()) {
		    player.sendMessage(component);
		}
	}
}
