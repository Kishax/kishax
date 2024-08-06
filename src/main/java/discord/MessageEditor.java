package discord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerInfo;

import common.ColorUtil;
import net.dv8tion.jda.api.entities.MessageEmbed;
import velocity.Config;
import velocity.Database;
import velocity.EventListener;
import velocity.Main;
import velocity.PlayerDisconnect;
import velocity.PlayerUtil;

public class MessageEditor
{
	private PreparedStatement ps = null;
	public Connection conn = null;
	public final Main plugin;
	private final Logger logger;
	private final Config config;
	private final Database db;
	private final DiscordListener discord;
	private final EmojiManager emoji;
	private final PlayerUtil pu;
	private String emojiId = null, avatarUrl = null, addMessage = null, 
			Emoji = null, FaceEmoji = null, reqServerName = null, 
			targetServerName = null, uuid = null, playerName = null;
	private MessageEmbed sendEmbed = null;
	private MessageEmbed createEmbed = null;
	
	@Inject
	public MessageEditor
	(
		Main plugin, Logger logger, ProxyServer server,
		Config config, Database db, DiscordListener discord,
		EmojiManager emoji, PlayerUtil pu
	)
	{
		this.plugin = plugin;
		this.logger = logger;
		this.config = config;
		this.db = db;
		this.discord = discord;
		this.emoji = emoji;
		this.pu = pu;
	}
	
	public void AddEmbedSomeMessage(String type, Player player, ServerInfo serverInfo, String serverName, String alternativePlayerName) 
	{
		if(Objects.isNull(player))
		{
			// player変数がnullかつalternativePlayerNameが与えられていたとき
			if(Objects.nonNull(alternativePlayerName))
			{
				// データベースからuuidを取ってくる
				uuid = pu.getPlayerUUIDByName(alternativePlayerName);
				playerName = alternativePlayerName;
			}
			else
			{
				logger.error("MessageEditor.AddEmbedSomeMessageメソッドの使い方が間違っています。");
				return;
			}
		}
		else
		{
			uuid = player.getUniqueId().toString();
			playerName = player.getUsername();
		}
		
	    avatarUrl = "https://minotar.net/avatar/" + uuid;
	    
	    String EmojiName = config.getString("Discord." + type + "EmojiName", "");

	    CompletableFuture<String> EmojiFutureId = emoji.createOrgetEmojiId(EmojiName);
	    CompletableFuture<String> FaceEmojiFutureId = emoji.createOrgetEmojiId(playerName, avatarUrl);
	    CompletableFuture<Void> allOf = CompletableFuture.allOf(EmojiFutureId, FaceEmojiFutureId);

	    allOf.thenRun(() -> 
	    {
	        try 
	        {
	            String currentServerName = null;
	            if (Objects.nonNull(serverInfo)) 
	            {
	                currentServerName = serverInfo.getName();
	            } 
	            else 
	            {
	                currentServerName = "";
	            }

	            targetServerName = serverName;
	            if (Objects.isNull(serverName)) 
	            {
	                targetServerName = "";
	            }

	            String EmojiId = EmojiFutureId.get(); // プラスとかマイナスとかの絵文字ID取得
	            String FaceEmojiId = FaceEmojiFutureId.get(); // minecraftのアバターの顔の絵文字Id取得
	            Emoji = emoji.getEmojiString(EmojiName, EmojiId);
	            FaceEmoji = emoji.getEmojiString(playerName, FaceEmojiId);
	            
	            String messageId = EventListener.PlayerMessageIds.getOrDefault(uuid, null);

	            addMessage = null;
	            switch (type) 
	            {
	            	case "AddMember":
	                	if (Objects.nonNull(Emoji) && Objects.nonNull(FaceEmoji)) 
	                	{
                            logger.info("Emoji ID retrieved: " + emojiId);

                            addMessage = Emoji + FaceEmoji +
                                    player.getUsername() + "が新規FMCメンバーになりました！:congratulations: ";
                        }
	                	else
	                	{
                            logger.info("Emoji ID is null");
                            addMessage = player.getUsername() + "が新規FMCメンバーになりました！:congratulations: ";
                        }

                        createEmbed = discord.createEmbed
                                (
                                		addMessage,
                                        ColorUtil.PINK.getRGB()
                                );
                        discord.sendBotMessage(createEmbed);
	            		break;
	            		
	                case "Start":
	    	            if (Objects.nonNull(Emoji) && Objects.nonNull(FaceEmoji) && Objects.nonNull(messageId)) 
	    	            {
	                        addMessage = "\n\n" + Emoji + FaceEmoji + playerName + "が" +
	                        		targetServerName+ "サーバーを起動させました。";
	                        discord.editBotEmbed(messageId, addMessage);
	    	            }
	                    break;
	
	                case "Exit":
	                	if (Objects.nonNull(Emoji) && Objects.nonNull(FaceEmoji) && Objects.nonNull(messageId)) 
	                	{
		                    addMessage = "\n\n" + Emoji + FaceEmoji + playerName + "が" +
		                            currentServerName + "サーバーから退出しました。";
		                    discord.editBotEmbed(messageId, addMessage);
		                    EventListener.PlayerMessageIds.remove(uuid);
	                	}
	                    break;
	
	                case "Move":
	                	if (Objects.nonNull(Emoji) && Objects.nonNull(FaceEmoji) && Objects.nonNull(messageId)) 
	                	{
		                    addMessage = "\n\n" + Emoji + FaceEmoji + playerName + "が" +
		                            currentServerName + "サーバーへ移動しました。";
		                    discord.editBotEmbed(messageId, addMessage);
	                	}
	                    break;
	
	                case "Request":
	                	if (Objects.nonNull(Emoji) && Objects.nonNull(FaceEmoji) && Objects.nonNull(messageId)) 
	                	{
		                    addMessage = "\n\n" + Emoji + FaceEmoji + playerName + "が" +
		                            reqServerName + "サーバーの起動リクエストを送りました。";
		                    discord.editBotEmbed(messageId, addMessage);
	                	}
	                    break;
	                    
	                case "Join":
	                	try {
		                	conn = db.getConnection();
		                	if (Objects.nonNull(Emoji) && Objects.nonNull(FaceEmoji)) 
		                	{
	                            logger.info("Emoji ID retrieved: " + emojiId);
	                            // 絵文字IDをアップデートしておく
	                            if(Objects.nonNull(conn))
	                            {
	                            	ps = conn.prepareStatement("UPDATE minecraft SET emid=? WHERE uuid=?;");
		                            ps.setString(1, FaceEmojiId);
		                            ps.setString(2, uuid);
		                            ps.executeUpdate();
	                            }
	
	                            addMessage = Emoji + FaceEmoji +
	                                    playerName + "が" + serverInfo.getName() +
	                                    "サーバーに参加しました。";
	                        } 
		                	else 
		                	{
	                            logger.info("Emoji ID is null");
	                            if(Objects.nonNull(conn))
	                            {
	                            	// 絵文字IDをアップデートしておく
		                            ps = conn.prepareStatement("UPDATE minecraft SET emid=? WHERE uuid=?;");
		                            ps.setString(1, null);
		                            ps.setString(2, uuid);
		                            ps.executeUpdate();
	                            }
	
	                            addMessage = playerName + "が" + serverInfo.getName() +
	                                    "サーバーに参加しました。";
	                        }
	
	                        createEmbed = discord.createEmbed
	                                (
	                                		addMessage,
	                                        ColorUtil.GREEN.getRGB()
	                                );
	                        discord.sendBotMessageAndgetMessageId(createEmbed).thenAccept(messageId2 ->
	                        {
	                            logger.info("Message sent with ID: " + messageId2);
	                            EventListener.PlayerMessageIds.put(uuid, messageId2);
	                        });
	                	}
	                	catch (SQLException | ClassNotFoundException e1) 
                        {
                            logger.error("An onConnection error occurred: " + e1.getMessage());
                            for (StackTraceElement element : e1.getStackTrace()) 
                            {
                                logger.error(element.toString());
                            }
                        }
                        break;
                        
	                case "FirstJoin":
                        try {
                            conn = db.getConnection();
                            if (Objects.nonNull(Emoji) && Objects.nonNull(FaceEmoji)) {
                                logger.info("Emoji ID retrieved: " + emojiId);
                                ps = conn.prepareStatement("INSERT INTO minecraft (name,uuid,server, emid) VALUES (?,?,?,?);");
                                ps.setString(1, playerName);
                                ps.setString(2, uuid);
                                ps.setString(3, serverInfo.getName());
                                ps.setString(4, FaceEmojiId);
                                ps.executeUpdate();

                                addMessage = Emoji + FaceEmoji +
                                        playerName + "が" + serverInfo.getName() +
                                        "サーバーに初参加です！";
                            } else {
                                logger.info("Emoji ID is null");
                                ps = conn.prepareStatement("INSERT INTO minecraft (name,uuid,server) VALUES (?,?,?);");
                                ps.setString(1, playerName);
                                ps.setString(2, uuid);
                                ps.setString(3, serverInfo.getName());
                                ps.executeUpdate();

                                addMessage = playerName + "が" + serverInfo.getName() +
                                        "サーバーに初参加です！";
                            }

                            createEmbed = discord.createEmbed
                                    (
                                            addMessage,
                                            ColorUtil.ORANGE.getRGB()
                                    );
                            discord.sendBotMessageAndgetMessageId(createEmbed).thenAccept(messageId2 -> {
                                logger.info("Message sent with ID: " + messageId2);
                                EventListener.PlayerMessageIds.put(uuid, messageId2);
                            });
                        } 
                        catch (SQLException | ClassNotFoundException e1) 
                        {
                            logger.error("An onConnection error occurred: " + e1.getMessage());
                            for (StackTraceElement element : e1.getStackTrace()) 
                            {
                                logger.error(element.toString());
                            }
                        }
	                    break;
	
	                case "RequestOK":
	                	if (Objects.nonNull(Emoji) && Objects.nonNull(FaceEmoji) && Objects.nonNull(messageId)) 
	                	{
		                    emoji.createOrgetEmojiId(playerName, avatarUrl).thenAccept(emojiId -> 
		                    {
		                        addMessage = "管理者が" + Emoji + FaceEmoji + playerName + "の" +
		                                reqServerName + "サーバー起動リクエストを受諾しました。";
		                        sendEmbed = discord.createEmbed
		                                (
		                                        addMessage,
		                                        ColorUtil.GREEN.getRGB()
		                                );
		                        discord.sendBotMessage(sendEmbed);
		                    });
	                	}
	                    break;
	
	                case "RequestCancel":
	                	if (Objects.nonNull(Emoji) && Objects.nonNull(FaceEmoji) && Objects.nonNull(messageId)) 
	                	{
		                    emoji.createOrgetEmojiId(playerName, avatarUrl).thenAccept(emojiId ->
		                    {
		                        addMessage = "管理者が" + Emoji + FaceEmoji + playerName + "の" +
		                                reqServerName + "サーバー起動リクエストをキャンセルしました。";
		                        sendEmbed = discord.createEmbed
		                                (
		                                        addMessage,
		                                        ColorUtil.RED.getRGB()
		                                );
		                        discord.sendBotMessage(sendEmbed);
		                    });
	                	}
	                    break;
	
	                case "RequestNoRes":
	                	if (Objects.nonNull(Emoji) && Objects.nonNull(FaceEmoji) && Objects.nonNull(messageId))
	                	{
		                    emoji.createOrgetEmojiId(playerName, avatarUrl).thenAccept(emojiId -> 
		                    {
		                        addMessage = "管理者が" + Emoji + FaceEmoji + playerName + "の" +
		                                reqServerName + "サーバー起動リクエストに対して、応答しませんでした。";
		                        sendEmbed = discord.createEmbed
		                                (
		                                        addMessage,
		                                        ColorUtil.RED.getRGB()
		                                );
		                        discord.sendBotMessage(sendEmbed);
		                    });
	                	}
	                    break;
	
	                default:
	                    break;
	            }
	        }
	        catch (Exception e1)
	        {
	            e1.printStackTrace();
	        }
	    });
	}
	
	public void AddEmbedSomeMessage(String type, Player player, String reqServer)
	{
		AddEmbedSomeMessage(reqServer, player, null, reqServer, null);
	}
	
	public void AddEmbedSomeMessage(String type, Player player, ServerInfo serverInfo)
	{
		AddEmbedSomeMessage(type, player, serverInfo, null, null);
	}
	
	public void AddEmbedSomeMessage(String type, Player player)
	{
		AddEmbedSomeMessage(type, player, null, null, null);
	}
	
	public void AddEmbedSomeMessage(String type, String alternativePlayerName)
	{
		AddEmbedSomeMessage(type, null, null, null, alternativePlayerName);
	}
	
	public void AddEmbedSomeMessage(String type, String alternativePlayerName, String reqServer)
	{
		AddEmbedSomeMessage(type, null, null, reqServer, alternativePlayerName);
	}
}
