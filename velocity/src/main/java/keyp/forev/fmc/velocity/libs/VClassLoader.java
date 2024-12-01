package keyp.forev.fmc.velocity.libs;

import java.util.Map;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import keyp.forev.fmc.common.libs.interfaces.PackageManager;
import keyp.forev.fmc.common.libs.interfaces.ClassLoader;

public class VClassLoader implements ClassLoader {
    // Mainクラスで一番最初に呼び出し、URLClassLoaderを作成する
    public CompletableFuture<Map<PackageManager, URLClassLoader>> makeURLClassLoaderFromJars(List<PackageManager> packages, Path dataDirectory) {
        Map<PackageManager, CompletableFuture<URLClassLoader>> futures = packages.stream()
            .collect(Collectors.toMap(
                pkg -> pkg,
                pkg -> {
                    Path jarPath = dataDirectory.resolve("libs/" + getFileNameFromURL(pkg.getUrl()));
                    return CompletableFuture.supplyAsync(() -> {
                        try {
                            URL url = jarPath.toUri().toURL();
                            URLClassLoader sysLoader = (URLClassLoader) java.lang.ClassLoader.getSystemClassLoader();
                            return new URLClassLoader(new URL[]{url}, sysLoader);
                        } catch (IOException e) {
                            e.printStackTrace();
                            return null;
                        }
                    });
                }
            ));

        return CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().join()
                )));
    }

    private String getFileNameFromURL(URL url) {
        String urlString = url.toString();
        return urlString.substring(urlString.lastIndexOf('/') + 1);
    }
}