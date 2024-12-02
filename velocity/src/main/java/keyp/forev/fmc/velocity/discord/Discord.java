package keyp.forev.fmc.velocity.discord;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.slf4j.Logger;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookMessage;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import keyp.forev.fmc.common.database.Database;
import keyp.forev.fmc.common.libs.ClassManager;
import keyp.forev.fmc.velocity.Main;
import keyp.forev.fmc.velocity.cmd.sub.VelocityRequest;
import keyp.forev.fmc.velocity.cmd.sub.interfaces.Request;
import keyp.forev.fmc.velocity.libs.VClassManager;
import keyp.forev.fmc.velocity.libs.VPackageManager;
import keyp.forev.fmc.velocity.util.config.VelocityConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;//
import net.dv8tion.jda.api.entities.Activity;//
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.interactions.commands.OptionType;//
import net.dv8tion.jda.api.interactions.commands.build.OptionData;//
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;//
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.GatewayIntent;//
import net.dv8tion.jda.api.requests.restaction.CommandCreateAction;//
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.MessageEditAction;

public class Discord {
    public static Object jdaInstance = null;
	public static JDA jda = null; // あとでコメントアウト
	public static boolean isDiscord = false;
    private final Logger logger;
    private final VelocityConfig config;
    private final Database db;
    private final Request req;
    private final ClassManager subcommandDataClazz = VClassManager.JDA.SUB_COMMAND.get(),
        optionDataClazz = VClassManager.JDA.OPTION_DATA.get(),
        optionTypeClazz = VClassManager.JDA.OPTION_TYPE.get(),
        entityMessageClazz = VClassManager.JDA.ENTITYS_MESSAGE.get(),
        entityActivityClazz = VClassManager.JDA.ENTITYS_ACTIVITY.get(),
        buttonClazz = VClassManager.JDA.BUTTON.get(),
        presenceActivityClazz = VClassManager.JDA.PRESENCE.get(),
        webhookClientClazz = VClassManager.CLUB_MINNCED.WEBHOOK_CLIENT.get(),
        webhookMessageClazz = VClassManager.CLUB_MINNCED.WEBHOOK_MESSAGE.get();
        
    public Discord (Logger logger, VelocityConfig config, Database db, Request req) {
    	this.logger = logger;
    	this.config = config;
        this.db = db;
        this.req = req;
    }

    public CompletableFuture<Object> loginDiscordBotAsync() {
        return CompletableFuture.supplyAsync(() -> {
            if (config.getString("Discord.Token","").isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }
            try {
                jdaInstance = createJDAInstance();
                jdaInstance.getClass().getMethod("awaitReady").invoke(jdaInstance);
                jdaInstance.getClass().getMethod("upsertCommand", String.class, String.class)
                    .invoke(jdaInstance, "fmc", "FMC commands");
                Object createFMCCommand = jdaInstance.getClass().getMethod("upsertCommand", String.class, String.class)
                    .invoke(jdaInstance, "fmc", "FMC commands");
                Object teraSubCommand = VClassManager.JDA.SUB_COMMAND.get()
                    .getInstance("tera", "テラリアコマンド")
                        .getClass().getMethod("addOptions", optionDataClazz.getClazz())
                            .invoke(
                                optionDataClazz.getInstance(optionDataClazz.getField("STRING").get(null), "action", "選択してください。")
                                .getClass().getMethod("addChoice", String.class, String.class)
                                    .invoke(optionDataClazz.getClazz(), "Start", "start")
                                .getClass().getMethod("addChoice", String.class, String.class)
                                    .invoke(optionDataClazz.getClazz(), "Stop", "stop")
                                .getClass().getMethod("addChoice", String.class, String.class)
                                    .invoke(optionDataClazz.getClazz(), "Status", "status")
                            );
                Object createImageSubcommand = subcommandDataClazz.getInstance("image_add_q", "画像マップをキューに追加するコマンド(urlか添付ファイルのどっちかを指定可能)")
                    .getClass().getMethod("addOptions", optionDataClazz.getClazz())
                        .invoke(
                            optionDataClazz.getInstance("image_add_q", "画像マップをキューに追加するコマンド(urlか添付ファイルのどっちかを指定可能)")
                                .getClass().getMethod("setRequired", boolean.class)
                                .invoke(optionDataClazz, true),
                            optionDataClazz.getInstance(optionDataClazz.getField("STRING").get(null), "url", "画像リンクの設定項目"),
                            optionDataClazz.getInstance(optionDataClazz.getField("ATTACHMENT").get(null), "image", "ファイルの添付項目"),
                            optionDataClazz.getInstance(optionDataClazz.getField("STRING").get(null), "title", "画像マップのタイトル設定項目"),
                            optionDataClazz.getInstance(optionDataClazz.getField("STRING").get(null), "comment", "画像マップのコメント設定項目")
                        );
                Object createSyncRuleBookSubcommand = subcommandDataClazz.getInstance("syncrulebook", "ルールブックの同期を行うコマンド");
                createFMCCommand.getClass().getMethod("addSubcommands", subcommandDataClazz.getClazz().arrayType())
                    .invoke(createFMCCommand, new Object[]{teraSubCommand, createImageSubcommand, createSyncRuleBookSubcommand})
                    .getClass().getMethod("queue").invoke(createFMCCommand);
                jdaInstance.getClass().getMethod("getPresence")
                    .invoke(jdaInstance)
                        .getClass().getMethod("setActivity", entityActivityClazz.getClazz())
                            .invoke(
                                presenceActivityClazz.getClazz().getMethod("playing", String.class, String.class)
                                    .invoke(entityActivityClazz.getClazz(), config.getString("Discord.Presence.Activity", "FMCサーバー"))
                            );
                isDiscord = true;
                logger.info("discord bot has been logged in.");
                return jdaInstance;
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassNotFoundException | InstantiationException | NoSuchFieldException e) {
                logger.error("An discord-bot-login error occurred: " + e.getMessage());
                for (StackTraceElement element : e.getStackTrace()) {
                    logger.error(element.toString());
                }
                return CompletableFuture.completedFuture(null);
            }
        });
    }

    private Object createJDAInstance() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, InstantiationException, NoSuchFieldException {
        Class<?> jdabuilderClass = VClassManager.JDA.JDA_BUILDER.get().getClazz();
        Class<?> gatewayIntentClass = VClassManager.JDA.GATEWAY_INTENTS.get().getClazz();
        Object intent = gatewayIntentClass.getField("GUILD_MESSAGES").get(null);
        Object intent2 = gatewayIntentClass.getField("MESSAGE_CONTENT").get(null);
        Object jdaBuilder = jdabuilderClass.getMethod("createDefault", String.class)
            .invoke(null, config.getString("Discord.Token"))
            .getClass().getMethod("addEventListeners", Object.class)
                .invoke(jdaBuilder, Main.getInjector().getInstance(DiscordEventListener.class))
            .getClass().getMethod("enableIntents", gatewayIntentClass)
                .invoke(jdaBuilder, intent, intent2)
            .getClass().getMethod("build").invoke(jdaBuilder);
        return jdaBuilder;
    }
    
    public CompletableFuture<Void> logoutDiscordBot() {
        return CompletableFuture.runAsync(() -> {
            try {
                if (jdaInstance != null) {
                    jdaInstance.getClass().getMethod("shutdown").invoke(jdaInstance);
                    isDiscord = false;
                    logger.info("Discord-Botがログアウトしました。");
                }
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                logger.error("An discord-bot-logout error occurred: " + e.getMessage());
                for (StackTraceElement element : e.getStackTrace()) {
                    logger.error(element.toString());
                }
            }
        });
    }

    
    public CompletableFuture<String> getMessageContent() throws ClassNotFoundException{
        CompletableFuture<String> future = new CompletableFuture<>();
        String ruleChannelId = Long.toString(config.getLong("Discord.Rule.ChannelId", 0));
        String ruleMessageId = Long.toString(config.getLong("Discord.Rule.MessageId", 0));
        Object ruleChannel = jdaInstance.getClass().getMethod("getTextChannelById", String.class)
            .invoke(jdaInstance, Long.toString(config.getLong("Discord.Rule.ChannelId", 0)));
        if (ruleChannel == null) {
            future.completeExceptionally(new IllegalArgumentException("チャンネルが見つかりませんでした。"));
            return future;
        }
        ruleChannel.getClass().getMethod("retrieveMessageById", String.class)
            .invoke(ruleChannel, ruleMessageId)
            .getClass().getMethod("queue", Consumer.class, Consumer.class)
                .invoke(ruleChannel,
                (Consumer<Object>) message -> {
                    try {
                        String content = (String) entityMessageClazz.getClazz().getMethod("getContentDisplay").invoke(message);
                        future.complete(content);
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                },
                (Consumer<Throwable>) throwable -> {
                    if (throwable instanceof ErrorResponseException e) {
                        future.completeExceptionally(new RuntimeException("エラーが発生しました: " + e.getErrorResponse().getMeaning()));
                    } else {
                        future.completeExceptionally(throwable);
                    }
                });
            ;
        return future;
    }

    
    public void sendRequestButtonWithMessage(String buttonMessage) {
    	if (config.getLong("Discord.AdminChannelId", 0) == 0 || !isDiscord) return;
		String channelId = Long.toString(config.getLong("Discord.AdminChannelId"));
        Object channel = jdaInstance.getClass().getMethod("getTextChannelById", String.class)
            .invoke(jdaInstance, channelId);
        if (channel == null) return;
        Object button1 = buttonClazz.getClazz().getMethod("success", String.class, String.class)
            .invoke(buttonClazz, "reqOK", "YES");
        Object button2 = buttonClazz.getClazz().getMethod("danger", String.class, String.class)
            .invoke(buttonClazz, "reqCancel", "NO");
        channel.getClass().getMethod("sendMessage", String.class)
            .invoke(channel, buttonMessage)
            .getClass().getMethod("setActionRow", buttonClazz.getClazz(), buttonClazz.getClazz())
                .invoke(channel, button1, button2)
            .getClass().getMethod("queue", Consumer.class)
                .invoke(channel,
                (Consumer<Object>) message -> {
                    try {
                        CompletableFuture.delayedExecutor(3, TimeUnit.MINUTES).execute(() -> {
                            if (!VelocityRequest.PlayerReqFlags.isEmpty()) {
                                String buttonMessage2 = (String) entityMessageClazz.getClazz().getMethod("getContentRaw").invoke(message);
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
                    } catch (Exception e) {
                        logger.error("An error occurred: " + e.getMessage());
                        for (StackTraceElement element : e.getStackTrace()) {
                            logger.error(element.toString());
                        }
                    }
                });
    }
    
    public void sendWebhookMessage(WebhookMessageBuilder builder) {
    	String webhookUrl = config.getString("Discord.Webhook_URL","");
    	if (webhookUrl.isEmpty()) return;
        CompletableFuture<?> sendFuture = (CompletableFuture<?>) webhookClientClazz.getClazz().getMethod("withUrl", String.class)
            .invoke(webhookClientClazz, webhookUrl)
            .getClass().getMethod("send", webhookMessageClazz.getClazz())
                .invoke(webhookClientClazz, builder.build());
        sendFuture.thenAccept(CompletableFuture::completedFuture).exceptionally(throwable -> {
            logger.error("A sendWebhookMessage error occurred: " + throwable.getMessage());
            for (StackTraceElement element : throwable.getStackTrace()) {
                logger.error(element.toString());
            }
            return null;
        });
    }

    
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

    
    public CompletableFuture<Void> editBotEmbed(String messageId, String additionalDescription) {
    	return editBotEmbed(messageId, additionalDescription, false);
    }

    
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

    
    public MessageEmbed addDescriptionToEmbed(MessageEmbed embed, String additionalDescription) {
        EmbedBuilder builder = new EmbedBuilder(embed);

        String existingDescription = embed.getDescription();
        String newDescription = (existingDescription != null ? existingDescription : "") + additionalDescription;

        builder.setDescription(newDescription);

        return builder.build();
    }

    
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

    
    public CompletableFuture<String> sendBotMessageAndgetMessageId(String content) {
    	return sendBotMessageAndgetMessageId(content, null, false);
    }

    
    public CompletableFuture<String> sendBotMessageAndgetMessageId(MessageEmbed embed) {
    	return sendBotMessageAndgetMessageId(null, embed, false);
    }

    
    public CompletableFuture<String> sendBotMessageAndgetMessageId(String content, boolean isChat) {
    	return sendBotMessageAndgetMessageId(content, null, isChat);
    }

    
    public CompletableFuture<String> sendBotMessageAndgetMessageId(MessageEmbed embed, boolean isChat) {
    	return sendBotMessageAndgetMessageId(null, embed, isChat);
    }

    
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

    
    public void sendBotMessage(String content) {
    	sendBotMessage(content, null);
    }

    
    public void sendBotMessage(MessageEmbed embed) {
    	sendBotMessage(null, embed);
    }
}
