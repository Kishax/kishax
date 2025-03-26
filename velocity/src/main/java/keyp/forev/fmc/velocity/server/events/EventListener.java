package keyp.forev.fmc.velocity.server.events;

import java.math.BigInteger;
import java.sql.Connection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;

import keyp.forev.fmc.common.database.Database;
import keyp.forev.fmc.common.server.Luckperms;
import keyp.forev.fmc.common.settings.PermSettings;
import keyp.forev.fmc.common.util.PlayerUtils;
import keyp.forev.fmc.velocity.util.RomaToKanji;
import keyp.forev.fmc.velocity.util.RomajiConversion;
import keyp.forev.fmc.velocity.util.config.VelocityConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import keyp.forev.fmc.velocity.server.cmd.sub.interfaces.Request;
import keyp.forev.fmc.velocity.discord.DiscordEventListener;
import keyp.forev.fmc.velocity.discord.MessageEditor;
import keyp.forev.fmc.velocity.server.BroadCast;
import keyp.forev.fmc.velocity.server.FMCBoard;
import keyp.forev.fmc.velocity.server.GeyserMC;
import keyp.forev.fmc.velocity.server.MineStatus;
import keyp.forev.fmc.velocity.server.PlayerDisconnect;
import keyp.forev.fmc.velocity.server.cmd.sub.Maintenance;
import keyp.forev.fmc.velocity.server.cmd.sub.StartServer;
import keyp.forev.fmc.velocity.Main;

public class EventListener {
  public static Set<String> playerInputers = new HashSet<>();
  public static Map<String, String> PlayerMessageIds = new HashMap<>();
  public static final Map<Player, Runnable> disconnectTasks = new HashMap<>();
  public static final Map<Player, Runnable> otherServerConnectTasks = new HashMap<>();
  public static final Map<Player, Integer> playerJoinHubIds = new HashMap<>();
  public static final Set<String> startingServers = new HashSet<>();
  private final Main plugin;
  private final ProxyServer server;
  private final VelocityConfig config;
  private final Logger logger;
  private final Database db;
  private final BroadCast bc;
  private final ConsoleCommandSource console;
  private final RomaToKanji conv;
  private String chatServerName = null, originalMessage = null, joinMessage = null;
  private Component component = null;
  private final PlayerUtils pu;
  private final PlayerDisconnect pd;
  private final RomajiConversion rc;
  private final MessageEditor discordME;
  private final MineStatus ms;
  private final GeyserMC gm;
  private final Maintenance mt;
  private final FMCBoard fb;
  private final Luckperms lp;
  private ServerInfo serverInfo = null;
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

  @Inject
  public EventListener(Main plugin, Logger logger, ProxyServer server, VelocityConfig config, Database db, BroadCast bc,
      ConsoleCommandSource console, RomaToKanji conv, PlayerUtils pu, PlayerDisconnect pd, RomajiConversion rc,
      MessageEditor discordME, MineStatus ms, GeyserMC gm, Maintenance mt, FMCBoard fb, Luckperms lp) {
    this.plugin = plugin;
    this.logger = logger;
    this.server = server;
    this.config = config;
    this.db = db;
    this.bc = bc;
    this.console = console;
    this.conv = conv;
    this.pu = pu;
    this.pd = pd;
    this.rc = rc;
    this.discordME = discordME;
    this.ms = ms;
    this.gm = gm;
    this.mt = mt;
    this.fb = fb;
    this.lp = lp;
  }

  @Subscribe
  public void onChat(PlayerChatEvent e) {
    if (e.getMessage().startsWith("/"))
      return;
    Player player = e.getPlayer();
    String playerName = player.getUsername();
    originalMessage = e.getMessage();
    if (playerInputers.contains(playerName)) {
      return; // プレイヤーの入力をキャンセル
    }
    player.getCurrentServer().ifPresent(serverConnection -> {
      RegisteredServer registeredServer = serverConnection.getServer();
      serverInfo = registeredServer.getServerInfo();
      chatServerName = serverInfo.getName();
    });
    int NameCount = playerName.length();
    StringBuilder space = new StringBuilder();
    for (int i = 0; i <= NameCount; i++) {
      space.append('\u0020'); // Unicodeのスペースを追加
    }
    server.getScheduler().buildTask(plugin, () -> {
      try {
        String urlRegex = "https?://\\S+";
        Pattern pattern = Pattern.compile(urlRegex);
        Matcher matcher = pattern.matcher(originalMessage);
        List<String> urls = new ArrayList<>();
        List<String> textParts = new ArrayList<>();
        int lastMatchEnd = 0;
        boolean isUrl = false;
        String mixtext = "";
        while (matcher.find()) {
          isUrl = true;
          urls.add(matcher.group());
          textParts.add(originalMessage.substring(lastMatchEnd, matcher.start()));
          lastMatchEnd = matcher.end();
        }
        component = Component.text(space + "(").color(NamedTextColor.GOLD);
        boolean isEnglish = false;
        if (originalMessage.length() >= 1) {
          String firstOneChars = originalMessage.substring(0, 1);
          if (".".equalsIgnoreCase(firstOneChars)) {
            isEnglish = true;
            originalMessage = originalMessage.substring(1);
          }
        }
        if (originalMessage.length() >= 2) {
          String firstTwoChars = originalMessage.substring(0, 2);
          if ("@n".equalsIgnoreCase(firstTwoChars)) {
            // 新しいEmbedをDiscordに送る(通知を鳴らす)
            DiscordEventListener.playerChatMessageId = null;
            originalMessage = originalMessage.substring(2);
          }
        }
        if (originalMessage.length() >= 3) {
          String firstThreeChars = originalMessage.substring(0, 3);
          if ("@en".equalsIgnoreCase(firstThreeChars)) {
            isEnglish = true;
            originalMessage = originalMessage.substring(3);
          }
        }
        if (!isUrl) {
          String kanjiPattern = "[\\u4E00-\\u9FFF]+";
          String hiraganaPattern = "[\\u3040-\\u309F]+";
          String katakanaPattern = "[\\u30A0-\\u30FF]+";
          if (detectMatches(originalMessage, kanjiPattern) || detectMatches(originalMessage, hiraganaPattern)
              || detectMatches(originalMessage, katakanaPattern) || isEnglish) {
            discordME.AddEmbedSomeMessage("Chat", player, serverInfo, originalMessage);
            return;
          }
          if (config.getBoolean("Conv.Mode")) {
            // Map方式
            String kanaMessage = conv.ConvRomaToKana(originalMessage);
            String kanjiMessage = conv.ConvRomaToKanji(kanaMessage);
            discordME.AddEmbedSomeMessage("Chat", player, serverInfo, kanjiMessage);
            component = component.append(Component.text(kanjiMessage + ")").color(NamedTextColor.GOLD));
            bc.sendSpecificServerMessage(component, chatServerName);
          } else {
            // pde方式
            String kanaMessage = rc.Romaji(originalMessage);
            String kanjiMessage = conv.ConvRomaToKanji(kanaMessage);
            discordME.AddEmbedSomeMessage("Chat", player, serverInfo, kanjiMessage);
            component = component.append(Component.text(kanjiMessage + ")").color(NamedTextColor.GOLD));
            bc.sendSpecificServerMessage(component, chatServerName);
          }
          return;
        }
        if (lastMatchEnd < originalMessage.length()) {
          textParts.add(originalMessage.substring(lastMatchEnd));
        }
        int textPartsSize = textParts.size();
        int urlsSize = urls.size();
        for (int i = 0; i < textPartsSize; i++) {
          if (Objects.nonNull(textParts) && textPartsSize != 0) {
            String text = textParts.get(i);
            String kanaMessage;
            String kanjiMessage;
            if (isEnglish) {
              // 英語
              mixtext += text;
            } else {
              // 日本語
              if (config.getBoolean("Conv.Mode")) {
                // Map方式
                kanaMessage = conv.ConvRomaToKana(text);
                kanjiMessage = conv.ConvRomaToKanji(kanaMessage);
              } else {
                // pde方式
                kanaMessage = rc.Romaji(text);
                kanjiMessage = conv.ConvRomaToKanji(kanaMessage);
              }
              mixtext += kanjiMessage;
              component = component.append(Component.text(kanjiMessage).color(NamedTextColor.GOLD));
            }
          }
          if (i < urlsSize) {
            String getUrl;
            String getUrl2;
            if (textParts.get(i).isEmpty()) {
              getUrl = urls.get(i);
              getUrl2 = urls.get(i);
            } else if (i != textPartsSize - 1) {
              getUrl = "\n" + urls.get(i) + "\n";
              getUrl2 = "\n" + space + urls.get(i);
            } else {
              getUrl = "\n" + urls.get(i);
              getUrl2 = "\n" + space + urls.get(i);
            }
            mixtext += getUrl;
            component = component
                .append(Component.text(getUrl2).color(NamedTextColor.GRAY).clickEvent(ClickEvent.openUrl(urls.get(i)))
                    .hoverEvent(HoverEvent.showText(Component.text("リンク" + (i + 1)))));
          }
        }
        if (!isEnglish) {
          component = component.append(Component.text(")").color(NamedTextColor.GOLD));
          bc.sendSpecificServerMessage(component, chatServerName);
        }

        discordME.AddEmbedSomeMessage("Chat", player, serverInfo, mixtext)
            .exceptionally(ex -> {
              logger.error("An Exception error occurred: " + ex.getMessage());
              for (StackTraceElement element : ex.getStackTrace()) {
                logger.error(element.toString());
              }
              return null;
            });
      } catch (Exception ex) {
        logger.error("An onChat error occurred: " + ex.getMessage());
        for (StackTraceElement element : ex.getStackTrace()) {
          logger.error(element.toString());
        }
      }
    }).schedule();
  }

  @Subscribe
  public void onPostLogin(PostLoginEvent event) {
    Player player = event.getPlayer();
    fb.addBoard(player);
  }

  @Subscribe
  public void onServerPreConnectEvent(ServerPreConnectEvent event) {
    Player player = event.getPlayer();
    String playerName = player.getUsername();
    String playerUUID = player.getUniqueId().toString();

    Set<String> allowOtherServers = new HashSet<>();
    try (Connection connection = db.getConnection()) {
      try (PreparedStatement ps = connection.prepareStatement(
          "SELECT name FROM status WHERE allow_prestart=?;")) {
        ps.setBoolean(1, true);
        try (ResultSet rs = ps.executeQuery()) {
          while (rs.next()) {
            allowOtherServers.add(rs.getString("name"));
          }
        }
      }
    } catch (SQLException | ClassNotFoundException e) {
      logger.error("An error occurred at EventListener#onServerPreConnectEvent: ", e);
    }

    String serverName = event.getOriginalServer().getServerInfo().getName();
    if (!allowOtherServers.contains(serverName)) {
      return;
    }

    if (startingServers.contains(serverName)) {
      pd.playerDisconnect(
          false,
          player,
          Component.text(serverName + "サーバーは現在起動中です！").color(NamedTextColor.GOLD));
      return;
    }

    final AtomicBoolean canConnect = new AtomicBoolean(false);
    final CountDownLatch latch = new CountDownLatch(1);

    CompletableFuture.runAsync(() -> {
      try (Connection conn = db.getConnection()) {
        try (PreparedStatement ps = conn.prepareStatement(
            "SELECT * FROM members WHERE name=? AND uuid=?;");
            PreparedStatement ps2 = conn.prepareStatement(
                "SELECT * FROM status WHERE name=?;")) {
          ps.setString(1, playerName);
          ps.setString(2, playerUUID);
          ps2.setString(1, serverName);
          try (ResultSet rs = ps.executeQuery();
              ResultSet rs2 = ps2.executeQuery()) {
            if (rs2.next() && !rs2.getBoolean("online")) {
              if (rs.next()) {
                if (rs.getBoolean("ban")) {
                  pd.playerDisconnect(
                      false,
                      player,
                      Component.text("You are banned from this server.").color(NamedTextColor.RED));
                  return;
                }

                int permLevel = lp.getPermLevel(playerName);
                boolean isSecond = false;
                for (Player otherServerConnectedPlayer : otherServerConnectTasks.keySet()) {
                  if (otherServerConnectedPlayer.getUniqueId().equals(player.getUniqueId())) {
                    isSecond = true;
                    otherServerConnectTasks.remove(player);
                    startingServers.add(serverName);
                    scheduler.schedule(() -> {
                      if (startingServers.contains(serverName)) {
                        startingServers.remove(serverName);
                      }
                    }, 120, TimeUnit.SECONDS);

                    if (permLevel > 1) {
                      Main.getInjector().getInstance(StartServer.class).execute2(player, serverName);
                    } else if (permLevel == 1) {
                      Main.getInjector().getProvider(Request.class).get().execute2(player, serverName);
                      ;
                    }
                    return;
                  }
                }

                if (!isSecond) {
                  Runnable task = () -> {
                    if (otherServerConnectTasks.containsKey(player)) {
                      otherServerConnectTasks.remove(player);
                    }
                  };
                  otherServerConnectTasks.put(player, task);
                  scheduler.schedule(() -> {
                    if (otherServerConnectTasks.containsKey(player)) {
                      Runnable removeTask = otherServerConnectTasks.get(player);
                      removeTask.run();
                    }
                  }, 10, TimeUnit.SECONDS);

                  Component message = Component.text("あなたは認証ユーザーです。").color(NamedTextColor.GREEN);
                  Component message2;
                  if (permLevel > 1) {
                    message2 = Component.text(serverName + "サーバーを起動するには、10秒以内にもう一度接続してください。")
                        .color(NamedTextColor.GOLD);
                  } else if (permLevel == 1) {
                    message2 = Component.text(serverName + "サーバーに起動リクエストを送信するには、10秒以内にもう一度接続してください。")
                        .color(NamedTextColor.GOLD);
                  } else {
                    pd.playerDisconnect(
                        false,
                        player,
                        Component.text("認証ユーザーではありません。").color(NamedTextColor.RED));
                    throw new Error(playerName + "は認証ユーザーではありません。");
                  }

                  TextComponent messages = Component.text()
                      .append(message)
                      .appendNewline()
                      .append(message2)
                      .build();

                  pd.playerDisconnect(
                      false,
                      player,
                      messages);
                  return;
                }
              } else {
                pd.playerDisconnect(
                    false,
                    player,
                    Component.text("認証ユーザーではありません。").color(NamedTextColor.RED));
                return;
              }
            }
          }
        }
      } catch (SQLException | ClassNotFoundException e) {
        logger.error("An error occurred at EventListener#onServerPreConnectEvent: ", e);
        pd.playerDisconnect(
            false,
            player,
            Component.text("データベース接続エラー: データベースに接続できませんでした。").color(NamedTextColor.RED));
      } finally {
        latch.countDown();
      }
    });

    try {
      latch.await(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      logger.warn("Thread interrupted while waiting for async task", e);
    }

    if (canConnect.get()) {
    } else {
    }
  }

  @Subscribe
  public void onServerPostConnect(ServerPostConnectEvent event) {
    Player player = event.getPlayer();
    fb.resendBoard(player.getUniqueId());
  }

  @Subscribe
  public void onServerSwitch(ServerConnectedEvent e) {
    Player player = e.getPlayer();
    String playerName = player.getUsername(),
        playerUUID = player.getUniqueId().toString();
    if (!disconnectTasks.isEmpty()) {
      for (Player disconnectPlayer : disconnectTasks.keySet()) {
        if (disconnectPlayer.getUniqueId().equals(player.getUniqueId())) {
          disconnectTasks.remove(disconnectPlayer);
        }
      }
    }
    RegisteredServer serverConnection = e.getServer();
    serverInfo = serverConnection.getServerInfo();
    String currentServerName = serverInfo.getName();
    Optional<RegisteredServer> previousServerInfo = e.getPreviousServer();
    if (Objects.isNull(serverInfo)) {
      pd.playerDisconnect(
          false,
          player,
          Component.text("コネクションエラー: 接続サーバー名が不明です。").color(NamedTextColor.RED));
      logger.error("コネクションエラー: 接続サーバー名が不明です。");
      return;
    }
    final AtomicBoolean isBedrock = new AtomicBoolean(false);
    if (gm.isGeyserPlayer(player)) {
      logger.info("GeyserMC player connected: " + playerName);
      String playerXuid = gm.getGeyserPlayerXuid(player);
      logger.info("Player XUID: " + playerXuid);
      isBedrock.set(true);
    } else {
      logger.info("Java player connected: " + playerName);
    }
    server.getScheduler().buildTask(plugin, () -> {
      try (Connection conn = db.getConnection()) {
        if (db.isMaintenance(conn)) {
          List<String> menteAllowMembers = mt.getMenteAllowMembers();
          if (player.hasPermission(PermSettings.SUPER_ADMIN.get())) {
            Component adminMenteMessage = Component.text("スーパーアドミン認証...PASS")
                .appendNewline()
                .appendNewline()
                .append(Component.text("ALL CORRECT"))
                .appendNewline()
                .appendNewline()
                .append(Component.text("メンテナンスモードが有効です。"))
                .color(NamedTextColor.GREEN);

            player.sendMessage(adminMenteMessage);
          } else if (menteAllowMembers.contains(playerName)) {
            Component confirmedMemberMenteMessage = Component.text("メンテメンバー認証...PASS")
                .appendNewline()
                .appendNewline()
                .append(Component.text("ALL CORRECT"))
                .appendNewline()
                .appendNewline()
                .append(Component.text("メンテナンスモードが有効です。"))
                .color(NamedTextColor.GREEN);

            player.sendMessage(confirmedMemberMenteMessage);
          } else {
            pd.playerDisconnect(
                false,
                player,
                Component.text("現在メンテナンス中です。").color(NamedTextColor.BLUE));
            return;
          }
        }
        String query2 = "SELECT * FROM members WHERE uuid=? ORDER BY id DESC LIMIT 1;";
        try (PreparedStatement ps2 = conn.prepareStatement(query2)) {
          ps2.setString(1, playerUUID);
          try (ResultSet yuyu = ps2.executeQuery();) {
            if (yuyu.next()) {
              if (yuyu.getBoolean("ban")) {
                pd.playerDisconnect(
                    true,
                    player,
                    Component.text("You are banned from this server.").color(NamedTextColor.RED));
                return;
              } else {
                // メッセージ送信
                component = Component.text(playerName + "が" + currentServerName + "サーバーに参加しました。")
                    .color(NamedTextColor.YELLOW);
                bc.sendSpecificServerMessage(component, currentServerName);
                joinMessage = config.getString("EventMessage.Join", "");
                if (!joinMessage.isEmpty()) {
                  joinMessage = joinMessage.replace("\\n", "\n");
                  player.sendMessage(Component.text(joinMessage).color(NamedTextColor.AQUA));
                }
                // 2時間経ってたら
                String query4 = "SELECT * FROM log WHERE uuid=? AND `join`=? ORDER BY id DESC LIMIT 1;";
                try (PreparedStatement ps4 = conn.prepareStatement(query4)) {
                  ps4.setString(1, playerUUID);
                  ps4.setBoolean(2, true);
                  try (ResultSet logs = ps4.executeQuery()) {
                    long beforejoin_sa_minute = 0;
                    if (logs.next()) {
                      ZonedDateTime nowTokyo = ZonedDateTime.now(ZoneId.of("Asia/Tokyo"));
                      long now_timestamp = nowTokyo.toEpochSecond();
                      Timestamp beforejoin_timeget = logs.getTimestamp("time");
                      long beforejoin_timestamp = beforejoin_timeget.getTime() / 1000L;
                      long beforejoin_sa = now_timestamp - beforejoin_timestamp;
                      if (beforejoin_sa < 0) {
                        logger.error("beforejoin_sa is less than 0.");
                      }
                      beforejoin_sa_minute = Math.max(beforejoin_sa / 60, 0); // マイナス値を防ぐためにMath.maxを使用
                    }
                    String updatedName = null;
                    if (!playerName.equals(yuyu.getString("name"))) {
                      // 一番最初に登録した名前と一致しなかったら
                      // MOJANG-APIからとってきた名前でレコードを更新させる
                      updatedName = !isBedrock.get() ? pu.getPlayerNameFromUUID(player.getUniqueId()) : playerName;
                      if (Objects.isNull(updatedName) || !(updatedName.equals(playerName))) {
                        pd.playerDisconnect(
                            true,
                            player,
                            Component.text("You are banned from this server.").color(NamedTextColor.RED));
                        return;
                      }
                      String query5 = "UPDATE members SET name=?, old_name=? WHERE uuid=?;";
                      try (PreparedStatement ps5 = conn.prepareStatement(query5)) {
                        ps5.setString(1, updatedName);
                        ps5.setString(2, yuyu.getString("name"));
                        ps5.setString(3, playerUUID);
                        int rsAffected5 = ps5.executeUpdate();
                        if (rsAffected5 > 0) {
                          player.sendMessage(
                              Component.text("MCIDの変更が検出されたため、データベースを更新しました。").color(NamedTextColor.GREEN));
                          // 過去の名前を解放するため、過去の名前のレコードがほかにもあったらそれをinvalid_loginへ移動
                          String query6 = "SELECT COUNT(*) FROM members WHERE name=?;";
                          try (PreparedStatement ps6 = conn.prepareStatement(query6)) {
                            ps6.setString(1, yuyu.getString("name"));
                            try (ResultSet rs6 = ps6.executeQuery()) {
                              if (rs6.next()) {
                                int count = rs6.getInt(1);
                                if (count >= 1) {
                                  String query7 = "INSERT INTO invalid_login SELECT * FROM members WHERE name=?;";
                                  try (PreparedStatement ps7 = conn.prepareStatement(query7)) {
                                    ps7.setString(1, yuyu.getString("name"));
                                    int rsAffected7 = ps7.executeUpdate();
                                    if (rsAffected7 > 0) {
                                      String query8 = "DELETE from members WHERE name=?;";
                                      try (PreparedStatement ps8 = conn.prepareStatement(query8)) {
                                        ps8.setString(1, yuyu.getString("name"));
                                        int rsAffected8 = ps8.executeUpdate();
                                        if (rsAffected8 > 0) {
                                          console.sendMessage(Component.text("過去の名前のレコードをinvalid_loginへ移動しました。")
                                              .color(NamedTextColor.GREEN));
                                        }
                                      }
                                    } else {
                                      console.sendMessage(Component.text("過去の名前のレコードをinvalid_loginへ移動できませんでした。")
                                          .color(NamedTextColor.RED));
                                    }
                                  }
                                }
                              }
                            }
                          }
                        }
                      }
                    }
                    // AmabassadorプラグインによるReconnectの場合 Or リログして〇秒以内の場合
                    if (EventListener.PlayerMessageIds.containsKey(playerUUID) && previousServerInfo.isPresent()) {
                      // どこからか移動してきたとき
                      RegisteredServer previousServer = previousServerInfo.get();
                      ServerInfo beforeServerInfo = previousServer.getServerInfo();
                      String beforeServerName = beforeServerInfo.getName();
                      db.insertLog(conn, "INSERT INTO `log` (name, uuid, server, `join`) VALUES (?, ?, ?, ?);",
                          new Object[] { playerName, playerUUID, currentServerName, true });
                      ms.updateMovePlayers(playerName, beforeServerName, currentServerName);
                      try {
                        discordME.AddEmbedSomeMessage("Move", player, serverInfo);
                      } catch (Exception ex) {
                        logger.error("An exception occurred while executing the AddEmbedSomeMessage method: {}",
                            ex.getMessage());
                        for (StackTraceElement ste : ex.getStackTrace()) {
                          logger.error(ste.toString());
                        }
                      }
                    } else {
                      if (beforejoin_sa_minute >= config.getInt("Interval.Login", 0)) {
                        if (previousServerInfo.isPresent()) {
                          // どこからか移動してきたとき
                          db.insertLog(conn, "INSERT INTO `log` (name, uuid, server, `join`) VALUES (?, ?, ?, ?);",
                              new Object[] { playerName, playerUUID, currentServerName, true });
                          RegisteredServer previousServer = previousServerInfo.get();
                          ServerInfo beforeServerInfo = previousServer.getServerInfo();
                          String beforeServerName = beforeServerInfo.getName();
                          ms.updateMovePlayers(playerName, beforeServerName, currentServerName);
                          try {
                            discordME.AddEmbedSomeMessage("Move", player, currentServerName);
                          } catch (Exception ex) {
                            logger.error("An exception occurred while executing the AddEmbedSomeMessage method: {}",
                                ex.getMessage());
                            for (StackTraceElement ste : ex.getStackTrace()) {
                              logger.error(ste.toString());
                            }
                          }
                        } else {
                          // 1回目のどこかのサーバーに上陸したとき
                          Object insertedId = db.insertLogAndGetColumnValue(1, conn,
                              "INSERT INTO `log` (name, uuid, server, `join`) VALUES (?, ?, ?, ?);",
                              new Object[] { playerName, playerUUID, currentServerName, true });
                          putJoinLogIdToMap(insertedId, player);
                          ms.updateJoinPlayers(playerName, currentServerName);
                          try {
                            discordME.AddEmbedSomeMessage("Join", player, serverInfo);
                          } catch (Exception ex) {
                            logger.error("An exception occurred while executing the AddEmbedSomeMessage method: {}",
                                ex.getMessage());
                            for (StackTraceElement ste : ex.getStackTrace()) {
                              logger.error(ste.toString());
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            } else {
              // DBにデータがなかったら (初参加)
              // MojangAPIによるUUID-MCIDチェックも行う
              // データベースに同一の名前がないか確認
              String current_name = !isBedrock.get() ? pu.getPlayerNameFromUUID(player.getUniqueId()) : playerName,
                  query3 = "SELECT * FROM members WHERE name=? ORDER BY id DESC LIMIT 1;";
              try (PreparedStatement ps3 = conn.prepareStatement(query3)) {
                ps3.setString(1, playerName);
                try (ResultSet yu = ps3.executeQuery()) {
                  if (yu.next() || Objects.isNull(current_name) || !(current_name.equals(playerName))) {
                    // クエリ実行より優先してキック
                    pd.playerDisconnect(
                        true,
                        player,
                        Component.text("You are banned from this server.").color(NamedTextColor.RED));
                    String query4 = "INSERT INTO members (name, uuid, ban) VALUES (?, ?, ?);";
                    try (PreparedStatement ps4 = conn.prepareStatement(query4)) {
                      ps4.setString(1, playerName);
                      ps4.setString(2, playerUUID);
                      ps4.setBoolean(3, true);
                      ps4.executeUpdate();
                    }
                    return;
                  }
                  String DiscordInviteUrl = config.getString("Discord.InviteUrl", "");
                  if (!DiscordInviteUrl.isEmpty()) {
                    component = Component.text(playerName + "が" + currentServerName + "サーバーに初参加しました。")
                        .color(NamedTextColor.YELLOW)
                        .appendNewline()
                        .append(Component.text("FMCサーバー")
                            .color(NamedTextColor.AQUA)
                            .decorate(
                                TextDecoration.BOLD,
                                TextDecoration.UNDERLINED))
                        .append(Component.text("へようこそ！")
                            .color(NamedTextColor.AQUA))
                        .appendNewline()
                        .append(Component.text("当サーバーでは、サーバーへ参加するにあたって、FMCアカウント作成と、それをマイクラアカウントと紐づける")
                            .color(NamedTextColor.AQUA))
                        .append(Component.text("WEB認証")
                            .color(NamedTextColor.LIGHT_PURPLE)
                            .decorate(
                                TextDecoration.BOLD,
                                TextDecoration.UNDERLINED))
                        .append(Component.text("を必須としています。")
                            .color(NamedTextColor.AQUA))
                        .appendNewline()
                        .append(Component.text("FMCユーザーは、サーバーを起動するためのリクエストを管理者へ送ることができます。今後色々なコンテンツを追加していく予定です！")
                            .color(NamedTextColor.AQUA))
                        .appendNewline()
                        .append(Component.text("ここを進んでください！")
                            .color(NamedTextColor.AQUA))
                        .appendNewline()
                        .append(Component.text("なにかわからないことがあったら、当サーバーの").color(NamedTextColor.AQUA))
                        .append(Component.text("Discord")
                            .color(NamedTextColor.BLUE)
                            .decorate(
                                TextDecoration.BOLD,
                                TextDecoration.UNDERLINED)
                            .clickEvent(ClickEvent.openUrl(DiscordInviteUrl))
                            .hoverEvent(HoverEvent.showText(Component.text("FMCサーバーのDiscordへいこう！"))))
                        .append(Component.text("にて質問してください！参加するには、上の「Discord」をクリックしてね。").color(NamedTextColor.AQUA));
                    player.sendMessage(component);
                  }
                  ms.updateJoinPlayers(playerName, currentServerName);
                  try {
                    discordME.AddEmbedSomeMessage("FirstJoin", player, serverInfo);
                  } catch (Exception ex) {
                    logger.error("An exception occurred while executing the AddEmbedSomeMessage method: {}",
                        ex.getMessage());
                    for (StackTraceElement ste : ex.getStackTrace()) {
                      logger.error(ste.toString());
                    }
                  }
                  if (config.getBoolean("Servers." + currentServerName + ".hub")) {
                    component = Component.text(playerName + "がhomeサーバーに初めてやってきました！").color(NamedTextColor.AQUA);
                    bc.sendExceptServerMessage(component, currentServerName);
                  }
                }
              }
            }
            // サーバー移動通知
            component = Component.text("サーバー移動通知: " + playerName + " -> " + currentServerName)
                .color(NamedTextColor.AQUA);
            bc.sendExceptServerMessage(component, currentServerName);
            // Amabassadorプラグインと競合している可能性あり
            // Main.getInjector().getInstance(velocity.PlayerUtil.class).updatePlayers();
            pu.updatePlayers();
          }
        }
      } catch (ClassNotFoundException | SQLException e1) {
        pd.playerDisconnect(
            false,
            player,
            Component.text("Database Server is closed now!!").color(NamedTextColor.BLUE));
        logger.error("An onConnection error occurred: " + e1.getMessage());
        for (StackTraceElement element : e1.getStackTrace()) {
          logger.error(element.toString());
        }
      }
    }).schedule();
  }

  @Subscribe
  public void onPlayerDisconnect(DisconnectEvent e) {
    Player player = e.getPlayer();
    String playerName = player.getUsername();
    if (playerInputers.contains(playerName)) {
      playerInputers.remove(playerName);
    }
    fb.removeBoard(player.getUniqueId());
    if (gm.isGeyserPlayer(player)) {
      logger.info("GeyserMC player disconnected: " + playerName);
    } else {
      logger.info("Java player disconnected: " + playerName);
    }
    player.getCurrentServer().ifPresent(serverConnection -> {
      RegisteredServer registeredServer = serverConnection.getServer();
      serverInfo = registeredServer.getServerInfo();
      ms.updateQuitPlayers(playerName, serverInfo.getName());
    });
    Runnable task = () -> {
      // プレイヤーがReconnectしなかった場合に実行する処理
      server.getScheduler().buildTask(plugin, () -> {
        // プレイヤーが最後にいたサーバーを取得
        player.getCurrentServer().ifPresent(currentServer -> {
          RegisteredServer registeredServer = currentServer.getServer();
          serverInfo = registeredServer.getServerInfo();
          console
              .sendMessage(Component.text("Player " + playerName + " disconnected from server: " + serverInfo.getName())
                  .color(NamedTextColor.GREEN));
          try {
            discordME.AddEmbedSomeMessage("Exit", player, serverInfo);
          } catch (Exception ex) {
            logger.error("An exception occurred while executing the AddEmbedSomeMessage method: {}", ex.getMessage());
            for (StackTraceElement ste : ex.getStackTrace()) {
              logger.error(ste.toString());
            }
          }
        });
      }).schedule();
    };
    // タイマーを設定し、一定時間後に処理Aを実行
    disconnectTasks.put(player, task);
    scheduler.schedule(() -> {
      if (disconnectTasks.containsKey(player)) {
        task.run();
        disconnectTasks.remove(player);
      }
    }, 10, TimeUnit.SECONDS);
  }

  private void putJoinLogIdToMap(Object insertedValue, Player player) {
    if (insertedValue instanceof BigInteger) {
      int logId = ((BigInteger) insertedValue).intValue();
      EventListener.playerJoinHubIds.put(player, logId);
    } else if (insertedValue instanceof Integer) {
      int logId = (Integer) insertedValue;
      EventListener.playerJoinHubIds.put(player, logId);
    } else {
      logger.error("Unexpected type for insertedValue: " + insertedValue.getClass().getName());
    }
  }

  private boolean detectMatches(String input, String pattern) {
    Pattern regex = Pattern.compile(pattern);
    Matcher matcher = regex.matcher(input);
    while (matcher.find()) {
      // String found = matcher.group();
      // System.out.println(message + ": " + found);
      return true;
    }
    return false;
  }
}
