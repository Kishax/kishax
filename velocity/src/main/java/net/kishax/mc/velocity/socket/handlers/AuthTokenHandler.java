package net.kishax.mc.velocity.socket.handlers;

import org.slf4j.Logger;

import com.google.inject.Inject;

import net.kishax.mc.common.socket.message.Message;
import net.kishax.mc.velocity.Main;

public class AuthTokenHandler {
  private final Logger logger;

  @Inject
  public AuthTokenHandler(Logger logger) {
    this.logger = logger;
  }

  public void handle(Message.Web.AuthToken authToken) {
    try {
      logger.info("Received auth token from Spigot for player: {} (action: {})",
          authToken.who.name, authToken.action);

      // kishax-api SQSワーカー経由で送信
      net.kishax.api.bridge.SqsWorker sqsWorker = Main.getKishaxSqsWorker();
      if (sqsWorker != null) {
        // AuthTokenをSqsWorker経由でWEBに送信
        String mcid = authToken.who.name;
        String uuid = authToken.who.uuid;

        // SqsWorkerのMcToWebMessageSenderを使用してメッセージ送信
        net.kishax.api.bridge.McToWebMessageSender sender = sqsWorker.getMcToWebSender();
        if (sender != null) {
          // auth_tokenメッセージを送信（型安全な専用メソッドを使用）
          sender.sendAuthToken(mcid, uuid, authToken.token, authToken.expiresAt, authToken.action);
          logger.info("✅ Auth token sent to WEB via kishax-api McToWebMessageSender for player: {}", mcid);
        } else {
          logger.error("❌ McToWebMessageSender is not available");
          throw new RuntimeException("McToWebMessageSender is not available");
        }
      } else {
        logger.error("❌ kishax-api SqsWorker not available");
        throw new RuntimeException("kishax-api SqsWorker not available - AWS fallback removed");
      }

    } catch (Exception e) {
      logger.error("Error handling auth token message: {}", e.getMessage());
      for (StackTraceElement element : e.getStackTrace()) {
        logger.error(element.toString());
      }
    }
  }
}
