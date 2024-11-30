package keyp.forev.fmc.common.libs.interfaces;

import java.net.URL;

import keyp.forev.fmc.common.libs.ClassManager;

public interface PackageManager {
    ClassManager getClassManager(ClassManager classManager);
    URL getUrl();
    String getClassName();
    String getVersion();
}
