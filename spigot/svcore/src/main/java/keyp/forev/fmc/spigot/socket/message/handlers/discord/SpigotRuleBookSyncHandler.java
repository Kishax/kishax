package keyp.forev.fmc.spigot.socket.message.handlers.discord;

import com.google.inject.Inject;

import keyp.forev.fmc.common.socket.message.Message;
import keyp.forev.fmc.common.socket.message.handlers.interfaces.discord.RuleBookSyncHandler;
import keyp.forev.fmc.spigot.server.InventoryCheck;

public class SpigotRuleBookSyncHandler implements RuleBookSyncHandler {
  private final InventoryCheck inv;

  @Inject
  public SpigotRuleBookSyncHandler(InventoryCheck inv) {
    this.inv = inv;
  }

  @Override
  public void handle(Message.Discord.RuleBook rulebook) {
    if (rulebook.sync) {
      inv.updateOnlinePlayerInventory();
    }
  }
}

