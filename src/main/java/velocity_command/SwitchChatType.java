package velocity_command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import velocity.Config;

public class SwitchChatType
{
	private final Config config;
	private TextComponent component = null;
	public static List<String> args1 = new ArrayList<>(Arrays.asList("switch","status"));
	
	@Inject
	public SwitchChatType
	(
		Config config
	)
	{
		this.config = config;
	}

	public void execute(CommandSource source,String[] args)
	{
		switch(args.length)
        {
        	case 0:
        	case 1:
        		source.sendMessage(Component.text("usage: /fmcp　chat <switch|status>").color(NamedTextColor.GREEN));
            	break;
            	
        	case 2:
    			switch(args[1].toLowerCase())
        		{
        			case "status":
        				
        				if (config.getBoolean("Discord.ChatType", false))
        	            {
        					component = Component.text("現在のチャットタイプは、編集Embedメッセージタイプです。").color(NamedTextColor.GREEN);
        	            }
        				else
        				{
        					component = Component.text("現在のチャットタイプは、プレーンテキストメッセージタイプです。").color(NamedTextColor.GREEN);
        				}
        				source.sendMessage(component);
        				break;
        				
        			default:
        				source.sendMessage(Component.text("usage: /fmcp　chat <switch|status>").color(NamedTextColor.GREEN));
        				break;
        				
        			case "switch":
        				Map<String, Object> DiscordConfig = (Map<String, Object>) config.getConfig().get("Discord");
        				if (config.getBoolean("Discord.ChatType", false))
        				{
        					DiscordConfig.put("ChatType", false);
        					component = Component.text("チャットタイプをプレーンテキストメッセージタイプに変更しました。").color(NamedTextColor.GREEN);
        				}
        				else
        				{
        					DiscordConfig.put("ChatType", true);
        					component = Component.text("チャットタイプを編集Embedメッセージタイプに変更しました。").color(NamedTextColor.GREEN);
        				}
        				break;
        		}
    			break;
    			
        	default:
        		source.sendMessage(Component.text("usage: /fmcp　chat <switch|status>").color(NamedTextColor.GREEN));
        		break;
        }
		return;
	}
}
