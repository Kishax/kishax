package net.kishax.mc.common.socket;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * SQSメッセージハンドラーインターface
 * Spigot/Velocityで実装される
 */
public interface SqsMessageHandler {
    
    /**
     * 汎用メッセージハンドラー（既存のSocket互換）
     */
    void handleMessage(JsonNode message);
    
    /**
     * Web認証完了処理
     */
    void handleAuthConfirm(String playerName, String playerUuid);
    
    /**
     * Webからのコマンド処理
     */
    void handleCommand(String commandType, String playerName, JsonNode data);
    
    /**
     * Webからのプレイヤーリクエスト処理
     */
    void handlePlayerRequest(String requestType, String playerName, JsonNode data);

    /**
     * Web→MC OTP送信処理
     */
    void handleOtpToMinecraft(String mcid, String uuid, String otp);
}