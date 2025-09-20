package net.kishax.mc.velocity.socket.message.handlers.web;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import net.kishax.mc.common.database.Database;
import net.kishax.mc.common.server.Luckperms;
import net.kishax.mc.common.settings.PermSettings;
import net.kishax.mc.common.socket.SocketSwitch;
import net.kishax.mc.common.socket.message.Message;
import net.kishax.mc.common.socket.message.handlers.interfaces.web.MinecraftWebConfirmHandler;
import net.kishax.mc.velocity.util.config.VelocityConfig;
import net.kishax.mc.common.socket.SqsClient;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import com.fasterxml.jackson.databind.JsonNode;

public class VelocityMinecraftWebConfirmHandler implements MinecraftWebConfirmHandler {
  private final Logger logger;
  private final ProxyServer server;
  private final Database db;
  private final VelocityConfig config;
  private final Luckperms lp;
  private final Provider<SocketSwitch> sswProvider;
  private final SqsClient sqsClient;

  @Inject
  public VelocityMinecraftWebConfirmHandler(Logger logger, ProxyServer server, Database db, VelocityConfig config,
      Luckperms lp, Provider<SocketSwitch> sswProvider, SqsClient sqsClient) {
    this.logger = logger;
    this.server = server;
    this.db = db;
    this.config = config;
    this.lp = lp;
    this.sswProvider = sswProvider;
    this.sqsClient = sqsClient;
  }

  @Override
  public void handle(Message.Web.MinecraftConfirm confirm) {
    String mineName = confirm.who.name;
    lp.addPermission(mineName, PermSettings.NEW_USER.get());

    Message msg = new Message();
    msg.mc = new Message.Minecraft();
    msg.mc.sync = new Message.Minecraft.Sync();
    msg.mc.sync.content = "MEMBER";

    // spigotsに通知し、serverCacheを更新させる
    try (Connection conn = db.getConnection()) {
      SocketSwitch ssw = sswProvider.get();
      ssw.sendSpigotServer(conn, msg);
    } catch (SQLException | ClassNotFoundException e) {
      logger.error("An error occurred while updating the database: " + e.getMessage(), e);
      for (StackTraceElement element : e.getStackTrace()) {
        logger.error(element.toString());
      }
    }
    Optional<Player> playerOptional = server.getAllPlayers().stream()
        .filter(player -> player.getUsername().equalsIgnoreCase(mineName))
        .findFirst();
    if (playerOptional.isPresent()) {
      Player player = playerOptional.get();
      TextComponent component;
      String DiscordInviteUrl = config.getString("Discord.InviteUrl", "");
      if (!DiscordInviteUrl.isEmpty()) {
        component = Component.text()
            .appendNewline()
            .append(Component.text("WEB認証")
                .color(NamedTextColor.LIGHT_PURPLE)
                .decorate(
                    TextDecoration.BOLD,
                    TextDecoration.UNDERLINED))
            .append(Component.text("が完了しました。")
                .color(NamedTextColor.AQUA))
            .appendNewline()
            .append(Component.text("池に飛び込もう！")
                .color(NamedTextColor.AQUA))
            .appendNewline()
            .appendNewline()
            .append(Component.text("Kishaxサーバーの")
                .color(NamedTextColor.AQUA))
            .append(Component.text("Discord")
                .color(NamedTextColor.BLUE)
                .decorate(
                    TextDecoration.BOLD,
                    TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.openUrl(DiscordInviteUrl))
                .hoverEvent(HoverEvent.showText(Component.text("KishaxサーバーのDiscordへいこう！"))))
            .append(Component.text("には参加しましたか？")
                .color(NamedTextColor.AQUA))
            .appendNewline()
            .append(Component.text("ここでは、個性豊かな色々なメンバーと交流ができます！")
                .color(NamedTextColor.AQUA))
            .appendNewline()
            .append(Component.text("ぜひ、参加してみてください！")
                .color(NamedTextColor.AQUA))
            .build();
        player.sendMessage(component);
      }
    }

    // Discord通知送信
    try {
      net.kishax.api.bridge.RedisClient redisClient = net.kishax.mc.velocity.Main.getKishaxRedisClient();
      if (redisClient != null) {
        net.kishax.api.bridge.DiscordMessageHandler discordHandler = new net.kishax.api.bridge.DiscordMessageHandler(
            redisClient);
        discordHandler.sendEmbedMessage(mineName + "が新規メンバーになりました！:congratulations:", 0xFFC0CB, "");
        logger.info("✅ Discord新規メンバー通知をRedis経由で送信しました: {}", mineName);
      } else {
        logger.warn("⚠️ RedisClient not available, Discord message not sent");
      }
    } catch (Exception e) {
      logger.error("❌ Discord新規メンバー通知送信でエラーが発生しました: {}", e.getMessage());
      for (StackTraceElement ste : e.getStackTrace()) {
        logger.error(ste.toString());
      }
    }

    // Web側に認証完了レスポンス送信
    sendAuthResponseToWeb(confirm.who.name, confirm.who.uuid, true, "WEB認証が完了しました。");
  }

  /**
   * Web側に認証レスポンスを送信
   */
  private void sendAuthResponseToWeb(String playerName, String playerUuid, boolean success, String message) {
    if (sqsClient == null) {
      logger.warn("SQSクライアントが利用できません。認証レスポンスを送信できません。");
      return;
    }

    try {
      sqsClient.sendAuthResponse(playerName, playerUuid, success, message)
          .thenRun(() -> logger.info("認証レスポンスを送信しました: {} ({}), success={}", playerName, playerUuid, success))
          .exceptionally(ex -> {
            logger.error("認証レスポンス送信に失敗しました: {} ({})", playerName, playerUuid, ex);
            return null;
          });
    } catch (Exception e) {
      logger.error("認証レスポンス送信でエラーが発生しました: {} ({})", playerName, playerUuid, e);
    }
  }

  /**
   * SQS経由でのWeb→MC認証確認メッセージを処理
   */
  public void handleWebToMinecraft(JsonNode confirmData) {
    try {
      String playerName = confirmData.path("who").path("name").asText();
      String playerUuid = confirmData.path("who").path("uuid").asText();

      if (playerName.isEmpty() || playerUuid.isEmpty()) {
        logger.warn("SQS認証確認メッセージに必要な情報が不足しています: {}", confirmData);
        return;
      }

      logger.info("SQS経由での認証確認処理を開始: {} ({})", playerName, playerUuid);

      // 直接認証処理を実行（既存のhandleロジックを簡略化）
      lp.addPermission(playerName, PermSettings.NEW_USER.get());

      // Web側に認証完了レスポンス送信
      sendAuthResponseToWeb(playerName, playerUuid, true, "WEB認証が完了しました。");

      // Discord通知送信
      try {
        net.kishax.api.bridge.RedisClient redisClient = net.kishax.mc.velocity.Main.getKishaxRedisClient();
        if (redisClient != null) {
          net.kishax.api.bridge.DiscordMessageHandler discordHandler = new net.kishax.api.bridge.DiscordMessageHandler(
              redisClient);
          discordHandler.sendEmbedMessage(playerName + "が新規メンバーになりました！:congratulations:", 0xFFC0CB, "");
          logger.info("✅ Discord新規メンバー通知をRedis経由で送信しました: {}", playerName);
        } else {
          logger.warn("⚠️ RedisClient not available, Discord message not sent");
        }
      } catch (Exception e) {
        logger.error("❌ Discord通知送信エラー", e);
      }

      logger.info("SQS経由での認証確認処理が完了しました: {} ({})", playerName, playerUuid);
    } catch (Exception e) {
      logger.error("SQS認証確認メッセージ処理でエラーが発生しました", e);
    }
  }
}
