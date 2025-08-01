require("dotenv").config();
const SlackNotifier = require("./SlackNotifier");
const UserManager = require("./UserManager");
const ConnectionManager = require("./ConnectionManager");
const ConfigManager = require("./ConfigManager");

// Node.js環境でWebSocketを使用可能にする
global.WebSocket = require("ws");

class GatherSlackBot {
  constructor() {
    this.slackNotifier = null;
    this.userManager = null;
    this.connectionManager = null;
    this.configManager = null;
    this.initialUsersLoaded = false;
    this.pendingEvents = [];
    this.hasNotifiedStartup = false;
    this.lastEmptySpaceNotification = 0;

    // 環境変数の取得と検証
    this.slackWebhookUrl = process.env.SLACK_WEBHOOK_URL;
    this.gatherApiKey = process.env.GATHER_API_KEY;
    this.gatherSpaceId = process.env.GATHER_SPACE_ID;

    if (!this.slackWebhookUrl || !this.gatherApiKey || !this.gatherSpaceId) {
      throw new Error(
        "環境変数が設定されていません。.envファイルを確認してください。",
      );
    }

    // 各マネージャーを初期化
    this.configManager = new ConfigManager();
    this.slackNotifier = new SlackNotifier(this.slackWebhookUrl);
    this.userManager = new UserManager();
    this.connectionManager = new ConnectionManager(
      this.gatherSpaceId,
      this.gatherApiKey,
    );

    console.log("🔧 設定確認:");
    console.log(`- Space ID: ${this.gatherSpaceId}`);
    console.log(`- API Key: ${this.gatherApiKey ? "設定済み" : "未設定"}`);
    console.log(
      `- Slack Webhook: ${this.slackWebhookUrl ? "設定済み" : "未設定"}`,
    );
  }

  async loadInitialUsers() {
    try {
      const currentMembers = await this.userManager.loadInitialUsers(
        this.connectionManager.getGame(),
      );

      // 現在のメンバーリストをSlackに通知
      if (this.configManager.isInitialMemberListNotificationEnabled()) {
        await this.slackNotifier.notifyMemberList(currentMembers, this.configManager);
        console.log(`✅ 初期メンバーリスト通知完了: ${currentMembers.length}人`);
      } else {
        console.log("⏸️ 初期メンバーリストの通知はスキップされました（設定により）");
      }

      this.initialUsersLoaded = true;

      // 初期ロード中に蓄積されたイベントを処理
      await this.processPendingEvents();

      // 定期的な接続監視を開始
      this.connectionManager.startConnectionMonitoring(
        this.userManager,
        this.slackNotifier,
      );
    } catch (error) {
      console.error("❌ 初期メンバーリスト取得エラー:", error);
      this.initialUsersLoaded = true;
      await this.processPendingEvents();
      this.connectionManager.startConnectionMonitoring(
        this.userManager,
        this.slackNotifier,
      );
    }
  }

  async processPendingEvents() {
    if (this.pendingEvents.length > 0) {
      console.log(
        `🔄 初期ロード完了後の保留イベント処理開始: ${this.pendingEvents.length}件`,
      );

      for (const event of this.pendingEvents) {
        console.log(
          `🔄 保留イベント処理: ${event.type} (ID: ${event.playerId})`,
        );

        if (event.type === "playerJoins") {
          if (!this.userManager.hasUser(event.playerId)) {
            console.log(
              `✅ 保留イベントを新規参加として処理: ${event.playerId}`,
            );
            await this.handlePlayerJoins(event.data, event.context);
          } else {
            console.log(
              `! 保留イベントは初期メンバーのため無視: ${event.playerId}`,
            );
          }
        } else if (event.type === "playerLeaves") {
          await this.handlePlayerLeaves(event.data, event.context);
        }
      }

      this.pendingEvents = [];
      console.log(`✅ 保留イベント処理完了`);
    }
  }

  async handlePlayerJoins(data, context) {
    const playerId = context.playerId;

    if (this.userManager.isRecentJoinEvent(playerId)) {
      console.log(`! 重複参加イベントを無視: ${playerId} (5秒以内に処理済み)`);
      return;
    }

    if (!this.userManager.hasUser(playerId)) {
      console.log(`✅ 新規参加として処理開始: ${playerId}`);
      this.userManager.addJoinEvent(playerId);

      console.log("📊 完全なイベントデータ:", JSON.stringify(data, null, 2));
      console.log("📊 完全なcontextデータ:", JSON.stringify(context, null, 2));

      // ユーザー名取得
      const result = await this.userManager.getUserName(
        playerId,
        this.connectionManager.getGame(),
        data,
        context,
      );
      this.userManager.addUser(playerId, result.name);

      console.log(
        `👤 ユーザー参加確定: ${result.name} (遅延取得: ${result.isDelayed})`,
      );

      // Slack通知を送信
      if (this.configManager.isJoinNotificationEnabled()) {
        await this.slackNotifier.notifyUserJoined(result.name);
      } else {
        console.log(`⏸️ 参加通知はスキップされました（設定により）: ${result.name}`);
      }
    } else {
      console.log(`! 既に接続済みのユーザーのため通知スキップ: ${playerId}`);
      console.log(
        `📊 キャッシュされた名前: ${this.userManager.getCachedUserName(playerId)}`,
      );
    }
  }

  async handlePlayerLeaves(data, context) {
    const playerId = context.playerId;

    console.log(`📤 退出処理チェック: ${playerId}`);
    console.log(
      `📊 connectedUsersに含まれている？: ${this.userManager.hasUser(playerId)}`,
    );

    if (this.userManager.hasUser(playerId)) {
      console.log(`✅ 退出処理開始: ${playerId}`);

      console.log(
        "📊 完全な退出イベントデータ:",
        JSON.stringify(data, null, 2),
      );
      console.log(
        "📊 完全な退出contextデータ:",
        JSON.stringify(context, null, 2),
      );

      // 退出時は遅延取得なし（キャッシュまたは即座に取得可能な情報のみ）
      const result = await this.userManager.getUserName(
        playerId,
        this.connectionManager.getGame(),
        data,
        context,
        true,
      );

      this.userManager.removeUser(playerId);

      console.log(`👤 ユーザー退出確定: ${result.name}`);
      console.log(
        `📊 退出後の接続ユーザー: [${this.userManager.getAllUserIds().join(", ")}]`,
      );

      console.log(`🔄 Slack退出通知を送信中: ${result.name}`);
      if (this.configManager.isLeaveNotificationEnabled()) {
        await this.slackNotifier.notifyUserLeft(result.name);
        console.log(`✅ Slack退出通知送信完了: ${result.name}`);
      } else {
        console.log(`⏸️ 退出通知はスキップされました（設定により）: ${result.name}`);
      }
    } else {
      console.log(`! 未接続のユーザーの退出イベント: ${playerId}`);
      console.log(
        `📊 現在の接続ユーザー一覧: [${this.userManager.getAllUserIds().join(", ")}]`,
      );
    }
  }

  async connect() {
    try {
      console.log("🔄 Gatherに接続中...");

      const connected = await this.connectionManager.connect();
      if (!connected) {
        this.connectionManager.scheduleReconnect(() => this.connect());
        return;
      }

      // 接続成功時のイベント
      this.connectionManager.subscribeToConnection(async (connected) => {
        if (connected) {
          // 起動通知は初回のみ送信（設定に応じて）
          if (!this.hasNotifiedStartup) {
            if (this.configManager.isStartupNotificationEnabled()) {
              await this.slackNotifier.notifyStartup();
            } else {
              console.log("⏸️ 起動通知はスキップされました（設定により）");
            }
            this.hasNotifiedStartup = true;
          } else {
            console.log("🔄 再接続完了（起動通知スキップ）");
          }

          // 初期メンバーリストを取得・通知
          await this.loadInitialUsers();
        } else {
          console.log("❌ Gatherから切断されました");
          this.initialUsersLoaded = false;

          // 再接続を試行
          this.connectionManager.scheduleReconnect(() => this.connect());
        }
      });

      // プレイヤー参加イベント
      this.connectionManager.subscribeToEvent(
        "playerJoins",
        async (data, context) => {
          try {
            const playerId = context.playerId;

            console.log(`📥 プレイヤー参加イベント受信 (ID: ${playerId})`);
            console.log(`📊 初期ロード完了: ${this.initialUsersLoaded}`);
            console.log(
              `📊 現在の接続ユーザー: [${this.userManager.getAllUserIds().join(", ")}]`,
            );
            console.log(
              `📊 このユーザーは既に接続済み？: ${this.userManager.hasUser(playerId)}`,
            );

            // 初期ロード完了前は保留
            if (!this.initialUsersLoaded) {
              console.log(`⏳ 初期ロード中のためイベントを保留: ${playerId}`);
              this.pendingEvents.push({
                type: "playerJoins",
                playerId: playerId,
                data: data,
                context: context,
              });
              return;
            }

            // 初期ロード完了後は通常処理
            await this.handlePlayerJoins(data, context);
          } catch (error) {
            console.error("❌ プレイヤー参加処理エラー:", error);
          }
        },
      );

      // プレイヤー退出イベント
      this.connectionManager.subscribeToEvent(
        "playerLeaves",
        async (data, context) => {
          try {
            const playerId = context.playerId;

            console.log(`📤 プレイヤー退出イベント受信 (ID: ${playerId})`);
            console.log(
              `📊 現在の接続ユーザー: [${this.userManager.getAllUserIds().join(", ")}]`,
            );
            console.log(
              `📊 このユーザーは接続済み？: ${this.userManager.hasUser(playerId)}`,
            );

            // 初期ロード完了前は保留
            if (!this.initialUsersLoaded) {
              console.log(`⏳ 初期ロード中のためイベントを保留: ${playerId}`);
              this.pendingEvents.push({
                type: "playerLeaves",
                playerId: playerId,
                data: data,
                context: context,
              });
              return;
            }

            // 初期ロード完了後は通常処理
            await this.handlePlayerLeaves(data, context);
          } catch (error) {
            console.error("❌ プレイヤー退出処理エラー:", error);
          }
        },
      );

      // プレイヤー移動イベント（デバッグ用）
      this.connectionManager.subscribeToEvent(
        "playerMoves",
        (data, context) => {
          // console.log(`🚶 ${context.playerId} が移動しました`);
        },
      );

      // チャットメッセージイベント
      this.connectionManager.subscribeToEvent(
        "playerChats",
        async (data, context) => {
          try {
            const playerId = context.playerId;
            const result = await this.userManager.getUserName(
              playerId,
              this.connectionManager.getGame(),
              null,
              context,
            );
            const message = data.contents;

            console.log(`💬 ${result.name}: ${message}`);
            // 必要に応じてチャットもSlackに転送可能
            // await this.slackNotifier.sendNotification(`💬 **${result.name}**: ${message}`, '#cccccc');
          } catch (error) {
            console.error("❌ チャット処理エラー:", error);
          }
        },
      );

      // エラーハンドリング
      this.connectionManager.subscribeToEvent("error", async (error) => {
        console.error("❌ Gatherエラー:", error);
        const errorMessage =
          error?.message || error?.toString() || "不明なエラー";
        await this.slackNotifier.notifyError(errorMessage);
      });
    } catch (error) {
      console.error("❌ 接続エラー:", error);
      this.connectionManager.scheduleReconnect(() => this.connect());
    }
  }

  getCurrentUserCount() {
    return this.userManager.getUserCount();
  }

  startStatusReporting(intervalMinutes = null) {
    if (!this.configManager.isStatusReportNotificationEnabled()) {
      console.log("⏸️ 定期的な状況報告は無効になっています（設定により）");
      return;
    }

    const reportInterval = intervalMinutes ?? this.configManager.getStatusReportInterval();
    const emptySpaceInterval = this.configManager.getEmptySpaceNotificationInterval();
    
    setInterval(
      async () => {
        const userCount = this.getCurrentUserCount();
        const now = Date.now();
        
        if (userCount === 0) {
          const timeSinceLastEmpty = now - this.lastEmptySpaceNotification;
          const emptyIntervalMs = emptySpaceInterval * 60 * 1000;
          
          if (timeSinceLastEmpty >= emptyIntervalMs) {
            await this.slackNotifier.notifyStatusReport(userCount, this.configManager);
            this.lastEmptySpaceNotification = now;
            console.log(`📊 定期報告（空室）: 参加者数 ${userCount}人`);
          } else {
            console.log(`⏸️ 空室通知スキップ（前回から${Math.round(timeSinceLastEmpty / 60000)}分経過）`);
          }
        } else {
          await this.slackNotifier.notifyStatusReport(userCount, this.configManager);
          console.log(`📊 定期報告: 参加者数 ${userCount}人`);
        }
      },
      reportInterval * 60 * 1000,
    );
  }

  healthCheck() {
    const connectionHealth = this.connectionManager.healthCheck();
    const status = {
      ...connectionHealth,
      userCount: this.getCurrentUserCount(),
    };
    console.log("💊 ヘルスチェック:", status);
    return status;
  }

  async disconnect() {
    try {
      if (this.configManager.isShutdownNotificationEnabled()) {
        await this.slackNotifier.notifyShutdown();
      } else {
        console.log("⏸️ 停止通知はスキップされました（設定により）");
      }
      await this.connectionManager.disconnect();
    } catch (error) {
      console.error("❌ 切断エラー:", error);
    }
  }
}

module.exports = GatherSlackBot;
