package f5.si.kishax.mc.common.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class ExtUtil {
  public static String getExtension(URL getUrl) throws IOException {
    HttpURLConnection connection = (HttpURLConnection) getUrl.openConnection();
    connection.setRequestMethod("GET");
    connection.connect();
    String contentType = connection.getContentType();

    final String ext;
    switch (contentType) {
      case "image/png" -> ext = "png";
      case "image/jpeg" -> ext = "jpeg";
      case "image/jpg" -> ext = "jpg";
      default -> {
        return null;
      }
    }
    return ext;
  }
}
