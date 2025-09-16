package net.kishax.mc.velocity.socket;

import com.google.inject.Inject;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kishax.mc.common.socket.SqsClient;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

import java.util.Optional;

/**
 * 認証エラー時のレスポンス処理
 */
public class VelocityAuthResponseHandler {
  private static final Logger logger = org.slf4j.LoggerFactory.getLogger(VelocityAuthResponseHandler.class);

  private final ProxyServer server;
  private final SqsClient sqsClient;

  @Inject
  public VelocityAuthResponseHandler(ProxyServer server, SqsClient sqsClient) {
    this.server = server;
    this.sqsClient = sqsClient;
  }

  /**
   * 認証失敗レスポンス送信
   */
  public void sendAuthFailureResponse(String playerName, String playerUuid, String reason) {
    logger.warn("認証失敗: {} ({}), 理由: {}", playerName, playerUuid, reason);

    // プレイヤーにメッセージ送信
    Optional<Player> playerOptional = server.getPlayer(playerName);
    if (playerOptional.isPresent()) {
      Player player = playerOptional.get();
      Component errorMessage = Component.text()
          .append(Component.text("WEB認証に失敗しました。")
              .color(NamedTextColor.RED))
          .appendNewline()
          .append(Component.text("理由: " + reason)
              .color(NamedTextColor.YELLOW))
          .appendNewline()
          .append(Component.text("/retry コマンドで再試行してください。")
              .color(NamedTextColor.AQUA))
          .build();

      player.sendMessage(errorMessage);
    }

    // Web側に失敗レスポンス送信
    sendAuthResponseToWeb(playerName, playerUuid, false, reason);
  }

  /**
   * 認証タイムアウト処理
   */
  public void handleAuthTimeout(String playerName, String playerUuid) {
    String timeoutReason = "認証がタイムアウトしました。";
    logger.warn("認証タイムアウト: {} ({})", playerName, playerUuid);

    Optional<Player> playerOptional = server.getPlayer(playerName);
    if (playerOptional.isPresent()) {
      Player player = playerOptional.get();
      Component timeoutMessage = Component.text()
          .append(Component.text("WEB認証がタイムアウトしました。")
              .color(NamedTextColor.RED))
          .appendNewline()
          .append(Component.text("/retry コマンドで再試行してください。")
              .color(NamedTextColor.AQUA))
          .build();

      player.sendMessage(timeoutMessage);
    }

    // Web側にタイムアウトレスポンス送信
    sendAuthResponseToWeb(playerName, playerUuid, false, timeoutReason);
  }

  /**
   * Web側に認証レスポンス送信
   */
  private void sendAuthResponseToWeb(String playerName, String playerUuid, boolean success, String message) {
    if (sqsClient == null) {
      logger.warn("SQSクライアントが利用できません。認証レスポンスを送信できません。");
      return;
    }

    try {
      sqsClient.sendAuthResponse(playerName, playerUuid, success, message)
          .thenRun(() -> logger.debug("認証レスポンスを送信しました: {} ({}), success={}", playerName, playerUuid, success))
          .exceptionally(ex -> {
            logger.error("認証レスポンス送信に失敗しました: {} ({})", playerName, playerUuid, ex);
            return null;
          });
    } catch (Exception e) {
      logger.error("認証レスポンス送信でエラーが発生しました: {} ({})", playerName, playerUuid, e);
    }
  }

  /**
   * プレイヤーステータス更新通知
   */
  public void notifyPlayerStatusChange(String playerName, String playerUuid, String status, String serverName) {
    if (sqsClient == null) {
      logger.debug("SQSクライアントが利用できません。プレイヤーステータス通知をスキップします。");
      return;
    }

    try {
      sqsClient.sendPlayerStatus(playerName, playerUuid, status, serverName)
          .thenRun(() -> logger.debug("プレイヤーステータス通知を送信しました: {} {} on {}", playerName, status, serverName))
          .exceptionally(ex -> {
            logger.error("プレイヤーステータス通知送信に失敗しました: {} {}", playerName, status, ex);
            return null;
          });
    } catch (Exception e) {
      logger.error("プレイヤーステータス通知送信でエラーが発生しました: {} {}", playerName, status, e);
    }
  }
}
