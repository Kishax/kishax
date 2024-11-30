package keyp.forev.fmc.velocity.libs;

import java.net.URLClassLoader;

import keyp.forev.fmc.common.libs.ClassManager;

// urlClassLoaderによって、インスタンスはバインドされるようにしたい
// guiceを使わずに(guiceを使う前にクラスロードするから、使えない)
public class VClassManager<T extends Enum<T>> extends ClassManager<T> {
    // あとでどっかに格納する？
    // public static URLClassLoader urlClassLoader; // ここ、ClassManagerクラスより、protected変数を継承してる。
    // URLClassLoaderをプールするためのクラスを作成する
    // というかそもそも、URLClassLoaderについての知識が足りていない
    // それは、Jarの中のパスに依存しているのか？
    // dataDirecotry/plugins/libsに依存しているのは、確認済み
    // 
    public VClassManager(Class<T> clazz, Class<?>[] parameterTypes, URLClassLoader urlClassLoader) {
        super(clazz, parameterTypes/*, urlClassLoader */);
        //VClassManager.urlClassLoader = urlClassLoader;
    }

    // 複数コンストラクタで、URLClassLoaderを使う場合
    public VClassManager(URLClassLoader urlClassLoader) {
        super(null, null/*, urlClassLoader */);
    }

    public enum JDA implements ClassManager.JDA {
        SUB_COMMAND("net.dv8tion.jda.api.interactions.commands.build.SubcommandData", new Class<?>[]{String.class, String.class}),
        TEXT_CHANNEL("net.dv8tion.jda.api.entities.channel.concrete.TextChannel", new Class<?>[]{String.class});

        private String clazzName;
        private Class<?>[] parameterTypes;

        JDA(String clazzName, Class<?>[] parameterTypes) {
            this.clazzName = clazzName;
            this.parameterTypes = parameterTypes;
        }
        
        @Override
        public ClassManager<?> get() throws ClassNotFoundException {
            Class<?> clazz = Class.forName(clazzName, true, VClassManager.urlClassLoader);
            return new ClassManager<>(clazz, parameterTypes, VClassManager.urlClassLoader);
        }
    }
}