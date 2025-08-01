class UserManager {
  constructor() {
    this.connectedUsers = new Set();
    this.userNameCache = new Map();
    this.processedJoinEvents = new Set();
  }

  async getUserName(
    playerId,
    game,
    data = null,
    context = null,
    isRetry = false,
  ) {
    if (this.userNameCache.has(playerId)) {
      const cachedName = this.userNameCache.get(playerId);
      console.log(
        `🔍 キャッシュからユーザー名取得: ${cachedName} (ID: ${playerId})`,
      );
      return { name: cachedName, isDelayed: false };
    }

    let playerName = null;
    console.log(`🔍 ユーザー名取得開始 (ID: ${playerId})`);

    try {
      const player = game.getPlayer(playerId);
      console.log(`📊 getPlayer結果:`, player);
      if (player && player.name && player.name.trim() !== "") {
        playerName = player.name.trim();
        console.log(`✅ getPlayerから取得: ${playerName}`);
      }
    } catch (error) {
      console.log("i getPlayer失敗:", error.message);
    }

    if (!playerName && game.players) {
      console.log(`📊 game.players:`, Object.keys(game.players));
      if (game.players[playerId]) {
        const playerData = game.players[playerId];
        console.log(`📊 playerData:`, playerData);
        if (playerData.name && playerData.name.trim() !== "") {
          playerName = playerData.name.trim();
          console.log(`✅ game.playersから取得: ${playerName}`);
        }
      }
    }

    if (!playerName && context && context.player) {
      console.log(`📊 context.player:`, context.player);
      if (context.player.name && context.player.name.trim() !== "") {
        playerName = context.player.name.trim();
        console.log(`✅ context.playerから取得: ${playerName}`);
      }
    }

    if (!playerName && data) {
      console.log(`📊 eventData:`, data);
      if (data.name && data.name.trim() !== "") {
        playerName = data.name.trim();
        console.log(`✅ eventDataから取得: ${playerName}`);
      }
    }

    if (!playerName && context) {
      console.log(`📊 context:`, context);
      if (context.name && context.name.trim() !== "") {
        playerName = context.name.trim();
        console.log(`✅ contextから取得: ${playerName}`);
      }
    }

    if (!playerName && game.players) {
      console.log("🔍 全プレイヤーから検索...");
      for (const [pid, pdata] of Object.entries(game.players)) {
        if (pid === playerId && pdata.name && pdata.name.trim() !== "") {
          playerName = pdata.name.trim();
          console.log(`✅ 全プレイヤー検索で発見: ${playerName}`);
          break;
        }
      }
    }

    if (playerName) {
      console.log(`✅ 即座に取得した名前: ${playerName} (ID: ${playerId})`);
      this.userNameCache.set(playerId, playerName);
      return { name: playerName, isDelayed: false };
    }

    if (!isRetry) {
      console.log("⏳ 遅延取得を試行...");

      return new Promise((resolve) => {
        setTimeout(async () => {
          try {
            const delayedPlayer = game.getPlayer(playerId);
            if (
              delayedPlayer &&
              delayedPlayer.name &&
              delayedPlayer.name.trim() !== ""
            ) {
              const delayedName = delayedPlayer.name.trim();
              this.userNameCache.set(playerId, delayedName);
              console.log(`✅ 遅延取得成功: ${delayedName} (ID: ${playerId})`);
              resolve({ name: delayedName, isDelayed: true });
            } else {
              console.log(
                `! 遅延取得でも名前が見つかりません (ID: ${playerId})`,
              );
              const fallbackName = "新しいユーザー";
              this.userNameCache.set(playerId, fallbackName);
              resolve({ name: fallbackName, isDelayed: true });
            }
          } catch (error) {
            console.log("i 遅延取得失敗:", error.message);
            const fallbackName = "新しいユーザー";
            this.userNameCache.set(playerId, fallbackName);
            resolve({ name: fallbackName, isDelayed: true });
          }
        }, 1500);
      });
    }

    const fallbackName = "新しいユーザー";
    console.log(
      `! ユーザー名取得失敗、デフォルト値を使用: ${fallbackName} (ID: ${playerId})`,
    );
    this.userNameCache.set(playerId, fallbackName);
    return { name: fallbackName, isDelayed: false };
  }

  addUser(playerId, playerName = null) {
    this.connectedUsers.add(playerId);
    if (playerName) {
      this.userNameCache.set(playerId, playerName);
    }
  }

  removeUser(playerId) {
    this.connectedUsers.delete(playerId);
  }

  hasUser(playerId) {
    return this.connectedUsers.has(playerId);
  }

  getUserCount() {
    return this.connectedUsers.size;
  }

  getAllUserIds() {
    return Array.from(this.connectedUsers);
  }

  getCachedUserName(playerId) {
    return this.userNameCache.get(playerId) || "新しいユーザー";
  }

  isRecentJoinEvent(playerId, timeWindowMs = 5000) {
    const recentEventKey = Array.from(this.processedJoinEvents).find(
      (key) =>
        key.startsWith(playerId) &&
        Date.now() - parseInt(key.split("-")[1]) < timeWindowMs,
    );
    return !!recentEventKey;
  }

  addJoinEvent(playerId) {
    const eventKey = `${playerId}-${Date.now()}`;
    this.processedJoinEvents.add(eventKey);

    // 古いイベント（10分以上）をクリーンアップ
    for (const key of this.processedJoinEvents) {
      if (Date.now() - parseInt(key.split("-")[1]) > 600000) {
        this.processedJoinEvents.delete(key);
      }
    }

    return eventKey;
  }

  getConnectedUsersList() {
    return Array.from(this.connectedUsers).map((playerId) => ({
      id: playerId,
      name: this.getCachedUserName(playerId),
    }));
  }

  async loadInitialUsers(game) {
    try {
      console.log("📋 初期メンバーリストを取得中...");

      await new Promise((resolve) => setTimeout(resolve, 2000));

      const players = game.players || {};
      const currentMembers = [];

      for (const [playerId, playerData] of Object.entries(players)) {
        if (playerData && playerData.name) {
          this.addUser(playerId, playerData.name);
          currentMembers.push(playerData.name);
          console.log(`📝 初期メンバー: ${playerData.name} (ID: ${playerId})`);
        }
      }

      console.log(`✅ 初期メンバーリスト取得完了: ${currentMembers.length}人`);
      return currentMembers;
    } catch (error) {
      console.error("❌ 初期メンバーリスト取得エラー:", error);
      return [];
    }
  }

  checkForChanges(currentPlayers) {
    const currentPlayerSet = new Set(currentPlayers);
    const trackedUsers = new Set(this.connectedUsers);

    const leftUsers = [];
    const joinedUsers = [];

    // 退出したユーザーを検出
    for (const userId of trackedUsers) {
      if (!currentPlayerSet.has(userId)) {
        leftUsers.push({
          id: userId,
          name: this.getCachedUserName(userId),
        });
      }
    }

    // 参加したユーザーを検出
    for (const userId of currentPlayerSet) {
      if (!trackedUsers.has(userId)) {
        joinedUsers.push({
          id: userId,
          name: this.getCachedUserName(userId),
        });
      }
    }

    return { leftUsers, joinedUsers };
  }
}

module.exports = UserManager;
