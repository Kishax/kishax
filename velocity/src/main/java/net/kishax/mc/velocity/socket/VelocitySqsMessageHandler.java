package net.kishax.mc.velocity.socket;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kishax.mc.common.socket.SqsMessageHandler;
import net.kishax.mc.common.socket.SqsClient;
import net.kishax.mc.common.socket.SocketSwitch;
import net.kishax.mc.common.socket.message.Message;
import net.kishax.mc.common.database.Database;
import net.kishax.mc.velocity.socket.message.handlers.web.VelocityMinecraftWebConfirmHandler;
import org.slf4j.Logger;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 * Velocity用SQSメッセージハンドラー
 */
public class VelocitySqsMessageHandler implements SqsMessageHandler {
  private static final Logger logger = org.slf4j.LoggerFactory.getLogger(VelocitySqsMessageHandler.class);

  private final ProxyServer proxyServer;
  private final VelocityMinecraftWebConfirmHandler webConfirmHandler;
  private final Provider<SocketSwitch> sswProvider;
  private final Database db;
  private final SqsClient sqsClient;

  @Inject
  public VelocitySqsMessageHandler(ProxyServer proxyServer, Provider<SocketSwitch> sswProvider, Database db,
      SqsClient sqsClient, VelocityMinecraftWebConfirmHandler webConfirmHandler) {
    this.proxyServer = proxyServer;
    this.webConfirmHandler = webConfirmHandler;
    this.sswProvider = sswProvider;
    this.db = db;
    this.sqsClient = sqsClient;
  }

  @Override
  public void handleMessage(JsonNode message) {
    // 既存のSocketメッセージ互換処理
    try {
      String messageType = message.path("type").asText();
      logger.debug("汎用メッセージ処理: {}", messageType);

      // 既存のメッセージハンドラーシステムを使用
      // 必要に応じて適切なハンドラーにルーティング

    } catch (Exception e) {
      logger.error("汎用メッセージ処理でエラーが発生しました", e);
    }
  }

  @Override
  public void handleAuthConfirm(String playerName, String playerUuid) {
    try {
      logger.info("Web認証完了: {} ({})", playerName, playerUuid);

      // 既存のWeb認証ハンドラーを使用
      JsonNode confirmData = createConfirmMessage(playerName, playerUuid);
      webConfirmHandler.handleWebToMinecraft(confirmData);

    } catch (Exception e) {
      logger.error("認証確認処理でエラーが発生しました: {} ({})", playerName, playerUuid, e);
    }
  }

  @Override
  public void handleCommand(String commandType, String playerName, JsonNode data) {
    try {
      logger.info("Webコマンド処理: {} from {}", commandType, playerName);

      switch (commandType) {
        case "teleport" -> handleTeleportCommand(playerName, data);
        case "server_switch" -> handleServerSwitchCommand(playerName, data);
        case "message" -> handleMessageCommand(playerName, data);
        default -> logger.warn("不明なコマンドタイプ: {}", commandType);
      }

    } catch (Exception e) {
      logger.error("コマンド処理でエラーが発生しました: {} from {}", commandType, playerName, e);
    }
  }

  @Override
  public void handlePlayerRequest(String requestType, String playerName, JsonNode data) {
    try {
      logger.info("プレイヤーリクエスト処理: {} from {}", requestType, playerName);

      switch (requestType) {
        case "server_status" -> handleServerStatusRequest(playerName, data);
        case "player_list" -> handlePlayerListRequest(playerName, data);
        case "server_info" -> handleServerInfoRequest(playerName, data);
        default -> logger.warn("不明なリクエストタイプ: {}", requestType);
      }

    } catch (Exception e) {
      logger.error("プレイヤーリクエスト処理でエラーが発生しました: {} from {}", requestType, playerName, e);
    }
  }

  @Override
  public void handleOtpToMinecraft(String mcid, String uuid, String otp) {
    try {
      logger.info("Web→MC OTP送信: {} ({}) OTP: {}", mcid, uuid, otp);

      // 既存のメッセージハンドラーシステムを使用してSpigotにOTPを送信
      JsonNode otpMessage = createOtpMessage(mcid, uuid, otp);
      forwardOtpToSpigot(otpMessage);

    } catch (Exception e) {
      logger.error("OTP送信処理でエラーが発生しました: {} ({})", mcid, uuid, e);
    }
  }

  /**
   * 認証完了メッセージを処理
   */
  @Override
  public void handleAuthCompletion(String playerName, String playerUuid, String message) {
    try {
      logger.info("🎉 認証完了通知: {} ({}) - {}", playerName, playerUuid, message);

      // Velocityから直接プレイヤーに認証完了メッセージを送信
      proxyServer.getPlayer(playerName).ifPresent(player -> {
        player.sendMessage(net.kyori.adventure.text.Component.text(message)
            .color(net.kyori.adventure.text.format.NamedTextColor.GREEN));
        logger.info("認証完了メッセージをプレイヤーに送信しました: {}", playerName);
      });

      if (proxyServer.getPlayer(playerName).isEmpty()) {
        logger.warn("認証完了通知対象プレイヤーがオンラインではありません: {}", playerName);
      }

      // 将来の拡張のための処理を呼び出し
      onAuthCompletionExtension(playerName, playerUuid, message);

    } catch (Exception e) {
      logger.error("認証完了処理でエラーが発生しました: {} ({})", playerName, playerUuid, e);
    }
  }

  /**
   * 認証完了時の拡張処理（将来の機能追加用）
   *
   * このメソッドは将来的に追加される認証完了後の処理のために用意されています。
   * 現在は空の実装ですが、後で具体的な処理を追加することができます。
   *
   * 例：
   * - 特別なウェルカムメッセージの送信
   * - イベント通知の送信
   * - 統計データの更新
   * - カスタムリワードの付与
   */
  protected void onAuthCompletionExtension(String playerName, String playerUuid, String message) {
    // 拡張性のための空メソッド
    // 将来的にここに追加の処理を実装できます
    logger.debug("認証完了拡張処理: {} ({}) - 現在は何も実行しません", playerName, playerUuid);
  }

  private JsonNode createConfirmMessage(String playerName, String playerUuid) {
    // handleWebToMinecraft用のメッセージ形式を作成 (直接who構造を返す)
    try {
      com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
      return mapper.createObjectNode()
          .set("who", mapper.createObjectNode()
              .put("name", playerName)
              .put("uuid", playerUuid));
    } catch (Exception e) {
      logger.error("確認メッセージ作成でエラーが発生しました", e);
      return null;
    }
  }

  private void handleTeleportCommand(String playerName, JsonNode data) {
    // テレポートコマンド処理
    String targetLocation = data.path("location").asText();
    logger.info("テレポートコマンド: {} → {}", playerName, targetLocation);

    // 既存のテレポート処理を呼び出し
  }

  private void handleServerSwitchCommand(String playerName, JsonNode data) {
    // サーバー切り替えコマンド処理
    String targetServer = data.path("server").asText();
    logger.info("サーバー切り替えコマンド: {} → {}", playerName, targetServer);

    // 既存のサーバー切り替え処理を呼び出し
  }

  private void handleMessageCommand(String playerName, JsonNode data) {
    // メッセージ送信コマンド処理
    String message = data.path("message").asText();
    logger.info("メッセージコマンド: {} → {}", playerName, message);

    // プレイヤーにメッセージを送信
    proxyServer.getPlayer(playerName).ifPresent(player -> {
      player.sendMessage(net.kyori.adventure.text.Component.text(message));
    });
  }

  private void handleServerStatusRequest(String playerName, JsonNode data) {
    // サーバーステータスリクエスト処理
    logger.info("サーバーステータスリクエスト from {}", playerName);

    // サーバー情報を収集してレスポンス
    // TODO: SqsClientを使ってレスポンスを送信
  }

  private void handlePlayerListRequest(String playerName, JsonNode data) {
    // プレイヤーリストリクエスト処理
    logger.info("プレイヤーリストリクエスト from {}", playerName);

    // プレイヤーリストを収集してレスポンス
    // TODO: SqsClientを使ってレスポンスを送信
  }

  private void handleServerInfoRequest(String playerName, JsonNode data) {
    // サーバー情報リクエスト処理
    logger.info("サーバー情報リクエスト from {}", playerName);

    // サーバー詳細情報を収集してレスポンス
    // TODO: SqsClientを使ってレスポンスを送信
  }

  private JsonNode createOtpMessage(String mcid, String uuid, String otp) {
    // MC側OTP送信用のメッセージ形式を作成
    try {
      com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
      return mapper.createObjectNode()
          .set("minecraft", mapper.createObjectNode()
              .set("otp", mapper.createObjectNode()
                  .put("mcid", mcid)
                  .put("uuid", uuid)
                  .put("otp", otp)
                  .put("action", "send_otp")));
    } catch (Exception e) {
      logger.error("OTPメッセージ作成でエラーが発生しました", e);
      return null;
    }
  }

  private void forwardOtpToSpigot(JsonNode otpMessage) {
    if (otpMessage == null) {
      logger.warn("OTPメッセージがnullのためSpigotへの転送をスキップしました");
      return;
    }

    try {
      // Messageオブジェクトを作成してSpigotに転送
      Message msg = new Message();
      msg.minecraft = new Message.Minecraft();
      msg.minecraft.otp = new Message.Minecraft.Otp();

      JsonNode otpData = otpMessage.path("minecraft").path("otp");
      msg.minecraft.otp.mcid = otpData.path("mcid").asText();
      msg.minecraft.otp.uuid = otpData.path("uuid").asText();
      msg.minecraft.otp.otp = otpData.path("otp").asText();
      msg.minecraft.otp.action = otpData.path("action").asText();

      // SocketSwitchを使用してSpigotに転送
      try (Connection conn = db.getConnection()) {
        SocketSwitch ssw = sswProvider.get();
        ssw.sendSpigotServer(conn, msg);
        logger.info("SpigotへOTPメッセージを転送しました: {} ({})", msg.minecraft.otp.mcid, msg.minecraft.otp.uuid);
      }

    } catch (Exception e) {
      logger.error("SpigotへのOTP転送でエラーが発生しました", e);
    }
  }

  /**
   * Web側にOTPレスポンスをSQS送信
   */
  public void sendOtpResponseToWeb(String mcid, String uuid, boolean success, String message) {
    if (sqsClient == null) {
      logger.warn("SQSクライアントが利用できません。OTPレスポンスを送信できません。");
      return;
    }

    try {
      Map<String, Object> responseData = new HashMap<>();
      responseData.put("mcid", mcid);
      responseData.put("uuid", uuid);
      responseData.put("success", success);
      responseData.put("message", message);
      responseData.put("timestamp", System.currentTimeMillis());

      sqsClient.sendGenericMessage("mc_otp_response", responseData)
          .thenRun(() -> logger.info("Web側にOTPレスポンスを送信しました: {} ({}), success={}", mcid, uuid, success))
          .exceptionally(ex -> {
            logger.error("OTPレスポンス送信に失敗しました: {} ({})", mcid, uuid, ex);
            return null;
          });

    } catch (Exception e) {
      logger.error("OTPレスポンス送信でエラーが発生しました: {} ({})", mcid, uuid, e);
    }
  }
}
