package velocity;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import javax.imageio.ImageIO;

import org.slf4j.Logger;

import com.google.inject.Inject;

public class EmojiManager
{
	private Connection conn = null;
	private PreparedStatement ps = null;
	private ResultSet minecrafts = null;
	private ResultSet[] resultsset = {minecrafts};
	
    private JDA jda = null;
    private final Logger logger;
    private final Config config;
    private final Database db;
    
    @Inject
    public EmojiManager(Logger logger, Config config, Database db)
    {
        this.logger = logger;
        this.config = config;
        this.db = db;
    }
    
    public void createEmoji(String emojiName, String imageUrl)
    {
    	this.jda = DiscordListener.jda;
        if (Objects.isNull(jda) || config.getLong("Discord.GuildId", 0) == 0) return;
        
    	String guildId = Long.valueOf(config.getLong("Discord.GuildId")).toString();
        Guild guild = jda.getGuildById(guildId);
        if (Objects.isNull(guild))
        {
            logger.info("Guild not found!");
            return;
        }
        
        // 絵文字が既に存在するかをチェック
        boolean emojiExists = guild.getEmotes().stream()
            .anyMatch(emote -> emote.getName().equals(emojiName));

        if (emojiExists)
        {
            logger.info(emojiName + "の絵文字はすでに追加されています。");
        }
        else
        {
        	try
            {
                BufferedImage bufferedImage = ImageIO.read(new URL(imageUrl));
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, "png", baos);
                byte[] imageBytes = baos.toByteArray();
                Icon icon = Icon.from(imageBytes);

                // Create the emote with the specified name and icon
                AuditableRestAction<Emote> action = guild.createEmote(emojiName, icon);
                action.queue(
                    success -> logger.info("Emoji created successfully!"),
                    failure -> logger.error("Failed to create emoji: " + failure.getMessage())
                );
            }
            catch (IOException e)
            {
            	logger.error("A createEmoji error occurred: " + e.getMessage());
                for (StackTraceElement element : e.getStackTrace()) 
                {
                    logger.error(element.toString());
                }
            }
        }
    }
    
    public void checkAndAddEmojis()
    {
        this.jda = DiscordListener.jda;
        if (Objects.isNull(jda) || config.getLong("Discord.ChannelId", 0) == 0) return;

        MessageChannel channel = jda.getTextChannelById(config.getLong("Discord.ChannelId"));
        if (Objects.isNull(channel))
        {
            logger.info("Channel not found!");
            return;
        }

        Guild guild = jda.getGuilds().get(0); // 最初のギルドを取得（適切なギルドを選択する必要があります）
        try
        {
            conn = db.getConnection();
            String sql = "SELECT * FROM minecraft;";
            ps = conn.prepareStatement(sql);
            minecrafts = ps.executeQuery();

            while (minecrafts.next())
            {
                String mineName = minecrafts.getString("name");
                String uuid = minecrafts.getString("uuid");

                // 絵文字が既に存在するかをチェック
                boolean emojiExists = guild.getEmotes().stream()
                    .anyMatch(emote -> emote.getName().equals(mineName));

                if (emojiExists)
                {
                    logger.info(mineName + "の絵文字はすでに追加されています。");
                }
                else
                {
                    logger.info(mineName + "を絵文字に追加しました。");
                    String imageUrl = "https://minotar.net/avatar/" + uuid;
                    logger.info("Downloading image from URL: " + imageUrl); // 画像URLをログに出力

                    try
                    {
                        BufferedImage bufferedImage = ImageIO.read(new URL(imageUrl));
                        if (Objects.isNull(bufferedImage))
                        {
                            logger.info("Failed to read image from URL: " + imageUrl);
                            continue;
                        }
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(bufferedImage, "png", baos);
                        byte[] imageBytes = baos.toByteArray();
                        Icon icon = Icon.from(imageBytes);

                        // Create the emote with the specified name and icon
                        AuditableRestAction<Emote> action = guild.createEmote(mineName, icon);
                        action.queue(
                            success -> logger.info("Emoji created successfully!"),
                            failure -> logger.error("Failed to create emoji: " + failure.getMessage())
                        );
                    }
                    catch (IOException e)
                    {
                        logger.error("Failed to download image: " + e.getMessage());
                        for (StackTraceElement element : e.getStackTrace())
                        {
                            logger.error(element.toString());
                        }
                    }
                }
            }
        }
        catch (SQLException | ClassNotFoundException e)
        {
            // スタックトレースをログに出力
            logger.error("A checkAndAddEmojis error occurred: " + e.getMessage());
            for (StackTraceElement element : e.getStackTrace())
            {
                logger.error(element.toString());
            }
        }
        finally
        {
            db.close_resorce(resultsset, conn, ps);
        }
    }

    private byte[] downloadImage(String imageUrl) throws IOException
    {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(imageUrl).build();
        try (Response response = client.newCall(request).execute())
        {
            if (response.isSuccessful() && response.body() != null)
            {
                try (InputStream inputStream = response.body().byteStream())
                {
                    return inputStream.readAllBytes();
                }
            }
        }
        return null;
    }
}
