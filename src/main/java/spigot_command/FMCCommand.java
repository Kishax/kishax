package spigot_command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.StringUtil;

import com.google.inject.Inject;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import spigot.Main;

public class FMCCommand implements TabExecutor
{
	private final List<String> subcommands = new ArrayList<>(Arrays.asList("reload","test","fv","mcvc"));
	
	@Inject
	public FMCCommand()
	{
		//
	}
	
	@Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args)
	{
		if(sender == null) return true;

    	if (args.length == 0 || !subcommands.contains(args[0].toLowerCase()))
    	{
    		BaseComponent[] component =
    			    new ComponentBuilder(ChatColor.YELLOW+"FMC COMMANDS LIST").bold(true).underlined(true)
    			        .append(ChatColor.AQUA+"\n\n/fmc reload")
    			        .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,"/fmc reload"))
    			        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("コンフィグ、リロードします！(クリックしてコピー)")))
    			        .append(ChatColor.AQUA+"\n\n/fmc test <arg-1>")
    			        .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,"/fmc test "))
    			        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("第一引数を返します！(クリックしてコピー)")))
    			        .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,"/fmc fv "))
    			        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("プロキシコマンドをフォワードします！(クリックしてコピー)")))
    			        .append(ChatColor.AQUA+"\n\n/fmcb mcvc")
                        .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/fmcp mcvc"))
                        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("MCVCモードの切り替えを行います！(クリックしてコピー)")))
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
			case "fv" -> 
			{
				Main.getInjector().getInstance(CommandForward.class).execute(sender, cmd, label, args);
				return true;
			}
				
			case "reload" -> 
			{
				Main.getInjector().getInstance(ReloadConfig.class).execute(sender, cmd, label, args);
				return true;
			}
				
			case "test" -> 
			{
				Main.getInjector().getInstance(Test.class).execute(sender, cmd, label, args);
				return true;
			}
				
			case "mcvc" -> 
			{
				Main.getInjector().getInstance(MCVC.class).execute(sender, cmd, label, args);
				return true; 
			}
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
	    	case 1 -> 
			{
				for (String subcmd : subcommands)
				{
					if (!sender.hasPermission("fmc." + subcmd)) continue;
					
					ret.add(subcmd);
				}
				return StringUtil.copyPartialMatches(args[0].toLowerCase(), ret, new ArrayList<>());
			}
	    	
	    	case 2 -> 
			{
				if (!sender.hasPermission("fmc." + args[0].toLowerCase())) return Collections.emptyList();
				switch (args[0].toLowerCase())
				{
					case "potion" -> 
					{
						for (PotionEffectType potion : PotionEffectType.values())
						{
							if (!sender.hasPermission("fmc.potion." + potion.getName().toLowerCase())) continue;
							ret.add(potion.getName());
						}
						return StringUtil.copyPartialMatches(args[1].toLowerCase(), ret, new ArrayList<>());
					}
					case "test" -> 
					{
						return StringUtil.copyPartialMatches(args[1].toLowerCase(), ret, new ArrayList<>());
					}
				}
            }
    	}

    	return Collections.emptyList();
    }
}