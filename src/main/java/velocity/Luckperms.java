package velocity;

import org.slf4j.Logger;

import com.google.inject.Inject;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.messaging.MessagingService;

public class Luckperms {
	// なお、LuckPerms = net.luckperms.api.LuckPerms, Luckperms = velocity.Luckperm
	private final LuckPerms lpapi;
	private final Logger logger;
	
	@Inject
	public Luckperms(LuckPerms lpapi, Logger logger) {
		this.lpapi = lpapi;
		this.logger = logger;
	}
	
	public void triggerNetworkSync() {
        MessagingService messagingService = lpapi.getMessagingService().orElse(null);

        if (messagingService != null) {
            messagingService.pushUpdate();
            logger.info("LuckPerms network sync triggered.");
        } else {
        	logger.error("Failed to get LuckPerms MessagingService.");
        }
    }
}