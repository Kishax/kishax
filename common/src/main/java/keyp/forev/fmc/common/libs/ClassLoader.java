package keyp.forev.fmc.common.libs;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import keyp.forev.fmc.common.libs.interfaces.PackageList;

public class ClassLoader {
    public CompletableFuture<List<Class<?>>> loadClassesFromJars(List<PackageList> packages, Path dataDirectory) {
        List<CompletableFuture<Class<?>>> futures = packages.stream()
            .map(pkg -> {
                Path jarPath = dataDirectory.resolve("libs/" + getFileNameFromURL(pkg.getUrl()));
                return loadClassFromJar(jarPath, pkg.getClassName());
            })
            .collect(Collectors.toList());
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList()));
    }

    public CompletableFuture<Class<?>> loadClassFromJar(Path jarPath, String className) {
        return CompletableFuture.supplyAsync(() -> {
            try (URLClassLoader classLoader = new URLClassLoader(new URL[]{jarPath.toUri().toURL()})) {
                return classLoader.loadClass(className);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    private String getFileNameFromURL(URL url) {
        String urlString = url.toString();
        return urlString.substring(urlString.lastIndexOf('/') + 1);
    }
}
