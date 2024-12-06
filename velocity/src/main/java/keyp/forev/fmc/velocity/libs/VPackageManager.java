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
            PackageType.MAIN, 
            DISCORD),
        CLUB_MINNCED_OPUS(
            "club.minnced:opus-java:1.1.1", 
            PackageType.COMPILE, 
            DISCORD),
        NV_WEBSOCKET_CLIENT(
            "com.neovisionaries:nv-websocket-client:2.14", 
            PackageType.COMPILE, 
            DISCORD),
        OKHTTP3(
            "com.squareup.okhttp3:okhttp:4.12.0", 
            PackageType.COMPILE, 
            DISCORD),
        COMMON_COLLECTIONS(
            "org.apache.commons:commons-collections4:4.4", 
            PackageType.COMPILE, 
            DISCORD
            ),
        CLUB_MINNCED_WEBHOOK(
            "club.minnced:discord-webhooks:0.8.0", 
            PackageType.MAIN, 
            DISCORD
            ),
        SF_TROVE(
            "net.sf.trove4j:core:3.1.0",
            PackageType.COMPILE,
            DISCORD
            ),
        GOOGLE_TINK(
            "'com.google.crypto.tink:tink:1.15.0'",
            PackageType.COMPILE,
            DISCORD
            ),
        ;
        private final String groupId;
        private final String artifactId;
        private final String version;
        private final String groupKey;
        private final String packageType;
        VPackage(String coordinates, String packageType, String groupKey) {
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
                return getMavenUrl();
            } catch (MalformedURLException e) {
                throw new RuntimeException("An error occurred while exec getUrl method in VPackageManager", e);
            }
        }

        private URL getMavenUrl() throws MalformedURLException {
            return new URL("https://repo1.maven.org/maven2/" + groupId.replace(".", "/") + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + ".jar");
        }
    }
}