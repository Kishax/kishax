package keyp.forev.fmc.velocity.libs;

import keyp.forev.fmc.common.libs.interfaces.PackageManager;
import java.net.MalformedURLException;
import java.net.URL;

public enum VPackageManager implements PackageManager {
    JDA("maven", "net.dv8tion:JDA:5.2.0")
    ,;
    private final String repositoryType;
    private final String coordinates;
    private final String groupId;
    private final String artifactId;
    private final String version;
    VPackageManager(String repositryType, String coordinates) {
        this.repositoryType = repositryType;
        this.coordinates = coordinates;
        String[] parts = coordinates.split(":");
        this.groupId = parts[0];
        this.artifactId = parts[1];
        this.version = parts[2];
    }

    @Override
    public String getName() {
        return getDeclaringClass().getSimpleName();
    }

    @Override
    public URL getUrl() {
        try {
            switch (repositoryType) {
                case "maven":
                    return getMavenUrl();
                default:
                    return null;
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String getCoordinates() {
        return coordinates;
    }
    
    @Override
    public String getVersion() {
        return version;
    }

    private URL getMavenUrl() throws MalformedURLException {
        return new URL("https://repo1.maven.org/maven2/" + groupId.replace(".", "/") + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + ".jar");
    }
}