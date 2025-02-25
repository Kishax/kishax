package keyp.forev.fmc.velocity.socket.message.handlers.minecraft.command;

import com.google.inject.Inject;

import keyp.forev.fmc.common.socket.message.Message;
import keyp.forev.fmc.common.socket.message.handlers.interfaces.minecraft.commands.ForwardHandler;
import keyp.forev.fmc.velocity.server.cmd.sub.CommandForwarder;

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
