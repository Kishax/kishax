package discord;

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
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.imageio.ImageIO;

import org.slf4j.Logger;

import com.google.inject.Inject;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import velocity.Config;
import velocity.Database;

public class EmojiManager
{
	private Connection conn = null;
	private PreparedStatement ps = null;
	private ResultSet minecrafts = null;
	private final ResultSet[] resultsset = {minecrafts};
	
    private JDA jda = null;
    private String emojiId = null;
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
    
    public CompletableFuture<String> createOrgetEmojiId(String emojiName, String imageUrl) throws URISyntaxException
    {
    	CompletableFuture<String> future = new CompletableFuture<>();
    	
    	this.jda = Discord.jda;
        if (Objects.isNull(jda) || config.getLong("Discord.GuildId", 0) == 0)
        {
        	future.complete(null);
            return future;
        }
        
        // emojiNameが空白かnullだった場合
        if (Objects.isNull(emojiName) || emojiName.isEmpty())
        {
        	future.complete(null);
            return future;
        }
        
    	String guildId = Long.toString(config.getLong("Discord.GuildId"));
        Guild guild = jda.getGuildById(guildId);
        if (Objects.isNull(guild))
        {
            //logger.info("Guild not found!");
            future.complete(null);
            return future;
        }
        
        // 絵文字が既に存在するかをチェックし、存在する場合はIDを取得
        @SuppressWarnings("null")
        Optional<Emote> existingEmote = guild.getEmotes().stream()
            .filter(emote -> emote.getName().equals(emojiName))
            .findFirst();
        
        if (existingEmote.isPresent())
        {
            emojiId = existingEmote.get().getId();
            //logger.info(emojiName + "の絵文字はすでに追加されています。");
            //logger.info("Existing Emoji ID: " + emojiId);
            future.complete(emojiId);
        }
        else
        {
        	if(Objects.isNull(imageUrl))
        	{
        		future.complete(null);
                return future;
        	}
        		
        	try 
            {
                URI uri = new URI(imageUrl);
                URL url = uri.toURL();
            
                BufferedImage bufferedImage = ImageIO.read(url);
                if (Objects.isNull(bufferedImage))
                {
                    logger.error("Failed to read image from URL: " + imageUrl);
                    future.complete(null);
                    return future;
                }
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, "png", baos);
                byte[] imageBytes = baos.toByteArray();
                Icon icon = Icon.from(imageBytes);
            
                // Create the emote with the specified name and icon
                AuditableRestAction<Emote> action = guild.createEmote(emojiName, icon);
                action.queue(
                    success -> 
                    {
                        logger.info(emojiName + "を絵文字に追加しました。");
                        emojiId = success.getId(); // 絵文字IDを取得
                        future.complete(emojiId);
                    },
                    failure -> 
                    {
                        logger.error("Failed to create emoji: " + failure.getMessage());
                        future.complete(null);
                    }
                );
            } 
            catch (IOException | URISyntaxException e) 
            {
                logger.error("A createEmoji error occurred: " + e.getMessage());
                for (StackTraceElement element : e.getStackTrace()) 
                {
                    logger.error(element.toString());
                }
                future.complete(null);
            }
        }
        return future;
    }
    
    public String getEmojiString(String emojiName, String emojiId)
    {
    	if(Objects.isNull(emojiId)) return null;
    	if(Objects.isNull(emojiName) || emojiName.isEmpty()) return null;
    	
    	return "<:" + emojiName + ":" + emojiId + ">";
    }
    
    public void updateEmojiIdsToDatabase()
    {
        this.jda = Discord.jda;
        if (Objects.isNull(jda) || config.getLong("Discord.ChannelId", 0) == 0) return;

        MessageChannel channel = jda.getTextChannelById(config.getLong("Discord.ChannelId"));
        if (Objects.isNull(channel))
        {
            //logger.info("Channel not found!");
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
            	emojiId = null; // while文の中で、最初にemojiIDを初期化しておく
            	
                String mineName = minecrafts.getString("name");
                String uuid = minecrafts.getString("uuid");
                String dbEmojiId = minecrafts.getString("emid");
                
                // 絵文字が既に存在するかをチェックし、存在する場合はIDを取得
                Optional<Emote> existingEmote = guild.getEmotes().stream()
                    .filter(emote -> emote.getName().equals(mineName))
                    .findFirst();

                if (existingEmote.isPresent())
                {
                    emojiId = existingEmote.get().getId();
                    //logger.info(mineName + "の絵文字はすでに追加されています。");
                    //logger.info("Existing Emoji ID: " + emojiId);
                    
                    // もし、emojiIdがminecrafts.getString("emid")と違ったら更新する
                    // データベース保存処理
                    if(Objects.nonNull(emojiId) && !emojiId.equals(dbEmojiId))
                    {
                    	sql = "UPDATE minecraft SET emid=? WHERE uuid=?;";
                    	ps = conn.prepareStatement(sql);
                    	ps.setString(1, emojiId);
                    	ps.setString(2, uuid);
                    	ps.executeUpdate();
                    }
                }
                else
                {
                    String imageUrl = "https://minotar.net/avatar/" + uuid;
                    //logger.info("Downloading image from URL: " + imageUrl); // 画像URLをログに出力

                    try
                    {
                        //logger.info("Downloading image from URL: " + imageUrl);
                        
                        URI uri = new URI(imageUrl);
                        URL url = uri.toURL();
                        BufferedImage bufferedImage = ImageIO.read(url);
                        if (Objects.isNull(bufferedImage))
                        {
                            logger.error("Failed to read image from URL: " + imageUrl);
                            continue;
                        }
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(bufferedImage, "png", baos);
                        byte[] imageBytes = baos.toByteArray();
                        Icon icon = Icon.from(imageBytes);

                        // Create the emote with the specified name and icon
                        AuditableRestAction<Emote> action = guild.createEmote(mineName, icon);
                        action.queue(
                        	success -> 
                            {
                                logger.info(mineName + "を絵文字に追加しました。");
                                emojiId = success.getId(); // 絵文字IDを取得
                                //logger.info("Emoji ID: " + emojiId);
                            },
                            failure -> logger.error("Failed to create emoji: " + failure.getMessage())
                        );
                        
                        // データベース更新処理
                        if(Objects.nonNull(emojiId))
                        {
                        	sql = "UPDATE minecraft SET emid=? WHERE uuid=?;";
                        	ps = conn.prepareStatement(sql);
                        	ps.setString(1, emojiId);
                        	ps.setString(2, uuid);
                        	ps.executeUpdate();
                        }
                    }
                    catch (IOException | URISyntaxException e)
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
    
    public CompletableFuture<String> createOrgetEmojiId(String emojiName)
    {
    	try 
        {
            return createOrgetEmojiId(emojiName, null);
        } 
        catch (URISyntaxException e) 
        {
            logger.error("A URISyntaxException error occurred: " + e.getMessage());
            for (StackTraceElement element : e.getStackTrace())
            {
                logger.error(element.toString());
            }
            return null;
        }
    }
}
