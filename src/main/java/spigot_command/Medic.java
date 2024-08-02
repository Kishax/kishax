package spigot_command;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.google.inject.Inject;

public class Medic
{
	@Inject
	public Medic()
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
  	  player.setHealth(20.0);
  	  player.sendMessage(ChatColor.GREEN+"傷の手当てが完了しました。");
	}
}
