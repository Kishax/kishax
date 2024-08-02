package spigot;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import com.google.inject.Inject;

public final class EventListener implements Listener
{
    public common.Main plugin;
    
    @Inject
	public EventListener(common.Main plugin)
	{
		this.plugin = plugin;
	}
	
	@EventHandler
    public void onPlayerJoin(PlayerJoinEvent e)
	{
    	Player player = e.getPlayer();
    	e.setJoinMessage(null);
    }
	
	@EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent e)
	{
		e.getPlayer().sendMessage("あんま寝てると寝ぼけてまうぞ！！");
    }
}
