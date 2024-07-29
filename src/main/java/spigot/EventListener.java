package spigot;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class EventListener implements Listener
{
    public common.Main plugin;
    public SocketSwitch socket;
    
	public EventListener(common.Main plugin,SocketSwitch socket)
	{
		this.plugin = plugin;
		this.socket = socket;
	}
	
	@EventHandler
    public void onPlayerJoin(PlayerJoinEvent e)
	{
    	Player player = e.getPlayer();
    	e.setJoinMessage(null);
    }
}
