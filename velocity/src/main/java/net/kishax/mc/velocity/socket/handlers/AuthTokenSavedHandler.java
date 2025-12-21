package net.kishax.mc.velocity.socket.handlers;

import org.slf4j.Logger;
import com.google.inject.Inject;
import net.kishax.mc.velocity.Main;

/**
 * Web側から認証トークンが保存されたことを通知するハンドラー
 */
public class AuthTokenSavedHandler {
  private final Logger logger;

  @Inject
  public AuthTokenSavedHandler(Logger logger) {
    this.logger = logger;
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
      
      // Spigotに通知を転送（コールバック機能を使用）
      if (Main.getAuthTokenSavedCallback() != null) {
        Main.getAuthTokenSavedCallback().onAuthTokenSaved(mcid, uuid, authToken);
        logger.info("📤 Auth token saved notification forwarded to Spigot for player: {}", mcid);
      } else {
        logger.warn("⚠️ Auth token saved callback is not registered");
      }
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
