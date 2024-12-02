package keyp.forev.fmc.velocity.discord;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;

//import club.minnced.discord.webhook.WebhookClient;
//import club.minnced.discord.webhook.send.WebhookMessage;
//import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import keyp.forev.fmc.common.database.Database;
import keyp.forev.fmc.common.libs.ClassManager;
import keyp.forev.fmc.velocity.Main;
import keyp.forev.fmc.velocity.cmd.sub.VelocityRequest;
import keyp.forev.fmc.velocity.cmd.sub.interfaces.Request;
import keyp.forev.fmc.velocity.libs.VClassManager;
import keyp.forev.fmc.velocity.libs.VPackageManager;
import keyp.forev.fmc.velocity.util.config.VelocityConfig;
//import net.dv8tion.jda.api.EmbedBuilder;
//import net.dv8tion.jda.api.JDA;
//import net.dv8tion.jda.api.JDABuilder;
//import net.dv8tion.jda.api.entities.Activity;
//import net.dv8tion.jda.api.entities.MessageEmbed;
//import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
//import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
//import net.dv8tion.jda.api.exceptions.ErrorResponseException;
//import net.dv8tion.jda.api.interactions.commands.OptionType;
//import net.dv8tion.jda.api.interactions.commands.build.OptionData;
//import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
//import net.dv8tion.jda.api.interactions.components.buttons.Button;
//import net.dv8tion.jda.api.requests.GatewayIntent;
//import net.dv8tion.jda.api.requests.restaction.CommandCreateAction;
//import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
//import net.dv8tion.jda.api.requests.restaction.MessageEditAction;
//import net.dv8tion.jda.api.entities.MessageEmbed;
//import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
//import net.dv8tion.jda.api.requests.restaction.MessageEditAction;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.MessageEditAction;

public class Discord {
    public static Object jdaInstance = null;
	//public static JDA jda = null;
	public static boolean isDiscord = false;
    private final Logger logger;
    private final VelocityConfig config;
    private final Database db;
    private final Request req;
    private final Class<?> jdaBuilderClazz, gatewayIntentsClazz, subcommandDataClazz, 
        optionDataClazz, optionTypeClazz, entityMessageClazz,
        entityActivityClazz, entityMessageEmbedClazz, buttonClazz,
        presenceActivityClazz, webhookClientClazz, webhookMessageClazz,
        embedBuilderClazz, errorResponseExceptionClazz;
    public Discord(Logger logger, VelocityConfig config, Database db, Request req) throws ClassNotFoundException {
    	this.logger = logger;
    	this.config = config;
        this.db = db;
        this.req = req;
        this.jdaBuilderClazz = VClassManager.JDA.JDA_BUILDER.get().getClazz();
        this.gatewayIntentsClazz = VClassManager.JDA.GATEWAY_INTENTS.get().getClazz();
        this.subcommandDataClazz = VClassManager.JDA.SUB_COMMAND.get().getClazz();
        this.optionDataClazz = VClassManager.JDA.OPTION_DATA.get().getClazz();
        this.optionTypeClazz = VClassManager.JDA.OPTION_TYPE.get().getClazz();
        this.entityMessageClazz = VClassManager.JDA.ENTITYS_MESSAGE.get().getClazz();
        this.entityActivityClazz = VClassManager.JDA.ENTITYS_ACTIVITY.get().getClazz();
        this.entityMessageEmbedClazz = VClassManager.JDA.ENTITYES_MESSAGE_EMBED.get().getClazz();
        this.buttonClazz = VClassManager.JDA.BUTTON.get().getClazz();
        this.presenceActivityClazz = VClassManager.JDA.PRESENCE.get().getClazz();
        this.webhookClientClazz = VClassManager.CLUB_MINNCED.WEBHOOK_CLIENT.get().getClazz();
        this.webhookMessageClazz = VClassManager.CLUB_MINNCED.WEBHOOK_MESSAGE.get().getClazz();
        this.embedBuilderClazz = VClassManager.JDA.EMBED_BUILDER.get().getClazz();
        this.errorResponseExceptionClazz = VClassManager.JDA.ERROR_RESPONSE_EXCEPTION.get().getClazz();
    }

    public CompletableFuture<Object> loginDiscordBotAsync() {
        return CompletableFuture.supplyAsync(() -> {
            if (config.getString("Discord.Token","").isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }
            try {
                Field gatewayIntent = gatewayIntentsClazz.getField("GUILD_MESSAGES");
                Field gatewayIntent2 = gatewayIntentsClazz.getField("MESSAGE_CONTENT");

                Enum<?> intent = (Enum<?>) gatewayIntent.get(null);
                Enum<?> intent2 = (Enum<?>) gatewayIntent2.get(null);

                Method createDefaultMethod = jdaBuilderClazz.getMethod("createDefault", String.class);
                Object jdaBuilder = createDefaultMethod.invoke(null, config.getString("Discord.Token"));

                Method addEventListenersMethod = jdaBuilder.getClass().getMethod("addEventListeners", Object.class);
                jdaBuilder = addEventListenersMethod.invoke(jdaBuilder, Main.getInjector().getInstance(DiscordEventListener.class));

                Method enableIntentsMethod = jdaBuilder.getClass().getMethod("enableIntents", gatewayIntentsClazz, gatewayIntentsClazz);
                jdaBuilder = enableIntentsMethod.invoke(jdaBuilder, intent, intent2);

                Method buildMethod = jdaBuilder.getClass().getMethod("build");
                jdaInstance = buildMethod.invoke(jdaBuilder);

                Method awaitReadyMethod = jdaInstance.getClass().getMethod("awaitReady");
                awaitReadyMethod.invoke(jdaInstance);

                Method upsertCommandMethod = jdaInstance.getClass().getMethod("upsertCommand", String.class, String.class);
                Object createFMCCommand = upsertCommandMethod.invoke(jdaInstance, "fmc", "FMC commands");

                Field optionTypeStringField = optionTypeClazz.getField("STRING");
                Field optionTypeAttachmentField = optionTypeClazz.getField("ATTACHMENT");

                Object stringType = optionTypeStringField.get(null);
                Object attachmentType = optionTypeAttachmentField.get(null);

                Constructor<?> subcommandC = subcommandDataClazz.getConstructor(String.class, String.class);
                Object teraSubCommand = subcommandC.newInstance("tera", "テラリアコマンド");

                Method addOptionsMethod = teraSubCommand.getClass().getMethod("addOptions", optionDataClazz);
                Object optionDataInstance = optionDataClazz.getConstructor(optionTypeClazz, String.class, String.class)
                    .newInstance(stringType, "action", "選択してください。");

                Method addChoiceMethod = optionDataInstance.getClass().getMethod("addChoice", String.class, String.class);
                addChoiceMethod.invoke(optionDataInstance, "Start", "start");
                addChoiceMethod.invoke(optionDataInstance, "Stop", "stop");
                addChoiceMethod.invoke(optionDataInstance, "Status", "status");

                addOptionsMethod.invoke(teraSubCommand, optionDataInstance);

                Object createImageSubcommand = subcommandDataClazz.getConstructor(String.class, String.class)
                    .newInstance("image_add_q", "画像マップをキューに追加するコマンド(urlか添付ファイルのどっちかを指定可能)");

                Constructor<?> optionDataC = optionDataClazz.getConstructor(optionTypeClazz, String.class, String.class);

                addOptionsMethod = createImageSubcommand.getClass().getMethod("addOptions", optionDataClazz);
                addOptionsMethod.invoke(createImageSubcommand,
                    optionDataC.newInstance(stringType, "url", "画像リンクの設定項目"),
                    optionDataC.newInstance(attachmentType, "image", "ファイルの添付項目"),
                    optionDataC.newInstance(stringType, "title", "画像マップのタイトル設定項目"),
                    optionDataC.newInstance(stringType, "comment", "画像マップのコメント設定項目")
                );

                Object createSyncRuleBookSubcommand = subcommandDataClazz.getConstructor(String.class, String.class)
                    .newInstance("syncrulebook", "ルールブックの同期を行うコマンド");

                Method addSubcommandsMethod = createFMCCommand.getClass().getMethod("addSubcommands", subcommandDataClazz.arrayType());
                addSubcommandsMethod.invoke(createFMCCommand, new Object[]{teraSubCommand, createImageSubcommand, createSyncRuleBookSubcommand});

                Method queueMethod = createFMCCommand.getClass().getMethod("queue");
                queueMethod.invoke(createFMCCommand);

                Method getPresenceMethod = jdaInstance.getClass().getMethod("getPresence");
                Object presence = getPresenceMethod.invoke(jdaInstance);

                Method setActivityMethod = presence.getClass().getMethod("setActivity", entityActivityClazz);
                Method playingMethod = presenceActivityClazz.getMethod("playing", String.class);
                Object activity = playingMethod.invoke(null, config.getString("Discord.Presence.Activity", "FMCサーバー"));
                setActivityMethod.invoke(presence, activity);

                isDiscord = true;
                logger.info("discord bot has been logged in.");
                return jdaInstance;
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException | NoSuchFieldException e) {
                logger.error("An discord-bot-login error occurred: " + e.getMessage());
                for (StackTraceElement element : e.getStackTrace()) {
                    logger.error(element.toString());
                }
                return CompletableFuture.completedFuture(null);
            }
        });
    }

    public CompletableFuture<Void> logoutDiscordBot() {
        return CompletableFuture.runAsync(() -> {
            try {
                if (jdaInstance != null) {
                    Method shutdownMethod = jdaInstance.getClass().getMethod("shutdown");
                    shutdownMethod.invoke(jdaInstance);
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

    public CompletableFuture<String> getMessageContent() throws Exception {
        CompletableFuture<String> future = new CompletableFuture<>();
        String ruleChannelId = Long.toString(config.getLong("Discord.Rule.ChannelId", 0));
        String ruleMessageId = Long.toString(config.getLong("Discord.Rule.MessageId", 0));

        Method getTextChannelByIdMethod = jdaInstance.getClass().getMethod("getTextChannelById", String.class);
        Object ruleChannel = getTextChannelByIdMethod.invoke(jdaInstance, ruleChannelId);

        if (ruleChannel == null) {
            future.completeExceptionally(new IllegalArgumentException("チャンネルが見つかりませんでした。"));
            return future;
        }

        Method retrieveMessageByIdMethod = ruleChannel.getClass().getMethod("retrieveMessageById", String.class);
        Object retrieveMessageByIdResult = retrieveMessageByIdMethod.invoke(ruleChannel, ruleMessageId);

        Method queueMethod = retrieveMessageByIdResult.getClass().getMethod("queue", Consumer.class, Consumer.class);
        queueMethod.invoke(retrieveMessageByIdResult,
            (Consumer<Object>) message -> {
                try {
                    Method getContentDisplayMethod = entityMessageClazz.getMethod("getContentDisplay");
                    String content = (String) getContentDisplayMethod.invoke(message);
                    future.complete(content);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            },
            (Consumer<Throwable>) throwable -> {
                try {
                    if (errorResponseExceptionClazz.isInstance(throwable)) {
                        Method getErrorResponseMethod = errorResponseExceptionClazz.getMethod("getErrorResponse");
                        Object errorResponse = getErrorResponseMethod.invoke(throwable);

                        Method getMeaningMethod = errorResponse.getClass().getMethod("getMeaning");
                        String meaning = (String) getMeaningMethod.invoke(errorResponse);

                        logger.error("A sendWebhookMessage error occurred: " + meaning);
                    } else {
                        logger.error("A sendWebhookMessage error occurred: " + throwable.getMessage());
                    }

                    for (StackTraceElement element : throwable.getStackTrace()) {
                        logger.error(element.toString());
                    }

                    future.completeExceptionally(throwable);
                } catch (Exception e) {
                    logger.error("An error occurred: " + e.getMessage());
                    for (StackTraceElement element : e.getStackTrace()) {
                        logger.error(element.toString());
                    }
                }
            }
        );

        return future;
    }

    
    public void sendRequestButtonWithMessage(String buttonMessage) {
    	if (config.getLong("Discord.AdminChannelId", 0) == 0 || !isDiscord) return;
		String channelId = Long.toString(config.getLong("Discord.AdminChannelId"));

        try {
            Method getTextChannelByIdMethod = jdaInstance.getClass().getMethod("getTextChannelById", String.class);
            Object channel = getTextChannelByIdMethod.invoke(jdaInstance, channelId);

            if (channel == null) return;

            Method successButtonMethod = buttonClazz.getMethod("success", String.class, String.class);
            Method dangerButtonMethod = buttonClazz.getMethod("danger", String.class, String.class);

            Object button1 = successButtonMethod.invoke(buttonClazz, "reqOK", "YES");
            Object button2 = dangerButtonMethod.invoke(buttonClazz, "reqCancel", "NO");

            Method sendMessageMethod = channel.getClass().getMethod("sendMessage", String.class);
            Object sendMessageResult = sendMessageMethod.invoke(channel, buttonMessage);

            Method setActionRowMethod = sendMessageResult.getClass().getMethod("setActionRow", buttonClazz, buttonClazz);
            Object setActionRowResult = setActionRowMethod.invoke(sendMessageResult, button1, button2);

            Method queueMethod = setActionRowResult.getClass().getMethod("queue", Consumer.class);
            queueMethod.invoke(setActionRowResult, (Consumer<Object>) message -> {
                try {
                    CompletableFuture.delayedExecutor(3, TimeUnit.MINUTES).execute(() -> {
                        if (!VelocityRequest.PlayerReqFlags.isEmpty()) {
                            try {
                                String buttonMessage2 = (String) entityMessageClazz.getMethod("getContentRaw").invoke(message);
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
                            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
                                    | NoSuchMethodException | SecurityException e) {
                                e.printStackTrace();
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
        } catch (Exception e) {
            logger.error("An error occurred: " + e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                logger.error(element.toString());
            }
        }
    }
    
    public void sendWebhookMessage(Object builder) {
    	String webhookUrl = config.getString("Discord.Webhook_URL","");

    	if (webhookUrl.isEmpty()) return;

        try {
            Method withUrlMethod = webhookClientClazz.getMethod("withUrl", String.class);
            Object webhookClient = withUrlMethod.invoke(webhookClientClazz, webhookUrl);

            Method buildMethod = builder.getClass().getMethod("build");
            Object buildResult = buildMethod.invoke(builder);

            Method sendMethod = webhookClient.getClass().getMethod("send", webhookMessageClazz);
            Object sendResult = sendMethod.invoke(webhookClient, buildResult);

            Method thenAcceptMethod = sendResult.getClass().getMethod("thenAccept", Consumer.class);
            thenAcceptMethod.invoke(sendResult, (Consumer<Object>) _p -> {
                try {
                    Method exceptionallyMethod = sendResult.getClass().getMethod("exceptionally", Function.class);
                    exceptionallyMethod.invoke(sendResult, (Function<Throwable, Object>) throwable -> {
                        logger.error("A sendWebhookMessage error occurred: " + throwable.getMessage());
                        for (StackTraceElement element : throwable.getStackTrace()) {
                            logger.error(element.toString());
                        }
                        return null;
                    });
                } catch (Exception e) {
                    logger.error("An error occurred: " + e.getMessage());
                    for (StackTraceElement element : e.getStackTrace()) {
                        logger.error(element.toString());
                    }
                }
            });
        } catch (Exception e) {
            logger.error("An error occurred: " + e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                logger.error(element.toString());
            }
        }
    }

    public CompletableFuture<Void> editBotEmbed(String messageId, String additionalDescription, boolean isChat) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        getBotMessage(messageId, currentEmbed -> {
            if (currentEmbed == null) {
                future.completeExceptionally(new RuntimeException("No embed found to edit."));
                return;
            }
            String channelId;
            if (isChat) {
                channelId = Long.toString(config.getLong("Discord.ChatChannelId", 0));
                if (channelId == "0" || !isDiscord) {
                    future.completeExceptionally(new RuntimeException("Chat channel ID is invalid or Discord is not enabled."));
                    return;
                }
            } else {
                channelId = Long.toString(config.getLong("Discord.ChannelId", 0));
                if (channelId == "0" || !isDiscord) {
                    future.completeExceptionally(new RuntimeException("Channel ID is invalid or Discord is not enabled."));
                    return;
                }
            }

            try {
                Method getTextChannelByIdMethod = jdaInstance.getClass().getMethod("getTextChannelById", String.class);
                Object channel = getTextChannelByIdMethod.invoke(jdaInstance, channelId);

                if (channel == null) {
                    future.completeExceptionally(new RuntimeException("Channel not found!"));
                    return;
                }

                Object newEmbed = addDescriptionToEmbed(currentEmbed, additionalDescription);

                Method editMessageEmbedsByIdMethod = channel.getClass().getMethod("editMessageEmbedsById", String.class, entityMessageEmbedClazz);
                Object messageAction = editMessageEmbedsByIdMethod.invoke(channel, messageId, newEmbed);

                Method queueMethod = messageAction.getClass().getMethod("queue", Consumer.class, Consumer.class);
                queueMethod.invoke(messageAction,
                    (Consumer<Object>) _p -> future.complete(null),
                    (Consumer<Throwable>) error -> {
                        future.completeExceptionally((Throwable) error);
                        logger.info("Failed to edit message with ID: " + messageId);
                    }
                );
            } catch (Exception e) {
                future.completeExceptionally(e);
                logger.error("An error occurred: " + e.getMessage());
                for (StackTraceElement element : e.getStackTrace()) {
                    logger.error(element.toString());
                }
            }
        }, isChat);

        return future;
    }

    
    public CompletableFuture<Void> editBotEmbed(String messageId, String additionalDescription) {
    	return editBotEmbed(messageId, additionalDescription, false);
    }

    
    public void getBotMessage(String messageId, Consumer<Object> embedConsumer, boolean isChat) {
        String channelId;
    	if (isChat) {
    		if (config.getLong("Discord.ChatChannelId", 0) == 0 || !isDiscord) return;
            channelId = Long.toString(config.getLong("Discord.ChatChannelId"));
    	} else {
    		if (config.getLong("Discord.ChannelId", 0) == 0 || !isDiscord) return;
    		channelId = Long.toString(config.getLong("Discord.ChannelId"));
    	}

        try {
            Object channel = jdaInstance.getClass().getMethod("getTextChannelById", String.class).invoke(jdaInstance, channelId);

            if (channel == null) return;

            Method retrieveMessageByIdMethod = channel.getClass().getMethod("retrieveMessageById", String.class);
            retrieveMessageByIdMethod.invoke(channel, messageId);

            Method queueMethod = retrieveMessageByIdMethod.getReturnType().getMethod("queue", Consumer.class, Consumer.class);
            queueMethod.invoke(retrieveMessageByIdMethod.invoke(channel, messageId),
                (Consumer<Object>) message -> {
                    try {
                        Method getEmbedsMethod = entityMessageClazz.getMethod("getEmbeds");
                        Object embeds = getEmbedsMethod.invoke(message);
                        if (embeds instanceof List<?>) {
                            // embedsがList<Object>型であることを保証しなければならない
                            boolean isList = ((List<?>) embeds).stream().allMatch(e -> e instanceof Object);
                            if (isList) {
                                @SuppressWarnings("unchecked")
                                List<Object> embedList = (List<Object>) embeds;
                                if (!embedList.isEmpty()) {
                                    embedConsumer.accept(embedList.get(0));
                                } else {
                                    embedConsumer.accept(null);
                                }
                            }
                        } else {
                            embedConsumer.accept(null);
                        }
                    } catch (Exception e) {
                        logger.error("An error occurred: " + e.getMessage());
                        for (StackTraceElement element : e.getStackTrace()) {
                            logger.error(element.toString());
                        }
                    }
                },
                (Consumer<Throwable>) error -> {
                    logger.error("A getBotMessage error occurred: " + error.getMessage());
                    for (StackTraceElement element : error.getStackTrace()) {
                        logger.error(element.toString());
                    }
                    embedConsumer.accept(null);
                }
            );
        } catch (Exception e) {
            logger.error("An error occurred: " + e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                logger.error(element.toString());
            }
        }
    }

    
    public Object addDescriptionToEmbed(Object embed, String additionalDescription) {
        try {
            Method getDescriptionMethod = embed.getClass().getMethod("getDescription");
            String description = (String) getDescriptionMethod.invoke(embed);

            Constructor<?> embedBuilderC = embedBuilderClazz.getConstructor(entityMessageEmbedClazz);
            Object builder = embedBuilderC.newInstance(embed);

            String newDescription = (description != null ? description : "") + additionalDescription;
            Method setDescriptionMethod = embedBuilderClazz.getMethod("setDescription", CharSequence.class);
            setDescriptionMethod.invoke(builder, newDescription);

            Method buildMethod = embedBuilderClazz.getMethod("build");

            return buildMethod.invoke(builder);
        } catch (Exception e) {
            logger.error("An error occurred: " + e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                logger.error(element.toString());
            }
            return null;
        }
    }
    
    public void editBotEmbedReplacedAll(String messageId, Object newEmbed) {
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
