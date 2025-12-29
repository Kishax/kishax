package net.kishax.mc.velocity.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * MC認証レベルチェック機能
 * 1分間隔でkishax-api APIを呼び出し、プレイヤーの権限を更新
 */
public class AuthLevelChecker {
  private static final Logger logger = LoggerFactory.getLogger(AuthLevelChecker.class);

  private final ProxyServer proxyServer;
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final String authApiUrl;
  private final String apiKey;
  private final ScheduledExecutorService scheduler;
  private final Map<UUID, AuthLevel> playerAuthLevels;
  private final Map<UUID, List<String>> playerProducts;
  private final LuckPerms luckPerms;

  public AuthLevelChecker(ProxyServer proxyServer, String authApiUrl, String apiKey) {
    this(proxyServer, authApiUrl, apiKey, LuckPermsProvider.get());
  }

  public AuthLevelChecker(ProxyServer proxyServer, String authApiUrl, String apiKey, LuckPerms luckPerms) {
    this.proxyServer = proxyServer;
    this.authApiUrl = authApiUrl;
    this.apiKey = apiKey;
    this.httpClient = HttpClient.newBuilder()
        .build();
    this.objectMapper = new ObjectMapper();
    this.objectMapper.registerModule(new JavaTimeModule());
    this.scheduler = Executors.newScheduledThreadPool(1);
    this.playerAuthLevels = new ConcurrentHashMap<>();
    this.playerProducts = new ConcurrentHashMap<>();
    this.luckPerms = luckPerms;

    logger.info("AuthLevelChecker initialized with API URL: {}", authApiUrl);
  }

  /**
   * 定期チェックを開始
   */
  public void startPeriodicCheck() {
    scheduler.scheduleAtFixedRate(() -> {
      try {
        for (Player player : proxyServer.getAllPlayers()) {
          checkAndUpdatePlayerAuthLevel(player);
        }
      } catch (Exception e) {
        logger.error("Error during periodic auth level check", e);
      }
    }, 0, 1, TimeUnit.MINUTES);

    logger.info("Started periodic auth level checks (every 1 minute)");
  }

  /**
   * 定期チェックを停止
   */
  public void stopPeriodicCheck() {
    scheduler.shutdown();
    try {
      if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
        scheduler.shutdownNow();
      }
    } catch (InterruptedException e) {
      scheduler.shutdownNow();
      Thread.currentThread().interrupt();
    }
    logger.info("Stopped periodic auth level checks");
  }

  /**
   * 個別プレイヤーの認証レベルをチェック
   */
  public CompletableFuture<Void> checkAndUpdatePlayerAuthLevel(Player player) {
    return CompletableFuture.runAsync(() -> {
      try {
        // kishax-api APIリクエスト作成
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("mcid", player.getUsername());
        requestBody.put("uuid", player.getUniqueId().toString());

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(authApiUrl + "/api/auth/check-permission"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .timeout(Duration.ofSeconds(10))
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();

        // API呼び出し
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
          // レスポンス解析
          AuthLevelResponse authResponse = objectMapper.readValue(response.body(), AuthLevelResponse.class);

          AuthLevel authLevel = AuthLevel.fromString(authResponse.getAuthLevel());
          List<String> activeProducts = authResponse.getActiveProducts();

          // キャッシュ更新
          AuthLevel previousLevel = playerAuthLevels.put(player.getUniqueId(), authLevel);
          playerProducts.put(player.getUniqueId(), activeProducts);

          // 権限レベルが変更された場合のみLuckPerms更新
          if (previousLevel != authLevel) {
            updatePlayerPermissions(player, authLevel, activeProducts);
            logger.info("Updated auth level for {}: {} -> {} (products: {})",
                player.getUsername(),
                previousLevel != null ? previousLevel : "null",
                authLevel,
                activeProducts.size());
          }
        } else {
          logger.warn("Auth API returned status {} for player {}: {}",
              response.statusCode(), player.getUsername(), response.body());
        }

      } catch (IOException | InterruptedException e) {
        logger.error("Failed to check auth level for player {}: {}", player.getUsername(), e.getMessage());
      }
    }).exceptionally((Throwable e) -> {
      logger.error("Error in auth level check for player {}", player.getUsername(), e);
      return null;
    });
  }

  /**
   * 認証レベルに応じたLuckPerms権限更新
   */
  private void updatePlayerPermissions(Player player, AuthLevel authLevel, List<String> activeProducts) {
    User user = luckPerms.getPlayerAdapter(Player.class).getUser(player);
    if (user == null) {
      logger.warn("Could not get LuckPerms user for player {}", player.getUsername());
      return;
    }

    // 全ての認証関連権限をクリア
    user.data().clear(node -> node.getKey().startsWith("group.new-user") ||
        node.getKey().startsWith("group.verified") ||
        node.getKey().startsWith("group.premium") ||
        node.getKey().startsWith("group.vip") ||
        node.getKey().startsWith("group.developer"));

    // 認証レベル別権限設定
    switch (authLevel) {
      case MC_UNAUTHENTICATED:
        applyNewPlayerPermissions(user);
        break;

      case MC_AUTHENTICATED_TRYING:
        applyTempPermissions(user);
        break;

      case MC_AUTHENTICATED_UNLINKED:
        applyUnlinkedPermissions(user);
        break;

      case MC_AUTHENTICATED_LINKED:
        applyLinkedPermissions(user);
        break;

      case MC_AUTHENTICATED_PRODUCT:
        applyProductPermissions(user, activeProducts);
        break;
    }

    // 変更を保存
    luckPerms.getUserManager().saveUser(user);
  }

  /**
   * 新規プレイヤー権限設定
   * MC_UNAUTHENTICATED: WEB認証未実施
   * 権限なし（認証を促すメッセージのみ表示）
   */
  private void applyNewPlayerPermissions(User user) {
    // 権限なし - WEB認証を促す
  }

  /**
   * 一時権限設定
   * MC_AUTHENTICATED_TRYING: 認証トークン発行済み、WEB認証待ち
   * 権限なし（認証URL表示済み、WEB認証完了待ち）
   */
  private void applyTempPermissions(User user) {
    // 権限なし - WEB認証完了待ち
    // 一時的な追加権限があればここに追加
  }

  /**
   * 未連携プレイヤー権限設定
   */
  private void applyUnlinkedPermissions(User user) {
    user.data().add(Node.builder("group.new-user").build());
  }

  /**
   * 連携済みプレイヤー権限設定
   */
  private void applyLinkedPermissions(User user) {
    user.data().add(Node.builder("group.new-user").build());
    // Kishaxアカウント連携ユーザー向け基本権限
  }

  /**
   * プロダクト購入済みプレイヤー権限設定
   */
  private void applyProductPermissions(User user, List<String> activeProducts) {
    // 基本権限
    user.data().add(Node.builder("group.new-user").build());

    // プロダクト別権限追加
    for (String product : activeProducts) {
      switch (product) {
        case "Premium Access":
          user.data().add(Node.builder("group.premium").build());
          break;
        case "VIP Package":
          user.data().add(Node.builder("group.vip").build());
          break;
        case "Developer Tools":
          user.data().add(Node.builder("group.developer").build());
          break;
        default:
          logger.info("Unknown product for permission mapping: {}", product);
          break;
      }
    }
  }

  /**
   * プレイヤーの現在の認証レベルを取得
   */
  public AuthLevel getPlayerAuthLevel(UUID playerUuid) {
    return playerAuthLevels.getOrDefault(playerUuid, AuthLevel.MC_UNAUTHENTICATED);
  }

  /**
   * プレイヤーのアクティブプロダクトを取得
   */
  public List<String> getPlayerProducts(UUID playerUuid) {
    return playerProducts.getOrDefault(playerUuid, List.of());
  }

  /**
   * API レスポンス用クラス
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class AuthLevelResponse {
    private String authLevel;
    private List<String> activeProducts;
    private String kishaxUserId;
    private String lastUpdated;  // Auth APIが返すタイムスタンプ（無視しても良い）

    // getters and setters
    public String getAuthLevel() {
      return authLevel;
    }

    public void setAuthLevel(String authLevel) {
      this.authLevel = authLevel;
    }

    public List<String> getActiveProducts() {
      return activeProducts;
    }

    public void setActiveProducts(List<String> activeProducts) {
      this.activeProducts = activeProducts;
    }

    public String getKishaxUserId() {
      return kishaxUserId;
    }

    public void setKishaxUserId(String kishaxUserId) {
      this.kishaxUserId = kishaxUserId;
    }

    public String getLastUpdated() {
      return lastUpdated;
    }

    public void setLastUpdated(String lastUpdated) {
      this.lastUpdated = lastUpdated;
    }
  }
}
