package spigot_command;

import java.util.Objects;

import org.bukkit.command.CommandSender;


public class Test
{
	public common.Main plugin;
	
	public Test(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args)
	{
		this.plugin = common.Main.getInstance();
		
		if(args.length == 1 || Objects.isNull(args[1]) || args[1].isEmpty())
  	  	{
			sender.sendMessage("引数を入力してください。");
			return;
  	  	}
  	  	sender.sendMessage("第1引数: "+args[1]);
  	  	return;
	}
}
