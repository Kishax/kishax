package keyp.forev.fmc.velocity.discord;

import java.awt.Color;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
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
import java.util.function.Consumer;

import org.slf4j.Logger;

import keyp.forev.fmc.common.database.Database;
import keyp.forev.fmc.velocity.libs.VClassManager;
import keyp.forev.fmc.velocity.util.config.VelocityConfig;

public class MineStatusReflect {
    private final Logger logger;
    private final VelocityConfig config;
    private final Database db;
    private final EmojiManager emoji;
    private final Class<?> embedBuilderClazz, entityMessageClazz;
    private final Long channelId, messageId;
    private final boolean require;

    public MineStatusReflect(Logger logger, VelocityConfig config, Database db, EmojiManager emoji) throws ClassNotFoundException {
        this.logger = logger;
        this.config = config;
        this.db = db;
        this.emoji = emoji;
        this.channelId = config.getLong("Discord.Status.ChannelId", 0);
        this.messageId = config.getLong("Discord.Status.MessageId", 0);
        this.require = channelId != 0 && messageId != 0;
        this.embedBuilderClazz = VClassManager.JDA.EMBED_BUILDER.get().getClazz();
        this.entityMessageClazz = VClassManager.JDA.ENTITYS_MESSAGE.get().getClazz();
    }

    public void start(Object jdaInstace) {
        if (!require) {
            logger.info("コンフィグの設定が不十分なため、ステータスをUPDATEできません。");
            return;
        }
        Timer timer = new Timer();
        int period = config.getInt("Discord.Status.Period", 20);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    updateStatus();
                } catch (Exception e) {
                    logger.error("Failed to update status: " + e.getMessage());
                }
            }
        }, 0, 1000*period);
    }

    private void updateStatus() throws Exception {
        Method getTextChannelById = Discord.jdaInstance.getClass().getMethod("getTextChannelById", long.class);
        Object channel = getTextChannelById.invoke(Discord.jdaInstance, channelId);
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
                try {
                    return emoji.getEmojiIds(uniquePlayersList).thenApply(emojiIds -> {
                        try {
                            return createStatusEmbed(channel, playersMap, emojiIds);
                        } catch (Exception e) {
                            logger.error("Failed to create status embed: " + e.getMessage());
                            return null;
                        }
                    });
                } catch (Exception e) {
                    logger.error("Failed to update status: " + e.getMessage());
                }
            }
            return CompletableFuture.completedFuture(null);
        }).thenAccept(statusEmbedFuture -> {
            if (statusEmbedFuture != null) {
                statusEmbedFuture.thenAccept(statusEmbed -> {
                    if (channel != null) {
                        try {
                            Method retrieveMessageById = channel.getClass().getMethod("retrieveMessageById", long.class);
                            Object message = retrieveMessageById.invoke(channel, messageId);
                            Method editMessageEmbeds = message.getClass().getMethod("editMessageEmbeds", entityMessageClazz);
                            editMessageEmbeds.invoke(message, statusEmbed);
                            Method queue = editMessageEmbeds.getClass().getMethod("queue", Consumer.class, Consumer.class);
                            queue.invoke(editMessageEmbeds, (Consumer<Object>) _p -> {}, (Consumer<Throwable>) error -> logger.error("Failed to update embed: " + error.getMessage()));
                        } catch (Exception e) {
                            logger.error("Failed to update embed: " + e.getMessage());
                            for (StackTraceElement ste : e.getStackTrace()) {
                                logger.error(ste.toString());
                            }
                        }
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
        }
        return future;
    }

    public CompletableFuture<Object> createStatusEmbed(Object channel, Map<String, String> playersMap, Map<String, String> emojiMap) throws Exception {
        CompletableFuture<Object> future = new CompletableFuture<>();
        Constructor<?> embedBuilderC = embedBuilderClazz.getConstructor();
        Object embed = embedBuilderC.newInstance();

        boolean maintenance = false, isOnline = false;
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        String formattedNow = now.format(formatter);

        Method setTitle = embedBuilderClazz.getMethod("setTitle", String.class);
        Method setFooter = embedBuilderClazz.getMethod("setFooter", String.class, String.class);
        Method addField = embedBuilderClazz.getMethod("addField", String.class, String.class, boolean.class);
        Method setColor = embedBuilderClazz.getMethod("setColor", Color.class);
        Method build = embedBuilderClazz.getMethod("build");
        setFooter.invoke(embed, "最終更新日時: " + formattedNow, null);

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
                addField.invoke(embed, ":green_circle: " + serverName + "  " +  currentPlayers + "/10", playersWithEmoji, false);
            } else {
                addField.invoke(embed, ":green_circle: " + serverName + "  0/10", "No Player", false);
            }
        }
        if (maintenance) {
            setTitle.invoke(embed, ":tools: 現在サーバーメンテナンス中");
            setColor.invoke(embed, Color.ORANGE);
        } else if (!isOnline) {
            setTitle.invoke(embed, ":red_circle: すべてのサーバーがオフライン");
            setColor.invoke(embed, Color.RED);
        } else {
            setTitle.invoke(embed, ":white_check_mark: 現在サーバー開放中");
            setColor.invoke(embed, Color.GREEN);
        }
        future.complete(build.invoke(embed));
        return future;
    }
}
