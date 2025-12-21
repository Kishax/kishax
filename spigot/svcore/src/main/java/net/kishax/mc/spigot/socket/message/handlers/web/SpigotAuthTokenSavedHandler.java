package net.kishax.mc.spigot.socket.message.handlers.web;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.slf4j.Logger;

import com.google.inject.Inject;

import net.kishax.mc.common.settings.Settings;
import net.kishax.mc.common.socket.message.Message;
import net.kishax.mc.common.socket.message.handlers.interfaces.web.AuthTokenSavedHandler;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Spigot側の認証トークン保存完了通知ハンドラー
 * Web側でDBに保存完了後、プレイヤーに認証URLを表示する
 */
public class SpigotAuthTokenSavedHandler implements AuthTokenSavedHandler {
  private final JavaPlugin plugin;
  private final BukkitAudiences audiences;
  private final Logger logger;

  @Inject
  public SpigotAuthTokenSavedHandler(JavaPlugin plugin, BukkitAudiences audiences, Logger logger) {
    this.plugin = plugin;
    this.audiences = audiences;
    this.logger = logger;
  }

  @Override
  public void handle(Message.Web.AuthTokenSaved authTokenSaved) {
    try {
      String playerName = authTokenSaved.who.name;
      String playerUuid = authTokenSaved.who.uuid;
      String authToken = authTokenSaved.token;

      logger.info("✅ Auth token saved notification received: {} ({}) Token: {}", 
          playerName, playerUuid, authToken);

      // メインスレッドで実行（Bukkit API呼び出しのため）
      Bukkit.getScheduler().runTask(plugin, () -> {
        Player player = Bukkit.getPlayer(playerName);

        if (player != null && player.getUniqueId().toString().equals(playerUuid)) {
          // 認証URL生成
          String confirmUrl = Settings.CONFIRM_URL.getValue() + "?t=" + authToken;
          
          // プレイヤーに認証URLを表示
          sendAuthUrlToPlayer(player, confirmUrl);
          
          logger.info("📤 Auth URL sent to player: {} - {}", playerName, confirmUrl);
        } else {
          if (player == null) {
            logger.warn("⚠️ Player is not online: {}", playerName);
          } else {
            logger.warn("⚠️ Player UUID mismatch: expected {}, got {}", 
                playerUuid, player.getUniqueId().toString());
          }
        }
      });

    } catch (Exception e) {
      logger.error("❌ Error handling auth token saved notification: {}", e.getMessage(), e);
    }
  }

  /**
   * プレイヤーに認証URLを送信
   */
  private void sendAuthUrlToPlayer(Player player, String confirmUrl) {
    try {
      Component welcomeMessage = Component.text("Kishaxサーバーへようこそ！")
          .color(NamedTextColor.GREEN)
          .appendNewline();

      Component introMessage = Component.text("サーバーに参加するには、KishaxアカウントとMinecraftアカウントをリンクさせる必要があります。")
          .color(NamedTextColor.WHITE)
          .appendNewline()
          .appendNewline();

      Component webAuth = Component.text("WEB認証")
          .color(NamedTextColor.GOLD)
          .decorate(TextDecoration.BOLD, TextDecoration.UNDERLINED)
          .clickEvent(ClickEvent.openUrl(confirmUrl))
          .hoverEvent(HoverEvent.showText(Component.text("クリックしてWEB認証ページを開く")));

      Component authInstruction = Component.text("より、手続きを進めてください！")
          .color(NamedTextColor.WHITE)
          .appendNewline()
          .appendNewline();

      Component accessMethodTitle = Component.text("[アクセス方法]")
          .color(NamedTextColor.GOLD)
          .decorate(TextDecoration.BOLD, TextDecoration.UNDERLINED)
          .appendNewline();

      Component javaUserInstruction = Component.text("Java版ユーザーは、")
          .color(NamedTextColor.WHITE)
          .append(Component.text("ココ")
              .color(NamedTextColor.GOLD)
              .decorate(TextDecoration.UNDERLINED)
              .clickEvent(ClickEvent.openUrl(confirmUrl))
              .hoverEvent(HoverEvent.showText(Component.text("クリックしてWEB認証ページを開く"))))
          .append(Component.text("をクリックしてアクセスしてください！"))
          .appendNewline();

      Component bedrockUserInstruction = Component.text("統合版ユーザーは、配布されたQRコードを読み取ってアクセスしてください！")
          .color(NamedTextColor.WHITE)
          .appendNewline()
          .appendNewline();

      Component finalMessage = Component.text("それでは、楽しいマイクラライフを！")
          .color(NamedTextColor.GREEN);

      // すべてのメッセージを結合して送信
      Component fullMessage = Component.empty()
          .append(welcomeMessage)
          .append(introMessage)
          .append(webAuth)
          .append(authInstruction)
          .append(accessMethodTitle)
          .append(javaUserInstruction)
          .append(bedrockUserInstruction)
          .append(finalMessage);

      audiences.player(player).sendMessage(fullMessage);

    } catch (Exception e) {
      logger.error("Error sending auth URL to player: {}", player.getName(), e);
      // Fallback: シンプルなメッセージ
      player.sendMessage("§a認証URLが準備できました！");
      player.sendMessage("§e" + confirmUrl);
    }
  }
}
