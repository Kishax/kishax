package f5.si.kishax.mc.common.socket.message.handlers.interfaces.minecraft.commands;

import f5.si.kishax.mc.common.socket.message.Message;

public interface TeleportPointHandler {
  void handle(Message.Minecraft.Command.Teleport.Point point);
}
