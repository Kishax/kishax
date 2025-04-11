package f5.si.kishax.mc.common.socket.message.handlers.interfaces.web;

import f5.si.kishax.mc.common.socket.message.Message;

public interface MinecraftWebConfirmHandler {
  void handle(Message.Web.MinecraftConfirm confirm);
}

