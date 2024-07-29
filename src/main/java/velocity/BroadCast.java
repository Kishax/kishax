package velocity;

import java.util.Objects;

import com.google.inject.Inject;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class BroadCast
{
	private final ProxyServer server;
	private final ConsoleCommandSource console;
	private String serverName = null;
	
	@Inject
	public BroadCast(ProxyServer server, ConsoleCommandSource console)
	{
		this.server = server;
		this.console = console;
	}
	
	public void broadcastComponent(Component component,String excepserver,Boolean only)
    {
    	for (Player player : server.getAllPlayers())
        {
    		serverName = null;
    		// プレイヤーが最後にいたサーバーを取得
	        player.getCurrentServer().ifPresent(currentServer ->
	        {
	            RegisteredServer server = currentServer.getServer();
	            serverName = server.getServerInfo().getName();
	        });
	        if(Objects.isNull(serverName)) continue;
	        // そのサーバーのみに送る。
        	if(only)
        	{
        		// excepserverと一致したサーバーにいるプレイヤーに
        		if(excepserver.equalsIgnoreCase(serverName))
        		{
        			player.sendMessage(component);
	        		continue;
        		}
        	}
        	
    		if(Objects.isNull(component)) return;
    		
        	if(Objects.isNull(excepserver))
        	{
        		player.sendMessage(component);
        		continue;
        	}
        	else if(!(serverName.equalsIgnoreCase(excepserver)))
        	{
        		player.sendMessage(component);
        		continue;
        	}
        }
    	
    	// コンソールにも出力
    	console.sendMessage(component);
    }
    
    public void broadcastMessage(String message, NamedTextColor color, String excepserver)
    {
        for (Player player : server.getAllPlayers())
        {
        	if(Objects.isNull(excepserver))
        	{
        		player.sendMessage(Component.text(message).color(color));
        		continue;
        	}
        	// プレイヤーが最後にいたサーバーを取得
	        player.getCurrentServer().ifPresent(currentServer ->
	        {
	            RegisteredServer server = currentServer.getServer();
	            String serverName = server.getServerInfo().getName();
	            if
	            (
	            	!(serverName.equalsIgnoreCase(excepserver))
	            )
	            {
	            	player.sendMessage(Component.text(message).color(color));
	            }
	        });
        }
        
        // コンソールにも出力
        console.sendMessage(Component.text(message).color(color));
    }
}
