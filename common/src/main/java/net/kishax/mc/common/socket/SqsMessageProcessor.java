package net.kishax.mc.common.socket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import org.slf4j.Logger;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SQSメッセージプロセッサー（mc-plugins用）
 * Web → MC の双方向通信を処理
 */
public class SqsMessageProcessor {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(SqsMessageProcessor.class);

    private final SqsClient sqsClient;
    private final String webToMcQueueUrl;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService executor;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final SqsMessageHandler messageHandler;

    @Inject
    public SqsMessageProcessor(SqsClient sqsClient, String webToMcQueueUrl, SqsMessageHandler messageHandler) {
        this.sqsClient = sqsClient;
        this.webToMcQueueUrl = webToMcQueueUrl;
        this.messageHandler = messageHandler;
        this.objectMapper = new ObjectMapper();
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "SQS-MC-Processor");
            t.setDaemon(true);
            return t;
        });
    }

    public void start() {
        if (running.compareAndSet(false, true)) {
            logger.info("MC-SQSメッセージプロセッサーを開始します");
            executor.scheduleWithFixedDelay(this::pollMessages, 0, 5, TimeUnit.SECONDS);
        }
    }

    public void stop() {
        if (running.compareAndSet(true, false)) {
            logger.info("MC-SQSメッセージプロセッサーを停止します");
            executor.shutdown();
            try {
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private void pollMessages() {
        if (!running.get()) {
            return;
        }

        try {
            ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                    .queueUrl(webToMcQueueUrl)
                    .maxNumberOfMessages(10)
                    .waitTimeSeconds(20)
                    .messageAttributeNames("All")
                    .build();

            ReceiveMessageResponse response = sqsClient.receiveMessage(request);
            List<Message> messages = response.messages();

            if (!messages.isEmpty()) {
                logger.debug("Web→MC SQSメッセージを受信しました: {} 件", messages.size());
            }

            for (Message message : messages) {
                try {
                    processMessage(message);
                    deleteMessage(message);
                } catch (Exception e) {
                    logger.error("メッセージ処理でエラーが発生しました: {}", message.messageId(), e);
                }
            }

        } catch (Exception e) {
            logger.error("SQSポーリングでエラーが発生しました", e);
        }
    }

    private void processMessage(Message message) throws Exception {
        String body = message.body();
        logger.debug("メッセージ処理開始: {}", message.messageId());

        JsonNode json = objectMapper.readTree(body);
        String messageType = json.path("type").asText();
        String source = message.messageAttributes().containsKey("source") 
            ? message.messageAttributes().get("source").stringValue() 
            : "unknown";

        logger.debug("メッセージタイプ: {}, ソース: {}", messageType, source);

        switch (messageType) {
            case "web_mc_auth_confirm" -> processWebMcAuthConfirm(json);
            case "web_mc_command" -> processWebMcCommand(json);
            case "web_mc_player_request" -> processWebMcPlayerRequest(json);
            default -> {
                logger.warn("不明なメッセージタイプです: {}", messageType);
                // 既存のSocketメッセージハンドラーに転送
                messageHandler.handleMessage(json);
            }
        }
    }

    private void processWebMcAuthConfirm(JsonNode json) {
        String playerName = json.path("playerName").asText();
        String playerUuid = json.path("playerUuid").asText();
        
        logger.info("Web認証完了通知を受信: {} ({})", playerName, playerUuid);
        
        // MC認証完了処理を実行
        messageHandler.handleAuthConfirm(playerName, playerUuid);
    }

    private void processWebMcCommand(JsonNode json) {
        String commandType = json.path("commandType").asText();
        String playerName = json.path("playerName").asText();
        JsonNode data = json.path("data");
        
        logger.info("Webコマンド受信: {} from {}", commandType, playerName);
        
        // コマンド処理を実行
        messageHandler.handleCommand(commandType, playerName, data);
    }

    private void processWebMcPlayerRequest(JsonNode json) {
        String requestType = json.path("requestType").asText();
        String playerName = json.path("playerName").asText();
        JsonNode data = json.path("data");
        
        logger.info("プレイヤーリクエスト受信: {} from {}", requestType, playerName);
        
        // リクエスト処理を実行
        messageHandler.handlePlayerRequest(requestType, playerName, data);
    }

    private void deleteMessage(Message message) {
        try {
            DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                    .queueUrl(webToMcQueueUrl)
                    .receiptHandle(message.receiptHandle())
                    .build();

            sqsClient.deleteMessage(deleteRequest);
            logger.debug("メッセージを削除しました: {}", message.messageId());
        } catch (Exception e) {
            logger.error("メッセージ削除でエラーが発生しました: {}", message.messageId(), e);
        }
    }
}