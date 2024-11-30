package keyp.forev.fmc.common.libs.interfaces;

import java.net.URL;
import java.util.EnumSet;

import keyp.forev.fmc.common.libs.ClassManager;

public interface PackageManager {
    EnumSet<ClassManager> getAll();
    ClassManager getClassManager(ClassManager classManager);
    URL getUrl();
    String getClassName();
    String getVersion();
}
