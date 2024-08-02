package spigot_command;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.google.inject.Inject;

public class Fly
{
	@Inject
	public Fly(common.Main plugin)
	{
		//
	}
	
	public void execute(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args)
	{
		if(!(sender instanceof Player))
  	  	{
  		  	sender.sendMessage(ChatColor.GREEN + "このプラグインはプレイヤーでなければ実行できません。");
  		  	return;
  	  	}
  	  	Player player = (Player) sender;
  	  	//we see sender is player, by doing so, it can substitute player variable for sender
  	  	player.setAllowFlight(true);
  	  	player.sendMessage(ChatColor.GREEN+"サバイバルで飛べるようになりました。");
	}
}
