package discord;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.proxy.ProxyServer;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookMessage;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.CommandCreateAction;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.MessageEditAction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import velocity.BroadCast;
import velocity.Config;
import velocity.Database;
import velocity.Main;
import velocity.PlayerUtil;
import velocity_command.Request;

public class Discord implements DiscordInterface {

	public static JDA jda = null;
	public static boolean isDiscord = false;
	
	private final Main plugin;
	private final ProxyServer server;
    private final Logger logger;
    private final Config config;
    private final Database db;
    private final BroadCast bc;
    private final PlayerUtil pu;
    private final MessageEditorInterface discordME;
    private String channelId = null;
    private MessageChannel channel= null;
	
    @Inject
    public Discord (
    	Main plugin, ProxyServer server, Logger logger, 
    	Config config, Database db, BroadCast bc, 
    	PlayerUtil pu, MessageEditorInterface discordME
    ) {
    	this.plugin = plugin;
    	this.server = server;
    	this.logger = logger;
    	this.config = config;
    	this.db = db;
    	this.bc = bc;
    	this.pu = pu;
    	this.discordME = discordME;
    }
    
    @Override
    public void loginDiscordBotAsync() {
    	if (config.getString("Discord.Token","").isEmpty()) return;
    	server.getScheduler().buildTask(plugin, () -> {
			try {
				jda = JDABuilder.createDefault(config.getString("Discord.Token"))
						.addEventListeners(Main.getInjector().getInstance(DiscordEventListener.class))
						.build();

                // Botが完全に起動するのを待つ
				jda.awaitReady();

                CommandCreateAction createTeraCommand = jda.upsertCommand("fmc", "FMC commands");
				createTeraCommand.addSubcommands(
					new SubcommandData("tera", "GCP commands")
						.addOptions(new OptionData(OptionType.STRING, "action", "Choose an action")
							.addChoice("Start", "start")
							.addChoice("Stop", "stop")
							.addChoice("Status", "status")
					)
				).queue();

	            jda.getPresence().setActivity(Activity.playing(config.getString("Discord.Presence.Activity", "FMCサーバー")));
	            
				isDiscord = true;
				logger.info("Discord-Botがログインしました。");
            } catch (InterruptedException e) {
				// スタックトレースをログに出力
	            logger.error("An discord-bot-login error occurred: " + e.getMessage());
	            for (StackTraceElement element : e.getStackTrace()) {
	                logger.error(element.toString());
	            }
			}
    	}).schedule();
    }
    
    @Override
    public CompletableFuture<Void> logoutDiscordBot() {
    	return CompletableFuture.runAsync(() -> {
	    	if (Objects.nonNull(jda)) {
	            jda.shutdown();
	            isDiscord = false;
	            logger.info("Discord-Botがログアウトしました。");
	        }
    	});
    }
    
    @Override
    public void sendRequestButtonWithMessage(String buttonMessage) {
    	if (config.getLong("Discord.AdminChannelId", 0) == 0 || !isDiscord) return;
		channelId = Long.toString(config.getLong("Discord.AdminChannelId"));
		
    	channel = jda.getTextChannelById(channelId);
        if (Objects.isNull(channel)) return;
        
        Button button1 = Button.success("reqOK", "YES");
        Button button2 = Button.danger("reqCancel", "NO");
        
        // メッセージにボタンを添えて送信
        MessageCreateAction action = channel.sendMessage(buttonMessage)
                .setActionRow(button1, button2);

        action.queue(message -> {
            // 3分後にボタンを削除
            CompletableFuture.delayedExecutor(3, TimeUnit.MINUTES).execute(() -> {
            	// フラグが立っていなかったら
            	if (!Request.PlayerReqFlags.isEmpty()) {
            		String buttonMessage2 = message.getContentRaw();
                	
            		// プレイヤー名・サーバー名、取得
                	String pattern = "<(.*?)>(.*?)が(.*?)サーバーの起動リクエストを送信しました。\n起動しますか？(.*?)";
                    Pattern compiledPattern = Pattern.compile(pattern);
                    Matcher matcher = compiledPattern.matcher(buttonMessage2);
                    if (matcher.find()) {
                    	String reqPlayerName = matcher.group(2);
                    	String reqServerName = matcher.group(3);
                    	String reqPlayerUUID = pu.getPlayerUUIDByNameFromDB(reqPlayerName);
                        
                        String query = "INSERT INTO log (name, uuid, reqsul, reqserver, reqsulstatus) VALUES (?, ?, ?, ?, ?);";
                    	try (Connection conn = db.getConnection();
                            PreparedStatement ps = conn.prepareStatement(query)) {
                        	ps.setString(1, reqPlayerName);
                        	ps.setString(2, reqPlayerUUID);
                        	ps.setBoolean(3, true);
                        	ps.setString(4, reqServerName);
                        	ps.setString(5, "nores");
                        	int rsAffected = ps.executeUpdate();
                            if (rsAffected > 0) {
                                // Discord通知
                                discordME.AddEmbedSomeMessage("RequestNoRes", reqPlayerName);
                                
                                // マイクラサーバーへ通知
                                bc.broadCastMessage(Component.text("管理者がリクエストに応答しませんでした。").color(NamedTextColor.BLUE));
                                
                                message.reply("管理者が応答しませんでした。").queue();
                                message.editMessageComponents().queue(); // ボタンを削除
                                
                                // フラグから削除
                                String playerUUID = pu.getPlayerUUIDByNameFromDB(reqPlayerName);
                                Request.PlayerReqFlags.remove(playerUUID); // フラグから除去
                            }
                        } catch (SQLException | ClassNotFoundException e1) {
                        	logger.error("A discord error occurred: " + e1.getMessage());
                            for (StackTraceElement element : e1.getStackTrace()) {
                                logger.error(element.toString());
                            }
                        }
                    }
            	}
            });
        });
    }

    @Override
    public void sendWebhookMessage(WebhookMessageBuilder builder) {
    	String webhookUrl = config.getString("Discord.Webhook_URL","");
    	if (webhookUrl.isEmpty()) return;
    		
        WebhookClient client = WebhookClient.withUrl(webhookUrl);
        
        //.addField(new EmbedField(true, "フィールド1", "値1"))
        WebhookMessage message = builder.build();
        
        client.send(message).thenAccept(response -> {
        	//logger.info("Message sent with ID: " + response.getId());
        }).exceptionally(throwable -> {
            logger.error("A sendWebhookMessage error occurred: " + throwable.getMessage());
            for (StackTraceElement element : throwable.getStackTrace()) {
                logger.error(element.toString());
            }

            return null;
        });
    }
    
    @Override
    public CompletableFuture<Void> editBotEmbed(String messageId, String additionalDescription, boolean isChat) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        getBotMessage(messageId, currentEmbed -> {
            if (Objects.isNull(currentEmbed)) {
                future.completeExceptionally(new RuntimeException("No embed found to edit."));
                return;
            }

            if (isChat) {
                if (config.getLong("Discord.ChatChannelId", 0) == 0 || !isDiscord) {
                    future.completeExceptionally(new RuntimeException("Chat channel ID is invalid or Discord is not enabled."));
                    return;
                }

                channelId = Long.toString(config.getLong("Discord.ChatChannelId"));
            } else {
                if (config.getLong("Discord.ChannelId", 0) == 0 || !isDiscord) {
                    future.completeExceptionally(new RuntimeException("Channel ID is invalid or Discord is not enabled."));
                    return;
                }

                channelId = Long.toString(config.getLong("Discord.ChannelId"));
            }

            channel = jda.getTextChannelById(channelId);
            if (Objects.isNull(channel)) {
                future.completeExceptionally(new RuntimeException("Channel not found!"));
                return;
            }

            // 現在のEmbedに新しい説明を追加
            MessageEmbed newEmbed = addDescriptionToEmbed(currentEmbed, additionalDescription);
            MessageEditAction messageAction = channel.editMessageEmbedsById(messageId, newEmbed);

            messageAction.queue(
                success -> future.complete(null),
                error -> {
                    future.completeExceptionally(error);
                    logger.info("Failed to edit message with ID: " + messageId);
                }
            );
        }, isChat);

        return future;
    }

    @Override
    public CompletableFuture<Void> editBotEmbed(String messageId, String additionalDescription) {
    	return editBotEmbed(messageId, additionalDescription, false);
    }
    
    @Override
    public void getBotMessage(String messageId, Consumer<MessageEmbed> embedConsumer, boolean isChat) {
    	if (isChat) {
    		if (config.getLong("Discord.ChatChannelId", 0) == 0 || !isDiscord) return;
            channelId = Long.toString(config.getLong("Discord.ChatChannelId"));
    	} else {
    		if (config.getLong("Discord.ChannelId", 0) == 0 || !isDiscord) return;
    		channelId = Long.toString(config.getLong("Discord.ChannelId"));
    	}
        
        channel = jda.getTextChannelById(channelId);
        
        if (Objects.isNull(channel)) {
            //logger.info("Channel not found!");
            return;
        }
        
        channel.retrieveMessageById(messageId).queue(
            message -> {
                List<MessageEmbed> embeds = message.getEmbeds();
                //logger.info("Message retrieved with " + embeds.size() + " embeds.");
                //logger.info("Message Id: "+messageId);
                if (!embeds.isEmpty()) {
                    // 最初のEmbedを取得して消費
                    embedConsumer.accept(embeds.get(0));
                } else {
                    logger.info("No embeds found in the message.");
                    embedConsumer.accept(null);
                }
            },
            error -> {
                logger.error("A getBotMessage error occurred: " + error.getMessage());
                for (StackTraceElement element : error.getStackTrace()) {
                    logger.error(element.toString());
                }

                embedConsumer.accept(null);
            }
        );
    }
    
    @Override
    public MessageEmbed addDescriptionToEmbed(MessageEmbed embed, String additionalDescription) {
        EmbedBuilder builder = new EmbedBuilder(embed);
        
        String existingDescription = embed.getDescription();
        String newDescription = (existingDescription != null ? existingDescription : "") + additionalDescription;
        
        builder.setDescription(newDescription);
        
        return builder.build();
    }
    
    @Override
    public void editBotEmbedReplacedAll(String messageId, MessageEmbed newEmbed) {
    	 if (config.getLong("Discord.ChannelId", 0)==0 || !isDiscord) return;
    	 
        // チャンネルIDは適切に設定してください
        channelId = Long.toString(config.getLong("Discord.ChannelId"));
        channel = jda.getTextChannelById(channelId);
        
        if (Objects.isNull(channel)) return;
        
        MessageEditAction messageAction = channel.editMessageEmbedsById(messageId, newEmbed);
        messageAction.queue(
            success -> {
                //
            }, error -> {
                logger.error("A editBotEmbedReplacedAll error occurred: " + error.getMessage());
                for (StackTraceElement element : error.getStackTrace()) {
                    logger.error(element.toString());
                }
            }
        );
    }
    
    @Override
    public CompletableFuture<String> sendBotMessageAndgetMessageId(String content, MessageEmbed embed, boolean isChat) {
    	CompletableFuture<String> future = new CompletableFuture<>();
    	
    	if (isChat) {
    		if (config.getLong("Discord.ChatChannelId", 0) == 0 || !isDiscord) {
    			future.complete(null);
                return future;
    		}
    		
    		channelId = Long.toString(config.getLong("Discord.ChatChannelId"));
    	} else {
    		if (config.getLong("Discord.ChannelId", 0)==0 || !isDiscord) {
            	future.complete(null);
                return future;
            }
    		
    		channelId = Long.toString(config.getLong("Discord.ChannelId"));
    	}
        
        channel = jda.getTextChannelById(channelId);
        
        if (Objects.isNull(channel)) {
        	logger.error("Channel not found!");
        	future.complete(null);
            return future;
        }
        
    	if (Objects.nonNull(embed)) {
    		// 埋め込みメッセージを送信
            MessageCreateAction messageAction = channel.sendMessageEmbeds(embed);
            messageAction.queue(response -> {
                // メッセージIDとチャンネルIDを取得
                String messageId = response.getId();
                future.complete(messageId);
                //logger.info("Message ID: " + messageId);
                //logger.info("Channel ID: " + channel.getId());
            }, failure -> {
            	logger.error("Failed to send embedded message: " + failure.getMessage());
                future.complete(null);
            });
        }
    	
    	if (Objects.nonNull(content) && !content.isEmpty()) {
    		// テキストメッセージを送信
            MessageCreateAction messageAction = channel.sendMessage(content);
            messageAction.queue(response -> {
                // メッセージIDとチャンネルIDを取得
                String messageId = response.getId();
                //logger.info("Message ID: " + messageId);
            	//logger.info("Channel ID: " + channel.getId());
            	future.complete(messageId);
            }, failure -> {
            	logger.error("Failed to send text message: " + failure.getMessage());
                future.complete(null);
            }
            );
    	}
    	
    	return future;
    }
    
    @Override
    public CompletableFuture<String> sendBotMessageAndgetMessageId(String content) {
    	return sendBotMessageAndgetMessageId(content, null, false);
    }
    
    @Override
    public CompletableFuture<String> sendBotMessageAndgetMessageId(MessageEmbed embed) {
    	return sendBotMessageAndgetMessageId(null, embed, false);
    }
    
    @Override
    public CompletableFuture<String> sendBotMessageAndgetMessageId(String content, boolean isChat) {
    	return sendBotMessageAndgetMessageId(content, null, isChat);
    }
    
    @Override
    public CompletableFuture<String> sendBotMessageAndgetMessageId(MessageEmbed embed, boolean isChat) {
    	return sendBotMessageAndgetMessageId(null, embed, isChat);
    }
    
    @Override
    public MessageEmbed createEmbed(String description, int color) {
        return new MessageEmbed(
            null, // URL
            null, // Title
            description, // Description
            null, // Type
            null, // Timestamp
            color, // Color
            null, // Thumbnail
            null, // SiteProvider
            null, // Author
            null, // VideoInfo
            null, // Footer
            null, // Image(Example: new MessageEmbed.ImageInfo(imageUrl, null, 0, 0))
            null  // Fields
        );
    }
    
    @Override
    public void sendBotMessage(String content, MessageEmbed embed) {
    	CompletableFuture<String> future = new CompletableFuture<>();
    	
        if (config.getLong("Discord.ChannelId", 0)==0 || !isDiscord) {
        	future.complete(null);
            return;
        }
        
    	channelId = Long.toString(config.getLong("Discord.ChannelId"));
        channel = jda.getTextChannelById(channelId);
        
        if (Objects.isNull(channel)) {
        	//logger.error("Channel not found!");
        	future.complete(null);
            return;
        }
        
    	if (Objects.nonNull(embed)) {
    		// 埋め込みメッセージを送信
            MessageCreateAction messageAction = channel.sendMessageEmbeds(embed);
            messageAction.queue(
                response -> {
                    //
                }, failure -> logger.error("Failed to send embedded message: " + failure.getMessage())
            );
        }
    	
    	if (Objects.nonNull(content) && !content.isEmpty()) {
    		// テキストメッセージを送信
            MessageCreateAction messageAction = channel.sendMessage(content);
            messageAction.queue(
                response -> {
                    //
                }, failure -> logger.error("Failed to send text message: " + failure.getMessage())
            );
    	}
    }
    
    @Override
    public void sendBotMessage(String content) {
    	sendBotMessage(content, null);
    }
    
    @Override
    public void sendBotMessage(MessageEmbed embed) {
    	sendBotMessage(null, embed);
    }
}
