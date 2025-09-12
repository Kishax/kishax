package net.kishax.mc.velocity.socket.handlers;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;

import net.kishax.mc.common.socket.message.Message;
import net.kishax.mc.velocity.aws.AwsSqsService;
import net.kishax.mc.velocity.Main;

public class AuthTokenHandler {
  private final Logger logger;
  private final Provider<AwsSqsService> awsSqsServiceProvider;

  @Inject
  public AuthTokenHandler(Logger logger, Provider<AwsSqsService> awsSqsServiceProvider) {
    this.logger = logger;
    this.awsSqsServiceProvider = awsSqsServiceProvider;
  }

  public void handle(Message.Web.AuthToken authToken) {
    try {
      logger.info("Received auth token from Spigot for player: {} (action: {})", 
          authToken.who.name, authToken.action);

      // kishax-aws SQSワーカー経由で送信（推奨）
      net.kishax.aws.SqsWorker sqsWorker = Main.getKishaxSqsWorker();
      if (sqsWorker != null) {
        try {
          // AuthTokenをSqsWorker経由でWEBに送信
          String messageType = "auth_token";
          String mcid = authToken.who.name;
          String uuid = authToken.who.uuid;
          
          // SqsWorkerのMcToWebMessageSenderを使用してメッセージ送信
          net.kishax.aws.McToWebMessageSender sender = sqsWorker.getMcToWebSender();
          if (sender != null) {
            // auth_tokenメッセージを送信（型安全な専用メソッドを使用）
            sender.sendAuthToken(mcid, uuid, authToken.token, authToken.expiresAt, authToken.action);
            logger.info("✅ Auth token sent to WEB via kishax-aws McToWebMessageSender for player: {}", mcid);
          } else {
            logger.warn("McToWebMessageSender is not available");
            throw new RuntimeException("McToWebMessageSender is not available");
          }
        } catch (Exception e) {
          logger.warn("kishax-aws sending failed: {}, falling back to legacy implementation", e.getMessage());
          
          // フォールバック: 既存のAwsSqsServiceを使用
          try {
            AwsSqsService awsSqsService = awsSqsServiceProvider.get();
            awsSqsService.sendAuthTokenToWeb(authToken);
            logger.info("Successfully forwarded auth token to Web via legacy SQS for player: {}", authToken.who.name);
          } catch (Exception legacyEx) {
            logger.warn("Legacy AWS SQS service also not available: {}", legacyEx.getMessage());
          }
        }
      } else {
        logger.warn("kishax-aws SqsWorker not available, falling back to legacy implementation");
        
        // フォールバック: 既存のAwsSqsServiceを使用
        try {
          AwsSqsService awsSqsService = awsSqsServiceProvider.get();
          awsSqsService.sendAuthTokenToWeb(authToken);
          logger.info("Successfully forwarded auth token to Web via legacy SQS for player: {}", authToken.who.name);
        } catch (Exception legacyEx) {
          logger.warn("Legacy AWS SQS service also not available: {}", legacyEx.getMessage());
        }
      }

    } catch (Exception e) {
      logger.error("Error handling auth token message: {}", e.getMessage());
      for (StackTraceElement element : e.getStackTrace()) {
        logger.error(element.toString());
      }
    }
  }
}
