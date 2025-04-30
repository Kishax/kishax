package net.kishax.mc.fabric.server;

import java.util.UUID;

import org.slf4j.Logger;

import com.google.inject.Inject;

import net.kishax.mc.common.database.Database;
import net.kishax.mc.common.server.Luckperms;
import net.kishax.mc.common.util.PlayerUtils;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class FabricLuckperms extends Luckperms {
  @Inject
  public FabricLuckperms(Logger logger, Database db, PlayerUtils pu) {
    super(logger, db, pu);
  }

  // only for fabric
  public boolean hasPermission(ServerCommandSource source, String permission) {
    if (source.getEntity() == null) return true; // コンソールからの実行は許可
    if (!(source.getEntity() instanceof PlayerEntity)) {
      source.sendMessage(Text.literal("Error: Command must be executed by a player."));
      return false;
    }
    ServerPlayerEntity player = source.getPlayer();
    if (player != null) {
      UserManager userManager = lpapi.getUserManager();
      UUID playerUUID = player.getUuid();
      User user = userManager.getUser(playerUUID);
      if (user == null) {
        source.sendMessage(Text.literal("Error: User not found in LuckPerms."));
        return false;
      }
      return user.getCachedData().getPermissionData().checkPermission(permission).asBoolean();
    }
    return false;
  }
}
