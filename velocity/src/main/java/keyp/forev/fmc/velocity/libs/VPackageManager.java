package keyp.forev.fmc.velocity.libs;

import keyp.forev.fmc.common.libs.PackageType;
import keyp.forev.fmc.common.libs.interfaces.PackageManager;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class VPackageManager {

    private static String DISCORD = "discord";

    public enum VPackage implements PackageManager {
        JDA(
            "net.dv8tion:JDA:5.2.0",
            "https://github.com/discord-jda/JDA/releases/download/v5.2.0/JDA-5.2.0-withDependencies.jar",
            PackageType.MAIN, 
            DISCORD),
        CLUB_MINNCED_WEBHOOK(
            "club.minnced:discord-webhooks:0.8.0", 
            PackageType.MAIN, 
            DISCORD
            ),
        ;
        private final String url;
        private final String groupId;
        private final String artifactId;
        private final String version;
        private final String groupKey;
        private final String packageType;
        VPackage(String coordinates, String packageType, String groupKey) {
            this.url = null;
            this.groupKey = groupKey;
            this.packageType = packageType;
            String[] parts = coordinates.split(":");
            this.groupId = parts[0];
            this.artifactId = parts[1];
            this.version = parts[2];
        }

        VPackage(String coordinates, String url, String packageType, String groupKey) {
            this.url = url;
            this.groupKey = groupKey;
            this.packageType = packageType;
            String[] parts = coordinates.split(":");
            this.groupId = parts[0];
            this.artifactId = parts[1];
            this.version = parts[2];
        }

        @Override
        public String getGroupKey() {
            return groupKey;
        }

        @Override
        public String getPackageType() {
            return packageType;
        }

        @Override
        public String getArtifactId() {
            return artifactId;
        }

        @Override
        public List<PackageManager> getDependencies() {
            List<PackageManager> dependencies = new ArrayList<>();  
            if (packageType.equals(PackageType.MAIN)) {
                List<PackageManager> lists = new ArrayList<>(List.of(values()));
                lists.stream().filter(pkg -> pkg.getGroupKey().equals(groupKey) && !pkg.equals(this)).forEach(dependencies::add);
            }
            return dependencies;
        }
        
        @Override
        public URL getUrl() {
            try {
                if (url != null) {
                    return new URL(url);
                } else {
                    return getMavenUrl();
                }
            } catch (MalformedURLException e) {
                throw new RuntimeException("An error occurred while exec getUrl method in VPackageManager", e);
            }
        }

        private URL getMavenUrl() throws MalformedURLException {
            return new URL("https://repo1.maven.org/maven2/" + groupId.replace(".", "/") + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + ".jar");
        }
    }
}