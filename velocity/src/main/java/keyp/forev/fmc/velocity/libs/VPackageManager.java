package keyp.forev.fmc.velocity.libs;

import keyp.forev.fmc.common.libs.ClassManager;
import keyp.forev.fmc.common.libs.interfaces.PackageManager;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.EnumSet;

public enum VPackageManager implements PackageManager {
    // パッケージから、使うimportリストへの1対1のマップが作れる
    // Map<PackageList, List<ClassLoader>>?
    // このマップを作ったときのメリット
    // 1. パッケージリストから、使うクラスローダーを取得できる
    JDA("maven", "net.dv8tion:JDA:5.2.0", "net.dv8tion.jda.api.JDA", EnumSet.of(
        ClassManager.SUB_COMMAND,
        ClassManager.TEXT_CHANNEL
    ))
    ,;
    private final String repositoryType;
    private final String groupId;
    private final String artifactId;
    private final String version;
    private final String className;
    private final EnumSet<ClassManager> classManagers;
    VPackageManager(String repositryType, String coordinates, String className, EnumSet<ClassManager> classManagers) {
        this.repositoryType = repositryType;
        String[] parts = coordinates.split(":");
        this.groupId = parts[0];
        this.artifactId = parts[1];
        this.version = parts[2];
        this.className = className;
        this.classManagers = classManagers;
    }

    @Override
    public EnumSet<ClassManager> getAll() {
        return classManagers;
    }

    @Override
    public ClassManager getClassManager(ClassManager classManager) {
        return classManagers.stream()
            .filter(cm -> cm.equals(classManager))
            .findFirst()
            .orElse(null);
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