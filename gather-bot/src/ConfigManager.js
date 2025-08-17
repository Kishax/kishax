const fs = require("fs");
const path = require("path");

class ConfigManager {
  constructor() {
    this.config = null;
    this.loadConfig();
  }

  loadConfig() {
    const configPath = path.join(process.cwd(), "config.jsonc");
    const defaultConfig = {
      notifications: {
        silentNotificationNobody: false,
        enableStartupNotification: true,
        enableInitialMemberListNotification: true,
        enableStatusReportNotification: true,
        enableShutdownNotification: true,
        enableJoinNotification: true,
        enableLeaveNotification: true,
        statusReportIntervalMinutes: 60,
        emptySpaceNotificationIntervalMinutes: 60,
      },
    };

    try {
      if (fs.existsSync(configPath)) {
        const configContent = fs.readFileSync(configPath, "utf8");
        const cleanedContent = this.removeComments(configContent);
        this.config = { ...defaultConfig, ...JSON.parse(cleanedContent) };
        console.log("✅ config.jsonc を読み込みました");
      } else {
        this.config = defaultConfig;
        console.log("⚠️ config.jsonc が見つかりません。デフォルト設定を使用します");
      }
    } catch (error) {
      console.error("❌ config.jsonc の読み込みエラー:", error.message);
      console.log("🔄 デフォルト設定を使用します");
      this.config = defaultConfig;
    }
  }

  removeComments(content) {
    return content
      .split("\n")
      .map((line) => {
        const commentIndex = line.indexOf("//");
        if (commentIndex !== -1) {
          return line.substring(0, commentIndex);
        }
        return line;
      })
      .join("\n");
  }

  get(key) {
    const keys = key.split(".");
    let value = this.config;
    
    for (const k of keys) {
      if (value && typeof value === "object" && k in value) {
        value = value[k];
      } else {
        return undefined;
      }
    }
    
    return value;
  }

  isSilentNotificationNobody() {
    return this.get("notifications.silentNotificationNobody") ?? false;
  }

  isStartupNotificationEnabled() {
    return this.get("notifications.enableStartupNotification") ?? true;
  }

  isInitialMemberListNotificationEnabled() {
    return this.get("notifications.enableInitialMemberListNotification") ?? true;
  }

  getStatusReportInterval() {
    return this.get("notifications.statusReportIntervalMinutes") ?? 60;
  }

  getEmptySpaceNotificationInterval() {
    return this.get("notifications.emptySpaceNotificationIntervalMinutes") ?? 60;
  }

  isStatusReportNotificationEnabled() {
    return this.get("notifications.enableStatusReportNotification") ?? true;
  }

  isShutdownNotificationEnabled() {
    return this.get("notifications.enableShutdownNotification") ?? true;
  }

  isJoinNotificationEnabled() {
    return this.get("notifications.enableJoinNotification") ?? true;
  }

  isLeaveNotificationEnabled() {
    return this.get("notifications.enableLeaveNotification") ?? true;
  }
}

module.exports = ConfigManager;