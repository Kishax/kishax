package keyp.forev.fmc.forge.util;

import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;

import com.google.inject.Inject;

import keyp.forev.fmc.common.Luckperms;
import keyp.forev.fmc.common.PlayerUtils;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import keyp.forev.fmc.common.Database;

public class ForgeLuckperms extends Luckperms {
	@Inject
	public ForgeLuckperms(Logger logger, Database db, PlayerUtils pu) {
		super(logger, db, pu);
	}
    
    // only for forge
	public boolean hasPermission(CommandSourceStack source, String permission) {
		if (source.getEntity() == null) return true;
		if (!(source.getEntity() instanceof ServerPlayer)) {
		    source.sendFailure(Component.literal("Error: Command must be executed by a player."));
		    return false;
		}
		ServerPlayer player = source.getPlayer();
		if (player != null) {
			UserManager userManager = lpapi.getUserManager();
			UUID playerUUID = player.getUUID();
			User user = userManager.getUser(playerUUID);
			if (Objects.isNull(user)) {
				source.sendFailure(Component.literal("Error: User not found in LuckPerms."));
				return false;
			}
			return user.getCachedData().getPermissionData().checkPermission(permission).asBoolean();
		}
		return false;
	}
}
