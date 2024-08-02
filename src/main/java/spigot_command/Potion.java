package spigot_command;

import java.util.Objects;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.google.inject.Inject;

public class Potion
{
	@Inject
	public Potion(common.Main plugin)
	{
		//
	}
	
	public void execute(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args)
	{
		if(!(sender instanceof Player))
  	  	{
  		  	sender.sendMessage(ChatColor.RED+"このコマンドはプレイヤーにしか実行できません！");
  		  	return;
  		}
	  	Player player = (Player) sender;
	  	//エフェクト名を入れてあるか
	  	if(args.length == 1 || Objects.isNull(args[1]) || args[1].isEmpty())
	  	{
	  		player.sendMessage(ChatColor.RED+"エフェクト名を入力してください。");
	  		return;
	  	}
	  	if(containsPotionEffectType(args[1]))
	  	{
	  		for(Entity entity : player.getNearbyEntities(10,10,10))
	  		{
	  			if(entity instanceof LivingEntity)
	  			{
	  				  ((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.getByName(args[1]),200,5));
	  			}
	  		}
	  		return;
	  	}
	  	else
	  	{
	  		player.sendMessage(ChatColor.RED + "正しいエフェクト名を入力してください。");
	  		return;
	  	}
	}
	
	private boolean containsPotionEffectType(String string)
    {
  	  	if(Objects.isNull(string) || string.isEmpty())
  	  	{
  	  		return false;
  	  	}
    	@SuppressWarnings("deprecation")
		PotionEffectType effectType = PotionEffectType.getByName(string);
    	if(effectType == null) return false;
    	return true;
    }
}
