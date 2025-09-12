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
          
          // SqsWorkerのWebToMcMessageSenderを使用してメッセージ送信
          net.kishax.aws.WebToMcMessageSender sender = sqsWorker.getWebToMcSender();
          if (sender != null) {
            // auth_tokenメッセージを構築（ObjectNodeとして）
            java.util.Map<String, Object> authData = new java.util.HashMap<>();
            authData.put("mcid", mcid);
            authData.put("uuid", uuid);
            authData.put("authToken", authToken.token);
            authData.put("expiresAt", authToken.expiresAt);
            authData.put("action", authToken.action);
            
            sender.sendGenericMessage(messageType, authData);
            logger.info("✅ Auth token sent to WEB via kishax-aws SqsWorker for player: {}", mcid);
          } else {
            logger.warn("WebToMcMessageSender is not available");
            throw new RuntimeException("WebToMcMessageSender is not available");
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
