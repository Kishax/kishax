package net.kishax.mc.common.socket.message.handlers.interfaces.minecraft;

import net.kishax.mc.common.socket.message.Message;

public interface OtpHandler {
  void handle(Message.Minecraft.Otp otp);
}