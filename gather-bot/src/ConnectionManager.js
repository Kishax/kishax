const { Game } = require("@gathertown/gather-game-client");

class ConnectionManager {
  constructor(spaceId, apiKey) {
    this.spaceId = spaceId;
    this.apiKey = apiKey;
    this.game = null;
    this.isConnecting = false;
    this.reconnectTimeout = null;
    this.connectionCheckInterval = null;

    if (!this.spaceId || !this.apiKey) {
      throw new Error("Gather Space IDまたはAPI Keyが設定されていません");
    }
  }

  async connect() {
    if (this.isConnecting) {
      console.log("! 既に接続試行中です");
      return false;
    }

    try {
      this.isConnecting = true;
      console.log("🔄 Gatherに接続中...");

      this.game = new Game(this.spaceId, () =>
        Promise.resolve({ apiKey: this.apiKey }),
      );

      await this.game.connect();
      console.log("🚀 Gather接続プロセス開始");
      return true;
    } catch (error) {
      console.error("❌ 接続エラー:", error);
      this.isConnecting = false;
      return false;
    }
  }

  subscribeToConnection(callback) {
    if (!this.game) {
      throw new Error("Gameオブジェクトが初期化されていません");
    }

    this.game.subscribeToConnection((connected) => {
      if (connected) {
        console.log("✅ Gatherに接続しました");
        this.isConnecting = false;
      } else {
        console.log("❌ Gatherから切断されました");
      }
      callback(connected);
    });
  }

  subscribeToEvent(eventName, callback) {
    if (!this.game) {
      throw new Error("Gameオブジェクトが初期化されていません");
    }
    this.game.subscribeToEvent(eventName, callback);
  }

  getGame() {
    return this.game;
  }

  isConnected() {
    return this.game ? this.game.isConnected : false;
  }

  scheduleReconnect(callback) {
    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
    }

    if (this.isConnecting) {
      console.log("! 既に接続試行中のため再接続をスキップ");
      return;
    }

    console.log("🔄 5秒後に再接続を試行します...");
    this.reconnectTimeout = setTimeout(() => {
      callback();
    }, 5000);
  }

  startConnectionMonitoring(userManager, slackNotifier, checkInterval = 30000) {
    if (this.connectionCheckInterval) {
      clearInterval(this.connectionCheckInterval);
    }

    console.log("🔍 定期接続状態チェックを開始（30秒間隔）");
    this.connectionCheckInterval = setInterval(async () => {
      await this.checkConnectionStatus(userManager, slackNotifier);
    }, checkInterval);
  }

  async checkConnectionStatus(userManager, slackNotifier) {
    try {
      console.log("🔍 定期接続状態チェック実行中...");

      if (!this.game || !this.game.players) {
        console.log("! ゲームオブジェクトまたはプレイヤー情報が利用できません");
        return;
      }

      const currentPlayers = Object.keys(this.game.players);
      const changes = userManager.checkForChanges(currentPlayers);

      console.log(`📊 現在のプレイヤー: [${currentPlayers.join(", ")}]`);
      console.log(
        `📊 追跡中のユーザー: [${userManager.getAllUserIds().join(", ")}]`,
      );

      // 退出したユーザーを処理
      for (const user of changes.leftUsers) {
        console.log(`🔍 退出を検出: ${user.id}`);
        userManager.removeUser(user.id);

        console.log(
          `👤 ユーザー退出確定（定期チェック）: ${user.name} (ID: ${user.id})`,
        );
        console.log(`🔄 Slack退出通知を送信中: ${user.name}`);

        await slackNotifier.notifyUserLeft(user.name);
        console.log(`✅ 退出通知送信完了: ${user.name}`);
      }

      // 参加したユーザーを処理
      for (const user of changes.joinedUsers) {
        if (!userManager.isRecentJoinEvent(user.id, 60000)) {
          console.log(`🔍 新規参加を検出（定期チェック）: ${user.id}`);

          userManager.addJoinEvent(user.id);

          // プレイヤー情報から名前を取得
          const playerData = this.game.players[user.id];
          let userName = "新しいユーザー";
          if (playerData && playerData.name && playerData.name.trim() !== "") {
            userName = playerData.name.trim();
          }

          userManager.addUser(user.id, userName);

          console.log(
            `👤 ユーザー参加確定（定期チェック）: ${userName} (ID: ${user.id})`,
          );
          console.log(`🔄 Slack参加通知を送信中: ${userName}`);

          await slackNotifier.notifyUserJoined(userName);
          console.log(`✅ 参加通知送信完了: ${userName}`);
        } else {
          console.log(`! 最近処理済みのため参加通知スキップ: ${user.id}`);
        }
      }

      console.log(
        `✅ 定期チェック完了 - 現在の追跡ユーザー数: ${userManager.getUserCount()}`,
      );
    } catch (error) {
      console.error("❌ 接続状態チェックエラー:", error);
    }
  }

  async disconnect() {
    try {
      // 定期チェックを停止
      if (this.connectionCheckInterval) {
        clearInterval(this.connectionCheckInterval);
        console.log("🛑 定期接続チェックを停止しました");
      }

      // 再接続タイマーを停止
      if (this.reconnectTimeout) {
        clearTimeout(this.reconnectTimeout);
        console.log("🛑 再接続タイマーを停止しました");
      }

      if (this.game) {
        this.game.disconnect();
        console.log("✅ Gatherから切断しました");
      }
    } catch (error) {
      console.error("❌ 切断エラー:", error);
    }
  }

  healthCheck() {
    return {
      isConnected: this.isConnected(),
      timestamp: new Date().toISOString(),
    };
  }
}

module.exports = ConnectionManager;
