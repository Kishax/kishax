package spigot_command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import spigot.Main;
import spigot.SocketSwitch;

public class CommandForward
{
	public common.Main plugin;
	public SocketSwitch ssw;
	
	public CommandForward(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args)
	{
		this.plugin = common.Main.getInstance();
		this.ssw = Main.getMaininstance().getSocket();
		String allcmd = "";
		for (String arg : args)
		{
			allcmd += " " + arg;
		}
		
		if (sender instanceof Player)
		{
			Player player = (Player) sender;
			// コマンドを打ったプレイヤー名をallcmdに乗せる
			allcmd = player.getName() + allcmd; 
		}
		else
		{
			// コンソールから打った場合
			allcmd = "?" + allcmd;
		}
		ssw.startSocketClient(allcmd);
	}
}
