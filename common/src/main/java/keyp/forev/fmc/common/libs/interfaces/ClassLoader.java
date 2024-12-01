package keyp.forev.fmc.common.libs.interfaces;

import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface ClassLoader {
    CompletableFuture<Map<PackageManager, URLClassLoader>> makeURLClassLoaderFromJars(List<PackageManager> packages, Path dataDirectory);
}
