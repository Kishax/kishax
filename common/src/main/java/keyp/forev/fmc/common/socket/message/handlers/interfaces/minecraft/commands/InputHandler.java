package keyp.forev.fmc.common.socket.message.handlers.interfaces.minecraft.commands;

import keyp.forev.fmc.common.socket.message.Message;

public interface InputHandler {
  void handle(Message.Minecraft.Command.Input input);
}
