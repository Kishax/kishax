package net.kishax.mc.common.socket.message.handlers.interfaces.minecraft;

import net.kishax.mc.common.socket.message.Message;

public interface SyncContentHandler {
  void handle(Message.Minecraft.Sync sync);
}
