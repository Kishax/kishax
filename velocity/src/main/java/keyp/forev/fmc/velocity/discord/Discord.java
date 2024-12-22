package keyp.forev.fmc.velocity.discord;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.slf4j.Logger;

import keyp.forev.fmc.common.database.Database;
import keyp.forev.fmc.common.libs.ClassManager;
import keyp.forev.fmc.velocity.Main;
import keyp.forev.fmc.velocity.discord.interfaces.ReflectionHandler;
import keyp.forev.fmc.velocity.libs.VClassManager;
import keyp.forev.fmc.velocity.libs.VPackageManager;
import keyp.forev.fmc.velocity.server.BroadCast;
import keyp.forev.fmc.velocity.server.cmd.sub.VelocityRequest;
import keyp.forev.fmc.velocity.server.cmd.sub.interfaces.Request;
import keyp.forev.fmc.velocity.util.config.VelocityConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import com.google.inject.Singleton;
import com.google.inject.Inject;
import com.google.inject.Provider;

import java.net.URL;
import java.net.URLClassLoader;

@Singleton
public class Discord {
    public static Object jdaInstance = null; // JDAインスタンス // ログイン時に代入される
	public static boolean isDiscord = false;
    private final Logger logger;
    private final VelocityConfig config;
    private final Database db;
    private final Provider<MessageEditor> discordMEProvider;
    private final Provider<Request> reqProvider;
    private final BroadCast bc;
    // JDA
    private final URLClassLoader jdaURLClassLoader;
    private final Class<?> jdaClazz, jdaBuilderClazz, listenerAdapterClazz,
        gatewayIntentsClazz, subcommandDataClazz, optionDataClazz, 
        optionTypeClazz, entityMessageClazz, entityActivityClazz, 
        entityMessageEmbedClazz, entityEmbedTypeClazz, buttonClazz, 
        presenceActivityClazz, embedBuilderClazz, errorResponseExceptionClazz, 
        cmdCreateActionClazz, restActionClazz, itemComponentClazz;
    @Inject
    public Discord(Logger logger, VelocityConfig config, Database db, Provider<MessageEditor> discordMEProvider, Provider<Request> reqProvider, BroadCast bc) throws ClassNotFoundException {
    	this.logger = logger;
    	this.config = config;
        this.db = db;
        this.reqProvider = reqProvider;
        this.discordMEProvider = discordMEProvider;
        this.bc = bc;
        this.jdaURLClassLoader = ClassManager.urlClassLoaderMap.get(VPackageManager.VPackage.JDA);
        this.jdaClazz = VClassManager.JDA.JDA.get().getClazz();
        this.jdaBuilderClazz = VClassManager.JDA.JDA_BUILDER.get().getClazz();
        this.gatewayIntentsClazz = VClassManager.JDA.GATEWAY_INTENTS.get().getClazz();
        this.subcommandDataClazz = VClassManager.JDA.SUB_COMMAND.get().getClazz();
        this.optionDataClazz = VClassManager.JDA.OPTION_DATA.get().getClazz();
        this.optionTypeClazz = VClassManager.JDA.OPTION_TYPE.get().getClazz();
        this.entityMessageClazz = VClassManager.JDA.ENTITYS_MESSAGE.get().getClazz();
        this.entityActivityClazz = VClassManager.JDA.ENTITYS_ACTIVITY.get().getClazz();
        this.entityMessageEmbedClazz = VClassManager.JDA.ENTITYS_MESSAGE_EMBED.get().getClazz();
        this.entityEmbedTypeClazz = VClassManager.JDA.ENTITYS_EMBED_TYPE.get().getClazz();
        this.buttonClazz = VClassManager.JDA.BUTTON.get().getClazz();
        this.presenceActivityClazz = VClassManager.JDA.PRESENCE.get().getClazz();
        this.embedBuilderClazz = VClassManager.JDA.EMBED_BUILDER.get().getClazz();
        this.errorResponseExceptionClazz = VClassManager.JDA.ERROR_RESPONSE_EXCEPTION.get().getClazz();
        this.cmdCreateActionClazz = VClassManager.JDA.COMMAND_CREATE_ACTION.get().getClazz();
        this.restActionClazz = VClassManager.JDA.REST_ACTION.get().getClazz();
        this.listenerAdapterClazz = VClassManager.JDA.LISTENER_ADAPTER.get().getClazz();
        this.itemComponentClazz = VClassManager.JDA.ITEM_COMPONENT.get().getClazz();
    }

    // カスタムクラスローダーの定義
    // Java 17以降のモジュールシステムの制限により、
    // ClassLoader.defineClass()メソッドに直接アクセスできない問題を解決するために使用
    private static class CustomClassLoader extends URLClassLoader {
        public CustomClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        public Class<?> defineNewClass(String name, byte[] bytes) {
            return super.defineClass(name, bytes, 0, bytes.length);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public CompletableFuture<Object> loginDiscordBotAsync() {
        return CompletableFuture.supplyAsync(() -> {
            if (config.getString("Discord.Token","").isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }
            try {
                logger.info("discord bot is trying to log in...");
                
                Method createDefault = jdaBuilderClazz.getMethod("createDefault", String.class);
                Object jdaBuilder = createDefault.invoke(jdaBuilderClazz, config.getString("Discord.Token"));

                List<Object> intentsList = new ArrayList<>();
                intentsList.add(Enum.valueOf((Class<Enum>) gatewayIntentsClazz, "GUILD_MESSAGES"));
                intentsList.add(Enum.valueOf((Class<Enum>) gatewayIntentsClazz, "MESSAGE_CONTENT"));

                jdaBuilder = jdaBuilderClazz.getMethod("enableIntents", Collection.class)
                    .invoke(jdaBuilder, intentsList);
                
                Method addEventListenerMethod = jdaBuilderClazz.getMethod("addEventListeners", Object[].class);

                DiscordEventListener listener = Main.getInjector().getInstance(DiscordEventListener.class);

                try {
                    // ASMを使用したクラス生成
                    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
                    String className = "DynamicListener_" + System.currentTimeMillis();
                    String classNameInternal = className.replace('.', '/');
                    String superClassInternal = listenerAdapterClazz.getName().replace('.', '/');

                    // クラス定義
                    cw.visit(Opcodes.V17, // Java 17バージョン
                        Opcodes.ACC_PUBLIC,
                        classNameInternal,
                        null,
                        superClassInternal,
                        null);
                    
                    // リスナーフィールドの追加
                    cw.visitField(
                        Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL,
                        "delegate",
                        "L" + listener.getClass().getName().replace('.', '/') + ";",
                        null,
                        null
                    ).visitEnd();

                    // コンストラクタの修正
                    MethodVisitor constructor = cw.visitMethod(
                        Opcodes.ACC_PUBLIC,
                        "<init>",
                        "(L" + listener.getClass().getName().replace('.', '/') + ";)V",
                        null,
                        null
                    );
                    constructor.visitCode();
                    // super()
                    constructor.visitVarInsn(Opcodes.ALOAD, 0);
                    constructor.visitMethodInsn(
                        Opcodes.INVOKESPECIAL,
                        superClassInternal,
                        "<init>",
                        "()V",
                        false
                    );
                    // this.delegate = delegate
                    constructor.visitVarInsn(Opcodes.ALOAD, 0);
                    constructor.visitVarInsn(Opcodes.ALOAD, 1);
                    constructor.visitFieldInsn(
                        Opcodes.PUTFIELD,
                        classNameInternal,
                        "delegate",
                        "L" + listener.getClass().getName().replace('.', '/') + ";"
                    );
                    constructor.visitInsn(Opcodes.RETURN);
                    constructor.visitMaxs(2, 2);
                    constructor.visitEnd();
                    
                    for (Method method : listener.getClass().getDeclaredMethods()) {
                        if (method.isAnnotationPresent(ReflectionHandler.class)) {
                            ReflectionHandler annotation = method.getAnnotation(ReflectionHandler.class);
                            Class<?> eventClazz = jdaURLClassLoader.loadClass(annotation.event());
                            String methodName = method.getName();
                            
                            // メソッド生成
                            MethodVisitor mv = cw.visitMethod(
                                Opcodes.ACC_PUBLIC,
                                methodName,
                                //"(L" + eventClazz.getName().replace('.', '/') + ";)V",
                                Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(eventClazz)),
                                null,
                                null
                            );
                            
                            mv.visitCode();

                            mv.visitVarInsn(Opcodes.ALOAD, 0);
                            mv.visitFieldInsn(
                                Opcodes.GETFIELD,
                                classNameInternal,
                                "delegate",
                                //"L" + listener.getClass().getName().replace('.', '/') + ";"
                                Type.getDescriptor(DiscordEventListener.class)  // 具体的な型を指定
                            );

                            // event 引数のロード
                            mv.visitVarInsn(Opcodes.ALOAD, 1);

                            // 具象イベントクラスから Object への変換は不要（自動的にアップキャスト）

                            // メソッド呼び出し
                            mv.visitMethodInsn(
                                Opcodes.INVOKEVIRTUAL,
                                //listener.getClass().getName().replace('.', '/'),
                                Type.getInternalName(DiscordEventListener.class),
                                methodName,
                                //"(L" + eventClazz.getName().replace('.', '/') + ";)V",
                                Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Object.class/*eventClazz*/)),
                                false
                            );
                            mv.visitInsn(Opcodes.RETURN);
                            mv.visitMaxs(2, 2);
                            mv.visitEnd();
                        }
                    }

                    // クラスローダーを使用してクラスを定義
                    //byte[] classBytes = cw.toByteArray();
                    //Class<?> dynamicClass = defineClass(className, classBytes);
                    byte[] classBytes = cw.toByteArray();
                    try (CustomClassLoader loader = new CustomClassLoader(
                        ((URLClassLoader) jdaURLClassLoader).getURLs(),
                        jdaURLClassLoader  // 親クラスローダーをJDAのローダーに変更
                    );) {
                        Class<?> dynamicClass = loader.defineNewClass(className, classBytes);

                        Object asmListener = dynamicClass.getDeclaredConstructor(listener.getClass())
                            .newInstance(listener);

                        jdaBuilder = addEventListenerMethod.invoke(jdaBuilder, new Object[] { new Object[] { asmListener } });
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new RuntimeException("Failed to create dynamic listener", e);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException("Failed to create dynamic listener", e);
                }

                try {
                    Method build = jdaBuilderClazz.getMethod("build");

                    try {
                        jdaInstance = build.invoke(jdaBuilder);
                        if (jdaInstance == null) {
                            throw new RuntimeException("JDA instance is null after build");
                        }
                    } catch (InvocationTargetException e) {
                        Throwable cause = e.getCause();
                        logger.error("Build failed with cause: ", cause);
                        throw new RuntimeException("Failed to build JDA: " + cause.getMessage(), cause);
                    }
                } catch (Exception e) {
                    logger.error("An error occurred: " + e.getMessage());
                    throw new RuntimeException("Failed to build JDA", e);
                }

                Method awaitReady = jdaClazz.getMethod("awaitReady");
                awaitReady.invoke(jdaInstance);
                
                Method upsertCommand = jdaClazz.getMethod("upsertCommand", String.class, String.class);
                Object createFMCCommand = upsertCommand.invoke(jdaInstance, "fmc", "FMC commands");

                Object stringType = Enum.valueOf((Class<Enum>) optionTypeClazz, "STRING");
                Object attachmentType = Enum.valueOf((Class<Enum>) optionTypeClazz, "ATTACHMENT");

                Constructor<?> subcommandC = subcommandDataClazz.getConstructor(String.class, String.class);
                Object createImageSubcommand = subcommandC.newInstance("image_add_q", "画像マップをキューに追加するコマンド(urlか添付ファイルのどっちかを指定可能)");
                Object createSyncRuleBookSubcommand = subcommandC.newInstance("syncrulebook", "ルールブックの同期を行うコマンド");

                Method addOptions = subcommandDataClazz.getMethod("addOptions", 
                    Array.newInstance(optionDataClazz, 0).getClass());

                Constructor<?> optionDataC = optionDataClazz.getConstructor(optionTypeClazz, String.class, String.class);
                List<Object> optionData = new ArrayList<>();
                optionData.add(optionDataC.newInstance(stringType, "url", "画像リンクの設定項目"));
                optionData.add(optionDataC.newInstance(attachmentType, "image", "ファイルの添付項目"));
                optionData.add(optionDataC.newInstance(stringType, "title", "画像マップのタイトル設定項目"));
                optionData.add(optionDataC.newInstance(stringType, "comment", "画像マップのコメント設定項目"));

                // 正しい型の配列に変換
                Object optionArray = Array.newInstance(optionDataClazz, optionData.size());
                for (int i = 0; i < optionData.size(); i++) {
                    Array.set(optionArray, i, optionData.get(i));
                }
                addOptions.invoke(createImageSubcommand, (Object) optionArray);

                Method addSubcommands = cmdCreateActionClazz.getMethod("addSubcommands",
                    Array.newInstance(subcommandDataClazz, 0).getClass());

                List<Object> subcommands = new ArrayList<>();
                subcommands.add(createImageSubcommand);
                subcommands.add(createSyncRuleBookSubcommand);

                // 正しい型の配列に変換
                Object subcommandsArray = Array.newInstance(subcommandDataClazz, subcommands.size());
                for (int i = 0; i < subcommands.size(); i++) {
                    Array.set(subcommandsArray, i, subcommands.get(i));
                }
                Object cmdResult = addSubcommands.invoke(createFMCCommand, (Object) subcommandsArray);

                Method queue = restActionClazz.getMethod("queue");
                queue.invoke(cmdResult);

                Method getPresence = jdaClazz.getMethod("getPresence");
                Object presence = getPresence.invoke(jdaInstance);

                Method setActivity = presenceActivityClazz.getMethod("setActivity", entityActivityClazz);
                Method playing = entityActivityClazz.getMethod("playing", String.class);
                Object activity = playing.invoke(null, config.getString("Discord.Presence.Activity", "FMCサーバー"));
                setActivity.invoke(presence, activity);

                isDiscord = true;
                logger.info("discord bot has been logged in.");
                return CompletableFuture.completedFuture(jdaInstance);
            } catch (Exception e) {
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
                    Method shutdown = jdaInstance.getClass().getMethod("shutdown");
                    shutdown.invoke(jdaInstance);
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

        Method getTextChannelById = jdaInstance.getClass().getMethod("getTextChannelById", String.class);
        Object ruleChannel = getTextChannelById.invoke(jdaInstance, ruleChannelId);

        if (ruleChannel == null) {
            future.completeExceptionally(new IllegalArgumentException("チャンネルが見つかりませんでした。"));
            return future;
        }

        Method retrieveMessageById = ruleChannel.getClass().getMethod("retrieveMessageById", String.class);
        Object retrieveMessageByIdResult = retrieveMessageById.invoke(ruleChannel, ruleMessageId);

        Method queue = retrieveMessageByIdResult.getClass().getMethod("queue", Consumer.class, Consumer.class);
        queue.invoke(retrieveMessageByIdResult,
            (Consumer<Object>) message -> {
                try {
                    Method getContentDisplay = entityMessageClazz.getMethod("getContentDisplay");
                    String content = (String) getContentDisplay.invoke(message);
                    future.complete(content);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            },
            (Consumer<Throwable>) throwable -> {
                try {
                    if (errorResponseExceptionClazz.isInstance(throwable)) {
                        Method getErrorResponse = errorResponseExceptionClazz.getMethod("getErrorResponse");
                        Object errorResponse = getErrorResponse.invoke(throwable);

                        Method getMeaning = errorResponse.getClass().getMethod("getMeaning");
                        String meaning = (String) getMeaning.invoke(errorResponse);

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

        Method getTextChannelById = jdaInstance.getClass().getMethod("getTextChannelById", String.class);
        Object channel = getTextChannelById.invoke(jdaInstance, channelId);

        if (channel == null) return;

        Method successButton = buttonClazz.getMethod("success", String.class, String.class);
        Method dangerButton = buttonClazz.getMethod("danger", String.class, String.class);

        Object button1 = successButton.invoke(buttonClazz, "reqOK", "YES");
        Object button2 = dangerButton.invoke(buttonClazz, "reqCancel", "NO");

        Method sendMessage = channel.getClass().getMethod("sendMessage", CharSequence.class);
        Object sendMessageResult = sendMessage.invoke(channel, buttonMessage);

        Object actionsArray = Array.newInstance(itemComponentClazz, 2);
        Method setActionRow = sendMessageResult.getClass().getMethod("setActionRow", actionsArray.getClass());
        Array.set(actionsArray, 0, button1);
        Array.set(actionsArray, 1, button2);
        Object setActionRowResult = setActionRow.invoke(sendMessageResult, (Object) actionsArray);

        Method queue = setActionRowResult.getClass().getMethod("queue", Consumer.class);
        queue.invoke(setActionRowResult, (Consumer<Object>) message -> {
            try {
                CompletableFuture.delayedExecutor(3, TimeUnit.MINUTES).execute(() -> {
                    if (!VelocityRequest.PlayerReqFlags.isEmpty()) {
                        try {
                            String buttonMessage2 = (String) entityMessageClazz.getMethod("getContentRaw").invoke(message);
                            Request req = reqProvider.get();
                            Map<String, String> reqMap = req.paternFinderMapForReq(buttonMessage2);
                            String reqPlayerName = reqMap.get("playerName"),
                                reqPlayerUUID = reqMap.get("playerUUID"),
                                reqServerName = reqMap.get("serverName"),
                                reqId = reqMap.get("reqId");
                            
                            try (Connection conn = db.getConnection()) {
                                db.insertLog(conn, "INSERT INTO log (name, uuid, reqsul, reqserver, reqsulstatus) VALUES (?, ?, ?, ?, ?);", new Object[] {reqPlayerName, reqPlayerUUID, true, reqServerName, "ok"});
                                db.updateLog(conn, "UPDATE requests SET checked = ? WHERE requuid = ?;", new Object[] {true, reqId});
                            } catch (SQLException | ClassNotFoundException e2) {
                                logger.error("A SQLException | ClassNotFoundException error occurred: {}", e2.getMessage());
                                for (StackTraceElement element : e2.getStackTrace()) {
                                    logger.error(element.toString());
                                }
                            }

                            if (!reqMap.isEmpty()) {
                                try (Connection conn = db.getConnection()) {
                                    db.insertLog(conn, "INSERT INTO log (name, uuid, reqsul, reqserver, reqsulstatus) VALUES (?, ?, ?, ?, ?);", new Object[] {reqPlayerName, reqPlayerUUID, true, reqServerName, "nores"});
                                } catch (SQLException | ClassNotFoundException e) {
                                    logger.error("A SQLException | ClassNotFoundException error occurred: {}", e.getMessage());
                                    for (StackTraceElement element : e.getStackTrace()) {
                                        logger.error(element.toString());
                                    }
                                }

                                try {
                                    MessageEditor discordME = discordMEProvider.get();
                                    discordME.AddEmbedSomeMessage("RequestNoRes", reqPlayerName);
                                } catch (Exception e1) {
                                    logger.error("An Exception error occurred: " + e1.getMessage());
                                    for (StackTraceElement element : e1.getStackTrace()) {
                                        logger.error(element.toString());
                                    }
                                }

                                bc.broadCastMessage(Component.text("管理者が"+reqPlayerName+"の"+reqServerName+"サーバーの起動リクエストに応答しませんでした。").color(NamedTextColor.BLUE));
                                VelocityRequest.PlayerReqFlags.remove(reqPlayerUUID);
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
                Method getTextChannelById = jdaInstance.getClass().getMethod("getTextChannelById", String.class);
                Object channel = getTextChannelById.invoke(jdaInstance, channelId);

                if (channel == null) {
                    future.completeExceptionally(new RuntimeException("Channel not found!"));
                    return;
                }

                Object newEmbed = addDescriptionToEmbed(currentEmbed, additionalDescription);
                Object entityMessageEmbedArray = Array.newInstance(entityMessageEmbedClazz, 1);
                Method editMessageEmbedsById = channel.getClass().getMethod("editMessageEmbedsById", String.class, 
                    entityMessageEmbedArray.getClass());
                Array.set(entityMessageEmbedArray, 0, newEmbed);
                Object messageAction = editMessageEmbedsById.invoke(channel, messageId, entityMessageEmbedArray);

                Method queue = messageAction.getClass().getMethod("queue", Consumer.class, Consumer.class);
                queue.invoke(messageAction,
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

        Method retrieveMessageById = channel.getClass().getMethod("retrieveMessageById", String.class);
        Object restActionMessage = retrieveMessageById.invoke(channel, messageId);

        Method queue = restActionMessage.getClass().getMethod("queue", Consumer.class, Consumer.class);
        queue.invoke(restActionMessage,
            (Consumer<Object>) message -> {
                try {
                    Method getEmbeds = entityMessageClazz.getMethod("getEmbeds");
                    Object embeds = getEmbeds.invoke(message);
                    if (embeds instanceof List<?>) {
                        // embedsがList<Object>型であることを保証しなければならない
                        boolean isList = ((List<?>) embeds).stream().allMatch(e -> e instanceof Object);
                        if (isList) {
                            @SuppressWarnings("unchecked")
                            List<Object> embedList = (List<Object>) embeds;
                            //List<Object> embedList = Array.newInstance();
                            Object entityEmbedsArray = Array.newInstance(entityMessageEmbedClazz, embedList.size());
                            for (int i = 0; i < embedList.size(); i++) {
                                Array.set(entityEmbedsArray, i, embedList.get(i));
                            }
                            Object zero = Array.get(entityEmbedsArray, 0);
                            if (!embedList.isEmpty()/*&& entityEmbedsArray != null */ ) {
                                embedConsumer.accept((Object) zero/*embedList.get(0)*//*entityEmbedsArray*/);
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
        Method getDescription = embed.getClass().getMethod("getDescription");
        String description = (String) getDescription.invoke(embed);

        Constructor<?> embedBuilderC = embedBuilderClazz.getConstructor(entityMessageEmbedClazz);
        Object builder = embedBuilderC.newInstance(embed);

        String newDescription = (description != null ? description : "") + additionalDescription;
        Method setDescription = embedBuilderClazz.getMethod("setDescription", CharSequence.class);
        setDescription.invoke(builder, newDescription);

        Method build = embedBuilderClazz.getMethod("build");

        return build.invoke(builder);
    }

    public void editBotEmbedReplacedAll(String messageId, Object newEmbed) throws Exception {
    	if (config.getLong("Discord.ChannelId", 0)==0 || !isDiscord) return;
        String channelId = Long.toString(config.getLong("Discord.ChannelId"));
        Method getTextChannelById = jdaInstance.getClass().getMethod("getTextChannelById", String.class);
        Object channel = getTextChannelById.invoke(jdaInstance, channelId);

        if (channel == null) return;

        Object entityMessageEmbedArray = Array.newInstance(entityMessageEmbedClazz, 1);
        Method editMessageEmbedsById = channel.getClass().getMethod("editMessageEmbedsById", String.class, 
            entityMessageEmbedArray.getClass());
        Array.set(entityMessageEmbedArray, 0, newEmbed);
        Object messageAction = editMessageEmbedsById.invoke(channel, messageId, (Object) entityMessageEmbedArray);

        Method queue = messageAction.getClass().getMethod("queue", Consumer.class, Consumer.class);
        queue.invoke(messageAction,
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

        Method getTextChannelById = jdaInstance.getClass().getMethod("getTextChannelById", String.class);
        Object channel = getTextChannelById.invoke(jdaInstance, channelId);

        if (channel == null) {
        	logger.error("Channel not found!");
        	future.complete(null);
            return future;
        }

    	if (embed != null) {
            Object entityMessageEmbedArray = Array.newInstance(entityMessageEmbedClazz, 0);
            Method sendMessageEmbeds = channel.getClass().getMethod("sendMessageEmbeds", entityMessageEmbedClazz, entityMessageEmbedArray.getClass());
            Object messageAction = sendMessageEmbeds.invoke(channel, embed, (Object) entityMessageEmbedArray);

            Method queue = messageAction.getClass().getMethod("queue", Consumer.class, Consumer.class);
            queue.invoke(messageAction,
                (Consumer<Object>) response -> {
                    try {
                        Method getId = entityMessageClazz.getMethod("getId");
                        String messageId = (String) getId.invoke(response);
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
            Method sendMessage = channel.getClass().getMethod("sendMessage", String.class);
            Object messageAction = sendMessage.invoke(channel, content);

            Method queue = messageAction.getClass().getMethod("queue", Consumer.class, Consumer.class);
            queue.invoke(messageAction,
                (Consumer<Object>) response -> {
                    try {
                        Method getId = entityMessageClazz.getMethod("getId");
                        String messageId = (String) getId.invoke(response);
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
        Thread.currentThread().setContextClassLoader(jdaURLClassLoader);
        // "net.dv8tion.jda.api.entities.MessageEmbed"の内部クラスを取得
        //ClassLoader loader = entityMessageEmbedClazz.getClassLoader();
        Class<?> embedTypeClazz = entityEmbedTypeClazz, 
            footerClazz = null, imageInfoClazz = null, 
            providerClazz = null, thumbnailClazz = null, videoInfoClazz = null,
            authorInfoClazz = null;
        Class<?>[] inClasses = entityMessageEmbedClazz.getDeclaredClasses();
        for (Class<?> m : inClasses) {
            String clazzName = m.getName();
            switch (clazzName) {
                case "net.dv8tion.jda.api.entities.MessageEmbed$Footer" -> footerClazz = m;
                case "net.dv8tion.jda.api.entities.MessageEmbed$ImageInfo" -> imageInfoClazz = m;
                case "net.dv8tion.jda.api.entities.MessageEmbed$Provider" -> providerClazz = m;
                case "net.dv8tion.jda.api.entities.MessageEmbed$Thumbnail" -> thumbnailClazz = m;
                case "net.dv8tion.jda.api.entities.MessageEmbed$VideoInfo" -> videoInfoClazz = m;
                case "net.dv8tion.jda.api.entities.MessageEmbed$AuthorInfo" -> authorInfoClazz = m;
            }
        }
        Constructor<?> messageEmbedC = entityMessageEmbedClazz.getConstructor(
            String.class, String.class, String.class,
            embedTypeClazz,
            java.time.OffsetDateTime.class,
            int.class,
            thumbnailClazz,
            providerClazz,
            authorInfoClazz,
            videoInfoClazz,
            footerClazz,
            imageInfoClazz,
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

        Method getTextChannelById = jdaInstance.getClass().getMethod("getTextChannelById", String.class);
        Object channel = getTextChannelById.invoke(jdaInstance, channelId);

        if (channel == null) {
        	future.complete(null);
            return;
        }
    	if (embed != null) {
            Object entityMessageEmbedArray = Array.newInstance(entityMessageEmbedClazz, 0);
            Method sendMessageEmbeds = channel.getClass().getMethod("sendMessageEmbeds", entityMessageEmbedClazz, entityMessageEmbedArray.getClass());
            Object messageAction = sendMessageEmbeds.invoke(channel, embed, (Object) entityMessageEmbedArray);

            Method queue = messageAction.getClass().getMethod("queue", Consumer.class, Consumer.class);
            queue.invoke(messageAction,
                (Consumer<Object>) response -> {
                    // 特に何もしない
                },
                (Consumer<Throwable>) failure -> logger.error("Failed to send embedded message: " + failure.getMessage())
            );
        }
    	if (content != null && !content.isEmpty()) {
            Method sendMessage = channel.getClass().getMethod("sendMessage", String.class);
            Object messageAction = sendMessage.invoke(channel, content);

            Method queue = messageAction.getClass().getMethod("queue", Consumer.class, Consumer.class);
            queue.invoke(messageAction,
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
