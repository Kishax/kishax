package net.kishax.mc.velocity.discord;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.function.Consumer;
import java.util.function.Function;

import com.google.inject.Inject;

import net.kishax.mc.common.libs.ClassManager;
import net.kishax.mc.velocity.libs.VClassManager;
import net.kishax.mc.velocity.libs.VPackageManager;
import net.kishax.mc.velocity.util.config.VelocityConfig;

import org.slf4j.Logger;

public class Webhooker {
  private final Logger logger;
  private final VelocityConfig config;
  // Club Minnced
  private final Class<?> webhookBuilderClazz, webhookClientClazz, webhookMessageClazz;
  private final URLClassLoader webhookURLClassLoader;

  @Inject
  public Webhooker(Logger logger, VelocityConfig config) throws ClassNotFoundException {
    this.logger = logger;
    this.config = config;
    this.webhookBuilderClazz = VClassManager.CLUB_MINNCED_WEBHOOK.WEBHOOK_MESSAGE_BUILDER.get().getClazz();
    this.webhookClientClazz = VClassManager.CLUB_MINNCED_WEBHOOK.WEBHOOK_CLIENT.get().getClazz();
    this.webhookMessageClazz = VClassManager.CLUB_MINNCED_WEBHOOK.WEBHOOK_MESSAGE.get().getClazz();
    this.webhookURLClassLoader = ClassManager.urlClassLoaderMap.get(VPackageManager.VPackage.CLUB_MINNCED_WEBHOOK);
  }

  public void sendWebhookMessage(String userName, String avatarUrl, String content) throws Exception {
    Thread.currentThread().setContextClassLoader(webhookURLClassLoader);
    String webhookUrl = config.getString("Discord.Webhook_URL","");
    if (webhookUrl.isEmpty()) return;
    logger.info("1");

    Constructor<?> webhookBuilderC = webhookBuilderClazz.getConstructor();
    Object webhookBuilder = webhookBuilderC.newInstance();
    logger.info("2");

    Method setUsername = webhookBuilderClazz.getMethod("setUsername", String.class);
    Object webhookMessage = setUsername.invoke(webhookBuilder, userName);
    logger.info("3");

    Method setAvatarUrl = webhookBuilderClazz.getMethod("setAvatarUrl", String.class);
    webhookMessage = setAvatarUrl.invoke(webhookBuilder, avatarUrl);
    logger.info("4");

    Method setContent = webhookBuilderClazz.getMethod("setContent", String.class);
    webhookMessage = setContent.invoke(webhookBuilder, content);
    logger.info("5");

    Method build = webhookBuilderClazz.getMethod("build");
    webhookMessage = build.invoke(webhookBuilder);
    logger.info("6");

    Method withUrl = webhookClientClazz.getMethod("withUrl", String.class);
    Object webhookClient = withUrl.invoke(null, webhookUrl);
    logger.info("7");

    Method send = webhookClient.getClass().getMethod("send", webhookMessageClazz);
    Object sendResult = send.invoke(webhookClient, webhookMessage);
    logger.info("8");

    Method thenAccept = sendResult.getClass().getMethod("thenAccept", Consumer.class);
    logger.info("9");

    thenAccept.invoke(sendResult, (Consumer<Object>) _p -> {
      try {
        logger.info("10");
        Method exceptionally = sendResult.getClass().getMethod("exceptionally", Function.class);
        logger.info("11");
        exceptionally.invoke(sendResult, (Function<Throwable, Object>) throwable -> {
          logger.info("12");
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
}
