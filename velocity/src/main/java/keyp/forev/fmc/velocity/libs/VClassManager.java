package keyp.forev.fmc.velocity.libs;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URLClassLoader;

import keyp.forev.fmc.common.libs.ClassManager;

public class VClassManager extends ClassManager {
    public static URLClassLoader urlClassLoader;
    public VClassManager() {
        super();
        urlClassLoader = urlClassLoaderBase;
    }

    public enum JDA {
        SUB_COMMAND("net.dv8tion.jda.api.interactions.commands.build.SubcommandData", String.class, String.class),
        TEXT_CHANNEL("net.dv8tion.jda.api.entities.channel.concrete.TextChannel", String.class)
        ,;
        private Class<?> clazz;
        private Class<?>[] parameterTypes;
        JDA(String className, Class<?>... parameterTypes) {
            try {
                this.clazz = Class.forName(className, true, urlClassLoader);
                this.parameterTypes = parameterTypes;
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        public Class<?> getClazz() {
            return clazz;
        }

        public Constructor<?> getConstructor() {
            try {
                return clazz.getConstructor(parameterTypes);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
                return null;
            }
        }

        public Object createInstance(Object... initargs) {
            try {
                Constructor<?> constructor = getConstructor();
                if (constructor != null) {
                    return constructor.newInstance(initargs);
                }
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
