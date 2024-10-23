package velocity;

import java.awt.Color;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;

import com.google.inject.Inject;

import discord.EmojiManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;

public class MineStatusReflect {

    private final Logger logger;
    private final Config config;
    private final DatabaseInterface db;
    private final EmojiManager emoji;
    private final Long channelId, messageId;
    private final boolean require;

    @Inject
    public MineStatusReflect(Logger logger, Config config, DatabaseInterface db, EmojiManager emoji) {
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
                success -> logger.info("Embed sent successfully!"),
                error -> logger.error("Failed to send embed: " + error.getMessage())
            );
        }
    }

    private void updateStatus(JDA jda) {
        TextChannel channel = jda.getTextChannelById(channelId);
        createStatusEmbed(channel).thenAccept(statusEmbed -> {
            if (channel != null && statusEmbed != null) {
                Message message = channel.retrieveMessageById(messageId).complete();
                message.editMessageEmbeds(statusEmbed.build()).queue(
                    success -> logger.info("Embed updated successfully!"),
                    error -> logger.error("Failed to update embed: " + error.getMessage())
                );
            }
        }).exceptionally(error -> {
            logger.error("Failed to create status embed: " + error.getMessage());
            return null;
        });
    }

    public CompletableFuture<EmbedBuilder> createStatusEmbed(TextChannel channel) {
        CompletableFuture<EmbedBuilder> future = new CompletableFuture<>();
        EmbedBuilder embed = new EmbedBuilder();
        String query = "SELECT * FROM status";
        try (Connection conn = db.getConnection();
            PreparedStatement ps = conn.prepareStatement(query)) {
            try (ResultSet rs = ps.executeQuery()) {
                boolean maintenance = false;
                boolean isOnline = false;
                while (rs.next()) {
                    String serverName = rs.getString("name"),
                        players = rs.getString("player_list");
                    int currentPlayers = rs.getInt("current_players");
                    boolean online = rs.getBoolean("online");
                    List<String> playersList = new ArrayList<>();
                    if (serverName.equals("maintenance") && online) {
                        maintenance = true;
                        break;
                    }
                    if (online) {
                        isOnline = true;
                        if (players != null && !players.trim().isEmpty()) {
                            String[] playerArray = players.split(",\\s*");
                            playersList.addAll(Arrays.asList(playerArray));
                        }
                        if (!playersList.isEmpty()) {
                            playersList.sort(String.CASE_INSENSITIVE_ORDER);
                            List<String> playersListWithEmoji = new ArrayList<>();
                            emoji.getEmojiIds(playersList).thenAccept(emojiIds -> {
                                for (int i = 0; i < playersList.size(); i++) {
                                    String emojiId = emojiIds.get(i);
                                    if (emojiId != null) {
                                        String playerName = playersList.get(i);
                                        String emojiString = emoji.getEmojiString(playerName, emojiId);
                                        playersListWithEmoji.add(emojiString + " " + playerName);
                                    } else {
                                        playersListWithEmoji.add(playersList.get(i));
                                    }
                                }
                            }).thenRun(() -> {
                                String playersWithEmoji = String.join("\n   ", playersListWithEmoji);
                                embed.addField(":green_circle: " + serverName, currentPlayers + "/10: " + playersWithEmoji, false);
                            });
                        } else {
                            embed.addField(":green_circle: " + serverName, "0/10: No Player", false);
                        }
                    }
                }
                if (maintenance) {
                    embed.setTitle(":red_circle: 現在サーバーメンテナンス中");
                    embed.setColor(Color.RED);
                } else if (!isOnline) {
                    embed.setTitle(":red_circle: すべてのサーバーがオフライン");
                    embed.setColor(Color.RED);
                } else {
                    embed.setColor(Color.GREEN);
                }
            }
        } catch (SQLException | ErrorResponseException | ClassNotFoundException e) {
            logger.info("MySQLサーバーに再接続を試みています。");
        }
        future.complete(null);
        return future;
    }
}
