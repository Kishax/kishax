package keyp.forev.fmc.velocity.socket.message.handlers.minecraft.command;

import com.google.inject.Inject;

import keyp.forev.fmc.common.socket.message.Message;
import keyp.forev.fmc.common.socket.message.handlers.interfaces.minecraft.commands.ImageMapHandler;
import keyp.forev.fmc.velocity.server.BroadCast;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class VelocityImageMapHandler implements ImageMapHandler {
    private final BroadCast bc;

    @Inject
    public VelocityImageMapHandler(BroadCast bc) {
        this.bc = bc;
    }

    @Override
    public void handle(Message.Minecraft.Command.ImageMap imagemap) {
        String playerName = imagemap.who.name;

        Component message = Component.text(playerName + "が画像マップを作成しました。(タイプ: " + imagemap.type + ")")
            .color(NamedTextColor.GRAY)
            .decorate(TextDecoration.ITALIC);

        bc.sendExceptPlayerMessage(message, playerName);
    }
}
