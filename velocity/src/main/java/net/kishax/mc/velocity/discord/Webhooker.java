package net.kishax.mc.velocity.discord;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.function.Consumer;

import com.google.inject.Inject;

import net.kishax.mc.common.libs.ClassManager;
import net.kishax.mc.velocity.libs.VClassManager;
import net.kishax.mc.velocity.libs.VPackageManager;
import net.kishax.mc.velocity.util.config.VelocityConfig;

import org.slf4j.Logger;

public class Webhooker {
  private final Logger logger;
  private final VelocityConfig config;
  // JDA Webhook
  private final Class<?> webhookClientClazz, webhookMessageCreateActionClazz;
  private final URLClassLoader jdaURLClassLoader;

  @Inject
  public Webhooker(Logger logger, VelocityConfig config) throws ClassNotFoundException {
    this.logger = logger;
    this.config = config;
    this.webhookClientClazz = VClassManager.JDA.WEBHOOK_CLIENT.get().getClazz();
    this.webhookMessageCreateActionClazz = VClassManager.JDA.WEBHOOK_MESSAGE_CREATE_ACTION.get().getClazz();
    this.jdaURLClassLoader = ClassManager.urlClassLoaderMap.get(VPackageManager.VPackage.JDA);
  }

  public void sendWebhookMessage(String userName, String avatarUrl, String content) throws Exception {
    Thread.currentThread().setContextClassLoader(jdaURLClassLoader);
    String webhookUrl = config.getString("Discord.Webhook_URL","");
    if (webhookUrl.isEmpty()) return;
    logger.info("1");

    Method fromUrl = webhookClientClazz.getMethod("fromUrl", String.class);
    Object webhook = fromUrl.invoke(null, webhookUrl);
    logger.info("2");

    Method sendMessage = webhook.getClass().getMethod("sendMessage", String.class);
    Object webhookMessageCreateAction = sendMessage.invoke(webhook, content);
    logger.info("3");

    Method setUsername = webhookMessageCreateAction.getClass().getMethod("setUsername", String.class);
    webhookMessageCreateAction = setUsername.invoke(webhookMessageCreateAction, userName);
    logger.info("4");

    Method setAvatarUrl = webhookMessageCreateAction.getClass().getMethod("setAvatarUrl", String.class);
    webhookMessageCreateAction = setAvatarUrl.invoke(webhookMessageCreateAction, avatarUrl);
    logger.info("5");

    Method queue = webhookMessageCreateAction.getClass().getMethod("queue", Consumer.class, Consumer.class);
    logger.info("6");

    queue.invoke(webhookMessageCreateAction, 
      (Consumer<Object>) _p -> logger.info("Webhook message sent successfully"),
      (Consumer<Throwable>) throwable -> {
        logger.error("A sendWebhookMessage error occurred: " + throwable.getMessage());
        for (StackTraceElement element : throwable.getStackTrace()) {
          logger.error(element.toString());
        }
      }
    );
    logger.info("7");
  }
}
