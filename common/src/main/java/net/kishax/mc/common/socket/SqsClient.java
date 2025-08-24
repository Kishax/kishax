package net.kishax.mc.common.socket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import org.slf4j.Logger;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

/**
 * SQSクライアント（mc-plugins → web通信用）
 * 既存のSocketSwitchの代替として機能
 */
public class SqsClient {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(SqsClient.class);

    private final SqsClient sqsClient;
    private final String mcToWebQueueUrl;
    private final String apiGatewayUrl;
    private final ObjectMapper objectMapper;

    @Inject
    public SqsClient(String region, String accessKey, String secretKey, String mcToWebQueueUrl, String apiGatewayUrl) {
        this.mcToWebQueueUrl = mcToWebQueueUrl;
        this.apiGatewayUrl = apiGatewayUrl;
        this.objectMapper = new ObjectMapper();

        // SQSクライアントを初期化
        this.sqsClient = software.amazon.awssdk.services.sqs.SqsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .build();
    }

    /**
     * MC認証レスポンスをWebに送信
     */
    public CompletableFuture<Void> sendAuthResponse(String playerName, String playerUuid, boolean success, String message) {
        Map<String, Object> payload = new TreeMap<>();
        payload.put("type", "mc_web_auth_response");
        payload.put("playerName", playerName);
        payload.put("playerUuid", playerUuid);
        payload.put("success", success);
        payload.put("message", message);
        payload.put("timestamp", System.currentTimeMillis());

        return sendMessage(payload);
    }

    /**
     * プレイヤー状態変更をWebに送信
     */
    public CompletableFuture<Void> sendPlayerStatus(String playerName, String playerUuid, String status, String serverName) {
        Map<String, Object> payload = new TreeMap<>();
        payload.put("type", "mc_web_player_status");
        payload.put("playerName", playerName);
        payload.put("playerUuid", playerUuid);
        payload.put("status", status); // online, offline, move
        payload.put("serverName", serverName);
        payload.put("timestamp", System.currentTimeMillis());

        return sendMessage(payload);
    }

    /**
     * サーバー情報をWebに送信
     */
    public CompletableFuture<Void> sendServerInfo(String serverName, String status, int playerCount, Map<String, Object> additionalData) {
        Map<String, Object> payload = new TreeMap<>();
        payload.put("type", "mc_web_server_info");
        payload.put("serverName", serverName);
        payload.put("status", status);
        payload.put("playerCount", playerCount);
        payload.put("additionalData", additionalData);
        payload.put("timestamp", System.currentTimeMillis());

        return sendMessage(payload);
    }

    /**
     * 汎用メッセージ送信（既存Socket互換）
     */
    public CompletableFuture<Void> sendGenericMessage(String messageType, Map<String, Object> data) {
        Map<String, Object> payload = new TreeMap<>();
        payload.put("type", messageType);
        payload.putAll(data);
        payload.put("timestamp", System.currentTimeMillis());

        return sendMessage(payload);
    }

    /**
     * SQSメッセージ送信（内部メソッド）
     */
    private CompletableFuture<Void> sendMessage(Map<String, Object> payload) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String jsonBody = objectMapper.writeValueAsString(payload);
                
                SendMessageRequest request = SendMessageRequest.builder()
                        .queueUrl(mcToWebQueueUrl)
                        .messageBody(jsonBody)
                        .messageAttributes(Map.of(
                                "messageType", software.amazon.awssdk.services.sqs.model.MessageAttributeValue.builder()
                                        .dataType("String")
                                        .stringValue(payload.get("type").toString())
                                        .build(),
                                "source", software.amazon.awssdk.services.sqs.model.MessageAttributeValue.builder()
                                        .dataType("String")
                                        .stringValue("mc-plugins")
                                        .build(),
                                "timestamp", software.amazon.awssdk.services.sqs.model.MessageAttributeValue.builder()
                                        .dataType("String")
                                        .stringValue(String.valueOf(System.currentTimeMillis()))
                                        .build()
                        ))
                        .build();

                SendMessageResponse response = sqsClient.sendMessage(request);
                logger.debug("SQSメッセージ送信成功: {}", response.messageId());
                return null;

            } catch (Exception e) {
                logger.error("SQSメッセージ送信でエラーが発生しました", e);
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * リソース解放
     */
    public void close() {
        if (sqsClient != null) {
            sqsClient.close();
        }
    }
}