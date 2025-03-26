package keyp.forev.fmc.common.socket.message.handlers.interfaces.minecraft.commands;

import keyp.forev.fmc.common.socket.message.Message;

public interface TeleportPlayerHandler {
  void handle(Message.Minecraft.Command.Teleport.Player player);
}
