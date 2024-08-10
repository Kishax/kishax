package velocity;

import java.util.Objects;

import com.google.inject.Inject;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

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
	
	public void broadCastMessage(Component component)
	{
		for (Player player : server.getAllPlayers())
        {
			player.sendMessage(component);
        }
	}
	
	private void sendServerMessageManager(Component component, String excepServer, Boolean only)
    {
		if(Objects.isNull(excepServer) || Objects.isNull(component)) return;
		
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
        		if(excepServer.equalsIgnoreCase(serverName))
        		{
        			player.sendMessage(component);
	        		continue;
        		}
        	}
        	
    		if(!(serverName.equalsIgnoreCase(excepServer)))
        	{
        		// excepserver以外のサーバーに通知
        		player.sendMessage(component);
        		continue;
        	}
        }
    	
    	// コンソールにも出力
    	console.sendMessage(component);
    }
    
	public void sendExceptServerMessage(Component component, String exceptServer)
	{
		sendServerMessageManager(component, exceptServer, false);
	}
	
	public void sendSpecificServerMessage(Component component, String excepServer)
	{
		sendServerMessageManager(component, excepServer, true);
	}
	
    // 特定のプレイヤーもしくは特定のプレイヤーを除外したすべてのプレイヤーへ送る
    private void sendPlayerMessageManager(TextComponent component, String specificPlayer, boolean isReverse)
    {
    	boolean checkPlayer;
    	for (Player player : server.getAllPlayers())
        {
    		
    		if(isReverse)
    		{
    			checkPlayer = !player.getUsername().equals(specificPlayer);
    		}
    		else
    		{
    			checkPlayer = player.getUsername().equals(specificPlayer);
    		}
    		
        	if(checkPlayer)
        	{
        		player.sendMessage(component);
        	}
        }
    }
    
    public void sendExceptPlayerMessage(TextComponent component, String specificPlayer)
    {
    	sendPlayerMessageManager(component, specificPlayer, true);
    }
    
    public void sendSpecificPlayerMessage(TextComponent component, String specificPlayer)
    {
    	sendPlayerMessageManager(component, specificPlayer, false);
    }
}
