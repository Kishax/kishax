package keyp.forev.fmc.velocity.libs;

import java.net.URLClassLoader;

import keyp.forev.fmc.common.libs.ClassManager;

public class VClassManager {
    public enum JDA {
        JDA_BUILDER("net.dv8tion.jda.api.JDABuilder"),
        EMBED_BUILDER("net.dv8tion.jda.api.EmbedBuilder"),
        GATEWAY_INTENTS("net.dv8tion.jda.api.requests.GatewayIntent"),
        SUB_COMMAND("net.dv8tion.jda.api.interactions.commands.build.SubcommandData"),
        TEXT_CHANNEL("net.dv8tion.jda.api.entities.channel.concrete.TextChannel"),
        OPTION_DATA("net.dv8tion.jda.api.interactions.commands.build.OptionData"),
        OPTION_TYPE("net.dv8tion.jda.api.interactions.commands.OptionType"),
        RESTACTION_MESSAGE("net.dv8tion.jda.api.entities.channel.middleman.MessageChannel"),
        ENTITYS_MESSAGE("net.dv8tion.jda.api.entities.Message"),
        ENTITYES_MESSAGE_EMBED("net.dv8tion.jda.api.entities.MessageEmbed"),
        ENTITYS_ACTIVITY("net.dv8tion.jda.api.entities.Activity"),
        BUTTON("net.dv8tion.jda.api.interactions.components.buttons.Button"),
        PRESENCE("net.dv8tion.jda.api.managers.Presence"),
        ERROR_RESPONSE_EXCEPTION("net.dv8tion.jda.api.exceptions.ErrorResponseException"),
        ;
        private String clazzName;
        private URLClassLoader urlClassLoader;
        JDA(String clazzName) {
            this.clazzName = clazzName;
            if (ClassManager.urlClassLoaderMap != null) {
                this.urlClassLoader = ClassManager.urlClassLoaderMap.get(VPackageManager.JDA);
            }
        }
        public ClassManager get() throws ClassNotFoundException {
            Class<?> clazz = Class.forName(clazzName, true, urlClassLoader);
            return new ClassManager(clazz);
        }
    }
    public enum CLUB_MINNCED {
        WEBHOOK_CLIENT("club.minnced.discord.webhook.WebhookClient"),
        WEBHOOK_MESSAGE("club.minnced.discord.webhook.send.WebhookMessage"),
        ;
        private String clazzName;
        private URLClassLoader urlClassLoader;
        CLUB_MINNCED(String clazzName) {
            this.clazzName = clazzName;
            if (ClassManager.urlClassLoaderMap != null) {
                this.urlClassLoader = ClassManager.urlClassLoaderMap.get(VPackageManager.CLUB_MINNCED);
            }
        }
        public ClassManager get() throws ClassNotFoundException {
            Class<?> clazz = Class.forName(clazzName, true, urlClassLoader);
            return new ClassManager(clazz);
        }
    }
}