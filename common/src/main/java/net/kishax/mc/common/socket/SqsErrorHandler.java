package net.kishax.mc.common.socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * SQS通信のエラーハンドリング
 */
public class SqsErrorHandler {
  private static final Logger logger = LoggerFactory.getLogger(SqsErrorHandler.class);

  private final SqsClient sqsClient;
  private final int maxRetries;
  private final long timeoutMs;

  public SqsErrorHandler(SqsClient sqsClient, int maxRetries, long timeoutMs) {
    this.sqsClient = sqsClient;
    this.maxRetries = maxRetries;
    this.timeoutMs = timeoutMs;
  }

  /**
   * リトライ機能付きメッセージ送信
   */
  public CompletableFuture<Boolean> sendWithRetry(CompletableFuture<Void> operation, String operationName) {
    return retryOperation(operation, operationName, 0);
  }

  /**
   * 認証タイムアウト処理
   */
  public void handleAuthTimeout(String playerName, String playerUuid, long timeoutMs) {
    logger.warn("認証タイムアウト: {} ({}) - {}ms", playerName, playerUuid, timeoutMs);

    String timeoutMessage = String.format("認証がタイムアウトしました。(%dms)", timeoutMs);

    sqsClient.sendAuthResponse(playerName, playerUuid, false, timeoutMessage)
        .exceptionally(ex -> {
          logger.error("認証タイムアウトレスポンス送信に失敗しました: {} ({})", playerName, playerUuid, ex);
          return null;
        });
  }

  /**
   * SQS接続エラー処理
   */
  public void handleConnectionError(Exception error, String context) {
    logger.error("SQS接続エラー ({}): {}", context, error.getMessage());

    // 接続エラーの種類に応じた処理
    if (error instanceof software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException) {
      logger.error("SQSキューが存在しません。設定を確認してください。");
    } else if (error instanceof software.amazon.awssdk.core.exception.SdkClientException) {
      logger.error("AWS SDK接続エラー。認証情報やネットワーク設定を確認してください。");
    } else if (error instanceof TimeoutException) {
      logger.error("SQSリクエストがタイムアウトしました。");
    } else {
      logger.error("予期しないSQSエラーが発生しました。", error);
    }
  }

  /**
   * メッセージ解析エラー処理
   */
  public void handleParseError(Exception error, String messageBody) {
    logger.error("メッセージ解析エラー: {}", error.getMessage());
    logger.debug("問題のメッセージボディ: {}", messageBody);

    // 解析できなかったメッセージを記録（デバッグ用）
    if (logger.isDebugEnabled()) {
      logger.debug("Parse error stack trace:", error);
    }
  }

  /**
   * 認証失敗処理
   */
  public void handleAuthFailure(String playerName, String playerUuid, String reason, Exception error) {
    logger.error("認証処理失敗: {} ({}) - {}", playerName, playerUuid, reason);

    if (error != null) {
      logger.debug("認証失敗の詳細エラー:", error);
    }

    String failureMessage = reason + (error != null ? " (エラー: " + error.getMessage() + ")" : "");

    sqsClient.sendAuthResponse(playerName, playerUuid, false, failureMessage)
        .exceptionally(ex -> {
          logger.error("認証失敗レスポンス送信に失敗しました: {} ({})", playerName, playerUuid, ex);
          return null;
        });
  }

  /**
   * メッセージ処理失敗時の通知
   */
  public void handleMessageProcessingError(String messageType, String playerName, Exception error) {
    logger.error("メッセージ処理エラー: type={}, player={}", messageType, playerName, error);

    // エラー通知をWeb側に送信（必要に応じて）
    if (sqsClient != null && playerName != null && !playerName.isEmpty()) {
      java.util.Map<String, Object> errorData = new java.util.HashMap<>();
      errorData.put("messageType", messageType);
      errorData.put("error", error.getMessage());
      errorData.put("timestamp", System.currentTimeMillis());

      sqsClient.sendGenericMessage("mc_processing_error", errorData)
          .exceptionally(ex -> {
            logger.error("エラー通知送信に失敗しました: {}", ex.getMessage());
            return null;
          });
    }
  }

  /**
   * 設定エラー処理
   */
  public void handleConfigurationError(String setting, String value, String expected) {
    logger.error("設定エラー: {} = '{}' (期待値: {})", setting, value, expected);

    // 設定エラーの場合、SQS機能を無効化することを推奨
    logger.warn("SQS機能が正常に動作しない可能性があります。設定を確認してください。");
  }

  /**
   * リトライ処理（内部メソッド）
   */
  private CompletableFuture<Boolean> retryOperation(CompletableFuture<Void> operation, String operationName,
      int attempt) {
    return operation
        .orTimeout(timeoutMs, TimeUnit.MILLISECONDS)
        .thenApply(v -> true) // 成功
        .exceptionally(ex -> {
          if (attempt < maxRetries) {
            logger.warn("{}が失敗しました。リトライ中... ({}/{}): {}", operationName, attempt + 1, maxRetries, ex.getMessage());

            // 指数バックオフでリトライ
            long retryDelay = Math.min(1000 * (1L << attempt), 10000); // 最大10秒

            try {
              Thread.sleep(retryDelay);
              return retryOperation(operation, operationName, attempt + 1).join();
            } catch (InterruptedException ie) {
              Thread.currentThread().interrupt();
              logger.error("リトライ中に中断されました: {}", operationName);
              return false;
            }
          } else {
            logger.error("{}が最大リトライ回数に達しました: {}", operationName, ex.getMessage());
            handleConnectionError((Exception) ex, operationName);
            return false;
          }
        });
  }

  /**
   * ヘルスチェック
   */
  public CompletableFuture<Boolean> healthCheck() {
    if (sqsClient == null) {
      return CompletableFuture.completedFuture(false);
    }

    // 簡単なメッセージ送信テスト
    java.util.Map<String, Object> healthData = new java.util.HashMap<>();
    healthData.put("type", "health_check");
    healthData.put("timestamp", System.currentTimeMillis());

    return sendWithRetry(
        sqsClient.sendGenericMessage("health_check", healthData),
        "SQS Health Check");
  }
}
