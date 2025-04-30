package net.kishax.mc.common.socket.message.handlers.interfaces.minecraft.commands;

import net.kishax.mc.common.socket.message.Message;

public interface TeleportPlayerHandler {
  void handle(Message.Minecraft.Command.Teleport.Player player);
}
