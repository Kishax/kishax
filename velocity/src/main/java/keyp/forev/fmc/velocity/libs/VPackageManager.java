package keyp.forev.fmc.velocity.libs;

import keyp.forev.fmc.common.libs.interfaces.PackageManager;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VPackageManager {

    public static Map<PackageManager, List<PackageManager>> dependencies = new HashMap<>(
        Map.of(
            VPackage.JDA, List.of(
                VPackage.CLUB_MINNCED_OPUS,
                VPackage.NV_WEBSOCKET_CLIENT,
                VPackage.OKHTTP3,
                VPackage.COMMON_COLLECTIONS
            )
        )
    );

    public enum VPackage implements PackageManager {
        JDA("maven", "net.dv8tion:JDA:5.2.0"),
        CLUB_MINNCED_OPUS("maven", "club.minnced:opus-java:1.1.1"),
        NV_WEBSOCKET_CLIENT("maven", "com.neovisionaries:nv-websocket-client:2.14"),
        OKHTTP3("maven", "com.squareup.okhttp3:okhttp:4.12.0"),
        COMMON_COLLECTIONS("maven", "org.apache.commons:commons-collections4:4.4"),
        CLUB_MINNCED_WEBHOOK("maven", "club.minnced:discord-webhooks:0.8.0"),
        ;
        private final String repositoryType;
        private final String coordinates;
        private final String groupId;
        private final String artifactId;
        private final String version;
        VPackage(String repositryType, String coordinates) {
            this.repositoryType = repositryType;
            this.coordinates = coordinates;
            String[] parts = coordinates.split(":");
            this.groupId = parts[0];
            this.artifactId = parts[1];
            this.version = parts[2];
        }

        @Override
        public List<PackageManager> getDependencies() {
            return dependencies.getOrDefault(this, Collections.emptyList());
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
}