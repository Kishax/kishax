package spigot_command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.google.inject.Inject;
import com.google.inject.Provider;

import spigot.SocketSwitch;

public class CommandForward {
	private final Provider<SocketSwitch> sswProvider;
	@Inject
	public CommandForward(Provider<SocketSwitch> sswProvider) {
		this.sswProvider = sswProvider;
	}
	
	public void execute(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
		// fv <player> fmcp <command>
		String allcmd = String.join(" ", args);
		if (sender instanceof Player player) {
			allcmd = player.getName() + " " + allcmd; 
		} else {
			allcmd = "? " + allcmd;
		}
		SocketSwitch ssw = sswProvider.get();
		ssw.sendVelocityServer(allcmd);
	}

	public void executeProxyCommand(Player player, String allcmd) {
		// fmcp <command>
		String playerName = player.getName();
		allcmd = playerName + " fv " + playerName + " " + allcmd;
		SocketSwitch ssw = sswProvider.get();
		ssw.sendVelocityServer(allcmd);
	}
}
