package discord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;

import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import common.ColorUtil;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import velocity.BroadCast;
import velocity.Config;
import velocity.DatabaseInterface;
import velocity.DiscordListener;
import velocity.EmojiManager;
import velocity.EventListener;
import velocity.Main;
import velocity.PlayerDisconnect;
import velocity.PlayerList;
import velocity.RomaToKanji;
import velocity.RomajiConversion;

public class MessageEditor
{
	public final Main plugin;
	public Connection conn = null;
	public ResultSet yuyu = null, yu = null, logs = null, rs = null, bj_logs = null, ismente = null;
	public ResultSet[] resultsets = {yuyu, yu, logs, rs, bj_logs, ismente};
	public PreparedStatement ps = null;
	public static Map<String, String> PlayerMessageIds = new HashMap<>();
	
	private final ProxyServer server;
	private final Config config;
	private final Logger logger;
	private final DatabaseInterface db;
	private final BroadCast bc;
	private final ConsoleCommandSource console;
	private final RomaToKanji conv;
	private String chatserverName = "";
	private final PlayerList pl;
	private final PlayerDisconnect pd;
	private final RomajiConversion rc;
	private final DiscordListener discord;
	private final EmojiManager emoji;
	private WebhookMessageBuilder builder = null;
	private String avatarUrl = null;
	private String emojiId = null;
	private MessageEmbed joinEmbed = null;
	private final String[] emojiIdHolder = new String[1];
	
	@Inject
	public MessageEditor
	(
		Main plugin, Logger logger, ProxyServer server,
		Config config, DatabaseInterface db, BroadCast bc,
		ConsoleCommandSource console, RomaToKanji conv, PlayerList pl,
		PlayerDisconnect pd, RomajiConversion rc, DiscordListener discord,
		EmojiManager emoji
	)
	{
		this.plugin = plugin;
		this.logger = logger;
		this.server = server;
		this.config = config;
		this.db = db;
		this.bc = bc;
		this.console = console;
		this.conv = conv;
		this.pl = pl;
		this.pd = pd;
		this.rc = rc;
		this.discord = discord;
		this.emoji = emoji;
	}
	
	public void ExitDiscordMessageAsync(Player player, ServerInfo serverInfo)
	{
		console.sendMessage(Component.text("Player " + player.getUsername() + " disconnected from server: " + serverInfo.getName()).color(NamedTextColor.GREEN));
		
		emojiId = null;
		joinEmbed = null;
		String ExitEmojiName = config.getString("Discord.ExitEmojiName","");
		// Discord絵文字を取得する
		// EmojiIDを取得する非同期処理が完了したのちに処理を続ける
		CompletableFuture<String> exitEmojiFuture = emoji.createOrgetEmojiId(ExitEmojiName);
		CompletableFuture<Void> allOf = CompletableFuture.allOf(exitEmojiFuture);

		allOf.thenRun(() ->
		{
			try
			{
				String currentServerName = serverInfo.getName();
		        String exitEmojiId = exitEmojiFuture.get(); // exitEmojiId の取得
	        	// PlayersMessageIdsマップから、messageIdを取得
	        	String messageId = EventListener.PlayerMessageIds.getOrDefault(player.getUniqueId().toString(), null);
	        	
            	if(!ExitEmojiName.isEmpty() && Objects.nonNull(messageId))
            	{
    				if(Objects.nonNull(exitEmojiId))
    				{
    					// ExitEmojiNameという名前のEmojiがあってそのEmojiIdを取得できた場合
    					// 絵文字String作成
	    				String ExitEmoji = emoji.getEmojiString(ExitEmojiName, exitEmojiId);
	    				discord.editBotEmbed
	    				(
	    					messageId, // 編集するメッセージID
	    					"\n\n" + ExitEmoji + player.getUsername() + "が" +
	    					currentServerName + "サーバーから退出しました。"
	    				);
    				}
    				else
    				{
    					discord.editBotEmbed
	    				(
	    					emojiIdHolder[0], // 編集するメッセージID
	    					"\n\n" + player.getUsername() + "が" +
	    					currentServerName + "サーバーから退出しました。"
	    				);
    				}
            	}
			}
			catch (Exception e1)
    		{
    			e1.printStackTrace();
    		}
			finally
			{
				// Velocityネットワークからの退出ゆえ、messageIdとUUIDのマッピングをクリアにする
				PlayerMessageIds.remove(player.getUniqueId().toString());
			}
    	});
	}
	
	public void JoinOrMoveDiscordMessageAsync(Player player, Optional <RegisteredServer> previousServerInfo, ServerInfo serverInfo)
	{
		avatarUrl = "https://minotar.net/avatar/"+player.getUniqueId().toString();
		emojiId = null;
		joinEmbed = null;
		String MoveEmojiName = config.getString("Discord.MoveEmojiName","");
		// Discord絵文字を取得する
		// EmojiIDを取得する非同期処理が完了したのちに処理を続ける
		CompletableFuture<String> emojiIdFuture = emoji.createOrgetEmojiId(player.getUsername(), avatarUrl);
		CompletableFuture<String> moveEmojiIdFuture = emoji.createOrgetEmojiId(MoveEmojiName);
		
		// 両方の非同期処理が完了した後に結果を処理する
		CompletableFuture<Void> allOf = CompletableFuture.allOf(emojiIdFuture, moveEmojiIdFuture);

		allOf.thenRun(() ->
		{
			try
			{
				String emojiId = emojiIdFuture.get(); // emojiId の取得
		        String moveEmojiId = moveEmojiIdFuture.get(); // moveEmojiId の取得
				// プレイヤーが以前のサーバーから移動した場合に処理を行う
		        if (previousServerInfo.isPresent())
		        {
		        	// PlayersMessageIdsマップから、messageIdを取得
		        	String messageId = EventListener.PlayerMessageIds.getOrDefault(player.getUniqueId().toString(), null);
		        	
		            String previousServerName = previousServerInfo.get().getServerInfo().getName();
		            String currentServerName = serverInfo.getName();
		            String hubServerName = config.getString("Servers.Hub","");
		            
		            // ホームサーバーから別のサーバーに移動したことを確認
		            if (previousServerName.equals(hubServerName) && !currentServerName.equals(hubServerName))
		            {
		            	// サーバー移動処理
		            	if(!MoveEmojiName.isEmpty() && Objects.nonNull(messageId))
		            	{
		    				if(Objects.nonNull(moveEmojiId))
		    				{
		    					// MoveEmojiNameという名前のEmojiがあってそのEmojiIdを取得できた場合
		    					// 絵文字String作成
    		    				String MoveEmoji = emoji.getEmojiString(MoveEmojiName, moveEmojiId);
    		    				discord.editBotEmbed
    		    				(
    		    					messageId, // 編集するメッセージID
    		    					"\n\n" + MoveEmoji + player.getUsername() + "が" +
    		    					currentServerName + "サーバーへ移動しました。"
    		    				);
		    				}
		    				else
		    				{
		    					discord.editBotEmbed
    		    				(
    		    					emojiIdHolder[0], // 編集するメッセージID
    		    					"\n\n" + player.getUsername() + "が" +
    		    					currentServerName + "サーバーへ移動しました。"
    		    				);
		    				}
		            	}
		            }
		        }
		        else
		        {
		        	// どこかサーバーに上陸したとき
		        	if(Objects.nonNull(emojiId))
	    			{
			        	joinEmbed = discord.createEmbed
    							(
    								emoji.getEmojiString(player.getUsername(), emojiId)+
    								player.getUsername()+"が"+serverInfo.getName()+
    								"サーバーに参加しました。",
    								ColorUtil.GREEN.getRGB()
    							);
			        	discord.sendBotMessageAndgetMessageId(joinEmbed).thenAccept(messageId ->
			        	{
			        		// messageIdをUUIDでマッピングし、あとで編集できるようにしておく
			        		PlayerMessageIds.put(player.getUniqueId().toString(), messageId);
	    				});
	    			}
			        else
			        {
			        	joinEmbed = discord.createEmbed
    							(
    								player.getUsername()+"が"+serverInfo.getName()+
    								"サーバーに参加しました。",
    								ColorUtil.GREEN.getRGB()
    							);
			        	discord.sendBotMessageAndgetMessageId(joinEmbed).thenAccept(messageId ->
			        	{
			        		// messageIdをUUIDでマッピングし、あとで編集できるようにしておく
			        		PlayerMessageIds.put(player.getUniqueId().toString(), messageId);
	    				});
			        }
		        }
			}
			catch (Exception e1)
			{
				e1.printStackTrace();
			}
		});
	}
}
