package keyp.forev.fmc.velocity.socket.message.handlers.minecraft.command;

import com.google.inject.Inject;

import keyp.forev.fmc.common.socket.message.Message;
import keyp.forev.fmc.common.socket.message.handlers.interfaces.minecraft.commands.TeleportPointHandler;
import keyp.forev.fmc.velocity.server.BroadCast;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class VelocityTeleportPointHandler implements TeleportPointHandler {
    private final BroadCast bc;

    @Inject
    public VelocityTeleportPointHandler(BroadCast bc) {
        this.bc = bc;
    }

    @Override
    public void handle(Message.Minecraft.Command.Teleport.Point point) {
        String playerName = point.who.name;

        Component message;

        if (point.register != null && point.register) {
            message = Component.text(playerName + "がポイント: " + point.name + "を設定しました。")
                .color(NamedTextColor.GRAY)
                .decorate(TextDecoration.ITALIC);
        } else if (point.back != null && point.back) {
            message = Component.text(playerName + "がもとの場所に戻りました。")
                .color(NamedTextColor.GRAY)
                .decorate(TextDecoration.ITALIC);
        } else {
            message = Component.text(playerName + "がポイント: " + point.name + "にテレポートしました。")
                .color(NamedTextColor.GRAY)
                .decorate(TextDecoration.ITALIC);
        }

        bc.sendExceptPlayerMessage(message, playerName);
    }
}
