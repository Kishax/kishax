package net.kishax.mc.velocity.discord;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

/**
 * Discord→MC ブロードキャストメッセージをRedisから受信し、全プレイヤーに配信するリスナー
 */
public class RedisBroadcastListener {
  private static final Logger logger = LoggerFactory.getLogger(RedisBroadcastListener.class);
  private static final String MC_BROADCAST_CHANNEL = "mc_broadcast";

  private final JedisPool jedisPool;
  private final ProxyServer server;
  private final ObjectMapper objectMapper;
  private Thread subscriberThread;
  private volatile boolean running = false;

  public RedisBroadcastListener(JedisPool jedisPool, ProxyServer server) {
    this.jedisPool = jedisPool;
    this.server = server;
    this.objectMapper = new ObjectMapper();
  }

  /**
   * Redisサブスクリプションを開始
   */
  public void start() {
    if (running) {
      logger.warn("RedisBroadcastListener is already running");
      return;
    }

    running = true;
    subscriberThread = new Thread(() -> {
      logger.info("Starting Redis subscription to channel: {}", MC_BROADCAST_CHANNEL);

      try (Jedis jedis = jedisPool.getResource()) {
        jedis.subscribe(new JedisPubSub() {
          @Override
          public void onMessage(String channel, String message) {
            if (!MC_BROADCAST_CHANNEL.equals(channel)) {
              return;
            }

            try {
              // JSONメッセージをパース
              JsonNode messageJson = objectMapper.readTree(message);
              String type = messageJson.get("type").asText();

              if ("discord_broadcast".equals(type)) {
                String author = messageJson.get("author").asText();
                String content = messageJson.get("content").asText();

                // 全プレイヤーにブロードキャスト
                broadcastToAllPlayers(author, content);
              }
            } catch (Exception e) {
              logger.error("Failed to process Redis broadcast message", e);
            }
          }

          @Override
          public void onSubscribe(String channel, int subscribedChannels) {
            logger.info("Subscribed to Redis channel: {}", channel);
          }

          @Override
          public void onUnsubscribe(String channel, int subscribedChannels) {
            logger.info("Unsubscribed from Redis channel: {}", channel);
          }
        }, MC_BROADCAST_CHANNEL);
      } catch (Exception e) {
        if (running) {
          logger.error("Redis subscription error", e);
        }
      }

      logger.info("Redis subscription stopped");
    }, "RedisBroadcastListener-Thread");

    subscriberThread.start();
    logger.info("RedisBroadcastListener started");
  }

  /**
   * Redisサブスクリプションを停止
   */
  public void stop() {
    if (!running) {
      return;
    }

    logger.info("Stopping RedisBroadcastListener...");
    running = false;

    if (subscriberThread != null && subscriberThread.isAlive()) {
      subscriberThread.interrupt();
      try {
        subscriberThread.join(5000); // 5秒待機
      } catch (InterruptedException e) {
        logger.warn("Interrupted while waiting for subscriber thread to stop", e);
        Thread.currentThread().interrupt();
      }
    }

    logger.info("RedisBroadcastListener stopped");
  }

  /**
   * 全プレイヤーにDiscordメッセージをブロードキャスト
   *
   * @param author  Discordメッセージの送信者名
   * @param content メッセージ内容
   */
  private void broadcastToAllPlayers(String author, String content) {
    Component message = Component.text()
        .append(Component.text("[Discord] ", NamedTextColor.BLUE))
        .append(Component.text("<", NamedTextColor.GRAY))
        .append(Component.text(author, NamedTextColor.AQUA))
        .append(Component.text("> ", NamedTextColor.GRAY))
        .append(Component.text(content, NamedTextColor.WHITE))
        .build();

    server.getAllPlayers().forEach(player -> player.sendMessage(message));

    logger.info("Broadcasted Discord message from {} to {} players", author, server.getAllPlayers().size());
  }
}
