package net.kishax.discord;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
// SQS Message import - JDA Message conflicts are handled with full class names
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SQSメッセージプロセッサー
 * SQSからメッセージを受信してDiscordに送信
 */
public class SqsMessageProcessor {
  private static final Logger logger = LoggerFactory.getLogger(SqsMessageProcessor.class);

  private final SqsClient sqsClient;
  private final JDA jda;
  private final Config config;
  private final ObjectMapper objectMapper;
  private final ScheduledExecutorService executor;
  private final AtomicBoolean running = new AtomicBoolean(false);
  private final EmojiManager emojiManager;
  private final MessageIdManager messageIdManager;

  public SqsMessageProcessor(SqsClient sqsClient, JDA jda, Config config) {
    this.sqsClient = sqsClient;
    this.jda = jda;
    this.config = config;
    this.objectMapper = new ObjectMapper();
    this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, "SQS-Processor");
      t.setDaemon(true);
      return t;
    });
    this.emojiManager = new EmojiManager(jda, config);
    this.messageIdManager = new MessageIdManager();

    // デフォルト絵文字IDを初期化
    emojiManager.updateDefaultEmojiId();
  }

  public void start() {
    if (running.compareAndSet(false, true)) {
      logger.info("SQSメッセージプロセッサーを開始します");
      executor.scheduleWithFixedDelay(this::pollMessages, 0, 5, TimeUnit.SECONDS);
    }
  }

  public void stop() {
    if (running.compareAndSet(true, false)) {
      logger.info("SQSメッセージプロセッサーを停止します");
      executor.shutdown();
      try {
        if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
          executor.shutdownNow();
        }
      } catch (InterruptedException e) {
        executor.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }
  }

  private void pollMessages() {
    if (!running.get()) {
      return;
    }

    try {
      ReceiveMessageRequest request = ReceiveMessageRequest.builder()
          .queueUrl(config.getSqsQueueUrl())
          .maxNumberOfMessages(config.getSqsMaxMessages())
          .waitTimeSeconds(config.getSqsWaitTimeSeconds())
          .build();

      ReceiveMessageResponse response = sqsClient.receiveMessage(request);
      List<software.amazon.awssdk.services.sqs.model.Message> messages = response.messages();

      if (!messages.isEmpty()) {
        logger.debug("SQSメッセージを受信しました: {} 件", messages.size());
      }

      for (software.amazon.awssdk.services.sqs.model.Message message : messages) {
        try {
          processMessage(message);
          deleteMessage(message);
        } catch (Exception e) {
          logger.error("メッセージ処理でエラーが発生しました: {}", message.messageId(), e);
        }
      }

    } catch (Exception e) {
      logger.error("SQSポーリングでエラーが発生しました", e);
    }
  }

  private void processMessage(software.amazon.awssdk.services.sqs.model.Message message) throws Exception {
    String body = message.body();
    logger.debug("メッセージ処理開始: {}", message.messageId());

    JsonNode json = objectMapper.readTree(body);
    String messageType = json.path("type").asText();

    switch (messageType) {
      case "server_status" -> processServerStatusMessage(json);
      case "player_request" -> processPlayerRequestMessage(json);
      case "broadcast" -> processBroadcastMessage(json);
      case "embed" -> processEmbedMessage(json);
      case "player_event" -> processPlayerEventMessage(json);
      case "webhook" -> processWebhookMessage(json);
      default -> logger.warn("不明なメッセージタイプです: {}", messageType);
    }
  }

  private void processServerStatusMessage(JsonNode json) {
    String serverName = json.path("serverName").asText();
    String status = json.path("status").asText(); // online, offline, starting

    TextChannel channel = jda.getTextChannelById(config.getDiscordChannelId());
    if (channel != null) {
      String emoji = switch (status) {
        case "online" -> "🟢";
        case "offline" -> "🔴";
        case "starting" -> "🟡";
        default -> "⚪";
      };

      channel.sendMessage(emoji + " **" + serverName + "** サーバーが " +
          switch (status) {
            case "online" -> "オンラインになりました";
            case "offline" -> "オフラインになりました";
            case "starting" -> "起動中です";
            default -> "状態が変更されました: " + status;
          }).queue();
    }

    logger.info("サーバーステータス更新: {} = {}", serverName, status);
  }

  private void processPlayerRequestMessage(JsonNode json) {
    String playerName = json.path("playerName").asText();
    String serverName = json.path("serverName").asText();
    String requestId = json.path("requestId").asText();

    TextChannel adminChannel = jda.getTextChannelById(config.getDiscordAdminChannelId());
    if (adminChannel != null) {
      String message = "**サーバー起動リクエスト**\\n" +
          "プレイヤー: " + playerName + "\\n" +
          "サーバー: " + serverName + "\\n" +
          "リクエストID: " + requestId;

      adminChannel.sendMessage(message)
          .addActionRow(
              net.dv8tion.jda.api.interactions.components.buttons.Button.success("reqOK", "承認"),
              net.dv8tion.jda.api.interactions.components.buttons.Button.danger("reqCancel", "拒否"))
          .queue();
    }

    logger.info("プレイヤーリクエスト受信: {} が {} サーバーの起動をリクエスト", playerName, serverName);
  }

  private void processBroadcastMessage(JsonNode json) {
    String content = json.path("content").asText();
    boolean isChat = json.path("isChat").asBoolean(false);

    String channelId = isChat ? config.getDiscordChatChannelId() : config.getDiscordChannelId();
    TextChannel channel = jda.getTextChannelById(channelId);

    if (channel != null) {
      channel.sendMessage(content).queue();
    }

    logger.info("ブロードキャストメッセージ送信: {} (chat={})", content, isChat);
  }

  private void processEmbedMessage(JsonNode json) {
    String content = json.path("content").asText();
    int color = json.path("color").asInt(ColorUtil.GREEN.getRGB());
    String channelId = json.path("channelId").asText(config.getDiscordChannelId());
    String messageId = json.path("messageId").asText("");
    boolean shouldEdit = json.path("edit").asBoolean(false);

    TextChannel channel = jda.getTextChannelById(channelId);
    if (channel == null) {
      logger.warn("チャンネルが見つかりません: {}", channelId);
      return;
    }

    EmbedBuilder embed = new EmbedBuilder()
        .setDescription(content)
        .setColor(color);

    if (shouldEdit && !messageId.isEmpty()) {
      // メッセージ編集
      channel.editMessageEmbedsById(messageId, embed.build()).queue(
          success -> logger.debug("Embedメッセージを編集しました: {}", messageId),
          error -> logger.error("Embedメッセージの編集に失敗しました: {}", messageId, error));
    } else {
      // 新規送信
      channel.sendMessageEmbeds(embed.build()).queue(
          message -> {
            logger.debug("Embedメッセージを送信しました: {}", message.getId());
            // メッセージIDを保存（必要に応じて）
          },
          error -> logger.error("Embedメッセージの送信に失敗しました", error));
    }
  }

  private void processPlayerEventMessage(JsonNode json) {
    String eventType = json.path("eventType").asText();
    String playerName = json.path("playerName").asText();
    String playerUuid = json.path("playerUuid").asText();
    String serverName = json.path("serverName").asText("");

    switch (eventType) {
      case "join" -> processPlayerJoin(playerName, playerUuid, serverName);
      case "leave" -> processPlayerLeave(playerName, playerUuid, serverName);
      case "move" -> processPlayerMove(playerName, playerUuid, serverName);
      case "chat" -> processPlayerChat(json);
      default -> logger.warn("不明なプレイヤーイベントタイプ: {}", eventType);
    }
  }

  private void processPlayerJoin(String playerName, String playerUuid, String serverName) {
    emojiManager.createOrGetEmojiId(playerName, "https://minotar.net/avatar/" + playerUuid)
        .thenAccept(emojiId -> {
          String emojiString = emojiManager.getEmojiString(playerName, emojiId);
          String content = (emojiString != null ? emojiString : "") + playerName + " が " + serverName + " サーバーに参加しました";

          EmbedBuilder embed = new EmbedBuilder()
              .setDescription(content)
              .setColor(ColorUtil.GREEN.getRGB());

          TextChannel channel = jda.getTextChannelById(config.getDiscordChannelId());
          if (channel != null) {
            channel.sendMessageEmbeds(embed.build()).queue(
                message -> messageIdManager.putPlayerMessageId(playerUuid, message.getId()));
          }
        });
  }

  private void processPlayerLeave(String playerName, String playerUuid, String serverName) {
    String messageId = messageIdManager.getPlayerMessageId(playerUuid);

    emojiManager.createOrGetEmojiId(playerName, "https://minotar.net/avatar/" + playerUuid)
        .thenAccept(emojiId -> {
          String emojiString = emojiManager.getEmojiString(playerName, emojiId);
          String content = (emojiString != null ? emojiString : "") + playerName + " が " + serverName + " サーバーから退出しました";

          if (messageId != null) {
            // 既存メッセージを編集
            TextChannel channel = jda.getTextChannelById(config.getDiscordChannelId());
            if (channel != null) {
              EmbedBuilder embed = new EmbedBuilder()
                  .setDescription(content)
                  .setColor(ColorUtil.RED.getRGB());

              channel.editMessageEmbedsById(messageId, embed.build()).queue();
              messageIdManager.removePlayerMessageId(playerUuid);
            }
          }
        });
  }

  private void processPlayerMove(String playerName, String playerUuid, String serverName) {
    String messageId = messageIdManager.getPlayerMessageId(playerUuid);

    emojiManager.createOrGetEmojiId(playerName, "https://minotar.net/avatar/" + playerUuid)
        .thenAccept(emojiId -> {
          String emojiString = emojiManager.getEmojiString(playerName, emojiId);
          String content = (emojiString != null ? emojiString : "") + playerName + " が " + serverName + " サーバーへ移動しました";

          EmbedBuilder embed = new EmbedBuilder()
              .setDescription(content)
              .setColor(ColorUtil.BLUE.getRGB());

          if (messageId != null) {
            // 既存メッセージを編集
            TextChannel channel = jda.getTextChannelById(config.getDiscordChannelId());
            if (channel != null) {
              channel.editMessageEmbedsById(messageId, embed.build()).queue();
            }
          } else {
            // 新規メッセージ
            TextChannel channel = jda.getTextChannelById(config.getDiscordChannelId());
            if (channel != null) {
              channel.sendMessageEmbeds(embed.build()).queue(
                  message -> messageIdManager.putPlayerMessageId(playerUuid, message.getId()));
            }
          }
        });
  }

  private void processPlayerChat(JsonNode json) {
    String playerName = json.path("playerName").asText();
    String playerUuid = json.path("playerUuid").asText();
    String chatMessage = json.path("message").asText();

    String chatMessageId = messageIdManager.getChatMessageId();

    emojiManager.createOrGetEmojiId(playerName, "https://minotar.net/avatar/" + playerUuid)
        .thenAccept(emojiId -> {
          String emojiString = emojiManager.getEmojiString(playerName, emojiId);
          String content = "<" + (emojiString != null ? emojiString : "") + playerName + "> " + chatMessage;

          EmbedBuilder embed = new EmbedBuilder()
              .setDescription(content)
              .setColor(ColorUtil.GREEN.getRGB());

          TextChannel chatChannel = jda.getTextChannelById(config.getDiscordChatChannelId());
          if (chatChannel != null) {
            if (chatMessageId != null) {
              // 既存チャットメッセージを編集
              chatChannel.editMessageEmbedsById(chatMessageId, embed.build()).queue();
            } else {
              // 新規チャットメッセージ
              chatChannel.sendMessageEmbeds(embed.build()).queue(
                  message -> messageIdManager.setChatMessageId(message.getId()));
            }
          }
        });
  }

  private void processWebhookMessage(JsonNode json) {
    String userName = json.path("userName").asText();
    String avatarUrl = json.path("avatarUrl").asText();
    String content = json.path("content").asText();

    // Webhookの実装はDiscordEventListenerで処理されるため、ここでは基本的な送信のみ
    TextChannel chatChannel = jda.getTextChannelById(config.getDiscordChatChannelId());
    if (chatChannel != null) {
      chatChannel.sendMessage(content).queue();
      logger.info("Webhookメッセージを送信しました: {}", userName);
    }
  }

  private void deleteMessage(software.amazon.awssdk.services.sqs.model.Message message) {
    try {
      DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
          .queueUrl(config.getSqsQueueUrl())
          .receiptHandle(message.receiptHandle())
          .build();

      sqsClient.deleteMessage(deleteRequest);
      logger.debug("メッセージを削除しました: {}", message.messageId());
    } catch (Exception e) {
      logger.error("メッセージ削除でエラーが発生しました: {}", message.messageId(), e);
    }
  }
}
