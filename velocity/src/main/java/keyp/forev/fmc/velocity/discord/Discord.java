package keyp.forev.fmc.velocity.discord;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.slf4j.Logger;

import com.google.inject.Inject;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookMessage;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import keyp.forev.fmc.common.Database;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandCreateAction;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.MessageEditAction;
import keyp.forev.fmc.velocity.cmd.Request;
import keyp.forev.fmc.velocity.cmd.RequestInterface;
import keyp.forev.fmc.velocity.util.Config;
import keyp.forev.fmc.velocity.Main;

public class Discord implements DiscordInterface {
	public static JDA jda = null;
	public static boolean isDiscord = false;
    private final Logger logger;
    private final Config config;
    private final Database db;
    private final RequestInterface req;
    private String channelId = null;
    private MessageChannel channel= null;
	
    @Inject
    public Discord (Logger logger, Config config, Database db, RequestInterface req) {
    	this.logger = logger;
    	this.config = config;
        this.db = db;
        this.req = req;
    }
    
    @Override
    public CompletableFuture<JDA> loginDiscordBotAsync() {
        return CompletableFuture.supplyAsync(() -> {
            if (config.getString("Discord.Token","").isEmpty()) {
                return null;
            }
            //server.getScheduler().buildTask(plugin, () -> {}).schedule();
            try {
                jda = JDABuilder.createDefault(config.getString("Discord.Token"))
                        .addEventListeners(Main.getInjector().getInstance(DiscordEventListener.class))
                        .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                        // .enableCache(CacheFlag.MESSAGE_CONTENT)
                        .build();

                // Botが完全に起動するのを待つ
                jda.awaitReady();
                CommandCreateAction createFMCCommand = jda.upsertCommand("fmc", "FMC commands");
                SubcommandData teraSubCommand = new SubcommandData("tera", "テラリアコマンド")
                    .addOptions(new OptionData(OptionType.STRING, "action", "選択してください。")
                        .addChoice("Start", "start")
                        .addChoice("Stop", "stop")
                        .addChoice("Status", "status")
                    );
                SubcommandData createImageSubcommand = new SubcommandData("image_add_q", "画像マップをキューに追加するコマンド(urlか添付ファイルのどっちかを指定可能)")
                    .addOptions(
                        //new OptionData(OptionType.BOOLEAN, "isqr", "QRコードか否かを選択するオプション").setRequired(true), // QRコードか否かを選択するオプション
                        new OptionData(OptionType.STRING, "url", "画像リンクの設定項目").setRequired(false),
                        new OptionData(OptionType.ATTACHMENT, "image", "ファイルの添付項目").setRequired(false),
                        new OptionData(OptionType.STRING, "title", "画像マップのタイトル設定項目").setRequired(false),
                        new OptionData(OptionType.STRING, "comment", "画像マップのコメント設定項目").setRequired(false)
                    );
                SubcommandData syncRuleBookSubcommand = new SubcommandData("syncrulebook", "ルールブックの同期を行うコマンド");
                createFMCCommand.addSubcommands(teraSubCommand, createImageSubcommand, syncRuleBookSubcommand).queue();
                jda.getPresence().setActivity(Activity.playing(config.getString("Discord.Presence.Activity", "FMCサーバー")));
                isDiscord = true;
                logger.info("discord bot has been logged in.");
                return jda;
            } catch (InterruptedException e) {
                // スタックトレースをログに出力
                logger.error("An discord-bot-login error occurred: " + e.getMessage());
                for (StackTraceElement element : e.getStackTrace()) {
                    logger.error(element.toString());
                }
                return null;
            }
        });
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
    public CompletableFuture<String> getMessageContent() {
        String ruleChannelId = Long.toString(config.getLong("Discord.Rule.ChannelId", 0));
        String ruleMessageId = Long.toString(config.getLong("Discord.Rule.MessageId", 0));
        CompletableFuture<String> future = new CompletableFuture<>();
        TextChannel ruleChannel = jda.getTextChannelById(ruleChannelId);
        if (ruleChannel == null) {
            future.completeExceptionally(new IllegalArgumentException("チャンネルが見つかりませんでした。"));
            return future;
        }
        ruleChannel.retrieveMessageById(ruleMessageId).queue(
            message -> {
                // メッセージの内容を取得してCompletableFutureに設定
                String content = message.getContentDisplay();
                future.complete(content);
            },
            throwable -> {
                if (throwable instanceof ErrorResponseException e) {
                    future.completeExceptionally(new RuntimeException("エラーが発生しました: " + e.getErrorResponse().getMeaning()));
                } else {
                    future.completeExceptionally(throwable);
                }
            }
        );
        return future;
    }

    @Override
    public void sendRequestButtonWithMessage(String buttonMessage) {
    	if (config.getLong("Discord.AdminChannelId", 0) == 0 || !isDiscord) return;
		channelId = Long.toString(config.getLong("Discord.AdminChannelId"));
    	channel = jda.getTextChannelById(channelId);
        if (Objects.isNull(channel)) return;
        Button button1 = Button.success("reqOK", "YES");
        Button button2 = Button.danger("reqCancel", "NO");
        MessageCreateAction action = channel.sendMessage(buttonMessage)
                .setActionRow(button1, button2);
        action.queue(message -> {
            CompletableFuture.delayedExecutor(3, TimeUnit.MINUTES).execute(() -> {
            	if (!Request.PlayerReqFlags.isEmpty()) {
            		String buttonMessage2 = message.getContentRaw();
                    Map<String, String> reqMap = req.paternFinderMapForReq(buttonMessage2);
				    if (!reqMap.isEmpty()) {
                        try (Connection conn = db.getConnection()) {
                            db.insertLog(conn, "INSERT INTO log (name, uuid, reqsul, reqserver, reqsulstatus) VALUES (?, ?, ?, ?, ?);", new Object[] {reqMap.get("playerName"), reqMap.get("playerUUID"), true, reqMap.get("serverName"), "nores"});
                        } catch (SQLException | ClassNotFoundException e) {
                            logger.error("A SQLException | ClassNotFoundException error occurred: {}", e.getMessage());
                            for (StackTraceElement element : e.getStackTrace()) {
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
        
        client.send(message).thenAccept(CompletableFuture::completedFuture).exceptionally(throwable -> {
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
                _p -> future.complete(null),
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
            _p -> {
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
                CompletableFuture::completedFuture, failure -> logger.error("Failed to send embedded message: " + failure.getMessage())
            );
        }
    	if (Objects.nonNull(content) && !content.isEmpty()) {
    		// テキストメッセージを送信
            MessageCreateAction messageAction = channel.sendMessage(content);
            messageAction.queue(
                _p -> {
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
