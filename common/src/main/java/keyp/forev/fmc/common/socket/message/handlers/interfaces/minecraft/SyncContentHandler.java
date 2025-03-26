package keyp.forev.fmc.common.socket.message.handlers.interfaces.minecraft;

import keyp.forev.fmc.common.socket.message.Message;

public interface SyncContentHandler {
  void handle(Message.Minecraft.Sync sync);
}

