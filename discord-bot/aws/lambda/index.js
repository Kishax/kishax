const AWS = require("aws-sdk");

// SQSクライアントを初期化
const sqs = new AWS.SQS({
  region: process.env.AWS_REGION || "ap-northeast-1",
});

const QUEUE_URL = process.env.SQS_QUEUE_URL;

/**
 * Lambda関数のメインハンドラー
 * API Gateway からのリクエストを受け取り、SQS にメッセージを送信
 */
exports.handler = async (event) => {
  console.log("Received event:", JSON.stringify(event, null, 2));

  try {
    // リクエストボディを解析
    let requestBody;
    if (event.body) {
      requestBody =
        typeof event.body === "string" ? JSON.parse(event.body) : event.body;
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
    const result = await sqs.sendMessage(sqsMessage).promise();

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
        message: "Message queued successfully",
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

/**
 * サーバーステータス更新用のメッセージタイプ
 * @param {string} serverName - サーバー名
 * @param {string} status - ステータス (online, offline, starting)
 */
function createServerStatusMessage(serverName, status) {
  return {
    type: "server_status",
    serverName: serverName,
    status: status,
    timestamp: new Date().toISOString(),
  };
}

/**
 * プレイヤーリクエスト用のメッセージタイプ
 * @param {string} playerName - プレイヤー名
 * @param {string} playerUUID - プレイヤーUUID
 * @param {string} serverName - サーバー名
 * @param {string} requestId - リクエストID
 */
function createPlayerRequestMessage(
  playerName,
  playerUUID,
  serverName,
  requestId,
) {
  return {
    type: "player_request",
    playerName: playerName,
    playerUUID: playerUUID,
    serverName: serverName,
    requestId: requestId,
    timestamp: new Date().toISOString(),
  };
}

/**
 * ブロードキャストメッセージ用のメッセージタイプ
 * @param {string} content - メッセージ内容
 * @param {boolean} isChat - チャットチャンネル向けかどうか
 */
function createBroadcastMessage(content, isChat = false) {
  return {
    type: "broadcast",
    content: content,
    isChat: isChat,
    timestamp: new Date().toISOString(),
  };
}

/**
 * Embedメッセージ用のメッセージタイプ
 * @param {object} embedData - Embed データ
 */
function createEmbedMessage(embedData) {
  return {
    type: "embed",
    embedData: embedData,
    timestamp: new Date().toISOString(),
  };
}

// エクスポート（テスト用）
module.exports = {
  createServerStatusMessage,
  createPlayerRequestMessage,
  createBroadcastMessage,
  createEmbedMessage,
};
