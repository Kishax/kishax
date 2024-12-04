package keyp.forev.fmc.common.libs.interfaces;

import java.net.URL;
import java.util.List;

public interface PackageManager {
    String getName();
    String getCoordinates();
    URL getUrl();
    String getVersion();
    List<PackageManager> getDependencies();
}
