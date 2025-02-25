package keyp.forev.fmc.common.socket.message.handlers.interfaces.web;

import keyp.forev.fmc.common.socket.message.Message;

public interface MinecraftWebConfirmHandler {
    void handle(Message.Web.MinecraftConfirm confirm);
}

