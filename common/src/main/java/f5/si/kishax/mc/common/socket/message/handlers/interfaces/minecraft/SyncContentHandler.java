package f5.si.kishax.mc.common.socket.message.handlers.interfaces.minecraft;

import f5.si.kishax.mc.common.socket.message.Message;

public interface SyncContentHandler {
  void handle(Message.Minecraft.Sync sync);
}

