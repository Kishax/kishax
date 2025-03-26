package keyp.forev.fmc.velocity.discord;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import javax.imageio.ImageIO;

import org.slf4j.Logger;

import com.google.inject.Inject;

import keyp.forev.fmc.common.database.Database;
import keyp.forev.fmc.velocity.libs.VClassManager;
import keyp.forev.fmc.velocity.util.config.VelocityConfig;

public class EmojiManager {
  public static String beDefaultEmojiId = null;
  private final Logger logger;
  private final VelocityConfig config;
  private final Database db;
  private final Class<?> iconClazz, roleClazz;

  @Inject
  public EmojiManager(Logger logger, VelocityConfig config, Database db) throws ClassNotFoundException {
    this.logger = logger;
    this.config = config;
    this.db = db;
    this.iconClazz = VClassManager.JDA.ENTITYS_ICON.get().getClazz();
    this.roleClazz = VClassManager.JDA.ROLE.get().getClazz();
  }

  public void updateDefaultEmojiId() throws Exception {
    CompletableFuture<String> future = createOrgetEmojiId(config.getString("Discord.BEDefaultEmojiName"));
    future.thenAccept(id -> EmojiManager.beDefaultEmojiId = id);
  }

  public CompletableFuture<Map<String, String>> getEmojiIds(List<String> emojiNames) throws Exception {
    CompletableFuture<Map<String, String>> future = new CompletableFuture<>();
    if (emojiNames.isEmpty()) {
      future.complete(null);
      return future;
    }

    if (Discord.jdaInstance == null || config.getLong("Discord.GuildId", 0) == 0 || emojiNames.isEmpty()) {
      future.complete(null);
      return future;
    }

    String guildId = Long.toString(config.getLong("Discord.GuildId"));

    Method getGuildById = Discord.jdaInstance.getClass().getMethod("getGuildById", String.class);
    Object guild = getGuildById.invoke(Discord.jdaInstance, guildId);

    if (guild == null) {
      future.complete(null);
      return future;
    }

    Map<String, String> emojiMap = new HashMap<>();

    Method getEmojis = guild.getClass().getMethod("getEmojis");
    List<?> emojis = (List<?>) getEmojis.invoke(guild);

    for (String eachEmojiName : emojiNames) {
      String eachEmojiId = null;
      for (Object emoji : emojis) {
        try {
          Method getName = emoji.getClass().getMethod("getName");
          String emojiName = (String) getName.invoke(emoji);

          if (emojiName.equals(eachEmojiName)) {
            Method getId = emoji.getClass().getMethod("getId");
            eachEmojiId = getId.invoke(emoji).toString();
            break;
          }
        } catch (Exception e) {
          logger.error("An error occurred while getting emoji ID: " + e.getMessage());
          for (StackTraceElement element : e.getStackTrace()) {
            logger.error(element.toString());
          }
        }
      }
      emojiMap.put(eachEmojiName, eachEmojiId);
    }
    future.complete(emojiMap);
    return future;
  }

  public CompletableFuture<String> createOrgetEmojiId(String emojiName, String imageUrl) throws Exception {
    CompletableFuture<String> future = new CompletableFuture<>();
    if (Discord.jdaInstance == null || config.getLong("Discord.GuildId", 0) == 0) {
      future.complete(null);
      return future;
    }

    if (emojiName == null || emojiName.isEmpty()) {
      future.complete(null);
      return future;
    }

    String guildId = Long.toString(config.getLong("Discord.GuildId"));
    //Guild guild = jda.getGuildById(guildId);
    Method getGuildById = Discord.jdaInstance.getClass().getMethod("getGuildById", String.class);
    Object guild = getGuildById.invoke(Discord.jdaInstance, guildId);

    if (guild == null) {
      future.complete(null);
      return future;
    }

    Method getEmojis = guild.getClass().getMethod("getEmojis");
    List<?> emojis = (List<?>) getEmojis.invoke(guild);

    Optional<?> existingEmote = emojis.stream()
      .filter(emote -> {
        try {
          Method getName = emote.getClass().getMethod("getName");
          String existedEmojiName = (String) getName.invoke(emote);
          return existedEmojiName.equals(emojiName);
        } catch (Exception e) {
          logger.error("An error occurred while getting emoji ID: " + e.getMessage());
          for (StackTraceElement element : e.getStackTrace()) {
            logger.error(element.toString());
          }
          return false;
        }
      })
    .findFirst();

    if (existingEmote.isPresent()) {
      Object emojiInfo = existingEmote.get();
      Method getId = emojiInfo.getClass().getMethod("getId");
      future.complete(getId.invoke(emojiInfo).toString());
    } else {
      if (emojiName.startsWith(".")) {
        return createOrgetEmojiId(config.getString("Discord.BEDefaultEmojiName", null));
      }
      if (imageUrl == null) {
        future.complete(null);
        return future;
      }
      try {
        URI uri = new URI(imageUrl);
        URL url = uri.toURL();

        BufferedImage bufferedImage = ImageIO.read(url);
        if (bufferedImage == null) {
          logger.error("Failed to read image from URL: " + imageUrl);
          future.complete(null);
          return future;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", baos);
        byte[] imageBytes = baos.toByteArray();
        Object icon = iconClazz.getMethod("from", byte[].class).invoke(null, imageBytes);

        Object roleArray = Array.newInstance(roleClazz, 0);
        Method createEmoji = guild.getClass().getMethod("createEmoji", String.class, iconClazz, roleArray.getClass());
        Object action = createEmoji.invoke(guild, emojiName, icon, roleArray);

        Method queue = action.getClass().getMethod("queue", Consumer.class, Consumer.class);
        queue.invoke(action, new Object[] {
          (Consumer<Object>) success -> {
            logger.info(emojiName + "を絵文字に追加しました。");
            try {
              Method getId = success.getClass().getMethod("getId");
              future.complete(getId.invoke(success).toString());
            } catch (Exception e) {
              logger.error("An error occurred while getting emoji ID: " + e.getMessage());
              for (StackTraceElement element : e.getStackTrace()) {
                logger.error(element.toString());
              }
              future.complete(null);
            }
          }, 
          (Consumer<Throwable>) failure -> {
            logger.error("Failed to create emoji: " + failure.getMessage());
            future.complete(null);
          }
        });
      } catch (IOException e) {
        logger.error("Failed to download image: " + e.getMessage());
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
    if (emojiName != null && emojiId == null) {
      if (emojiName.startsWith(".")) {
        if (EmojiManager.beDefaultEmojiId != null) {
          return "<:" + config.getString("Discord.BEDefaultEmojiName") + ":" + EmojiManager.beDefaultEmojiId + "> "; // steveの絵文字で返す
        } else {
          return "";
        }
      }
    }
    if (emojiName != null && emojiId != null) {
      if (emojiName.isEmpty() || emojiId.isEmpty()) return null;

      if (EmojiManager.beDefaultEmojiId instanceof String && emojiId.equals(EmojiManager.beDefaultEmojiId)) {
        if (emojiName.startsWith(".")) {
          return "<:" + config.getString("Discord.BEDefaultEmojiName") + ":" + EmojiManager.beDefaultEmojiId + ">"; // steveの絵文字で返す
        }
      }
    }
    return "<:" + emojiName + ":" + emojiId + ">";
  }

  public void updateEmojiIdsToDatabase() throws Exception {
    if (Discord.jdaInstance == null || config.getLong("Discord.ChannelId", 0) == 0) return;

    Method getTextChannelById = Discord.jdaInstance.getClass().getMethod("getTextChannelById", long.class);
    Object channel = getTextChannelById.invoke(Discord.jdaInstance, config.getLong("Discord.ChannelId"));

    if (channel == null) return;

    Method getGuild = Discord.jdaInstance.getClass().getMethod("getGuilds");
    List<?> guilds = (List<?>) getGuild.invoke(Discord.jdaInstance);
    Object guild = guilds.get(0);
    String query = "SELECT * FROM members;";
    try (Connection conn = db.getConnection();
        PreparedStatement ps = conn.prepareStatement(query);) {
      try (ResultSet minecrafts = ps.executeQuery()) {
        String emojiId = null;
        while (minecrafts.next()) {
          emojiId = null;

          String mineName = minecrafts.getString("name");
          String uuid = minecrafts.getString("uuid");
          String dbEmojiId = minecrafts.getString("emid");

          Method getEmojis = guild.getClass().getMethod("getEmojis");
          List<?> emojis = (List<?>) getEmojis.invoke(guild);
          Optional<?> existingEmote = emojis.stream()
            .filter(emote -> {
              try {
                Method getName = emote.getClass().getMethod("getName");
                String existedEmojiName = (String) getName.invoke(emote);
                return existedEmojiName.equals(mineName);
              } catch (Exception e) {
                logger.error("An error occurred while getting emoji ID: " + e.getMessage());
                for (StackTraceElement element : e.getStackTrace()) {
                  logger.error(element.toString());
                }
                return false;
              }
            })
          .findFirst();

          if (existingEmote.isPresent()) {
            Object emojiInfo = existingEmote.get();
            Method getId = emojiInfo.getClass().getMethod("getId");
            emojiId = getId.invoke(emojiInfo).toString();

            if(emojiId != null && !emojiId.equals(dbEmojiId)) {
              String query2 = "UPDATE members SET emid=? WHERE uuid=?;";
              try (PreparedStatement ps2 = conn.prepareStatement(query2);) {
                ps2.setString(1, emojiId);
                ps2.setString(2, uuid);
                ps2.executeUpdate();
              }
            }
          } else {
            String imageUrl = "https://minotar.net/avatar/" + uuid;
            try {
              URI uri = new URI(imageUrl);
              URL url = uri.toURL();
              BufferedImage bufferedImage = ImageIO.read(url);
              if (bufferedImage == null) {
                logger.error("Failed to read image from URL: " + imageUrl);
                continue;
              }

              ByteArrayOutputStream baos = new ByteArrayOutputStream();
              ImageIO.write(bufferedImage, "png", baos);
              byte[] imageBytes = baos.toByteArray();

              Object icon = iconClazz.getMethod("from", byte[].class).invoke(null, imageBytes);

              Method createEmoji = guild.getClass().getMethod("createEmoji", String.class, iconClazz);;
              Object action = createEmoji.invoke(guild, mineName, icon);

              Method queue = action.getClass().getMethod("queue", Consumer.class, Consumer.class);
              queue.invoke(action, new Object[] {
                (Consumer<Object>) success -> {
                  logger.info(mineName + "を絵文字に追加しました。");
                  try {
                    Method getId = success.getClass().getMethod("getId");
                    String query2 = "UPDATE members SET emid=? WHERE uuid=?;";
                    try (Connection conn2 = db.getConnection();
                        PreparedStatement ps2 = conn2.prepareStatement(query2);) {
                      ps2.setString(1, getId.invoke(success).toString());
                      ps2.setString(2, uuid);
                      ps2.executeUpdate();
                    }
                  } catch (Exception e) {
                    logger.error("An error occurred while getting emoji ID: " + e.getMessage());
                    for (StackTraceElement element : e.getStackTrace()) {
                      logger.error(element.toString());
                    }
                  }
                }, 
                (Consumer<Throwable>) failure -> {
                  logger.error("Failed to create emoji: " + failure.getMessage());
                }
              });
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

  public CompletableFuture<String> createOrgetEmojiId(String emojiName) throws Exception {
    return createOrgetEmojiId(emojiName, null);
  }
}
