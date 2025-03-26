package keyp.forev.fmc.common.socket.message.handlers.interfaces.minecraft.commands;

import keyp.forev.fmc.common.socket.message.Message;

public interface ForwardHandler {
  void handle(Message.Minecraft.Command.Forward forward);
}
