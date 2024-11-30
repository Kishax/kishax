package keyp.forev.fmc.common.libs;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URLClassLoader;

public class ClassManager {
    protected Class<?> clazz;
    protected Class<?>[] parameterTypes;
    protected URLClassLoader urlClassLoaderBase;

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