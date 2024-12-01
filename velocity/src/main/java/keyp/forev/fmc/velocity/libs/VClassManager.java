package keyp.forev.fmc.velocity.libs;

import java.net.URLClassLoader;

import keyp.forev.fmc.common.libs.ClassManager;

public class VClassManager {
    public static URLClassLoader urlClassLoader;

    public enum JDA {
        SUB_COMMAND(
            "net.dv8tion.jda.api.interactions.commands.build.SubcommandData",
            new Class<?>[]{String.class, String.class}
        ),
        TEXT_CHANNEL(
            "net.dv8tion.jda.api.entities.channel.concrete.TextChannel",
            new Class<?>[]{String.class}
        );

        private String clazzName;
        private Class<?>[] parameterTypes;

        JDA(String clazzName, Class<?>[] parameterTypes) {
            this.clazzName = clazzName;
            this.parameterTypes = parameterTypes;
            //Class<?> declaringClass = this.getDeclaringClass();
            //declaringClass.getDeclaredMethod("get");
            //declaringClass.getGenericInterfaces().clone();
            //declaringClass.getDeclaredMethod("get");
            // declaringClassをキーにしたマップよりurlClassLoaderが取得できれば...
        }
        
        public ClassManager get() throws ClassNotFoundException {
            Class<?> clazz = Class.forName(clazzName, true, VClassManager.urlClassLoader);
            return new ClassManager(clazz, parameterTypes, VClassManager.urlClassLoader);
        }
    }
}