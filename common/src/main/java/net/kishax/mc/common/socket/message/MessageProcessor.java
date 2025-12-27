package net.kishax.mc.common.socket.message;

import java.util.Optional;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.ProvisionException;

import net.kishax.mc.common.socket.message.handlers.interfaces.discord.*;
import net.kishax.mc.common.socket.message.handlers.interfaces.minecraft.*;
import net.kishax.mc.common.socket.message.handlers.interfaces.minecraft.commands.*;
import net.kishax.mc.common.socket.message.handlers.interfaces.web.*;

public class MessageProcessor {
  private final Injector injector;

  @Inject
  public MessageProcessor(Injector injector) {
    this.injector = injector;
  }

  public void process(Message msg) throws ProvisionException {
    Optional.ofNullable(msg.web)
        .map(web -> web.confirm)
        .ifPresent(confirm -> injector.getInstance(MinecraftWebConfirmHandler.class).handle(confirm));

    Optional.ofNullable(msg.web)
        .map(web -> web.authTokenSaved)
        .ifPresent(authTokenSaved -> injector.getInstance(AuthTokenSavedHandler.class).handle(authTokenSaved));

    Optional.ofNullable(msg.discord)
        .map(discord -> discord.rulebook)
        .ifPresent(rulebook -> injector.getInstance(RuleBookSyncHandler.class).handle(rulebook));

    // mcフィールドの処理
    if (msg.mc != null) {
      Optional.ofNullable(msg.mc.server)
          .ifPresent(server -> {
            injector.getInstance(ServerActionHandler.class).handle(server);
          });

      Optional.ofNullable(msg.mc.sync)
          .ifPresent(sync -> {
            injector.getInstance(SyncContentHandler.class).handle(sync);
          });

      Optional.ofNullable(msg.mc.cmd)
          .ifPresent(cmd -> {
            Optional.ofNullable(cmd.teleport)
                .ifPresent(teleport -> {
                  Optional.ofNullable(teleport.point)
                      .ifPresent(point -> injector.getInstance(TeleportPointHandler.class).handle(point));

                  Optional.ofNullable(teleport.player)
                      .ifPresent(player -> injector.getInstance(TeleportPlayerHandler.class).handle(player));
                });

            Optional.ofNullable(cmd.forward)
                .ifPresent(forward -> injector.getInstance(ForwardHandler.class).handle(forward));

            Optional.ofNullable(cmd.input)
                .ifPresent(input -> injector.getInstance(InputHandler.class).handle(input));
          });

      Optional.ofNullable(msg.mc.otp)
          .ifPresent(otp -> {
            try {
              injector.getInstance(OtpHandler.class).handle(otp);
            } catch (ProvisionException e) {
              // OtpHandlerが存在しない場合はスキップ
            }
          });
    }
  }
}
