package net.kishax.mc.spigot.server.cmd.sub;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;

import net.kishax.mc.common.database.Database;
import net.kishax.mc.common.server.Luckperms;
import net.kishax.mc.common.server.interfaces.ServerHomeDir;
import net.kishax.mc.common.settings.Settings;
import net.kishax.mc.common.socket.SocketSwitch;
import net.kishax.mc.common.socket.message.Message;
import java.security.SecureRandom;
import net.kishax.mc.spigot.server.ImageMap;
import net.kishax.mc.spigot.server.textcomponent.TCUtils;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class Confirm {
  public static final Set<Player> confirmMap = new HashSet<>();
  private final BukkitAudiences audiences;
  private final Logger logger;
  private final Database db;
  private final Luckperms lp;
  private final ImageMap im;
  private final String thisServerName;
  private final Provider<SocketSwitch> sswProvider;

  @Inject
  public Confirm(BukkitAudiences audiences, Logger logger, Database db, Luckperms lp, ImageMap im,
      ServerHomeDir shd, Provider<SocketSwitch> sswProvider) {
    this.audiences = audiences;
    this.logger = logger;
    this.db = db;
    this.lp = lp;
    this.im = im;
    this.thisServerName = shd.getServerName();
    this.sswProvider = sswProvider;
  }

  public void execute(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
    if (sender instanceof Player player) {
      if (!Confirm.confirmMap.contains(player)) {
        String playerName = player.getName(),
            playerUUID = player.getUniqueId().toString();
        int permLevel = lp.getPermLevel(playerName);
        if (permLevel < 1) {
          try (Connection conn = db.getConnection()) {
            int ifMapId = checkExistConfirmMap(conn, new Object[] { thisServerName, true, playerName });
            Map<String, Object> memberMap = db.getMemberMap(conn, player.getName());
            if (!memberMap.isEmpty()) {
              if (memberMap.get("id") instanceof Integer) {
                // トークンの生成と有効期限設定（10分間）
                String authToken = generateAuthToken(player);
                long expiresAt = System.currentTimeMillis() + (10 * 60 * 1000);

                // データベースにトークンを保存
                db.updateAuthToken(conn, playerUUID, authToken, expiresAt);

                // 先に処理中メッセージを表示
                sendProcessingMessage(player);

                // Velocity経由でWeb側にプレイヤー情報とトークンを送信
                // Web側でDB保存後、mc_auth_token_savedメッセージが返ってきて
                // SpigotAuthTokenSavedHandlerがURLを表示する
                sendAuthTokenToVelocity(conn, player, authToken, expiresAt, "create");

                // QRコード生成・配布（URL表示後に行う予定だったが、ここでは行わない）
                // URL表示とQRコード配布はSpigotAuthTokenSavedHandlerに移譲
                // String confirmUrl = Settings.CONFIRM_URL.getValue() + "?t=" + authToken;
                // String[] imageArgs = { "image", "createqr", confirmUrl };
                // if (ifMapId == -1) {
                //   im.executeImageMapForConfirm(player, imageArgs);
                // } else {
                //   im.giveMapToPlayer(player, ifMapId);
                // }
              }
            }
          } catch (SQLException | ClassNotFoundException e2) {
            Component errorMessage = Component.text("WEB認証のQRコード生成に失敗しました。")
                .color(NamedTextColor.RED);
            audiences.player(player).sendMessage(errorMessage);
            logger.error("A SQLException error occurred: " + e2.getMessage());
            for (StackTraceElement element : e2.getStackTrace()) {
              logger.error(element.toString());
            }
          }
        } else {
          Component message = Component.text("WEB認証済みプレイヤーが通過しました！")
              .color(NamedTextColor.GREEN);
          audiences.player(player).sendMessage(message);
        }
        Confirm.confirmMap.add(player);
      }
    } else {
      if (sender != null) {
        sender.sendMessage("プレイヤーからのみ実行可能です。");
      }
    }
  }

  /**
   * テスト用の認証フロー実行メソッド
   * コンソールから呼び出され、指定されたテストプレイヤー情報を使用して認証フローを実行します
   */
  public void executeTestFlow(String testPlayerName, String testPlayerUuid) {
    try {
      logger.info("テスト認証フローを開始: {} ({})", testPlayerName, testPlayerUuid);

      try (Connection conn = db.getConnection()) {
        // TestPlayerがmembersテーブルに存在するかチェック
        Map<String, Object> memberMap = db.getMemberMap(conn, testPlayerName);

        if (memberMap.isEmpty()) {
          // テスト用プレイヤーが存在しない場合は自動作成
          logger.info("🔨 テスト用プレイヤーがmembersテーブルに存在しないため、自動作成します");
          int insertedId = db.insertTestMember(conn, testPlayerName, testPlayerUuid, thisServerName);

          if (insertedId > 0) {
            logger.info("✅ テスト用プレイヤーを作成しました - ID: {}", insertedId);
          } else {
            logger.error("❌ テスト用プレイヤーの作成に失敗しました");
            throw new RuntimeException("テスト用プレイヤーの作成に失敗しました");
          }
        } else {
          logger.info("📋 テスト用プレイヤーは既にmembersテーブルに存在します - ID: {}", memberMap.get("id"));
        }

        // トークンの生成と有効期限設定（10分間）
        String authToken = generateTestAuthToken(testPlayerName, testPlayerUuid);
        long expiresAt = System.currentTimeMillis() + (10 * 60 * 1000);

        logger.info("テスト認証トークンを生成: {}", authToken);

        // データベースにトークンを保存（テスト用プレイヤー情報で）
        db.updateAuthToken(conn, testPlayerUuid, authToken, expiresAt);
        logger.info("テスト認証トークンをデータベースに保存しました");

        // 新形式のURL（トークンベース）を使用
        String confirmUrl = Settings.CONFIRM_URL.getValue() + "?t=" + authToken;

        // Velocity経由でWeb側にテストプレイヤー情報とトークンを送信
        sendTestAuthTokenToVelocity(conn, testPlayerName, testPlayerUuid, authToken, expiresAt, "test");

        logger.info("✅ テスト認証フローが完了しました");
        logger.info("🔗 テスト認証URL: {}", confirmUrl);
        logger.info("📧 このURLを使用してWebページからの認証をテストできます");

      } catch (SQLException | ClassNotFoundException e) {
        logger.error("テスト認証フロー実行中にデータベースエラーが発生しました: {}", e.getMessage(), e);
        throw new RuntimeException(e);
      }

    } catch (Exception e) {
      logger.error("テスト認証フロー実行中にエラーが発生しました: {}", e.getMessage(), e);
      throw e;
    }
  }

  /**
   * テスト用認証トークンを生成
   */
  private String generateTestAuthToken(String testPlayerName, String testPlayerUuid) {
    return "TEST_" + generateOTP(24) + "_" + System.currentTimeMillis();
  }

  /**
   * Velocity経由でWeb側にテスト用認証トークン情報を送信
   */
  private void sendTestAuthTokenToVelocity(Connection conn, String testPlayerName, String testPlayerUuid,
      String token, long expiresAt, String action) {
    try {
      Message msg = new Message();
      msg.web = new Message.Web();
      msg.web.authToken = new Message.Web.AuthToken();
      msg.web.authToken.who = new Message.Minecraft.Who();
      msg.web.authToken.who.name = testPlayerName;
      msg.web.authToken.who.uuid = testPlayerUuid;
      msg.web.authToken.token = token;
      msg.web.authToken.expiresAt = expiresAt;
      msg.web.authToken.action = action;

      SocketSwitch ssw = sswProvider.get();
      ssw.sendVelocityServer(conn, msg);

      logger.info("テスト認証トークン情報をVelocityに送信しました: {}", testPlayerName);
    } catch (Exception e) {
      logger.error("テスト認証トークン情報のVelocity送信に失敗しました: {}", e.getMessage(), e);
    }
  }

  private void sendConfirmationMessage(Player player, String confirmUrl) {

    Component welcomeMessage = Component.text("Kishaxサーバーへようこそ！")
        .color(NamedTextColor.GREEN)
        .appendNewline();

    Component introMessage = Component.text("サーバーに参加するには、KishaxアカウントとMinecraftアカウントをリンクさせる必要があります。")
        .color(NamedTextColor.WHITE)
        .appendNewline()
        .appendNewline();

    Component webAuth = Component.text("WEB認証")
        .color(NamedTextColor.GOLD)
        .decorate(
            TextDecoration.BOLD,
            TextDecoration.UNDERLINED)
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

    Component javaUserInstruction = TCUtils.JAVA_USER.get()
        .append(Component.text("は、"))
        .append(Component.text("ココ")
            .color(NamedTextColor.GOLD)
            .decorate(TextDecoration.UNDERLINED)
            .clickEvent(ClickEvent.openUrl(confirmUrl))
            .hoverEvent(HoverEvent.showText(Component.text("クリックしてWEB認証ページを開く"))))
        .append(Component.text("をクリックしてアクセスしてください！"))
        .appendNewline();

    Component bedrockUserInstruction = TCUtils.BEDROCK_USER.get()
        .append(Component.text("は、配布されたQRコードを読み取ってアクセスしてください！"))
        .appendNewline()
        .appendNewline();

    Component finalMessage = Component.text("それでは、楽しいマイクラライフを！")
        .color(NamedTextColor.GREEN);

    // 即座にすべてのメッセージを送信
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
  }

  /**
   * 認証URL発行処理中メッセージを送信
   */
  private void sendProcessingMessage(Player player) {
    Component processingMessage = Component.text()
        .append(Component.text("⏳ ").color(NamedTextColor.YELLOW))
        .append(Component.text("認証URLを発行中です...").color(NamedTextColor.WHITE))
        .appendNewline()
        .append(Component.text("少々お待ちください。").color(NamedTextColor.GRAY))
        .build();

    audiences.player(player).sendMessage(processingMessage);
  }

  private int checkExistConfirmMap(Connection conn, Object[] args) throws SQLException {
    String query = "SELECT * FROM images WHERE server=? AND confirm=? AND name=?";
    PreparedStatement ps = conn.prepareStatement(query);
    for (int i = 0; i < args.length; i++) {
      ps.setObject(i + 1, args[i]);
    }
    try (ResultSet rs = ps.executeQuery()) {
      if (rs.next()) {
        return rs.getInt("mapid");
      } else {
        return -1;
      }
    }
  }

  /**
   * 認証トークンを生成
   */
  private String generateAuthToken(Player player) {
    return generateOTP(32) + "_" + System.currentTimeMillis();
  }

  private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
  private static final SecureRandom random = new SecureRandom();

  private static String generateOTP(int length) {
    StringBuilder otp = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      int index = random.nextInt(CHARACTERS.length());
      otp.append(CHARACTERS.charAt(index));
    }
    return otp.toString();
  }

  /**
   * Velocity経由でWeb側に認証トークン情報を送信
   */
  private void sendAuthTokenToVelocity(Connection conn, Player player, String token, long expiresAt, String action) {
    try {
      Message msg = new Message();
      msg.web = new Message.Web();
      msg.web.authToken = new Message.Web.AuthToken();
      msg.web.authToken.who = new Message.Minecraft.Who();
      msg.web.authToken.who.name = player.getName();
      msg.web.authToken.who.uuid = player.getUniqueId().toString();
      msg.web.authToken.token = token;
      msg.web.authToken.expiresAt = expiresAt;
      msg.web.authToken.action = action;

      SocketSwitch ssw = sswProvider.get();
      ssw.sendVelocityServer(conn, msg);

      logger.info("Sent auth token info to Velocity for player: {}", player.getName());
    } catch (Exception e) {
      logger.error("Failed to send auth token info to Velocity: {}", e.getMessage());
    }
  }
}
