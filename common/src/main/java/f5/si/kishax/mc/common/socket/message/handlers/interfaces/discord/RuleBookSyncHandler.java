package f5.si.kishax.mc.common.socket.message.handlers.interfaces.discord;

import f5.si.kishax.mc.common.socket.message.Message;

public interface RuleBookSyncHandler {
  void handle(Message.Discord.RuleBook rulebook);
}

