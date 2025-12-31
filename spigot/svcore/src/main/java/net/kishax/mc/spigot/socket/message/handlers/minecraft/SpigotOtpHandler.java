package net.kishax.mc.spigot.socket.message.handlers.minecraft;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;

import net.kishax.mc.common.database.Database;
import net.kishax.mc.common.socket.SocketSwitch;
import net.kishax.mc.common.socket.message.Message;
import net.kishax.mc.common.socket.message.handlers.interfaces.minecraft.OtpHandler;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.plugin.java.JavaPlugin;
import java.sql.Connection;

/**
 * Spigot側のOTPメッセージハンドラー
 */
public class SpigotOtpHandler implements OtpHandler {
  private final JavaPlugin plugin;
  private final BukkitAudiences audiences;
  private final Logger logger;
  private final Database db;
  private final Provider<SocketSwitch> sswProvider;

  @Inject
  public SpigotOtpHandler(JavaPlugin plugin, BukkitAudiences audiences, Logger logger, Database db,
      Provider<SocketSwitch> sswProvider) {
    this.plugin = plugin;
    this.audiences = audiences;
    this.logger = logger;
    this.db = db;
    this.sswProvider = sswProvider;
  }

  /**
   * Velocity→Spigot OTP処理
   */
  public void handle(Message.Minecraft.Otp otp) {
    logger.info("Velocity→Spigot OTP受信: {} ({}) OTP: {}", otp.mcid, otp.uuid, otp.otp);

    // TEST_トークンの場合は追加のログ出力
    boolean isTestMode = isTestToken(otp.mcid, otp.uuid);
    if (isTestMode) {
      logger.info("🧪 テストモードでのOTP処理を開始します");
    }

    // メインスレッドで実行（Bukkit API呼び出しのため）
    // runTask内でエラーハンドリングを行う
    Bukkit.getScheduler().runTask(plugin, () -> {
      try {
        Player player = Bukkit.getPlayer(otp.mcid);
        boolean success = false;
        String responseMessage;

        // テスト用プレイヤー（TestPlayer）とTEST_トークンの特別処理
        if (otp.mcid.equals("TestPlayer") && otp.uuid.equals("00000000-0000-0000-0000-000000000000")) {
          // テスト用プレイヤーの場合は常に成功として処理
          success = true;
          responseMessage = "テスト用プレイヤーのOTPを処理しました。";
          logger.info("🧪 テスト用プレイヤー {} のOTPを処理しました: {}", otp.mcid, otp.otp);

          // コンソールにOTP入力指示を表示
          logger.info("=== テスト認証フロー ===");
          logger.info("OTP: {} をWEBの方で入力してください！", otp.otp);
          logger.info("======================");

          // Bukkit コンソールにも出力
          Bukkit.getConsoleSender().sendMessage("§a=== テスト認証フロー ===");
          Bukkit.getConsoleSender().sendMessage("§eOTP: §b" + otp.otp + " §eをWEBの方で入力してください！");
          Bukkit.getConsoleSender().sendMessage("§a======================");

        } else if (player != null && player.getUniqueId().toString().equals(otp.uuid)) {
          // 通常のプレイヤーにOTPを送信
          sendOtpToPlayer(player, otp.otp);
          success = true;
          responseMessage = "OTPをプレイヤーに送信しました。";
          logger.info("プレイヤー {} にOTPを送信しました: {}", player.getName(), otp.otp);
        } else {
          String errorMessage = player == null
              ? "プレイヤーがオンラインではありません。"
              : "プレイヤーのUUIDが一致しません。";
          responseMessage = errorMessage;
          logger.warn("プレイヤーが見つからないかUUIDが一致しません: {} ({})", otp.mcid, otp.uuid);
        }

        // Velocityにレスポンス送信（必ず1回のみ）
        sendOtpResponseToVelocity(otp.mcid, otp.uuid, success, responseMessage);

      } catch (Exception e) {
        // runTask内のエラーハンドリング
        logger.error("OTP処理中にエラーが発生しました: {} ({})", otp.mcid, otp.uuid, e);
        sendOtpResponseToVelocity(otp.mcid, otp.uuid, false, "Spigot側でOTP処理エラーが発生しました。");
      }
    });
  }

  /**
   * プレイヤーにOTPを送信
   */
  private void sendOtpToPlayer(Player player, String otp) {
    try {
      // OTPをチャットで表示（Adventure Component使用）
      Component otpMessage = Component.text()
          .append(Component.text("【WEB認証】").color(NamedTextColor.GOLD))
          .appendNewline()
          .append(Component.text("ワンタイムパスワード: ").color(NamedTextColor.WHITE))
          .append(Component.text(otp)
              .color(NamedTextColor.BLUE)
              .decorate(TextDecoration.UNDERLINED)
              .clickEvent(ClickEvent.copyToClipboard(otp))
              .hoverEvent(HoverEvent.showText(Component.text("クリックしてコピー"))))
          .appendNewline()
          .append(Component.text("このコードをWEB認証ページで入力してください。").color(NamedTextColor.GRAY))
          .build();

      // Adventure APIでメッセージ送信
      audiences.player(player).sendMessage(otpMessage);

    } catch (Exception e) {
      logger.error("プレイヤーへのOTP送信でエラーが発生しました: {}", player.getName(), e);
      // fallback: 簡単なメッセージ
      player.sendMessage("§6【WEB認証】");
      player.sendMessage("§fワンタイムパスワード: §9§n" + otp);
      player.sendMessage("§7このコードをWEB認証ページで入力してください。");
    }
  }

  /**
   * Velocityにレスポンス送信
   */
  private void sendOtpResponseToVelocity(String mcid, String uuid, boolean success, String responseMessage) {
    try {
      Message msg = new Message();
      msg.mc = new Message.Minecraft();
      msg.mc.otp = new Message.Minecraft.Otp();
      msg.mc.otp.mcid = mcid;
      msg.mc.otp.uuid = uuid;
      msg.mc.otp.otp = success ? "SUCCESS" : "ERROR";
      msg.mc.otp.action = success ? "otp_response_success" : "otp_response_error";

      // レスポンスメッセージを追加のフィールドで送信（後で追加予定）

      try (Connection conn = db.getConnection()) {
        SocketSwitch ssw = sswProvider.get();
        ssw.sendVelocityServer(conn, msg);
        logger.info("VelocityにOTPレスポンスを送信しました: {} ({}), success={}", mcid, uuid, success);
      }

    } catch (Exception e) {
      logger.error("VelocityへのOTPレスポンス送信でエラーが発生しました: {} ({})", mcid, uuid, e);
    }
  }

  /**
   * テスト用トークンかどうかを判定
   */
  private boolean isTestToken(String mcid, String uuid) {
    return mcid.equals("TestPlayer") && uuid.equals("00000000-0000-0000-0000-000000000000");
  }
}
