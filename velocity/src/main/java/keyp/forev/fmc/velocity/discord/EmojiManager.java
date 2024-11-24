package keyp.forev.fmc.velocity.discord;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.imageio.ImageIO;

import org.slf4j.Logger;

import com.google.inject.Inject;

import keyp.forev.fmc.common.Database;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import keyp.forev.fmc.velocity.util.Config;
import keyp.forev.fmc.velocity.util.GeyserMC;

public class EmojiManager {
    public static String beDefaultEmojiId = null;
    private JDA jda = null;
    private String emojiId = null;
    private final Logger logger;
    private final Config config;
    private final Database db;
    @SuppressWarnings("unused")
    private final GeyserMC gm;
    @Inject
    public EmojiManager (Logger logger, Config config, Database db, GeyserMC gm) {
        this.logger = logger;
        this.config = config;
        this.db = db;
        this.gm = gm;
    }

    public void updateDefaultEmojiId() {
        CompletableFuture<String> future = createOrgetEmojiId(config.getString("Discord.BEDefaultEmojiName"));
        future.thenAccept(id -> EmojiManager.beDefaultEmojiId = id);
    }

    public CompletableFuture<Map<String, String>> getEmojiIds(List<String> emojiNames) {
        CompletableFuture<Map<String, String>> future = new CompletableFuture<>();
        if (emojiNames.isEmpty()) {
            future.complete(null);
            return future;
        }

    	this.jda = Discord.jda;
        if (Objects.isNull(jda) || config.getLong("Discord.GuildId", 0) == 0 || emojiNames.isEmpty()) {
        	future.complete(null);
            return future;
        }
    	String guildId = Long.toString(config.getLong("Discord.GuildId"));
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            //logger.info("Guild not found!");
            future.complete(null);
            return future;
        }
        Map<String, String> emojiMap = new HashMap<>();
        List<RichCustomEmoji> emojis = guild.getEmojis();
        for (String eachEmojiName : emojiNames) {
            String eachEmojiId = null;
            for (RichCustomEmoji emoji : emojis) {
                if (emoji.getName().equals(eachEmojiName)) {
                    eachEmojiId = emoji.getId();
                    break;
                }
            }
            emojiMap.put(eachEmojiName, eachEmojiId);
        }
        future.complete(emojiMap);
        return future;
    }

    public CompletableFuture<String> createOrgetEmojiId(String emojiName, String imageUrl) throws URISyntaxException {
    	CompletableFuture<String> future = new CompletableFuture<>();
    	this.jda = Discord.jda;
        if (Objects.isNull(jda) || config.getLong("Discord.GuildId", 0) == 0) {
        	future.complete(null);
            return future;
        }
        // emojiNameが空白かnullだった場合
        if (Objects.isNull(emojiName) || emojiName.isEmpty()) {
        	future.complete(null);
            return future;
        }
        
    	String guildId = Long.toString(config.getLong("Discord.GuildId"));
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            //logger.info("Guild not found!");
            future.complete(null);
            return future;
        }
        // 絵文字が既に存在するかをチェックし、存在する場合はIDを取得
        Optional<RichCustomEmoji> existingEmote = guild.getEmojis().stream()
            .filter(emote -> emote.getName().equals(emojiName))
            .findFirst();
        if (existingEmote.isPresent()) {
            emojiId = existingEmote.get().getId();
            //logger.info(emojiName + "の絵文字はすでに追加されています。");
            //logger.info("Existing Emoji ID: " + emojiId);
            future.complete(emojiId);
        } else {
            // 統合版プレイヤーはdiscordにスキンがあれば、絵文字IDをとってくるようにするが、
            // なければ、steveの絵文字で返す
            if (emojiName.startsWith(".")) {
                return createOrgetEmojiId(config.getString("Discord.BEDefaultEmojiName", null));
            }
        	if (Objects.isNull(imageUrl)) {
        		future.complete(null);
                return future;
        	}
        	try {
                URI uri = new URI(imageUrl);
                URL url = uri.toURL();
            
                BufferedImage bufferedImage = ImageIO.read(url);
                if (Objects.isNull(bufferedImage)) {
                    logger.error("Failed to read image from URL: " + imageUrl);
                    future.complete(null);
                    return future;
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, "png", baos);
                byte[] imageBytes = baos.toByteArray();
                Icon icon = Icon.from(imageBytes);
            
                // Create the emote with the specified name and icon
                AuditableRestAction<RichCustomEmoji> action = guild.createEmoji(emojiName, icon);
                action.queue(
                    success -> {
                        logger.info(emojiName + "を絵文字に追加しました。");
                        emojiId = success.getId(); // 絵文字IDを取得
                        future.complete(emojiId);
                    }, failure -> {
                        logger.error("Failed to create emoji: " + failure.getMessage());
                        future.complete(null);
                    }
                );
            } catch (IOException | URISyntaxException e) {
                logger.error("A createEmoji error occurred: " + e.getMessage());
                for (StackTraceElement element : e.getStackTrace()) {
                    logger.error(element.toString());
                }
                future.complete(null);
            }
        }
        return future;
    }
    
    public String getEmojiString(String emojiName, String emojiId) {
        if (emojiName == null && emojiId == null) return null;
        // 非同期メソッドMineStatusReflect.updateStatusの中で、
        // EmojiManager.getEmojiIdsより取得したemojiMapの中に統合版プレイヤーがいた場合、
        // MineStatusReflect.createStatusEmbedメソッドで、
        //  ".key100011138" -> nullとなることがあるため
        if (emojiName != null && emojiId == null) {
            if (emojiName.startsWith(".")) {
                if (EmojiManager.beDefaultEmojiId != null) {
                    // MineStatusReflect.createStatusEmbedメソッドの簡潔なコードの都合上、最後に空白
                    return "<:" + config.getString("Discord.BEDefaultEmojiName") + ":" + EmojiManager.beDefaultEmojiId + "> "; // steveの絵文字で返す
                } else {
                    return "";
                }
            }
        }
        if (emojiName != null && emojiId != null) {
            if (emojiName.isEmpty() || emojiId.isEmpty()) return null;
            // Join時のAddEmbedSomeMessageメソッドで、絵文字が追加されていない場合に通るので必要
            if (EmojiManager.beDefaultEmojiId instanceof String && emojiId.equals(EmojiManager.beDefaultEmojiId)) {
                if (emojiName.startsWith(".")) {
                    return "<:" + config.getString("Discord.BEDefaultEmojiName") + ":" + EmojiManager.beDefaultEmojiId + ">"; // steveの絵文字で返す
                }
            }
        }
    	return "<:" + emojiName + ":" + emojiId + ">";
    }
    
    public void updateEmojiIdsToDatabase() {
        this.jda = Discord.jda;
        if (Objects.isNull(jda) || config.getLong("Discord.ChannelId", 0) == 0) return;

        MessageChannel channel = jda.getTextChannelById(config.getLong("Discord.ChannelId"));
        if (Objects.isNull(channel)) {
            //logger.info("Channel not found!");
            return;
        }

        Guild guild = jda.getGuilds().get(0); // 最初のギルドを取得（適切なギルドを選択する必要があります）
        String query = "SELECT * FROM members;";
        try (Connection conn = db.getConnection();
            PreparedStatement ps = conn.prepareStatement(query);) {
            try (ResultSet minecrafts = ps.executeQuery()) {
                while (minecrafts.next()) {
                    emojiId = null; // while文の中で、最初にemojiIDを初期化しておく
                    
                    String mineName = minecrafts.getString("name");
                    String uuid = minecrafts.getString("uuid");
                    String dbEmojiId = minecrafts.getString("emid");
                    
                    // 絵文字が既に存在するかをチェックし、存在する場合はIDを取得
                    Optional<RichCustomEmoji> existingEmote = guild.getEmojis().stream()
                        .filter(emote -> emote.getName().equals(mineName))
                        .findFirst();
    
                    if (existingEmote.isPresent()) {
                        emojiId = existingEmote.get().getId();
                        //logger.info(mineName + "の絵文字はすでに追加されています。");
                        //logger.info("Existing Emoji ID: " + emojiId);
                        
                        // もし、emojiIdがminecrafts.getString("emid")と違ったら更新する
                        // データベース保存処理
                        if(Objects.nonNull(emojiId) && !emojiId.equals(dbEmojiId)) {
                            String query2 = "UPDATE members SET emid=? WHERE uuid=?;";
                            try (PreparedStatement ps2 = conn.prepareStatement(query2);) {
                                ps2.setString(1, emojiId);
                                ps2.setString(2, uuid);
                                ps2.executeUpdate();
                            }
                        }
                    } else {
                        String imageUrl = "https://minotar.net/avatar/" + uuid;
                        //logger.info("Downloading image from URL: " + imageUrl); // 画像URLをログに出力
                        try {
                            //logger.info("Downloading image from URL: " + imageUrl);
                            URI uri = new URI(imageUrl);
                            URL url = uri.toURL();
                            BufferedImage bufferedImage = ImageIO.read(url);
                            if (Objects.isNull(bufferedImage)) {
                                logger.error("Failed to read image from URL: " + imageUrl);
                                continue;
                            }
    
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            ImageIO.write(bufferedImage, "png", baos);
                            byte[] imageBytes = baos.toByteArray();
                            Icon icon = Icon.from(imageBytes);
    
                            // Create the emote with the specified name and icon
                            AuditableRestAction<RichCustomEmoji> action = guild.createEmoji(mineName, icon);
                            action.queue(
                                success -> {
                                    logger.info(mineName + "を絵文字に追加しました。");
                                    emojiId = success.getId(); // 絵文字IDを取得
                                    //logger.info("Emoji ID: " + emojiId);
                                }, failure -> logger.error("Failed to create emoji: " + failure.getMessage())
                            );
                            
                            // データベース更新処理
                            if (Objects.nonNull(emojiId)) {
                                String query2 = "UPDATE members SET emid=? WHERE uuid=?;";
                                try (Connection conn2 = db.getConnection();
                                    PreparedStatement ps2 = conn2.prepareStatement(query2);) {
                                    ps2.setString(1, emojiId);
                                    ps2.setString(2, uuid);
                                    ps2.executeUpdate();
                                }
                            }
                        } catch (IOException | URISyntaxException e) {
                            logger.error("Failed to download image: " + e.getMessage());
                            for (StackTraceElement element : e.getStackTrace()) {
                                logger.error(element.toString());
                            }
                        }
                    }
                }
            }
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("A checkAndAddEmojis error occurred: " + e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                logger.error(element.toString());
            }
        }
    }
    
    public CompletableFuture<String> createOrgetEmojiId(String emojiName) {
    	try {
            return createOrgetEmojiId(emojiName, null);
        } catch (URISyntaxException e) {
            logger.error("A URISyntaxException error occurred: " + e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                logger.error(element.toString());
            }
            
            return null;
        }
    }
}
