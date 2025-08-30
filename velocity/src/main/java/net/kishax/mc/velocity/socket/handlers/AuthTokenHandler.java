package net.kishax.mc.velocity.socket.handlers;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;

import net.kishax.mc.common.socket.message.Message;
import net.kishax.mc.velocity.aws.AwsSqsService;

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

      // AWS SQSサービスが利用可能かチェック
      try {
        AwsSqsService awsSqsService = awsSqsServiceProvider.get();
        awsSqsService.sendAuthTokenToWeb(authToken);
        logger.info("Successfully forwarded auth token to Web via SQS for player: {}", authToken.who.name);
      } catch (Exception e) {
        logger.warn("AWS SQS service not available, skipping auth token forwarding: {}", e.getMessage());
      }

    } catch (Exception e) {
      logger.error("Error handling auth token message: {}", e.getMessage());
      for (StackTraceElement element : e.getStackTrace()) {
        logger.error(element.toString());
      }
    }
  }
}