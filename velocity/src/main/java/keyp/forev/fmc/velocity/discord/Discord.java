package keyp.forev.fmc.velocity.discord;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;

import keyp.forev.fmc.common.database.Database;
import keyp.forev.fmc.common.libs.ClassManager;
import keyp.forev.fmc.velocity.Main;
import keyp.forev.fmc.velocity.cmd.sub.VelocityRequest;
import keyp.forev.fmc.velocity.cmd.sub.interfaces.Request;
import keyp.forev.fmc.velocity.libs.VClassManager;
import keyp.forev.fmc.velocity.libs.VPackageManager;
import keyp.forev.fmc.velocity.util.config.VelocityConfig;
import com.google.inject.Singleton;

@Singleton
public class Discord {
    public static Object jdaInstance = null; // JDAインスタンス // ログイン時に代入される
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

                // リスナーの登録は、独自アノテーションクラスを使用して動的に行う
                // Method addEventListenersMethod = jdaBuilder.getClass().getMethod("addEventListeners", Object[].class);
                // jdaBuilder = addEventListenersMethod.invoke(jdaBuilder, Main.getInjector().getInstance(DiscordEventListener.class));
                try {
                    DynamicEventRegister.registerListeners(
                        Main.getInjector().getInstance(DiscordEventListener.class),
                        jdaInstance,
                        ClassManager.urlClassLoaderMap.get(VPackageManager.JDA)
                    );
                } catch (Exception e) {
                    logger.error("An error occurred: " + e.getMessage());
                    for (StackTraceElement element : e.getStackTrace()) {
                        logger.error(element.toString());
                    }
                }

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
                Object createImageSubcommand = subcommandC.newInstance("image_add_q", "画像マップをキューに追加するコマンド(urlか添付ファイルのどっちかを指定可能)");
                Object createSyncRuleBookSubcommand = subcommandC.newInstance("syncrulebook", "ルールブックの同期を行うコマンド");

                Method addOptionsMethod = createImageSubcommand.getClass().getMethod("addOptions", optionDataClazz);
                Constructor<?> optionDataC = optionDataClazz.getConstructor(optionTypeClazz, String.class, String.class);
                addOptionsMethod.invoke(createImageSubcommand,
                    optionDataC.newInstance(stringType, "url", "画像リンクの設定項目"),
                    optionDataC.newInstance(attachmentType, "image", "ファイルの添付項目"),
                    optionDataC.newInstance(stringType, "title", "画像マップのタイトル設定項目"),
                    optionDataC.newInstance(stringType, "comment", "画像マップのコメント設定項目")
                );

                Method addSubcommandsMethod = createFMCCommand.getClass().getMethod("addSubcommands", subcommandDataClazz.arrayType());
                addSubcommandsMethod.invoke(createFMCCommand, new Object[]{createImageSubcommand, createSyncRuleBookSubcommand});

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


    public void sendRequestButtonWithMessage(String buttonMessage) throws Exception {
    	if (config.getLong("Discord.AdminChannelId", 0) == 0 || !isDiscord) return;
		String channelId = Long.toString(config.getLong("Discord.AdminChannelId"));

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
    }

    public void sendWebhookMessage(Object builder) throws Exception {
    	String webhookUrl = config.getString("Discord.Webhook_URL","");

    	if (webhookUrl.isEmpty()) return;

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
    }

    public CompletableFuture<Void> editBotEmbed(String messageId, String additionalDescription, boolean isChat) throws Exception {
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


    public CompletableFuture<Void> editBotEmbed(String messageId, String additionalDescription) throws Exception {
    	return editBotEmbed(messageId, additionalDescription, false);
    }


    public void getBotMessage(String messageId, Consumer<Object> embedConsumer, boolean isChat) throws Exception {
        String channelId;
    	if (isChat) {
    		if (config.getLong("Discord.ChatChannelId", 0) == 0 || !isDiscord) return;
            channelId = Long.toString(config.getLong("Discord.ChatChannelId"));
    	} else {
    		if (config.getLong("Discord.ChannelId", 0) == 0 || !isDiscord) return;
    		channelId = Long.toString(config.getLong("Discord.ChannelId"));
    	}
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
                    embedConsumer.accept(null);
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
    }


    public Object addDescriptionToEmbed(Object embed, String additionalDescription) throws Exception {
        Method getDescriptionMethod = embed.getClass().getMethod("getDescription");
        String description = (String) getDescriptionMethod.invoke(embed);

        Constructor<?> embedBuilderC = embedBuilderClazz.getConstructor(entityMessageEmbedClazz);
        Object builder = embedBuilderC.newInstance(embed);

        String newDescription = (description != null ? description : "") + additionalDescription;
        Method setDescriptionMethod = embedBuilderClazz.getMethod("setDescription", CharSequence.class);
        setDescriptionMethod.invoke(builder, newDescription);

        Method buildMethod = embedBuilderClazz.getMethod("build");

        return buildMethod.invoke(builder);
    }

    public void editBotEmbedReplacedAll(String messageId, Object newEmbed) throws Exception {
    	if (config.getLong("Discord.ChannelId", 0)==0 || !isDiscord) return;
        String channelId = Long.toString(config.getLong("Discord.ChannelId"));
        Method getTextChannelByIdMethod = jdaInstance.getClass().getMethod("getTextChannelById", String.class);
        Object channel = getTextChannelByIdMethod.invoke(jdaInstance, channelId);

        if (channel == null) return;

        Method editMessageEmbedsByIdMethod = channel.getClass().getMethod("editMessageEmbedsById", String.class, entityMessageEmbedClazz);
        Object messageAction = editMessageEmbedsByIdMethod.invoke(channel, messageId, newEmbed);

        Method queueMethod = messageAction.getClass().getMethod("queue", Consumer.class, Consumer.class);
        queueMethod.invoke(messageAction,
            (Consumer<Object>) _p -> {
                // 特に何もしない
            },
            (Consumer<Throwable>) error -> {
                logger.error("A editBotEmbedReplacedAll error occurred: " + error.getMessage());
                for (StackTraceElement element : error.getStackTrace()) {
                    logger.error(element.toString());
                }
            }
        );
    }


    public CompletableFuture<String> sendBotMessageAndgetMessageId(String content, Object embed, boolean isChat) throws Exception {
    	CompletableFuture<String> future = new CompletableFuture<>();
        String channelId;
    	if (isChat) {
            channelId = Long.toString(config.getLong("Discord.ChatChannelId", 0));
    		if (channelId == "0" || !isDiscord) {
    			future.complete(null);
                return future;
    		}
    	} else {
            channelId = Long.toString(config.getLong("Discord.ChannelId"));
    		if (channelId == "0" || !isDiscord) {
            	future.complete(null);
                return future;
            }
    	}

        Method getTextChannelByIdMethod = jdaInstance.getClass().getMethod("getTextChannelById", String.class);
        Object channel = getTextChannelByIdMethod.invoke(jdaInstance, channelId);

        if (channel == null) {
        	logger.error("Channel not found!");
        	future.complete(null);
            return future;
        }

    	if (embed != null) {
            Method sendMessageEmbedsMethod = channel.getClass().getMethod("sendMessageEmbeds", entityMessageEmbedClazz);
            Object messageAction = sendMessageEmbedsMethod.invoke(channel, embed);

            Method queueMethod = messageAction.getClass().getMethod("queue", Consumer.class, Consumer.class);
            queueMethod.invoke(messageAction,
                (Consumer<Object>) response -> {
                    try {
                        Method getIdMethod = entityMessageClazz.getMethod("getId");
                        String messageId = (String) getIdMethod.invoke(response);
                        future.complete(messageId);
                    } catch (Exception e) {
                        logger.error("Failed to send embedded message: " + e.getMessage());
                        future.complete(null);
                    }
                },
                (Consumer<Throwable>) failure -> {
                    logger.error("Failed to send embedded message: " + failure.getMessage());
                    future.complete(null);
                }
            );
        }

        if (content != null && !content.isEmpty()) {
            Method sendMessageMethod = channel.getClass().getMethod("sendMessage", String.class);
            Object messageAction = sendMessageMethod.invoke(channel, content);

            Method queueMethod = messageAction.getClass().getMethod("queue", Consumer.class, Consumer.class);
            queueMethod.invoke(messageAction,
                (Consumer<Object>) response -> {
                    try {
                        Method getIdMethod = entityMessageClazz.getMethod("getId");
                        String messageId = (String) getIdMethod.invoke(response);
                        future.complete(messageId);
                    } catch (Exception e) {
                        logger.error("Failed to send text message: " + e.getMessage());
                        future.complete(null);
                    }
                },
                (Consumer<Throwable>) failure -> {
                    logger.error("Failed to send text message: " + failure.getMessage());
                    future.complete(null);
                }
            );
        }
    	return future;
    }


    public CompletableFuture<String> sendBotMessageAndgetMessageId(String content) throws Exception {
    	return sendBotMessageAndgetMessageId(content, null, false);
    }


    public CompletableFuture<String> sendBotMessageAndgetMessageId(Object embed) throws Exception {
    	return sendBotMessageAndgetMessageId(null, embed, false);
    }


    public CompletableFuture<String> sendBotMessageAndgetMessageId(String content, boolean isChat) throws Exception {
    	return sendBotMessageAndgetMessageId(content, null, isChat);
    }


    public CompletableFuture<String> sendBotMessageAndgetMessageId(Object embed, boolean isChat) throws Exception {
    	return sendBotMessageAndgetMessageId(null, embed, isChat);
    }


    public Object createEmbed(String description, int color) throws Exception {
        // "net.dv8tion.jda.api.entities.MessageEmbed"の内部クラスを取得
        ClassLoader loader = entityMessageEmbedClazz.getClassLoader();
        Constructor<?> messageEmbedC = entityMessageEmbedClazz.getConstructor(
            String.class, String.class, String.class,
            Class.forName("net.dv8tion.jda.api.entities.MessageEmbed$EmbedType", true, loader),
            Class.forName("java.time.OffsetDateTime"),
            int.class,
            Class.forName("net.dv8tion.jda.api.entities.MessageEmbed$Thumbnail", true, loader),
            Class.forName("net.dv8tion.jda.api.entities.MessageEmbed$Provider", true, loader),
            Class.forName("net.dv8tion.jda.api.entities.MessageEmbed$AuthorInfo", true, loader),
            Class.forName("net.dv8tion.jda.api.entities.MessageEmbed$VideoInfo", true, loader),
            Class.forName("net.dv8tion.jda.api.entities.MessageEmbed$Footer", true, loader),
            Class.forName("net.dv8tion.jda.api.entities.MessageEmbed$ImageInfo", true, loader),
            List.class
        );
        return messageEmbedC.newInstance(
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
            null, // Image
            null  // Fields
        );
    }


    public void sendBotMessage(String content, Object embed) throws Exception {
    	CompletableFuture<String> future = new CompletableFuture<>();
        if (config.getLong("Discord.ChannelId", 0)==0 || !isDiscord) {
        	future.complete(null);
            return;
        }
    	String channelId = Long.toString(config.getLong("Discord.ChannelId"));

        Method getTextChannelByIdMethod = jdaInstance.getClass().getMethod("getTextChannelById", String.class);
        Object channel = getTextChannelByIdMethod.invoke(jdaInstance, channelId);

        if (channel == null) {
        	future.complete(null);
            return;
        }
    	if (embed != null) {
            Method sendMessageEmbedsMethod = channel.getClass().getMethod("sendMessageEmbeds", entityMessageEmbedClazz);
            Object messageAction = sendMessageEmbedsMethod.invoke(channel, embed);

            Method queueMethod = messageAction.getClass().getMethod("queue", Consumer.class, Consumer.class);
            queueMethod.invoke(messageAction,
                (Consumer<Object>) response -> {
                    // 特に何もしない
                },
                (Consumer<Throwable>) failure -> logger.error("Failed to send embedded message: " + failure.getMessage())
            );
        }
    	if (content != null && !content.isEmpty()) {
            Method sendMessageMethod = channel.getClass().getMethod("sendMessage", String.class);
            Object messageAction = sendMessageMethod.invoke(channel, content);

            Method queueMethod = messageAction.getClass().getMethod("queue", Consumer.class, Consumer.class);
            queueMethod.invoke(messageAction,
                (Consumer<Object>) response -> {
                    // 特に何もしない
                },
                (Consumer<Throwable>) failure -> logger.error("Failed to send text message: " + failure.getMessage())
            );
    	}
    }


    public void sendBotMessage(String content) throws Exception {
    	sendBotMessage(content, null);
    }


    public void sendBotMessage(Object embed) throws Exception {
    	sendBotMessage(null, embed);
    }
}
