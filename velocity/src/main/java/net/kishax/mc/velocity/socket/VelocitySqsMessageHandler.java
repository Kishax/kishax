package net.kishax.mc.velocity.socket;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kishax.mc.common.socket.SqsMessageHandler;
import net.kishax.mc.velocity.socket.message.handlers.web.VelocityMinecraftWebConfirmHandler;
import net.kishax.mc.velocity.socket.VelocityAuthResponseHandler;
import org.slf4j.Logger;

/**
 * Velocity用SQSメッセージハンドラー
 */
public class VelocitySqsMessageHandler implements SqsMessageHandler {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(VelocitySqsMessageHandler.class);

    private final ProxyServer proxyServer;
    private final Injector injector;
    private final VelocityMinecraftWebConfirmHandler webConfirmHandler;
    private final VelocityAuthResponseHandler authResponseHandler;

    @Inject
    public VelocitySqsMessageHandler(ProxyServer proxyServer, Injector injector) {
        this.proxyServer = proxyServer;
        this.injector = injector;
        this.webConfirmHandler = injector.getInstance(VelocityMinecraftWebConfirmHandler.class);
        this.authResponseHandler = injector.getInstance(VelocityAuthResponseHandler.class);
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

    private JsonNode createConfirmMessage(String playerName, String playerUuid) {
        // 既存のMinecraftWebConfirmHandler用のメッセージ形式を作成
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.createObjectNode()
                    .put("web", mapper.createObjectNode()
                            .set("confirm", mapper.createObjectNode()
                                    .set("who", mapper.createObjectNode()
                                            .put("name", playerName)
                                            .put("uuid", playerUuid))));
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
            // 既存のSocketシステムを使用してSpigotに転送
            // TODO: 実際のSocket転送実装
            logger.info("SpigotへOTPメッセージを転送: {}", otpMessage.toString());
            
        } catch (Exception e) {
            logger.error("SpigotへのOTP転送でエラーが発生しました", e);
        }
    }
}