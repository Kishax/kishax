package keyp.forev.fmc.common.libs;

import java.lang.reflect.Field;
import java.net.URLClassLoader;
import java.util.Map;

import keyp.forev.fmc.common.libs.interfaces.PackageManager;

public class ClassManager {
    public static Map<PackageManager, URLClassLoader> urlClassLoaderMap;
    private Class<?> clazz;
    // private URLClassLoader urlClassLoader;
    // このクラスにアクセスするときは、
    // このクラスを継承しているEnumクラス郡を媒介にすることで、
    // このクラスを取得することができる
    // 例として、velocity/Mainクラスに記載
    public ClassManager(Class<?> clazz) {
        this.clazz = clazz;
    }

    // 複数コンストラクタで、URLClassLoaderを使う場合
    public ClassManager(URLClassLoader urlClassLoaderBase) {
        this((Class<?>) null);
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public Field getField(String name) {
        try {
            return clazz.getField(name);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            return null;
        }
    }
}