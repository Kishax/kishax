package velocity;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.messaging.MessagingService;

public class Luckperms
{
	public static LuckPerms lp;
	public static Main plugin;
	
	public Luckperms()
	{
		plugin = Main.getInstance();
		lp = Main.getlpInstance();
	}
	
	public static void triggerNetworkSync()
	{
        MessagingService messagingService = Main.getlpInstance().getMessagingService().orElse(null);

        if (messagingService != null)
        {
            messagingService.pushUpdate();
            Main.getInstance().getLogger().info("LuckPerms network sync triggered.");
        }
        else
        {
        	Main.getInstance().getLogger().error("Failed to get LuckPerms MessagingService.");
        }
    }
}