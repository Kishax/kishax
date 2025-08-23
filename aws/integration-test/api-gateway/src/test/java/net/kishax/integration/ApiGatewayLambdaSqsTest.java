package net.kishax.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.apigateway.ApiGatewayClient;
import software.amazon.awssdk.services.apigateway.model.TestInvokeMethodRequest;
import software.amazon.awssdk.services.apigateway.model.TestInvokeMethodResponse;
import software.amazon.awssdk.services.sqs.model.Message;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * API Gateway → Lambda → SQS 統合フローのテスト
 * 
 * テストフロー:
 * 1. API Gateway テストコンソール経由でLambda関数呼び出し
 * 2. Lambda関数がSQSにメッセージ送信
 * 3. SQSからメッセージ受信確認
 */
@Tag("integration")
public class ApiGatewayLambdaSqsTest {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiGatewayLambdaSqsTest.class);
    
    private ApiGatewayClient apiGatewayClient;
    private String sqsQueueUrl;
    
    @BeforeEach
    void setUp() {
        apiGatewayClient = TestConfig.createApiGatewayClient();
        sqsQueueUrl = TestConfig.SQS_QUEUE_URL;
        
        logger.info("Test setup - SQS Queue URL: {}", sqsQueueUrl);
    }
    
    @Test
    void shouldProcessApiGatewayRequestThroughLambdaToSqs() throws Exception {
        // テストメッセージ作成
        Map<String, Object> testMessage = Map.of(
            "type", "api_gateway_test",
            "message", "API Gateway → Lambda → SQS integration test",
            "timestamp", java.time.Instant.now().toString(),
            "source", "api-gateway-test"
        );
        
        String requestBody = TestUtils.toJson(testMessage);
        logger.info("Testing API Gateway with message: {}", requestBody);
        
        // SQS事前状態確認
        int initialMessageCount = TestUtils.getSQSMessageCount(sqsQueueUrl);
        logger.info("Initial SQS message count: {}", initialMessageCount);
        
        // API Gateway テスト実行
        TestInvokeMethodRequest testRequest = TestInvokeMethodRequest.builder()
            .restApiId(TestConfig.API_GATEWAY_ID)
            .resourceId(getDiscordResourceId())
            .httpMethod("POST")
            .body(requestBody)
            .build();
        
        TestInvokeMethodResponse response = apiGatewayClient.testInvokeMethod(testRequest);
        
        // API Gateway レスポンス確認
        assertThat(response.status())
            .as("API Gateway should return 200")
            .isEqualTo(200);
        
        logger.info("API Gateway test response: {} - {}", response.status(), response.body());
        
        // Lambda処理完了まで少し待機
        TestUtils.waitFor(Duration.ofSeconds(3));
        
        // SQSメッセージ確認
        List<Message> messages = TestUtils.receiveSQSMessages(
            sqsQueueUrl,
            10,
            Duration.ofSeconds(5)
        );
        
        // メッセージ受信確認
        assertThat(messages)
            .as("Should receive message from SQS after API Gateway invocation")
            .isNotEmpty();
        
        // メッセージ内容確認
        Message receivedMessage = messages.get(0);
        Map<String, Object> receivedMessageBody = TestUtils.parseJson(receivedMessage.body());
        
        logger.info("Received SQS message: {}", receivedMessage.body());
        
        // 送信したメッセージ内容が含まれているか確認
        assertThat(receivedMessageBody)
            .as("Received message should contain original test data")
            .containsEntry("type", testMessage.get("type"))
            .containsEntry("source", testMessage.get("source"));
        
        // クリーンアップ
        TestUtils.deleteSQSMessage(sqsQueueUrl, receivedMessage.receiptHandle());
        
        logger.info("API Gateway → Lambda → SQS integration test completed successfully");
    }
    
    @Test
    void shouldHandleInvalidRequestGracefully() throws Exception {
        // 不正なリクエストボディでテスト
        String invalidRequestBody = "{ invalid json }";
        
        TestInvokeMethodRequest testRequest = TestInvokeMethodRequest.builder()
            .restApiId(TestConfig.API_GATEWAY_ID)
            .resourceId(getDiscordResourceId())
            .httpMethod("POST")
            .body(invalidRequestBody)
            .build();
        
        TestInvokeMethodResponse response = apiGatewayClient.testInvokeMethod(testRequest);
        
        // エラーハンドリング確認
        logger.info("Invalid request response: {} - {}", response.status(), response.body());
        
        // 4xx または 5xx のエラーコードが返されることを確認
        assertThat(response.status())
            .as("Invalid request should return error status")
            .isGreaterThanOrEqualTo(400);
    }
    
    /**
     * Discord リソースIDを取得
     * 実際の実装では動的に取得するが、テスト用に固定値を使用
     */
    private String getDiscordResourceId() {
        // API Gateway の /discord リソースID
        // 実際の値は AWS Console または CLI で確認
        return "0it440"; // TestConfig で管理することも可能
    }
}