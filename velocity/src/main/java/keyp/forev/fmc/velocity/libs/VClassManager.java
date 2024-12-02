package keyp.forev.fmc.velocity.libs;

import java.net.URLClassLoader;

import keyp.forev.fmc.common.libs.ClassManager;

public class VClassManager {
    public enum JDA {
        JDA_BUILDER("net.dv8tion.jda.api.JDABuilder", new Class<?>[]{}),
        GATEWAY_INTENTS("net.dv8tion.jda.api.requests.GatewayIntent", new Class<?>[]{int.class}),
        SUB_COMMAND("net.dv8tion.jda.api.interactions.commands.build.SubcommandData", new Class<?>[]{String.class, String.class}),
        TEXT_CHANNEL("net.dv8tion.jda.api.entities.channel.concrete.TextChannel", new Class<?>[]{String.class}),
        OPTION_DATA("net.dv8tion.jda.api.interactions.commands.build.OptionData", new Class<?>[]{String.class, String.class}),
        OPTION_TYPE("net.dv8tion.jda.api.interactions.commands.OptionType", new Class<?>[]{int.class}),
        RESTACTION_MESSAGE("net.dv8tion.jda.api.entities.channel.middleman.MessageChannel", new Class<?>[]{String.class}),
        ENTITYS_MESSAGE("net.dv8tion.jda.api.entities.Message", new Class<?>[]{String.class}),
        ENTITYS_ACTIVITY("net.dv8tion.jda.api.entities.Activity", new Class<?>[]{String.class}),
        BUTTON("net.dv8tion.jda.api.interactions.components.buttons.Button", new Class<?>[]{String.class, String.class}),
        PRESENCE("net.dv8tion.jda.api.managers.Presence", new Class<?>[]{String.class}),
        ;
        private String clazzName;
        private Class<?>[] parameterTypes;
        private URLClassLoader urlClassLoader;
        JDA(String clazzName, Class<?>[] parameterTypes) {
            this.clazzName = clazzName;
            this.parameterTypes = parameterTypes;
            if (ClassManager.urlClassLoaderMap != null) {
                this.urlClassLoader = ClassManager.urlClassLoaderMap.get(VPackageManager.JDA);
            }
        }
        public ClassManager get() throws ClassNotFoundException {
            Class<?> clazz = Class.forName(clazzName, true, urlClassLoader);
            return new ClassManager(clazz, parameterTypes);
        }
    }
    public enum CLUB_MINNCED {
        WEBHOOK_CLIENT("club.minnced.discord.webhook.WebhookClient", new Class<?>[]{}),
        WEBHOOK_MESSAGE("club.minnced.discord.webhook.send.WebhookMessage", new Class<?>[]{String.class}),
        ;
        private String clazzName;
        private Class<?>[] parameterTypes;
        private URLClassLoader urlClassLoader;
        CLUB_MINNCED(String clazzName, Class<?>[] parameterTypes) {
            this.clazzName = clazzName;
            this.parameterTypes = parameterTypes;
            if (ClassManager.urlClassLoaderMap != null) {
                this.urlClassLoader = ClassManager.urlClassLoaderMap.get(VPackageManager.CLUB_MINNCED);
            }
        }
        public ClassManager get() throws ClassNotFoundException {
            Class<?> clazz = Class.forName(clazzName, true, urlClassLoader);
            return new ClassManager(clazz, parameterTypes);
        }
    }
}