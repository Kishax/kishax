package spigot_command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.google.inject.Inject;

import spigot.SocketSwitch;

public class CommandForward {

	private final SocketSwitch ssw;
	
	@Inject
	public CommandForward(SocketSwitch ssw) {
		this.ssw = ssw;
	}
	
	public void execute(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
		String allcmd = "";
		for (String arg : args) {
			allcmd += " " + arg;
		}
		
		if (sender instanceof Player player) {
			player = (Player) sender;
			// コマンドを打ったプレイヤー名をallcmdに乗せる
			allcmd = player.getName() + allcmd; 
		} else {
			// コンソールから打った場合
			allcmd = "?" + allcmd;
		}
		
		ssw.startSocketClient(allcmd);
	}
}
