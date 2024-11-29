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
    private URLClassLoader urlClassLoader;

    public ClassLoader() {
        this.urlClassLoader = (URLClassLoader) java.lang.ClassLoader.getSystemClassLoader();
    }

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
            try {
                addURLToClassLoader(jarPath.toUri().toURL());
                return urlClassLoader.loadClass(className);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    private void addURLToClassLoader(URL url) throws IOException {
        URLClassLoader sysLoader = (URLClassLoader) java.lang.ClassLoader.getSystemClassLoader();
        URLClassLoader urlClassLoader = new URLClassLoader(new URL[]{url}, sysLoader);
        this.urlClassLoader = urlClassLoader;
    }

    private String getFileNameFromURL(URL url) {
        String urlString = url.toString();
        return urlString.substring(urlString.lastIndexOf('/') + 1);
    }
}