package keyp.forev.fmc.common.libs.interfaces;

import java.net.URL;
import java.util.List;

public interface PackageManager {
    URL getUrl();
    String getArtifactId();
    String getGroupKey();
    String getPackageType();
    List<PackageManager> getDependencies();
}
