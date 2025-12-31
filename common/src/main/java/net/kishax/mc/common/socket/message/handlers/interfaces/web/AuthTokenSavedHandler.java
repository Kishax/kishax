package net.kishax.mc.common.socket.message.handlers.interfaces.web;

import net.kishax.mc.common.socket.message.Message;

public interface AuthTokenSavedHandler {
  void handle(Message.Web.AuthTokenSaved authTokenSaved);
}

