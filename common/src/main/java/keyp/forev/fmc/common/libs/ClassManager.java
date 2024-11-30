package keyp.forev.fmc.common.libs;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URLClassLoader;

public class ClassManager {
    protected Class<?> clazz;
    protected Class<?>[] parameterTypes;
    protected URLClassLoader urlClassLoaderBase;

    public ClassManager(Class<?> clazz, Class<?>[] parameterTypes, URLClassLoader urlClassLoaderBase) {
        this.clazz = clazz;
        this.parameterTypes = parameterTypes;
        this.urlClassLoaderBase = urlClassLoaderBase;
    }

    protected Class<?> getClazzBase() {
        return clazz;
    }

    protected Constructor<?> getConstructorBase() {
        try {
            return clazz.getConstructor(parameterTypes);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            return null;
        }
    }

    protected Object createInstanceBase(Object... initargs) {
        try {
            Constructor<?> constructor = getConstructorBase();
            if (constructor != null) {
                return constructor.newInstance(initargs);
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    //public EnumSet<ClassManager> getAll() {
    //        return classManagers;
    //}

    public interface JDA {
    }
}