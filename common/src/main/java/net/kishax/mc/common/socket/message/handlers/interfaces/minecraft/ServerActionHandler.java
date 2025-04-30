package net.kishax.mc.common.socket.message.handlers.interfaces.minecraft;

import net.kishax.mc.common.socket.message.Message;

public interface ServerActionHandler {
  void handle(Message.Minecraft.Server server);
}

