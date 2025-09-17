package net.kishax.mc.velocity.socket.message.handlers.minecraft;

import com.google.inject.Inject;

import net.kishax.mc.common.socket.message.Message;
import net.kishax.mc.common.socket.message.handlers.interfaces.minecraft.OtpHandler;

import org.slf4j.Logger;

/**
 * Velocity側のOTPメッセージハンドラー
 * SpigotからのOTPレスポンスを受信してWeb側にSQS送信
 */
public class VelocityOtpHandler implements OtpHandler {
  private final Logger logger;

  @Inject
  public VelocityOtpHandler(Logger logger) {
    this.logger = logger;
  }

  /**
   * SpigotからのOTPレスポンス処理
   */
  @Override
  public void handle(Message.Minecraft.Otp otp) {
    try {
      logger.info("Spigot→Velocity OTPレスポンス受信: {} ({}) status: {}", otp.mcid, otp.uuid, otp.action);

      boolean success = "otp_response_success".equals(otp.action);
      String responseMessage;

      if (success) {
        responseMessage = "プレイヤーにOTPを送信しました。";
      } else {
        responseMessage = "OTP送信に失敗しました。プレイヤーがオンラインではない可能性があります。";
      }

      // Web側にSQSレスポンス送信（kishax-aws経由）
      net.kishax.mc.velocity.Main.sendOtpResponseToWeb(otp.mcid, otp.uuid, success, responseMessage);

    } catch (Exception e) {
      logger.error("OTPレスポンス処理でエラーが発生しました: {} ({})", otp.mcid, otp.uuid, e);

      // エラー時もWeb側に通知（kishax-aws経由）
      try {
        net.kishax.mc.velocity.Main.sendOtpResponseToWeb(otp.mcid, otp.uuid, false, "Velocity側でOTP処理エラーが発生しました。");
      } catch (Exception sqsError) {
        logger.error("エラーレスポンス送信に失敗しました: {} ({})", otp.mcid, otp.uuid, sqsError);
      }
    }
  }
}
