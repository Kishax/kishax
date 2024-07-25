package velocity;

import org.slf4j.Logger;

import com.google.inject.Inject;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.messaging.MessagingService;

public class Luckperms
{
	// なお、LuckPerms = net.luckperms.api.LuckPerms, Luckperms = velocity.Luckperm
	private final LuckPerms lp;
	private final Logger logger;
	
	@Inject
	public Luckperms(LuckPerms lp, Logger logger)
	{
		this.lp = lp;
		this.logger = logger;
	}
	
	public void triggerNetworkSync()
	{
        MessagingService messagingService = lp.getMessagingService().orElse(null);

        if (messagingService != null)
        {
            messagingService.pushUpdate();
            logger.info("LuckPerms network sync triggered.");
        }
        else
        {
        	logger.error("Failed to get LuckPerms MessagingService.");
        }
    }
}