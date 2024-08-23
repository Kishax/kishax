package velocity_command;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import velocity.Config;

public class SwitchRomajiConvType
{
	private final Config config;
	private final Logger logger;
	private String convserverName = null;
	public String value1 = null, value2 = null;
	
	@Inject
	public SwitchRomajiConvType(Config config, Logger logger)
	{
		this.config = config;
		this.logger = logger;
	}
	
	public void execute(@NotNull CommandSource source,String[] args)
	{
		Map<String, Object> ConvConfig = config.getStringObjectMap("Conv");
		if(Objects.isNull(ConvConfig))
		{
			source.sendMessage(Component.text("コンフィグの設定が不十分です。").color(NamedTextColor.RED));
			return;
		}
		
		if (!(source instanceof Player))
		{
			//source.sendMessage(Component.text("このコマンドはプレイヤーのみが実行できます。").color(NamedTextColor.RED));
			if(Objects.isNull(convserverName))
			{
				source.sendMessage(Component.text("現在のサーバーの値がNullです。").color(NamedTextColor.RED));
				return;
			}
			
			if(config.getBoolean("Conv.Mode"))
			{
				source.sendMessage(Component.text("ローマ字変換がpde方式になりました。").color(NamedTextColor.GREEN));
				ConvConfig.put("Mode", false);
				try
				{
					config.saveConfig();
				}
				catch (IOException e)
				{
					logger.error("An IOException error occurred: " + e.getMessage());
					for (StackTraceElement element : e.getStackTrace()) 
					{
						logger.error(element.toString());
					}
				}
			}
			else
			{
				source.sendMessage(Component.text("ローマ字変換がMap方式になりました。").color(NamedTextColor.GREEN));
				ConvConfig.put("Mode", true);
				try
				{
					config.saveConfig();
				}
				catch (IOException e)
				{
					logger.error("An IOException error occurred: " + e.getMessage());
					for (StackTraceElement element : e.getStackTrace()) 
					{
						logger.error(element.toString());
					}
				}
			}
		}
		else
		{
			Player player = (Player) source;
			
			// プレイヤーの現在のサーバーを取得
			player.getCurrentServer().ifPresent(serverConnection ->
			{
				RegisteredServer server = serverConnection.getServer();
				convserverName = server.getServerInfo().getName();
			});
			
			if(Objects.isNull(convserverName))
			{
				source.sendMessage(Component.text("現在のサーバーの値がNullです。").color(NamedTextColor.RED));
				return;
			}
			
			if(config.getBoolean("Conv.Mode"))
			{
				source.sendMessage(Component.text("ローマ字変換がpde方式になりました。").color(NamedTextColor.GREEN));
				ConvConfig.put("Mode", false);
				try
				{
					config.saveConfig();
				}
				catch (IOException e)
				{
					logger.error("An IOException error occurred: " + e.getMessage());
					for (StackTraceElement element : e.getStackTrace()) 
					{
						logger.error(element.toString());
					}
				}
			}
			else
			{
				source.sendMessage(Component.text("ローマ字変換がMap方式になりました。").color(NamedTextColor.GREEN));
				ConvConfig.put("Mode", true);
				try
				{
					config.saveConfig();
				}
				catch (IOException e)
				{
					logger.error("An IOException e error occurred: " + e.getMessage());
					for (StackTraceElement element : e.getStackTrace()) 
					{
						logger.error(element.toString());
					}
				}
			}
		}
	}
}
