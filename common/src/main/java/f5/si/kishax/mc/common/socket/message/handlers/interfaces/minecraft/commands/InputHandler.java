package f5.si.kishax.mc.common.socket.message.handlers.interfaces.minecraft.commands;

import f5.si.kishax.mc.common.socket.message.Message;

public interface InputHandler {
  void handle(Message.Minecraft.Command.Input input);
}
