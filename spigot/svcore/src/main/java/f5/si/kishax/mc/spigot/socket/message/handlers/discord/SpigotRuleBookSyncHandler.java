package f5.si.kishax.mc.spigot.socket.message.handlers.discord;

import com.google.inject.Inject;

import f5.si.kishax.mc.common.socket.message.Message;
import f5.si.kishax.mc.common.socket.message.handlers.interfaces.discord.RuleBookSyncHandler;
import f5.si.kishax.mc.spigot.server.InventoryCheck;

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

