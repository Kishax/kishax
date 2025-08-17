package net.kishax.mc.velocity.socket.message.handlers.minecraft.command;

import com.google.inject.Inject;

import net.kishax.mc.common.socket.message.Message;
import net.kishax.mc.common.socket.message.handlers.interfaces.minecraft.commands.ForwardHandler;
import net.kishax.mc.velocity.server.cmd.sub.CommandForwarder;

public class VelocityForwardHandler implements ForwardHandler {
  private final CommandForwarder cf;

  @Inject
  public VelocityForwardHandler(CommandForwarder cf) {
    this.cf = cf;
  }

  public void handle(Message.Minecraft.Command.Forward forward) {
    cf.forwardCommand(forward.who.name, forward.cmd, forward.target);
  }
}
