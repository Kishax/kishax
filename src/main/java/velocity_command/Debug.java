package velocity_command;

import java.io.IOException;
import java.util.Map;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import velocity.Config;
import velocity.Main;

public class Debug
{
	private final Main plugin;
	private final ProxyServer server;
	private final Config config;
	
	public String value1 = null, value2 = null;
	
	@Inject
	public Debug(Main plugin,ProxyServer server, Config config)
	{
		this.plugin = plugin;
		this.server = server;
		this.config = config;
	}
	
	public void execute(CommandSource source,String[] args)
	{
		Map<String, Object> DebugConfig = (Map<String, Object>) config.getConfig().get("Debug");
		Map<String, Object> DiscordConfig = (Map<String, Object>) config.getConfig().get("Discord");
		if
		(
			!(config.getString("Debug.Webhook_URL","").isEmpty()) && 
			!(config.getString("Discord.Webhook_URL","").isEmpty())
		)
		{
			value1 = config.getString("Debug.Webhook_URL");
			value2 = config.getString("Discord.Webhook_URL");
			
			DiscordConfig.put("Webhook_URL", value1);
			DebugConfig.put("Webhook_URL", value2);
			
			if(config.getBoolean("Debug.Mode"))
			{
				source.sendMessage(Component.text("デバッグモードがOFFになりました。").color(NamedTextColor.GREEN));
				DebugConfig.put("Mode", false);
				try
				{
					config.saveConfig();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
			else
			{
				source.sendMessage(Component.text("デバッグモードがONになりました。").color(NamedTextColor.GREEN));
				DebugConfig.put("Mode", true);
				try
				{
					config.saveConfig();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}
		else
		{
			source.sendMessage(Component.text("コンフィグの設定が不十分です。").color(NamedTextColor.RED));
		}
	}
}
