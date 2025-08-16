package net.kishax.mc.velocity.libs;

import java.net.URLClassLoader;

import net.kishax.mc.common.libs.ClassManager;

public class VClassManager {
  public enum JDA {
    JDA("net.dv8tion.jda.api.JDA"),
    JDA_BUILDER("net.dv8tion.jda.api.JDABuilder"),
    LISTENER_ADAPTER("net.dv8tion.jda.api.hooks.ListenerAdapter"),
    EMBED_BUILDER("net.dv8tion.jda.api.EmbedBuilder"),
    GATEWAY_INTENTS("net.dv8tion.jda.api.requests.GatewayIntent"),
    SUB_COMMAND("net.dv8tion.jda.api.interactions.commands.build.SubcommandData"),
    TEXT_CHANNEL("net.dv8tion.jda.api.entities.channel.concrete.TextChannel"),
    OPTION_DATA("net.dv8tion.jda.api.interactions.commands.build.OptionData"),
    OPTION_TYPE("net.dv8tion.jda.api.interactions.commands.OptionType"),
    ENTITYS_MESSAGE("net.dv8tion.jda.api.entities.Message"),
    ENTITYS_MESSAGE_EMBED("net.dv8tion.jda.api.entities.MessageEmbed"),
    ENTITYS_ACTIVITY("net.dv8tion.jda.api.entities.Activity"),
    ENTITYS_ICON("net.dv8tion.jda.api.entities.Icon"),
    ENTITYS_EMBED_TYPE("net.dv8tion.jda.api.entities.EmbedType"),
    BUTTON("net.dv8tion.jda.api.interactions.components.buttons.Button"),
    PRESENCE("net.dv8tion.jda.api.managers.Presence"),
    ERROR_RESPONSE_EXCEPTION("net.dv8tion.jda.api.exceptions.ErrorResponseException"),
    EVENT_MESSAGE_UPDATE("net.dv8tion.jda.api.events.message.MessageUpdateEvent"),
    COMMAND_CREATE_ACTION("net.dv8tion.jda.api.requests.restaction.CommandCreateAction"),
    REST_ACTION("net.dv8tion.jda.api.requests.RestAction"),
    ITEM_COMPONENT("net.dv8tion.jda.api.interactions.components.ItemComponent"),
    ROLE("net.dv8tion.jda.api.entities.Role"),
    WEBHOOK_CLIENT("net.dv8tion.jda.api.entities.Webhook"),
    WEBHOOK_MESSAGE_CREATE_ACTION("net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction"),
    ;

    private String clazzName;
    private URLClassLoader urlClassLoader;

    JDA(String clazzName) {
      this.clazzName = clazzName;
      if (ClassManager.urlClassLoaderMap != null) {
        this.urlClassLoader = ClassManager.urlClassLoaderMap.get(VPackageManager.VPackage.JDA);
      }
    }

    public ClassManager get() throws ClassNotFoundException {
      // Class<?> clazz = Class.forName(clazzName, true, urlClassLoader);
      Class<?> clazz = urlClassLoader.loadClass(clazzName);
      return new ClassManager(clazz);
    }
  }
}
