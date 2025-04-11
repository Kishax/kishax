package f5.si.kishax.mc.common.socket.message.handlers.interfaces.minecraft.commands;

import f5.si.kishax.mc.common.socket.message.Message;

public interface TeleportPlayerHandler {
  void handle(Message.Minecraft.Command.Teleport.Player player);
}
