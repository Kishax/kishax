package spigot;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;

import com.google.inject.Inject;

public final class EventListener implements Listener
{
    public common.Main plugin;
    
    @Inject
	public EventListener(common.Main plugin)
	{
		this.plugin = plugin;
	}
	
	// MCVCをONにすると、ベッドで寝れなくなるため、必要なメソッド
	@EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent e)
	{
		if(Rcon.isMCVC)
		{
			// プレイヤーがベッドに入ったかどうかを確認
	        if (e.getBedEnterResult() == PlayerBedEnterEvent.BedEnterResult.OK)
	        {
	            World world = e.getPlayer().getWorld();
	            // 時間を朝に設定 (1000 ticks = 朝6時)
	            world.setTime(1000);
	            // 天気を晴れに設定
	            world.setStorm(false);
	            world.setThundering(false);
	            // メッセージをプレイヤーに送信
	            e.getPlayer().sendMessage("おはようございます！時間を朝にしました。");
	        }
		}
    }
}
