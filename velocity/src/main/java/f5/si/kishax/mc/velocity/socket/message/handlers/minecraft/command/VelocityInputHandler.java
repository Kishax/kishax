package f5.si.kishax.mc.velocity.socket.message.handlers.minecraft.command;

import f5.si.kishax.mc.common.socket.message.Message;
import f5.si.kishax.mc.common.socket.message.handlers.interfaces.minecraft.commands.InputHandler;
import f5.si.kishax.mc.velocity.server.events.EventListener;

public class VelocityInputHandler implements InputHandler {
  public VelocityInputHandler() {
  }

  @Override
  public void handle(Message.Minecraft.Command.Input input) {
    if (input.mode) {
      EventListener.playerInputers.add(input.who.name);
    } else {
      EventListener.playerInputers.remove(input.who.name);
    }
  }
}
