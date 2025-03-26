package keyp.forev.fmc.velocity.socket.message.handlers.minecraft.command;

import keyp.forev.fmc.common.socket.message.Message;
import keyp.forev.fmc.common.socket.message.handlers.interfaces.minecraft.commands.InputHandler;
import keyp.forev.fmc.velocity.server.events.EventListener;

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
