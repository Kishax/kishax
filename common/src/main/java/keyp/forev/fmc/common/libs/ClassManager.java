package keyp.forev.fmc.common.libs;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public enum ClassManager {
    SUB_COMMAND("net.dv8tion.jda.api.interactions.commands.build.SubcommandData", String.class, String.class),
    TEXT_CHANNEL("net.dv8tion.jda.api.entities.channel.concrete.TextChannel", String.class)
    ,;
    // 以下、どうにかして、ClassLoaderインスタンスを取得する
    //private final ClassLoader urlClassLoader = new ClassLoaderInterface();
    private Class<?> clazz;
    private Class<?>[] parameterTypes;
    ClassManager(String className, Class<?>... parameterTypes) {
        try {
            //this.clazz = Class.forName(className); //←ココ
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