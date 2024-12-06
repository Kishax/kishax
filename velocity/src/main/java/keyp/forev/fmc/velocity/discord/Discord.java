package keyp.forev.fmc.velocity.discord;

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
import java.util.function.Function;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;

import keyp.forev.fmc.common.database.Database;
import keyp.forev.fmc.common.libs.ClassManager;
import keyp.forev.fmc.velocity.Main;
import keyp.forev.fmc.velocity.cmd.sub.VelocityRequest;
import keyp.forev.fmc.velocity.cmd.sub.interfaces.Request;
import keyp.forev.fmc.velocity.discord.interfaces.ReflectionHandler;
import keyp.forev.fmc.velocity.libs.VClassManager;
import keyp.forev.fmc.velocity.libs.VPackageManager;
import keyp.forev.fmc.velocity.util.config.VelocityConfig;

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
    private final Provider<Request> reqProvider;
    // JDA
    private final URLClassLoader jdaURLClassLoader;
    private final Class<?> jdaClazz, jdaBuilderClazz, listenerAdapterClazz,
        gatewayIntentsClazz, subcommandDataClazz, optionDataClazz, 
        optionTypeClazz, entityMessageClazz, entityActivityClazz, 
        entityMessageEmbedClazz, entityMessageChannel, buttonClazz, presenceActivityClazz, 
        embedBuilderClazz, errorResponseExceptionClazz, cmdCreateActionClazz, 
        restActionClazz, eventListenerClazz;
    // Club Minnced
    private final Class<?> webhookBuilderClazz, webhookClientClazz, webhookMessageClazz;
    @Inject
    public Discord(Logger logger, VelocityConfig config, Database db, Provider<Request> reqProvider) throws ClassNotFoundException {
    	this.logger = logger;
    	this.config = config;
        this.db = db;
        this.reqProvider = reqProvider;
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
        this.entityMessageChannel = VClassManager.JDA.ENTITYS_MESSAGE_CHANNEL.get().getClazz();
        this.buttonClazz = VClassManager.JDA.BUTTON.get().getClazz();
        this.presenceActivityClazz = VClassManager.JDA.PRESENCE.get().getClazz();
        this.webhookClientClazz = VClassManager.CLUB_MINNCED_WEBHOOK.WEBHOOK_CLIENT.get().getClazz();
        this.webhookMessageClazz = VClassManager.CLUB_MINNCED_WEBHOOK.WEBHOOK_MESSAGE.get().getClazz();
        this.webhookBuilderClazz = VClassManager.CLUB_MINNCED_WEBHOOK.WEBHOOK_MESSAGE_BUILDER.get().getClazz();
        this.embedBuilderClazz = VClassManager.JDA.EMBED_BUILDER.get().getClazz();
        this.errorResponseExceptionClazz = VClassManager.JDA.ERROR_RESPONSE_EXCEPTION.get().getClazz();
        this.cmdCreateActionClazz = VClassManager.JDA.COMMAND_CREATE_ACTION.get().getClazz();
        this.restActionClazz = VClassManager.JDA.REST_ACTION.get().getClazz();
        this.eventListenerClazz = VClassManager.JDA.EVENTLISTENER.get().getClazz();
        this.listenerAdapterClazz = VClassManager.JDA.LISTENER_ADAPTER.get().getClazz();
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

    @SuppressWarnings("unchecked")
    public CompletableFuture<Object> loginDiscordBotAsync() {
        return CompletableFuture.supplyAsync(() -> {
            if (config.getString("Discord.Token","").isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }
            try {
                logger.info("Discord-Bot is logging in...");
                
                Method createDefault = jdaBuilderClazz.getMethod("createDefault", String.class);
                Object jdaBuilder = createDefault.invoke(jdaBuilderClazz, config.getString("Discord.Token"));

                List<Object> intentsList = new ArrayList<>();
                intentsList.add(Enum.valueOf((Class<Enum>) gatewayIntentsClazz, "GUILD_MESSAGES"));
                intentsList.add(Enum.valueOf((Class<Enum>) gatewayIntentsClazz, "MESSAGE_CONTENT"));

                jdaBuilder = jdaBuilderClazz.getMethod("enableIntents", Collection.class)
                    .invoke(jdaBuilder, intentsList);
                
                // Method addEventListeners = jdaBuilder.getClass().getMethod("addEventListeners", Object[].class);
                // jdaBuilder = addEventListeners.invoke(jdaBuilder, Main.getInjector().getInstance(DiscordEventListener.class));

                // リスナーの登録は、独自アノテーションクラスを使用して動的に行う
                List<Object> listenerProxys = new ArrayList<>();
                Method addEventListenerMethod = jdaBuilderClazz.getMethod("addEventListeners", Object[].class);

                DiscordEventListener listener = Main.getInjector().getInstance(DiscordEventListener.class);

                /*
                try {
                    // ByteBuddyを使用した動的クラス生成
                    Object bytebuddyListener = new ByteBuddy()
                        .subclass(listenerAdapterClazz)
                        .method(ElementMatchers.named("onMessageReceived"))
                        .intercept(MethodDelegation.to(new Object() {
                            @SuppressWarnings("unused")
                            public void handle(@Origin Method method, @AllArguments Object[] args) {
                                try {
                                    Object event = args[0];
                                    // リフレクションでメソッドを取得
                                    Method getMessageMethod = event.getClass().getMethod("getMessage");
                                    Object message = getMessageMethod.invoke(event);
                                    
                                    Method getContentRawMethod = message.getClass().getMethod("getContentRaw");
                                    String content = (String) getContentRawMethod.invoke(message);
                                    
                                    Method getChannelMethod = message.getClass().getMethod("getChannel");
                                    Object channel = getChannelMethod.invoke(message);
                                    
                                    Method sendMessageMethod = channel.getClass().getMethod("sendMessage", String.class);
                                    Object action = sendMessageMethod.invoke(channel, content);
                                    
                                    Method queueMethod = action.getClass().getMethod("queue");
                                    queueMethod.invoke(action);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }))
                        .make()
                        .load(jdaURLClassLoader)
                        .getLoaded()
                        .getDeclaredConstructor()
                        .newInstance();
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException("Failed to create dynamic listener", e);
                }
                */
                /*
                try {
                    // javassistを使用した動的クラス生成
                    // ClassPool pool = ClassPool.getDefault();
                    ClassPool pool = new ClassPool(true);
                    pool.appendClassPath(new LoaderClassPath(jdaURLClassLoader));

                    // クラスの生成
                    logger.info("create dynamic class...");
                    String className = "DynamicListener_" + System.currentTimeMillis();
                    CtClass dynamicClass = pool.makeClass(className);
                    dynamicClass.setModifiers(Modifier.PUBLIC);
                    logger.info("set java version...");
                    dynamicClass.getClassFile().setMajorVersion(52);

                    // スーパークラスの設定
                    logger.info("set superclass: " + listenerAdapterClazz.getName());
                    dynamicClass.setSuperclass(pool.get(listenerAdapterClazz.getName()));

                    // 必要なパッケージのインポート
                    pool.importPackage(listenerAdapterClazz.getName());

                    for (Method method : listener.getClass().getDeclaredMethods()) {
                        if (method.isAnnotationPresent(ReflectionHandler.class)) {
                            ReflectionHandler annotation = method.getAnnotation(ReflectionHandler.class);
                            String eventClazzPath = annotation.event();
                            String methodName = method.getName();

                            // 必要なパッケージのインポート
                            // pool.importPackage(eventClazzPath);
                            pool.importPackage(entityMessageClazz.getName());
                            pool.importPackage(entityMessageChannel.getName());

                            // イベントクラスの取得
                            CtClass eventClass = pool.get(eventClazzPath);
                            
                            // メソッドのパラメータ設定
                            CtClass[] paramTypes = new CtClass[] { eventClass };

                            // Class<?> eventClazz = jdaURLClassLoader.loadClass(eventClazzPath);

                            // メソッドの作成
                            CtMethod ctmethod = new CtMethod(CtClass.voidType, methodName, paramTypes, dynamicClass);
                            ctmethod.setModifiers(Modifier.PUBLIC);

                            // メソッド本体の設定（完全修飾名を使用）
                            //String body = 
                            //    "{ \n" +
                            //    "    " + entityMessageClazz.getName() + " message = $1.getMessage(); \n" +
                            //    "    " + entityMessageChannel.getName() + " channel = message.getChannel(); \n" +
                            //    "    String content = message.getContentRaw(); \n" +
                            //    "    channel.sendMessage(content).queue(); \n" +
                            //    "}";
                            
                            String body =
                                "{ \n" +
                                "    java.lang.System.out.println(\"Hello!\");\n" +
                                "}";

                            logger.info("body: " + body);

                            // メソッドの追加
                            ctmethod.setBody(body);
                            dynamicClass.addMethod(ctmethod);
                            break;
                        }
                    }

                    // クラスをロード
                    Class<?> dynamicListenerClazz = dynamicClass.toClass(jdaURLClassLoader, Discord.class.getProtectionDomain());
                    Object listeners = dynamicListenerClazz.getDeclaredConstructor().newInstance();
                    listenerProxys.add(listeners);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException("Failed to create dynamic listener", e);
                }*/

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
                    
                    // デフォルトコンストラクタの追加
                    MethodVisitor constructor = cw.visitMethod(
                        Opcodes.ACC_PUBLIC,
                        "<init>",
                        "()V",
                        null,
                        null
                    );
                    constructor.visitCode();
                    constructor.visitVarInsn(Opcodes.ALOAD, 0);
                    constructor.visitMethodInsn(
                        Opcodes.INVOKESPECIAL,
                        superClassInternal,
                        "<init>",
                        "()V",
                        false
                    );
                    constructor.visitInsn(Opcodes.RETURN);
                    constructor.visitMaxs(1, 1);
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
                                "(L" + eventClazz.getName().replace('.', '/') + ";)V",
                                null,
                                null
                            );
                            
                            mv.visitCode();
                            mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
                            mv.visitLdcInsn("Hello!");
                            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
                            mv.visitInsn(Opcodes.RETURN);
                            mv.visitMaxs(2, 2);
                            mv.visitEnd();
                            
                            cw.visitEnd();
                        }
                    }

                    // クラスローダーを使用してクラスを定義
                    //byte[] classBytes = cw.toByteArray();
                    //Class<?> dynamicClass = defineClass(className, classBytes);
                    byte[] classBytes = cw.toByteArray();
                    try (CustomClassLoader loader = new CustomClassLoader(
                        ((URLClassLoader) jdaURLClassLoader).getURLs(),
                        jdaURLClassLoader.getParent()
                    );) {
                        Class<?> dynamicClass = loader.defineNewClass(className, classBytes);

                        Object asmListener = dynamicClass.getDeclaredConstructor().newInstance();

                        //listenerProxys.add(asmListener);
                        jdaBuilder = addEventListenerMethod.invoke(jdaBuilder, new Object[] { new Object[] { asmListener } });
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new RuntimeException("Failed to create dynamic listener", e);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException("Failed to create dynamic listener", e);
                }

                // プロキシを使用したリスナーの登録
                /*
                try {
                    for (Method method : listener.getClass().getDeclaredMethods()) {
                        if (method.isAnnotationPresent(ReflectionHandler.class)) {
                            ReflectionHandler annotation = method.getAnnotation(ReflectionHandler.class);
                            Class<?> eventClazz = jdaURLClassLoader.loadClass(annotation.event());

                            logger.info("EventListener class loader: " + eventListenerClazz.getClassLoader());
                            logger.info("Discord class loader: " + Discord.class.getClassLoader());
                            logger.info("JDA class loader: " + jdaClazz.getClassLoader());
                            logger.info("JarLoader class loader: " + JarLoader.class.getClassLoader());
                            logger.info("listener class loader: " + listener.getClass().getClassLoader());

                            for (Class<?> iface : eventListenerClazz.getInterfaces()) {
                                logger.info("Interface: " + iface.getName() + " | Loader: " + iface.getClassLoader());
                            }

                            String methodName = method.getName();
                            Object proxy = Proxy.newProxyInstance(
                                eventListenerClazz.getClassLoader(),
                                // jdaURLClassLoader
                                // Discord.class.getClassLoader()
                                new Class<?>[]{listenerAdapterClazz}, // ここで必ずEventListenerの型を指定
                                new InvocationHandler() {
                                    @Override
                                    public Object invoke(Object proxy, Method eventMethod, Object[] args) throws Throwable {
                                        try {
                                            if (eventMethod.getName().equals(methodName) && args != null && args.length == 1 && eventClazz.isAssignableFrom(args[0].getClass())) {
                                                method.invoke(listener, args[0]);
                                            }
                                            return null;
                                        } catch (Exception e) {
                                            logger.error("Error in proxy invocation: ", e);
                                            throw e;
                                        }
                                    }
                                }
                            );

                            if (proxy == null) {
                                throw new IllegalStateException("Failed to create proxy for " + eventListenerClazz.getName());
                            }

                            listenerProxys.add(proxy);
                        }
                    }
                } catch (IllegalArgumentException e) {
                    logger.error("Failed to create proxy due to illegal arguments: " + e.getMessage());
                    throw e;
                } catch (Exception e) {
                    logger.error("Unexpected error creating proxy: " + e.getMessage());
                }
                */

                logger.info("listenerProxys: " + listenerProxys);
                logger.info("jdaBuilder: " + jdaBuilder);
                
                // 可変長引数として渡すために配列をラップ
                //Object[] proxyArray = listenerProxys.toArray(new Object[0]);
                //jdaBuilder = addEventListenerMethod.invoke(jdaBuilder, (Object) proxyArray);
                //jdaBuilder = addEventListenerMethod.invoke(jdaBuilder, new Object[] { listenerProxys.toArray() });

                //for (Object proxy : listenerProxys) {
                //    jdaBuilder = addEventListenerMethod.invoke(jdaBuilder, new Object[] { new Object[] { proxy } });
                //}

                try {
                    Method build = jdaBuilderClazz.getMethod("build");
                    logger.info("build: " + build);

                    try {
                        jdaInstance = build.invoke(jdaBuilder);
                        if (jdaInstance == null) {
                            throw new RuntimeException("JDA instance is null after build");
                        }
                        logger.info("jdaInstance: " + jdaInstance);
                    } catch (InvocationTargetException e) {
                        // 実際の例外を取得
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

                Method addOptions = subcommandDataClazz.getMethod("addOptions", optionDataClazz);
                Constructor<?> optionDataC = optionDataClazz.getConstructor(optionTypeClazz, String.class, String.class);
                addOptions.invoke(createImageSubcommand,
                    optionDataC.newInstance(stringType, "url", "画像リンクの設定項目"),
                    optionDataC.newInstance(attachmentType, "image", "ファイルの添付項目"),
                    optionDataC.newInstance(stringType, "title", "画像マップのタイトル設定項目"),
                    optionDataC.newInstance(stringType, "comment", "画像マップのコメント設定項目")
                );

                Method addSubcommands = cmdCreateActionClazz.getMethod("addSubcommands", subcommandDataClazz.arrayType());
                Object cmdResult = addSubcommands.invoke(createFMCCommand, new Object[]{createImageSubcommand, createSyncRuleBookSubcommand});

                Method queue = restActionClazz.getMethod("queue");
                queue.invoke(cmdResult);

                Method getPresence = jdaClazz.getMethod("getPresence");
                Object presence = getPresence.invoke(jdaInstance);

                Method setActivity = presenceActivityClazz.getMethod("setActivity", entityActivityClazz);
                Method playing = presenceActivityClazz.getMethod("playing", String.class);
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

    // クラス定義用のヘルパーメソッド(asm)
    private Class<?> defineClass(String name, byte[] bytes) throws Exception {
        Method defineClassMethod = ClassLoader.class.getDeclaredMethod(
            "defineClass",
            String.class, byte[].class, int.class, int.class
        );
        defineClassMethod.setAccessible(true);
        return (Class<?>) defineClassMethod.invoke(
            jdaURLClassLoader,
            name,
            bytes,
            0,
            bytes.length
        );
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

        Method sendMessage = channel.getClass().getMethod("sendMessage", String.class);
        Object sendMessageResult = sendMessage.invoke(channel, buttonMessage);

        Method setActionRow = sendMessageResult.getClass().getMethod("setActionRow", buttonClazz, buttonClazz);
        Object setActionRowResult = setActionRow.invoke(sendMessageResult, button1, button2);

        Method queue = setActionRowResult.getClass().getMethod("queue", Consumer.class);
        queue.invoke(setActionRowResult, (Consumer<Object>) message -> {
            try {
                CompletableFuture.delayedExecutor(3, TimeUnit.MINUTES).execute(() -> {
                    if (!VelocityRequest.PlayerReqFlags.isEmpty()) {
                        try {
                            String buttonMessage2 = (String) entityMessageClazz.getMethod("getContentRaw").invoke(message);
                            Request req = reqProvider.get();
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

    public void sendWebhookMessage(String userName, String avatarUrl, String content) throws Exception {
        Constructor<?> webhookBuilderC = webhookBuilderClazz.getConstructor();
        Object webhookBuilder = webhookBuilderC.newInstance();

        Method setUsername = webhookBuilderClazz.getMethod("setUsername", String.class);
        Method setAvatarUrl = webhookBuilderClazz.getMethod("setAvatarUrl", String.class);
        Method setContent = webhookBuilderClazz.getMethod("setContent", String.class);
        Method build = webhookBuilderClazz.getMethod("build");
        setUsername.invoke(webhookBuilder, userName);
        setAvatarUrl.invoke(webhookBuilder, avatarUrl);
        setContent.invoke(webhookBuilder, content);
        build.invoke(webhookBuilder);
        
    	String webhookUrl = config.getString("Discord.Webhook_URL","");

    	if (webhookUrl.isEmpty()) return;

        Method withUrl = webhookClientClazz.getMethod("withUrl", String.class);
        Object webhookClient = withUrl.invoke(null, webhookUrl);

        Method send = webhookClient.getClass().getMethod("send", webhookMessageClazz);
        Object sendResult = send.invoke(webhookClient, webhookBuilder);

        Method thenAccept = sendResult.getClass().getMethod("thenAccept", Consumer.class);
        thenAccept.invoke(sendResult, (Consumer<Object>) _p -> {
            try {
                Method exceptionally = sendResult.getClass().getMethod("exceptionally", Function.class);
                exceptionally.invoke(sendResult, (Function<Throwable, Object>) throwable -> {
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
                Method getTextChannelById = jdaInstance.getClass().getMethod("getTextChannelById", String.class);
                Object channel = getTextChannelById.invoke(jdaInstance, channelId);

                if (channel == null) {
                    future.completeExceptionally(new RuntimeException("Channel not found!"));
                    return;
                }

                Object newEmbed = addDescriptionToEmbed(currentEmbed, additionalDescription);

                Method editMessageEmbedsById = channel.getClass().getMethod("editMessageEmbedsById", String.class, entityMessageEmbedClazz);
                Object messageAction = editMessageEmbedsById.invoke(channel, messageId, newEmbed);

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
        retrieveMessageById.invoke(channel, messageId);

        Method queue = retrieveMessageById.getReturnType().getMethod("queue", Consumer.class, Consumer.class);
        queue.invoke(retrieveMessageById.invoke(channel, messageId),
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

        Method editMessageEmbedsById = channel.getClass().getMethod("editMessageEmbedsById", String.class, entityMessageEmbedClazz);
        Object messageAction = editMessageEmbedsById.invoke(channel, messageId, newEmbed);

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
            Method sendMessageEmbeds = channel.getClass().getMethod("sendMessageEmbeds", entityMessageEmbedClazz);
            Object messageAction = sendMessageEmbeds.invoke(channel, embed);

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

        Method getTextChannelById = jdaInstance.getClass().getMethod("getTextChannelById", String.class);
        Object channel = getTextChannelById.invoke(jdaInstance, channelId);

        if (channel == null) {
        	future.complete(null);
            return;
        }
    	if (embed != null) {
            Method sendMessageEmbeds = channel.getClass().getMethod("sendMessageEmbeds", entityMessageEmbedClazz);
            Object messageAction = sendMessageEmbeds.invoke(channel, embed);

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
