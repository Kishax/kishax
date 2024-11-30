package keyp.forev.fmc.velocity.libs;

import java.net.URLClassLoader;

import keyp.forev.fmc.common.libs.ClassManager;

public class VClassManager extends ClassManager {
    // JDAクラスに挿入するため、static化する
    public static URLClassLoader urlClassLoader;

    public VClassManager(Class<?> clazz, Class<?>[] parameterTypes, URLClassLoader urlClassLoader) {
        super(clazz, parameterTypes, urlClassLoader);
        VClassManager.urlClassLoader = urlClassLoader;
        //if (urlClassLoader == null) {
        //    throw new IllegalStateException("URLClassLoader is not set");
        //}
        //initializeJDAClasses();
    }

    public enum JDA implements ClassManager.JDA  {
        SUB_COMMAND("net.dv8tion.jda.api.interactions.commands.build.SubcommandData", new Class<?>[]{String.class, String.class}),
        TEXT_CHANNEL("net.dv8tion.jda.api.entities.channel.concrete.TextChannel", new Class<?>[]{String.class})
        ,;
        private String clazzName;
        private Class<?>[] parameterTypes;
        JDA(String clazzName, Class<?>[] parameterTypes) {
            this.clazzName = clazzName;
            this.parameterTypes = parameterTypes;
        }

        public ClassManager getClassManager() throws ClassNotFoundException {
            Class<?> clazz = Class.forName(clazzName, true, VClassManager.urlClassLoader);
            return new ClassManager(clazz, parameterTypes, VClassManager.urlClassLoader);
        }
    }
}
