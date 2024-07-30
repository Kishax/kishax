package velocity_command;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import velocity.BroadCast;
import velocity.Config;
import velocity.Main;

public class SwitchRomajiConvType
{
	private final Config config;
	private final BroadCast bc;
	private String convserverName = null;
	public String value1 = null, value2 = null;
	
	@Inject
	public SwitchRomajiConvType(Main plugin,ProxyServer server, Config config, BroadCast bc)
	{
		this.config = config;
		this.bc = bc;
	}
	
	public void execute(CommandSource source,String[] args)
	{
		Map<String, Object> ConvConfig = (Map<String, Object>) config.getConfig().get("Conv");
			
		if(Objects.isNull(config.getBoolean("Conv.Mode")))
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
				bc.broadcastComponent(Component.text("ローマ字変換がpde方式になりました。").color(NamedTextColor.GREEN), "", true);
				ConvConfig.put("Mode", false);
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
				source.sendMessage(Component.text("ローマ字変換がMap方式になりました。").color(NamedTextColor.GREEN));
				ConvConfig.put("Mode", true);
				try
				{
					config.saveConfig();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
            return;
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
				bc.broadcastComponent(Component.text("ローマ字変換がpde方式になりました。").color(NamedTextColor.GREEN), convserverName, true);
				ConvConfig.put("Mode", false);
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
				bc.broadcastComponent(Component.text("ローマ字変換がMap方式になりました。").color(NamedTextColor.GREEN), convserverName, true);
				ConvConfig.put("Mode", true);
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
	}
}
