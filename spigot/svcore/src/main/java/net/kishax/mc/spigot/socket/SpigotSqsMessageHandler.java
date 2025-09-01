package net.kishax.mc.spigot.socket;

import com.fasterxml.jackson.databind.JsonNode;
import net.kishax.mc.common.socket.SqsMessageHandler;
import net.kishax.mc.common.socket.SqsClient;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spigot用SQSメッセージハンドラー
 */
public class SpigotSqsMessageHandler implements SqsMessageHandler {
  private static final Logger logger = LoggerFactory.getLogger(SpigotSqsMessageHandler.class);

  private final JavaPlugin plugin;
  private final SqsClient sqsClient;

  public SpigotSqsMessageHandler(JavaPlugin plugin, SqsClient sqsClient) {
    this.plugin = plugin;
    this.sqsClient = sqsClient;
  }

  @Override
  public void handleMessage(JsonNode message) {
    // 既存のSocketメッセージ互換処理
    try {
      String messageType = message.path("type").asText();
      logger.debug("汎用メッセージ処理: {}", messageType);

      // 既存のSpigotメッセージハンドラーシステムを使用
      // 必要に応じて適切なハンドラーにルーティング

    } catch (Exception e) {
      logger.error("汎用メッセージ処理でエラーが発生しました", e);
    }
  }

  @Override
  public void handleAuthConfirm(String playerName, String playerUuid) {
    try {
      logger.info("Web認証完了: {} ({})", playerName, playerUuid);

      // Spigotでの認証確認処理
      Player player = Bukkit.getPlayer(playerName);
      if (player != null) {
        // プレイヤーにメッセージ送信（Spigot側では通常Velocity経由なのでスキップ可能）
        player.sendMessage("§aWEB認証が完了しました！");
      }

      // 認証完了レスポンスを送信
      sendAuthResponseToWeb(playerName, playerUuid, true, "Spigot側でWEB認証を確認しました。");

    } catch (Exception e) {
      logger.error("認証確認処理でエラーが発生しました: {} ({})", playerName, playerUuid, e);
      sendAuthResponseToWeb(playerName, playerUuid, false, "Spigot側で認証処理エラーが発生しました。");
    }
  }

  @Override
  public void handleCommand(String commandType, String playerName, JsonNode data) {
    try {
      logger.info("Webコマンド処理: {} from {}", commandType, playerName);

      switch (commandType) {
        case "teleport" -> handleTeleportCommand(playerName, data);
        case "message" -> handleMessageCommand(playerName, data);
        case "gamemode" -> handleGamemodeCommand(playerName, data);
        case "give_item" -> handleGiveItemCommand(playerName, data);
        default -> logger.warn("不明なコマンドタイプ: {}", commandType);
      }

    } catch (Exception e) {
      logger.error("コマンド処理でエラーが発生しました: {} from {}", commandType, playerName, e);
    }
  }

  @Override
  public void handlePlayerRequest(String requestType, String playerName, JsonNode data) {
    try {
      logger.info("プレイヤーリクエスト処理: {} from {}", requestType, playerName);

      switch (requestType) {
        case "inventory" -> handleInventoryRequest(playerName, data);
        case "location" -> handleLocationRequest(playerName, data);
        case "stats" -> handleStatsRequest(playerName, data);
        default -> logger.warn("不明なリクエストタイプ: {}", requestType);
      }

    } catch (Exception e) {
      logger.error("プレイヤーリクエスト処理でエラーが発生しました: {} from {}", requestType, playerName, e);
    }
  }

  @Override
  public void handleOtpToMinecraft(String mcid, String uuid, String otp) {
    // OTP処理はVelocity→Spigot socket通信で処理されるため、この実装は不要
    logger.warn("SpigotSqsMessageHandlerでのOTP処理は廃止されました。Velocity→Spigot socket通信を使用してください。");
  }

  /**
   * テレポートコマンド処理
   */
  private void handleTeleportCommand(String playerName, JsonNode data) {
    String location = data.path("location").asText();
    logger.info("テレポートコマンド: {} → {}", playerName, location);

    Bukkit.getScheduler().runTask(plugin, () -> {
      Player player = Bukkit.getPlayer(playerName);
      if (player != null) {
        // 座標解析とテレポート処理
        String[] coords = location.split(",");
        if (coords.length >= 3) {
          try {
            double x = Double.parseDouble(coords[0]);
            double y = Double.parseDouble(coords[1]);
            double z = Double.parseDouble(coords[2]);

            org.bukkit.Location loc = new org.bukkit.Location(player.getWorld(), x, y, z);
            player.teleport(loc);
            player.sendMessage("§aテレポートしました！");
          } catch (NumberFormatException e) {
            player.sendMessage("§c座標の形式が正しくありません。");
          }
        } else {
          player.sendMessage("§c座標の形式が正しくありません。");
        }
      }
    });
  }

  /**
   * メッセージ送信コマンド処理
   */
  private void handleMessageCommand(String playerName, JsonNode data) {
    String message = data.path("message").asText();
    logger.info("メッセージコマンド: {} → {}", playerName, message);

    Bukkit.getScheduler().runTask(plugin, () -> {
      Player player = Bukkit.getPlayer(playerName);
      if (player != null) {
        player.sendMessage("§e[Web] " + message);
      }
    });
  }

  /**
   * ゲームモード変更コマンド処理
   */
  private void handleGamemodeCommand(String playerName, JsonNode data) {
    String gamemode = data.path("gamemode").asText();
    logger.info("ゲームモード変更コマンド: {} → {}", playerName, gamemode);

    Bukkit.getScheduler().runTask(plugin, () -> {
      Player player = Bukkit.getPlayer(playerName);
      if (player != null) {
        try {
          org.bukkit.GameMode mode = org.bukkit.GameMode.valueOf(gamemode.toUpperCase());
          player.setGameMode(mode);
          player.sendMessage("§aゲームモードを " + gamemode + " に変更しました！");
        } catch (IllegalArgumentException e) {
          player.sendMessage("§c無効なゲームモードです: " + gamemode);
        }
      }
    });
  }

  /**
   * アイテム付与コマンド処理
   */
  private void handleGiveItemCommand(String playerName, JsonNode data) {
    String itemName = data.path("item").asText();
    int amount = data.path("amount").asInt(1);
    logger.info("アイテム付与コマンド: {} → {} x{}", playerName, itemName, amount);

    Bukkit.getScheduler().runTask(plugin, () -> {
      Player player = Bukkit.getPlayer(playerName);
      if (player != null) {
        try {
          org.bukkit.Material material = org.bukkit.Material.valueOf(itemName.toUpperCase());
          org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(material, amount);
          player.getInventory().addItem(item);
          player.sendMessage("§a" + itemName + " x" + amount + " を付与しました！");
        } catch (IllegalArgumentException e) {
          player.sendMessage("§c無効なアイテムです: " + itemName);
        }
      }
    });
  }

  /**
   * インベントリリクエスト処理
   */
  private void handleInventoryRequest(String playerName, JsonNode data) {
    logger.info("インベントリリクエスト from {}", playerName);

    Bukkit.getScheduler().runTask(plugin, () -> {
      Player player = Bukkit.getPlayer(playerName);
      if (player != null && sqsClient != null) {
        // インベントリ情報を収集してレスポンス
        java.util.Map<String, Object> inventoryData = new java.util.HashMap<>();
        inventoryData.put("items", player.getInventory().getContents().length);
        inventoryData.put("level", player.getLevel());
        inventoryData.put("health", player.getHealth());

        sqsClient.sendGenericMessage("spigot_inventory_response", inventoryData);
      }
    });
  }

  /**
   * 位置リクエスト処理
   */
  private void handleLocationRequest(String playerName, JsonNode data) {
    logger.info("位置リクエスト from {}", playerName);

    Bukkit.getScheduler().runTask(plugin, () -> {
      Player player = Bukkit.getPlayer(playerName);
      if (player != null && sqsClient != null) {
        org.bukkit.Location loc = player.getLocation();
        java.util.Map<String, Object> locationData = new java.util.HashMap<>();
        locationData.put("world", loc.getWorld().getName());
        locationData.put("x", loc.getX());
        locationData.put("y", loc.getY());
        locationData.put("z", loc.getZ());

        sqsClient.sendGenericMessage("spigot_location_response", locationData);
      }
    });
  }

  /**
   * 統計リクエスト処理
   */
  private void handleStatsRequest(String playerName, JsonNode data) {
    logger.info("統計リクエスト from {}", playerName);

    Bukkit.getScheduler().runTask(plugin, () -> {
      Player player = Bukkit.getPlayer(playerName);
      if (player != null && sqsClient != null) {
        java.util.Map<String, Object> statsData = new java.util.HashMap<>();
        statsData.put("playTime", player.getStatistic(org.bukkit.Statistic.PLAY_ONE_MINUTE));
        statsData.put("deaths", player.getStatistic(org.bukkit.Statistic.DEATHS));
        statsData.put("mobKills", player.getStatistic(org.bukkit.Statistic.MOB_KILLS));

        sqsClient.sendGenericMessage("spigot_stats_response", statsData);
      }
    });
  }


  /**
   * Web側に認証レスポンス送信
   */
  private void sendAuthResponseToWeb(String playerName, String playerUuid, boolean success, String message) {
    if (sqsClient == null) {
      logger.warn("SQSクライアントが利用できません。認証レスポンスを送信できません。");
      return;
    }

    try {
      sqsClient.sendAuthResponse(playerName, playerUuid, success, message)
          .thenRun(() -> logger.debug("認証レスポンスを送信しました: {} ({}), success={}", playerName, playerUuid, success))
          .exceptionally(ex -> {
            logger.error("認証レスポンス送信に失敗しました: {} ({})", playerName, playerUuid, ex);
            return null;
          });
    } catch (Exception e) {
      logger.error("認証レスポンス送信でエラーが発生しました: {} ({})", playerName, playerUuid, e);
    }
  }
}
