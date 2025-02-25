package keyp.forev.fmc.velocity.socket.message.handlers.minecraft.command;

import com.google.inject.Inject;

import keyp.forev.fmc.common.socket.message.Message;
import keyp.forev.fmc.common.socket.message.handlers.interfaces.minecraft.commands.TeleportPlayerHandler;
import keyp.forev.fmc.velocity.server.BroadCast;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class VelocityTeleportPlayerHandler implements TeleportPlayerHandler {
    private final BroadCast bc;

    @Inject
    public VelocityTeleportPlayerHandler(BroadCast bc) {
        this.bc = bc;
    }

    @Override
    public void handle(Message.Minecraft.Command.Teleport.Player player) {
        String playerName = player.who.name;

        Component message = Component.text(playerName + "が" + player.target + "に" + (player.reverse ? "逆" : "") + "テレポートしました。")
            .color(NamedTextColor.GRAY)
            .decorate(TextDecoration.ITALIC);

        bc.sendExceptPlayerMessage(message, playerName);
    }
}

