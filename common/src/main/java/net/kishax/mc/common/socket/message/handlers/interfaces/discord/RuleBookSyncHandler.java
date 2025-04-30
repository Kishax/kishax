package net.kishax.mc.common.socket.message.handlers.interfaces.discord;

import net.kishax.mc.common.socket.message.Message;

public interface RuleBookSyncHandler {
  void handle(Message.Discord.RuleBook rulebook);
}

