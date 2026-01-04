package net.kishax.mc.velocity.socket.handlers;

import org.slf4j.Logger;
import com.google.inject.Inject;
import net.kishax.mc.velocity.socket.VelocitySqsMessageHandler;

/**
 * Web側から認証トークンが保存されたことを通知するハンドラー
 */
public class AuthTokenSavedHandler {
  private final Logger logger;
  private final VelocitySqsMessageHandler sqsMessageHandler;

  @Inject
  public AuthTokenSavedHandler(Logger logger, VelocitySqsMessageHandler sqsMessageHandler) {
    this.logger = logger;
    this.sqsMessageHandler = sqsMessageHandler;
  }

  /**
   * Web側からの認証トークン保存完了通知を処理
   * 
   * @param mcid Minecraftプレイヤー名
   * @param uuid プレイヤーUUID
   * @param authToken 認証トークン
   */
  public void handle(String mcid, String uuid, String authToken) {
    try {
      logger.info("✅ Received auth token saved notification from WEB for player: {} ({})", mcid, uuid);
      
      // VelocitySqsMessageHandlerを使用してSpigotに通知を転送
      sqsMessageHandler.handleAuthTokenSaved(mcid, uuid, authToken);
      logger.info("📤 Auth token saved notification forwarded to Spigot for player: {}", mcid);
    } catch (Exception e) {
      logger.error("❌ Error handling auth token saved notification: {}", e.getMessage(), e);
    }
  }

  /**
   * 認証トークン保存完了通知用のコールバックインターフェース
   */
  public interface AuthTokenSavedCallback {
    void onAuthTokenSaved(String mcid, String uuid, String authToken);
  }
}



