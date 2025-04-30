package net.kishax.mc.common.socket.message.handlers.interfaces.web;

import net.kishax.mc.common.socket.message.Message;

public interface MinecraftWebConfirmHandler {
  void handle(Message.Web.MinecraftConfirm confirm);
}

