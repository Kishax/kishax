package f5.si.kishax.mc.common.libs;

import java.lang.reflect.Field;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

import f5.si.kishax.mc.common.libs.interfaces.PackageManager;

public class ClassManager {
  public static Map<PackageManager, URLClassLoader> urlClassLoaderMap = new HashMap<>();
  private Class<?> clazz;

  public ClassManager(Class<?> clazz) {
    this.clazz = clazz;
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
