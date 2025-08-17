const GatherSlackBot = require("./src/GatherSlackBot");

// メイン実行関数
async function main() {
  console.log("🚀 Gather Slack Bot 起動中...");
  console.log(`📅 起動時刻: ${new Date().toLocaleString("ja-JP")}`);

  const bot = new GatherSlackBot();

  // 正常終了時の処理
  const gracefulShutdown = async (signal) => {
    console.log(`\n🛑 ${signal} 受信: Bot停止中...`);
    await bot.disconnect();
    process.exit(0);
  };

  process.on("SIGINT", () => gracefulShutdown("SIGINT"));
  process.on("SIGTERM", () => gracefulShutdown("SIGTERM"));

  // 未処理エラーのハンドリング
  process.on("unhandledRejection", (reason, promise) => {
    console.error("未処理のPromise拒否:", reason);
    bot.slackNotifier?.notifyUnhandledError(reason);
  });

  process.on("uncaughtException", (error) => {
    console.error("未処理の例外:", error);
    bot.slackNotifier?.notifyCriticalError(error.message);
    process.exit(1);
  });

  try {
    // Bot接続開始
    await bot.connect();

    // 設定に基づいてステータス報告を開始
    bot.startStatusReporting();

    // 30分ごとにヘルスチェック
    setInterval(
      () => {
        bot.healthCheck();
      },
      30 * 60 * 1000,
    );

    console.log("🚀 Gather Slack Bot が正常に起動しました！");
    console.log("💡 停止するには: Ctrl+C");
  } catch (error) {
    console.error("❌ Bot起動エラー:", error);
    await bot.slackNotifier?.notifyStartupError(error.message);
    process.exit(1);
  }
}

// メイン関数実行
if (require.main === module) {
  main().catch((error) => {
    console.error("❌ メイン関数エラー:", error);
    process.exit(1);
  });
}

module.exports = { GatherSlackBot };
