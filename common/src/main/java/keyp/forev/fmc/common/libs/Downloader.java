package keyp.forev.fmc.common.libs;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import keyp.forev.fmc.common.libs.interfaces.PackageManager;

public class Downloader {
    public CompletableFuture<Boolean> downloadPackage(URL url, Path targetPath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // もしファイルが存在していたらダウンロードしない
                if (Files.exists(targetPath)) {
                    return true;
                }
                System.out.println("Downloading: " + url + " to " + targetPath);
                Files.createDirectories(targetPath.getParent());
                try (InputStream in = url.openStream()) {
                    Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    public CompletableFuture<List<Boolean>> downloadPackages(List<PackageManager> packages, Path dataDirectory) {
        List<CompletableFuture<Boolean>> futures = packages.stream()
            .map(pkg -> {
                URL url = pkg.getUrl();
                // ダウンロードパスに依存
                Path targetPath = PackageType.getTargetPath(pkg, dataDirectory);
                return downloadPackage(url, targetPath);
            })
            .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList()));
    }
}