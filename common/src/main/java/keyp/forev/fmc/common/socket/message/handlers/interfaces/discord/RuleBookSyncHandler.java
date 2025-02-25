package keyp.forev.fmc.common.socket.message.handlers.interfaces.discord;

import keyp.forev.fmc.common.socket.message.Message;

public interface RuleBookSyncHandler {
    void handle(Message.Discord.RuleBook rulebook);
}

