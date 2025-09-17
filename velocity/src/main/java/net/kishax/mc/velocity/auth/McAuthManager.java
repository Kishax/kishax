package net.kishax.mc.velocity.auth;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MC認証マネージャー
 * Guice管理下でMcAuthServiceを提供
 */
@Singleton
public class McAuthManager {
  private static final Logger logger = LoggerFactory.getLogger(McAuthManager.class);

  private final McAuthService authService;

  @Inject
  public McAuthManager(McAuthService authService) {
    this.authService = authService;
    logger.info("McAuthManager initialized with authService");
  }

  /**
   * 認証サービスを取得
   *
   * @return McAuthServiceインスタンス
   */
  public McAuthService getAuthService() {
    return authService;
  }

  /**
   * 認証サービスを停止
   */
  public void shutdown() {
    if (authService != null) {
      authService.shutdown();
      logger.info("McAuthManager shutdown completed");
    }
  }
}
