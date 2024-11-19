package keyp.forev.fmc.velocity.util;

import java.awt.Color;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;

import com.google.inject.Inject;

import keyp.forev.fmc.common.Database;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import keyp.forev.fmc.velocity.discord.EmojiManager;

public class MineStatusReflect {

    private final Logger logger;
    private final Config config;
    private final Database db;
    private final EmojiManager emoji;
    private final Long channelId, messageId;
    private final boolean require;

    @Inject
    public MineStatusReflect(Logger logger, Config config, Database db, EmojiManager emoji) {
        this.logger = logger;
        this.config = config;
        this.db = db;
        this.emoji = emoji;
        this.channelId = config.getLong("Discord.Status.ChannelId", 0);
        this.messageId = config.getLong("Discord.Status.MessageId", 0);
        this.require = channelId != 0 && messageId != 0;
    }

    public void start(JDA jda) {
        if (!require) {
            logger.info("コンフィグの設定が不十分なため、ステータスをUPDATEできません。");
            return;
        }
        Timer timer = new Timer();
        int period = config.getInt("Discord.Status.Period", 20);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                updateStatus(jda);
            }
        }, 0, 1000*period);
    }

    public void sendEmbedMessage(JDA jda) {
        TextChannel channel = jda.getTextChannelById(channelId);
        EmbedBuilder embed = new EmbedBuilder().setTitle("後にこれがステータスとなる").setColor(Color.GREEN);
        if (channel != null) {
            channel.sendMessageEmbeds(embed.build()).queue(
                _p -> {},// logger.info("Embed sent successfully!")
                error -> logger.error("Failed to send embed: " + error.getMessage())
            );
        }
    }

    private void updateStatus(JDA jda) {
        TextChannel channel = jda.getTextChannelById(channelId);
        getPlayersMap().thenCompose(playersMap -> {
            if (playersMap != null) {
                Set<String> uniquePlayersSet = new HashSet<>();
                for (Map.Entry<String, String> entry : playersMap.entrySet()) {
                    String players = entry.getValue();
                    if (players != null && !players.trim().isEmpty()) {
                        String[] playerArray = players.split(",\\s*");
                        uniquePlayersSet.addAll(Arrays.asList(playerArray));
                    }
                }
                List<String> uniquePlayersList = new ArrayList<>(uniquePlayersSet);
                // 例えば、"home"->nullとかであれば、getEmojiIdsはnullを返す
                return emoji.getEmojiIds(uniquePlayersList).thenApply(emojiIds -> {
                    return createStatusEmbed(channel, playersMap, emojiIds);
                });
            }
            return CompletableFuture.completedFuture(null);
        }).thenAccept(statusEmbedFuture -> {
            if (statusEmbedFuture != null) {
                statusEmbedFuture.thenAccept(statusEmbed -> {
                    if (channel != null) {
                        Message message = channel.retrieveMessageById(messageId).complete();
                        message.editMessageEmbeds(statusEmbed).queue(
                            _p -> {},// logger.info("Embed updated successfully!")
                            error -> logger.error("Failed to update embed: " + error.getMessage())
                        );
                    }
                });
            }
        }).exceptionally(error -> {
            logger.error("Failed to update status: " + error.getMessage());
            return null;
        });
    }

    public CompletableFuture<Map<String, String>> getPlayersMap() {
        CompletableFuture<Map<String, String>> future = new CompletableFuture<>();
        String query = "SELECT * FROM status";
        try (Connection conn = db.getConnection();
            PreparedStatement ps = conn.prepareStatement(query)) {
            try (ResultSet rs = ps.executeQuery()) {
                Map<String, String> playersMap = new HashMap<>();
                while (rs.next()) {
                    String serverName = rs.getString("name"),
                        players = rs.getString("player_list");
                    boolean online = rs.getBoolean("online");
                    if (online) {
                        playersMap.put(serverName, players);
                    }
                }
                future.complete(playersMap);
            }
        } catch (SQLException | ClassNotFoundException e) {
            logger.info("MySQLサーバーに再接続を試みています。");
            future.completeExceptionally(e);
            // これが、exceptionallyにいくなら、exceptionallyで、
            // discordメッセージ編集で、「MySQLサーバーにアクセスできません」と出せば、エラー発見しやすい
        }
        return future;
    }

    public CompletableFuture<MessageEmbed> createStatusEmbed(TextChannel channel, Map<String, String> playersMap, Map<String, String> emojiMap) {
        CompletableFuture<MessageEmbed> future = new CompletableFuture<>();
        EmbedBuilder embed = new EmbedBuilder();
        boolean maintenance = false, isOnline = false;
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        String formattedNow = now.format(formatter);
        embed.setFooter("最終更新日時: " + formattedNow, null);
        for (Map.Entry<String, String> entry : playersMap.entrySet()) {
            isOnline = true;
            String serverName = entry.getKey(),
                players = entry.getValue();
            List<String> playersList = new ArrayList<>();
            if (serverName.equals("maintenance")) {
                maintenance = true;
                continue;
            }
            if (serverName.equals("proxy")) {
                continue;
            }
            if (players != null && !players.trim().isEmpty()) {
                String[] playerArray = players.split(",\\s*");
                playersList.addAll(Arrays.asList(playerArray));
            }
            int currentPlayers = playersList.size();
            //if (index != 0) embed.addField("", "", false);
            if (!playersList.isEmpty()) {
                playersList.sort(String.CASE_INSENSITIVE_ORDER);
                List<String> playersListWithEmoji = new ArrayList<>();
                for (int i = 0; i < playersList.size(); i++) {
                    String playerName = playersList.get(i);
                    String emojiId = emojiMap.get(playerName);
                    String emojiString = emoji.getEmojiString(playerName, emojiId);
                    playersListWithEmoji.add(emojiString + playerName);
                }
                String playersWithEmoji = String.join("\n\n", playersListWithEmoji);
                embed.addField(":green_circle: " + serverName + "  " +  currentPlayers + "/10", playersWithEmoji, false);
            } else {
                embed.addField(":green_circle: " + serverName + "  0/10", "No Player", false);
            }
        }
        if (maintenance) {
            embed.setTitle(":tools: 現在サーバーメンテナンス中");
            embed.setColor(Color.ORANGE);
        } else if (!isOnline) {
            embed.setTitle(":red_circle: すべてのサーバーがオフライン");
            embed.setColor(Color.RED);
        } else {
            embed.setTitle(":white_check_mark: 現在サーバー開放中");
            embed.setColor(Color.GREEN);
        }
        future.complete(embed.build());
        return future;
    }
}
