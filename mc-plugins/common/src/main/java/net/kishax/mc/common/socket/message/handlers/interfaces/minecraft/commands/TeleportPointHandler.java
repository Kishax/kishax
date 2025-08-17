package net.kishax.mc.common.socket.message.handlers.interfaces.minecraft.commands;

import net.kishax.mc.common.socket.message.Message;

public interface TeleportPointHandler {
  void handle(Message.Minecraft.Command.Teleport.Point point);
}
