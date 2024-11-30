package keyp.forev.fmc.common.libs;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URLClassLoader;

// わからないことが一点あって、それは、
// この場合、ClassManagerクラスから、
// (interface) >> VClassManagerクラス >> (enum JDA) >> (JDAの)ClassManagerクラス
// が得られるが、これは双方向からもいけるのかどうか？
// それによって、毎回、どちらかのクラスから、クラスをロードするかを決める必要があり、
// 現在、ClassManagerクラスとVClassManagerクラスでは、複数コンストラクタを使っているが
// それの結果によって、どちらかを削除する必要がある
// 予想は、VClassManagerクラスから、取得したほうが良いのかもしれない。
// なぜなら、VClassManagerクラスは、ClassManagerクラスを継承しているため。
// 一度、毎回、VClassManagerクラスから取得することを試してみるため、
// したのこのクラスでのurlClassLoaderはコメントアウトし、VClassManagerクラスで管理するものとする
public class ClassManager<T> {
    protected Class<T> clazz;
    protected Class<?>[] parameterTypes;
    //protected URLClassLoader urlClassLoader;

    // 普通にこのクラスにアクセスするときは、
    // VClassManager<JDA> manager = new VClassManager<>(JDA.class, new Class<?>[]{}, urlClassLoader);
    // このようにして、取得が可能になる
    // しかし、そんなことをしなくても、
    // このクラスを継承しているVClassManagerを媒介にするか、
    // このクラスを継承しているEnumを媒介にすることで、
    // このクラスを取得することができる
    // 例として、velocity/Mainクラスに記載
    public ClassManager(Class<T> clazz, Class<?>[] parameterTypes/*, URLClassLoader urlClassLoader*/) {
        this.clazz = clazz;
        this.parameterTypes = parameterTypes;
        /*this.urlClassLoader = urlClassLoader;*/
    }

    // 複数コンストラクタで、URLClassLoaderを使う場合
    public ClassManager(URLClassLoader urlClassLoaderBase) {
        this(null, null/*, null*/);
    }

    public Class<T> getClazz() {
        return clazz;
    }

    public Constructor<T> getConstructor() {
        try {
            return clazz.getConstructor(parameterTypes);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return null;
        }
    }

    public T createInstance(Object... initargs) {
        try {
            Constructor<T> constructor = getConstructor();
            if (constructor != null) {
                return constructor.newInstance(initargs);
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    public interface JDA {
        ClassManager<SubCommand> SUB_COMMAND = new ClassManager<>(SubCommand.class, new Class<?>[]{String.class, String.class});
        //ClassManager<TextChannel> TEXT_CHANNEL = new ClassManager<>(TextChannel.class, new Class<?>[]{String.class}, null);
        // 継承先のクラスで定義されたパッケージ情報の変数を使い、ClassManagerクラスを生成
        ClassManager<?> get() throws ClassNotFoundException;
    }

    public enum SubCommand {
        COMMAND1, COMMAND2;
    }

    //public enum TextChannel {
    //    CHANNEL1, CHANNEL2;
    //}
}