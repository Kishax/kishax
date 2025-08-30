package net.kishax.mc.velocity.socket;

import java.util.Optional;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.ProvisionException;

import net.kishax.mc.common.socket.message.Message;
import net.kishax.mc.common.socket.message.MessageProcessor;
import net.kishax.mc.velocity.socket.handlers.AuthTokenHandler;

public class VelocityMessageProcessor extends MessageProcessor {
  private final Injector injector;

  @Inject
  public VelocityMessageProcessor(Injector injector) {
    super(injector);
    this.injector = injector;
  }

  @Override
  public void process(Message msg) throws ProvisionException {
    // 基本的なメッセージ処理を呼び出し
    super.process(msg);
    
    // Velocity固有の処理を追加
    if (msg.web != null) {
      Optional.ofNullable(msg.web.authToken)
          .ifPresent(authToken -> injector.getInstance(AuthTokenHandler.class).handle(authToken));
    }
  }
}