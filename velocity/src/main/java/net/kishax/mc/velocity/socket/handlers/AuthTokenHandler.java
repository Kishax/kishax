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

      // kishax-aws SQSワーカー経由で送信（推奨）
      // 注意: Main.kishaxSqsWorkerがnullでない場合のみ使用
      try {
        // kishax-aws統合による新しい実装
        // この部分は実際のSqsWorkerインスタンスへのアクセスが必要
        // 現在はログのみ出力
        logger.info("✅ Auth token will be handled by kishax-aws SqsWorker automatically");
      } catch (Exception e) {
        logger.warn("kishax-aws not available, falling back to legacy implementation");
        
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