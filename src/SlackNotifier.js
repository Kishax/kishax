const axios = require("axios");

class SlackNotifier {
	constructor(webhookUrl) {
		this.webhookUrl = webhookUrl;
		
		if (!this.webhookUrl) {
			throw new Error("Slack Webhook URLが設定されていません");
		}
	}

	async sendNotification(message, color = "#36a64f") {
		try {
			const payload = {
				username: "Gather Bot",
				icon_emoji: ":office:",
				attachments: [
					{
						color: color,
						text: message,
						mrkdwn_in: ["text"],
						footer: "Gather Town",
						ts: Math.floor(Date.now() / 1000),
					},
				],
			};

			const response = await axios.post(this.webhookUrl, payload, {
				timeout: 10000,
				headers: {
					"Content-Type": "application/json",
				},
			});

			console.log(`✅ Slack通知送信成功: ${message}`);
			return response.status === 200;
		} catch (error) {
			console.error("❌ Slack通知送信エラー:", error.message);
			return false;
		}
	}

	async notifyUserJoined(playerName) {
		const message = `🎉 *${playerName}* さんがGatherスペースに参加しました！`;
		return await this.sendNotification(message, "#36a64f");
	}

	async notifyUserLeft(playerName) {
		const message = `👋 *${playerName}* さんがGatherスペースから退出しました`;
		console.log(`🔄 退出通知準備: ${message}`);

		try {
			const success = await this.sendNotification(message, "#ff9900");
			if (success) {
				console.log(`✅ 退出通知送信成功: ${playerName}`);
			} else {
				console.log(`❌ 退出通知送信失敗: ${playerName}`);
			}
			return success;
		} catch (error) {
			console.error(`❌ 退出通知エラー: ${error.message}`);
			return false;
		}
	}

	async notifyMemberList(members) {
		if (members.length > 0) {
			const memberList = members.join(", ");
			const message = `📋 **現在のGatherメンバー** (${members.length}人)\n${memberList}`;
			return await this.sendNotification(message, "#36a64f");
		} else {
			return await this.sendNotification("📋 現在Gatherスペースには誰もいません", "#808080");
		}
	}

	async notifyStartup() {
		return await this.sendNotification("🤖 Gather Bot が起動しました！監視を開始します", "#0099ff");
	}

	async notifyShutdown() {
		return await this.sendNotification("🤖 Gather Bot を停止します", "#ff0000");
	}

	async notifyError(errorMessage) {
		return await this.sendNotification(`! Gather Bot でエラーが発生: ${errorMessage}`, "#ff0000");
	}

	async notifyStartupError(errorMessage) {
		return await this.sendNotification(`! Bot起動失敗: ${errorMessage}`, "#ff0000");
	}

	async notifyUnhandledError(reason) {
		return await this.sendNotification(`! 未処理エラー: ${reason}`, "#ff0000");
	}

	async notifyCriticalError(errorMessage) {
		return await this.sendNotification(`! 重大エラー: ${errorMessage}`, "#ff0000");
	}

	async notifyStatusReport(userCount) {
		const message = `📊 現在のGatherスペース参加者数: ${userCount}人`;
		return await this.sendNotification(message, "#808080");
	}
}

module.exports = SlackNotifier;