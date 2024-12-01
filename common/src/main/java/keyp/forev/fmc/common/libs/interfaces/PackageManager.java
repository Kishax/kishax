package keyp.forev.fmc.common.libs.interfaces;

import java.net.URL;

public interface PackageManager {
    String getName();
    String getCoordinates();
    URL getUrl();
    String getVersion();
}
