package keyp.forev.fmc.velocity.libs;

import keyp.forev.fmc.common.libs.interfaces.PackageList;
import java.net.MalformedURLException;
import java.net.URL;

public enum VelocityPackageList implements PackageList {
    JDA("maven", "net.dv8tion:JDA:5.2.0", "net.dv8tion.jda.api.JDA");
    private final String repositoryType;
    private final String groupId;
    private final String artifactId;
    private final String version;
    private final String className;
    VelocityPackageList(String repositryType, String coordinates, String className) {
        this.repositoryType = repositryType;
        String[] parts = coordinates.split(":");
        this.groupId = parts[0];
        this.artifactId = parts[1];
        this.version = parts[2];
        this.className = className;
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
    public String getClassName() {
        return className;
    }

    @Override
    public String getVersion() {
        return version;
    }

    private URL getMavenUrl() throws MalformedURLException {
        return new URL("https://repo1.maven.org/maven2/" + groupId.replace(".", "/") + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + ".jar");
    }
}