package f5.si.kishax.mc.common.libs;

import java.net.URL;
import java.nio.file.Path;

import f5.si.kishax.mc.common.libs.interfaces.PackageManager;

public class PackageType {
  public static String MAIN = "MAIN";
  public static String COMPILE = "COMPILE";
  public static String RUNTIME = "RUNTIME";

  public static Path getTargetPath(PackageManager pkg, Path dataDirectory) {
    String groupKey = pkg.getGroupKey();
    String packageType = pkg.getPackageType();
    String url = getFileNameFromURL(pkg.getUrl());
    Path targetPath = null;
    switch (packageType) {
      case "MAIN" -> {
        targetPath = dataDirectory.resolve("libs/" + groupKey + "/main/" + url);
      }
      case "COMPILE" -> {
        targetPath = dataDirectory.resolve("libs/" + groupKey + "/compile/" + url);
      }
      case "RUNTIME" -> {
        targetPath = dataDirectory.resolve("libs/" + groupKey + "/runtime/" + url);
      }
    }

    if (targetPath == null) {
      targetPath = dataDirectory.resolve("libs/" + groupKey + "/main/" + url);
    }
    return targetPath;
  }

  private static String getFileNameFromURL(URL url) {
    String urlString = url.toString();
    return urlString.substring(urlString.lastIndexOf('/') + 1);
  }
}
