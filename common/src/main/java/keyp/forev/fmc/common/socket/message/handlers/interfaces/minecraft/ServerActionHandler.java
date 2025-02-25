package keyp.forev.fmc.common.socket.message.handlers.interfaces.minecraft;

import keyp.forev.fmc.common.socket.message.Message;

public interface ServerActionHandler {
    void handle(Message.Minecraft.Server server);
}

