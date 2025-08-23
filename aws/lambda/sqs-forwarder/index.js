import { SQSClient, SendMessageCommand } from "@aws-sdk/client-sqs";
import { SSMClient, GetParameterCommand } from "@aws-sdk/client-ssm";

// AWSクライアントを初期化
const sqsClient = new SQSClient({
  region: process.env.AWS_REGION || "ap-northeast-1",
});

const ssmClient = new SSMClient({
  region: process.env.AWS_REGION || "ap-northeast-1",
});

// SSMからSQS_QUEUE_URLを取得する関数
async function getQueueUrl() {
  try {
    const command = new GetParameterCommand({
      Name: "/kishax/sqs/queue-url",
      WithDecryption: true
    });
    const result = await ssmClient.send(command);
    return result.Parameter.Value;
  } catch (error) {
    console.error("Failed to get SQS Queue URL from SSM:", error);
    throw error;
  }
}

/**
 * Lambda関数のメインハンドラー
 * API Gateway からのリクエストを受け取り、SQS にメッセージを送信
 */
export const handler = async (event) => {
  console.log("Received event:", JSON.stringify(event, null, 2));

  try {
    // SSMからSQS Queue URLを取得
    const QUEUE_URL = await getQueueUrl();
    console.log("SQS Queue URL retrieved:", QUEUE_URL);
    
    // リクエストボディを解析
    let requestBody;
    if (event.body) {
      if (typeof event.body === "string") {
        try {
          requestBody = JSON.parse(event.body);
        } catch (e) {
          console.log("Failed to parse body as JSON, using as is:", event.body);
          requestBody = { message: event.body };
        }
      } else {
        requestBody = event.body;
      }
    } else {
      requestBody = event;
    }

    // リクエストタイプを判定
    const messageType = requestBody.type || "unknown";

    // SQS メッセージを構築
    const sqsMessage = {
      QueueUrl: QUEUE_URL,
      MessageBody: JSON.stringify(requestBody),
      MessageAttributes: {
        messageType: {
          DataType: "String",
          StringValue: messageType,
        },
        source: {
          DataType: "String",
          StringValue: "velocity-plugin",
        },
        timestamp: {
          DataType: "String",
          StringValue: new Date().toISOString(),
        },
      },
    };

    // SQS にメッセージを送信
    const command = new SendMessageCommand(sqsMessage);
    const result = await sqsClient.send(command);

    console.log("Message sent to SQS:", result.MessageId);

    // 成功レスポンスを返す
    return {
      statusCode: 200,
      headers: {
        "Content-Type": "application/json",
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Methods": "POST, OPTIONS",
        "Access-Control-Allow-Headers": "Content-Type, Authorization",
      },
      body: JSON.stringify({
        success: true,
        messageId: result.MessageId,
        "message": "Message queued successfully",
      }),
    };
  } catch (error) {
    console.error("Error processing request:", error);

    // エラーレスポンスを返す
    return {
      statusCode: 500,
      headers: {
        "Content-Type": "application/json",
        "Access-Control-Allow-Origin": "*",
      },
      body: JSON.stringify({
        success: false,
        error: error.message,
        message: "Failed to process request",
      }),
    };
  }
};