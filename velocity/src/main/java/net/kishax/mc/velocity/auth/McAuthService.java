package net.kishax.mc.velocity.auth;

import net.kishax.api.auth.AuthLevel;
import net.kishax.api.auth.AuthLevelResponse;
import net.kishax.api.auth.McAuthClient;
import net.kishax.api.auth.McAuthClientConfig;
import net.kishax.mc.velocity.util.config.VelocityConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MC認証サービス
 * VelocityプラグインからMC認証APIを利用するためのサービス
 */
@Singleton
public class McAuthService {
  private static final Logger logger = LoggerFactory.getLogger(McAuthService.class);

  private final McAuthClient client;
  private final ExecutorService executor;

  @Inject
  public McAuthService(VelocityConfig config) {
    String apiUrl = config.getString("McAuth.ApiUrl");
    String apiKey = config.getString("McAuth.ApiKey");

    if (apiUrl == null || apiUrl.trim().isEmpty()) {
      throw new IllegalStateException("McAuth.ApiUrl is not configured in config.yml");
    }
    if (apiKey == null || apiKey.trim().isEmpty()) {
      throw new IllegalStateException("McAuth.ApiKey is not configured in config.yml");
    }

    McAuthClientConfig clientConfig = new McAuthClientConfig(apiUrl, apiKey);
    this.client = new McAuthClient(clientConfig.getApiUrl(), clientConfig.getApiKey());
    this.executor = Executors.newCachedThreadPool(r -> {
      Thread t = new Thread(r, "McAuthService-Thread");
      t.setDaemon(true);
      return t;
    });

    logger.info("McAuthService initialized with API URL: {}", apiUrl);
  }

  /**
   * プレイヤーの認証レベルを非同期で取得
   *
   * @param mcid MinecraftプレイヤーID
   * @param uuid プレイヤーUUID
   * @return 認証レベルレスポンスのCompletableFuture
   */
  public CompletableFuture<AuthLevelResponse> checkPermissionAsync(String mcid, String uuid) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        return client.checkPermission(mcid, uuid);
      } catch (McAuthClient.McAuthException e) {
        logger.error("Failed to check permission for mcid={}, uuid={}", mcid, uuid, e);
        throw new RuntimeException("Permission check failed", e);
      }
    }, executor);
  }

  /**
   * プレイヤーの認証レベルを同期で取得
   *
   * @param mcid MinecraftプレイヤーID
   * @param uuid プレイヤーUUID
   * @return 認証レベルレスポンス
   * @throws McAuthClient.McAuthException API呼び出しでエラーが発生した場合
   */
  public AuthLevelResponse checkPermission(String mcid, String uuid) throws McAuthClient.McAuthException {
    return client.checkPermission(mcid, uuid);
  }

  /**
   * プレイヤーが指定された認証レベル以上かチェック
   *
   * @param mcid          MinecraftプレイヤーID
   * @param uuid          プレイヤーUUID
   * @param requiredLevel 必要な認証レベル
   * @return 認証レベルが十分な場合true
   */
  public CompletableFuture<Boolean> hasPermissionLevel(String mcid, String uuid, AuthLevel requiredLevel) {
    return checkPermissionAsync(mcid, uuid).thenApply(response -> {
      AuthLevel playerLevel = response.getAuthLevel();
      return isLevelSufficient(playerLevel, requiredLevel);
    });
  }

  /**
   * プレイヤーが指定された製品を購入済みかチェック
   *
   * @param mcid        MinecraftプレイヤーID
   * @param uuid        プレイヤーUUID
   * @param productName 製品名
   * @return 製品を購入済みの場合true
   */
  public CompletableFuture<Boolean> hasProduct(String mcid, String uuid, String productName) {
    return checkPermissionAsync(mcid, uuid).thenApply(response -> {
      List<String> activeProducts = response.getActiveProducts();
      return activeProducts != null && activeProducts.contains(productName);
    });
  }

  /**
   * APIサーバーのヘルスチェック
   *
   * @return サーバーが正常な場合true
   */
  public CompletableFuture<Boolean> healthCheck() {
    return CompletableFuture.supplyAsync(client::healthCheck, executor);
  }

  /**
   * 認証レベルが必要レベル以上かチェック
   *
   * @param playerLevel   プレイヤーの認証レベル
   * @param requiredLevel 必要な認証レベル
   * @return 認証レベルが十分な場合true
   */
  private boolean isLevelSufficient(AuthLevel playerLevel, AuthLevel requiredLevel) {
    // AuthLevelのordinal値で比較（高い値ほど高い権限）
    return playerLevel.ordinal() >= requiredLevel.ordinal();
  }

  /**
   * サービスを停止してリソースをクリーンアップ
   */
  public void shutdown() {
    logger.info("Shutting down McAuthService");
    executor.shutdown();
    client.close();
  }
}
