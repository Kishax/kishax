package bungee_command;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import bungee.Main;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

public class FMCCommand extends Command implements TabExecutor
{
	public Main plugin;
	public List<String> subcommands = new ArrayList<>(Arrays.asList("debug","hub","reload","ss","req","start","stp","retry","debug","cancel"));
	public List<String> anylists = new ArrayList<>(Arrays.asList("true","false"));
	
    public FMCCommand(Main plugin) {
        super("fmcb");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args)
    {
        if (args.length == 0 || !subcommands.contains(args[0].toLowerCase()))
        {
        	if(sender.hasPermission("fmc.bungee.commandslist"))
        	{
        		BaseComponent[] component =
        			    new ComponentBuilder(ChatColor.YELLOW+"FMC COMMANDS LIST").bold(true).underlined(true)
        			    	.append(ChatColor.AQUA+"\n/hub")
        			    	.append(ChatColor.AQUA+"\n/fmcb hub")
        			        .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,"/fmcb hub" ))
        			        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("ホームサーバーに帰還")))
        			        .append(ChatColor.AQUA+"\n\n/fmcb ss <server>")
        			        .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,"/fmcb ss "))
        			        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("サーバーステータス取得")))
        			        .append(ChatColor.AQUA+"\n\n/fmcb start <server>")
        			        .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,"/fmcb start "))
        			        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("サーバーを起動")))
        			        .append(ChatColor.AQUA+"\n\n/fmcb req <server>")
        			        .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,"/fmcb req"))
        			        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("サーバー起動リクエスト")))
        			        .append(ChatColor.AQUA+"\n\n/fmcb cancel")
        			        .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,"/fmcb cancel"))
        			        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("「キャンセルしました」メッセージ")))
        			        .append(ChatColor.AQUA+"\n\n/fmcb debug")
        			        .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,"/fmcb debug"))
        			        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("管理者専用デバッグモード")))
        			        .append(ChatColor.AQUA+"\n\n/fmcb reload")
        			        .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,"/fmcb reload"))
        			        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("コンフィグ、リロード")))
        			        .create();
        		sender.sendMessage(component);
        	}
        	else
        	{
        		sender.sendMessage(new TextComponent(ChatColor.RED+"You do not have permission for fmc commands."));
        	}
            return;
        }

        String subCommand = args[0];

        switch (subCommand.toLowerCase())
        {
            case "debug":
                new Debug(sender, args);
                break;
                
            case "start":
            	new StartServer(sender, args);
            	break;
            
            case "ss":
            	new SetServer(sender, args);
                break;
            	
            case "hub":
            	new Hub().execute(sender, args);
            	break;

            case "retry":
            	new Retry(sender, args);
            	break;
            
            case "reload":
            	new ReloadConfig(sender, args);
            	break;
            	
            case "stp":
            	new ServerTeleport(sender, args);
            	break;
            	
            case "req":
            	new Request(sender, args);
            	break;
            	
            case "cancel":
            	new Cancel(sender,args);
            	break;
            	
            default:
                sender.sendMessage(new TextComponent("Unknown subcommand: " + subCommand));
        }
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args)
    {
    	List<String> ret = new ArrayList<>();
        
        switch (args.length)
    	{
	    	case 1:
	    		for (String subcmd : subcommands)
	    		{
	    			if (!sender.hasPermission("fmc.bungee." + subcmd)) continue;
	      
	    			ret.add(subcmd);
	    		}
	    		return ret;
	    	
	    	case 2:
	    		
	    		if (!sender.hasPermission("fmc.bungee." + args[0].toLowerCase())) return Collections.emptyList();
	    		
	    		switch (args[0].toLowerCase())
	    		{
	    			case "something":
	    				for (String any : anylists)
	    				{
	    					//if (!sender.hasPermission("fmc.bungee.debug." + any)) continue;    		   
	    					ret.add(any);
	    				}
	    				return ret;
	    				
	    			case "start":
	    			case "ss":
	    			case "req":
	    			case "stp":
	    				for (ServerInfo serverInfo : ProxyServer.getInstance().getServers().values())
	    		        {
	    		            ret.add(serverInfo.getName());
	    		        }
	    				return ret;
	    		}
    	}
        return Collections.emptyList();
    }
}
