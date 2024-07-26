package spigot_command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.StringUtil;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;

public class FMCCommand implements CommandExecutor,TabExecutor{
	
	private List<String> subcommands = new ArrayList<>(Arrays.asList("reload","potion","medic","fly","test","fv"));
	public common.Main plugin;
	
	public FMCCommand(common.Main plugin2)
	{
		this.plugin = plugin2;
	}
	
	@Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args)
	{
    	if (args.length == 0 || !subcommands.contains(args[0].toLowerCase()))
    	{
    		BaseComponent[] component =
    			    new ComponentBuilder(ChatColor.YELLOW+"FMC COMMANDS LIST").bold(true).underlined(true)
    			    	.append(ChatColor.AQUA+"\n/fmc potion <effect type>")
    			        .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,"/fmc potion " ))
    			        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("半径10マスのエンティティにエフェクト付与します！(クリックしてコピー)")))
    			        .append(ChatColor.AQUA+"\n\n/fmc fly")
    			        .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,"/fmc fly"))
    			        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("空、飛べるよ！(クリックしてコピー)")))
    			        .append(ChatColor.AQUA+"\n\n/fmc reload")
    			        .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,"/fmc reload"))
    			        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("コンフィグ、リロードします！(クリックしてコピー)")))
    			        .append(ChatColor.AQUA+"\n\n/fmc test <arg-1>")
    			        .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,"/fmc test "))
    			        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("第一引数を返します！(クリックしてコピー)")))
    			        .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,"/fmc fv "))
    			        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("プロキシコマンドをフォワードします！(クリックしてコピー)")))
    			        .append(ChatColor.AQUA+"\n\n/fmc medic")
    			        .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,"/fmc medic"))
    			        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("ライフが回復します！(クリックしてコピー)")))
    			        .create();
    		sender.spigot().sendMessage(component);
    		return true;
    	}

    	if (!sender.hasPermission("fmc." + args[0]))
    	{
    		sender.sendMessage("access-denied");
    		return true;
    	}
    	

      switch (args[0].toLowerCase())
      {
      	  case "fv":
      		  new CommandForward(sender, cmd, label, args);
      		  return true;
      		  
	      case "reload":
	    	  new ReloadConfig(sender, cmd, label, args);
	    	  return true;
	    	  
	      case "potion":
	    	  new Potion(sender, cmd, label, args);
	    	  return true;
	    	  
	      case "medic":
	    	  if(!(sender instanceof Player))
	    	  {
	    		  sender.sendMessage(ChatColor.GREEN + "このプラグインはプレイヤーでなければ実行できません。");
	    		  return true;
	    	  }
	    	  Player player = (Player) sender;
	    	  //we see sender is player, by doing so, it can substitute player variable for sender
	    	  player.setHealth(20.0);
	    	  player.sendMessage(ChatColor.GREEN+"傷の手当てが完了しました。");
	    	  return true;
	    	  
	      case "fly":
	    	  new Fly(sender, cmd, label, args);
	    	  return true;
	    	  
	      case "test":
	    	  new Test(sender, cmd, label, args);
	    	  return true; 
	      }
	      return true;
	}

    @SuppressWarnings("deprecation")
	@Override
	public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args)
    {
    	List<String> ret = new ArrayList<>();

    	switch (args.length)
    	{
	    	case 1:
	    		for (String subcmd : subcommands)
	    		{
	    			if (!sender.hasPermission("fmc." + subcmd)) continue;
	      
	    			ret.add(subcmd);
	    		}
	    		return StringUtil.copyPartialMatches(args[0].toLowerCase(), ret, new ArrayList<String>());
	    	
	    	case 2:
	    		if (!sender.hasPermission("fmc." + args[0].toLowerCase())) return Collections.emptyList();
	    		switch (args[0].toLowerCase())
	    		{
	    			case "potion":
	    				for (PotionEffectType potion : PotionEffectType.values())
	    				{
	    					if (!sender.hasPermission("fmc.potion." + potion.getName().toLowerCase())) continue;    		   
	    					ret.add(potion.getName());
	    				}
	    				return StringUtil.copyPartialMatches(args[1].toLowerCase(), ret, new ArrayList<String>());
	    			case "test":
	    				
	    				return StringUtil.copyPartialMatches(args[1].toLowerCase(), ret, new ArrayList<String>());
	    		}
    	}
    	return Collections.emptyList();
    }
}
