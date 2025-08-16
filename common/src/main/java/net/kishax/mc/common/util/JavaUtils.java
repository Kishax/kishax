package net.kishax.mc.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.regex.Pattern;

public class JavaUtils {
  private static final Pattern UUID_PATTERN = Pattern
      .compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

  public static boolean isUUID(String str) {
    return UUID_PATTERN.matcher(str).matches();
  }

  public class Time {
    public enum Format {
      YYYY_MM_DD_HH_MM_SS("yyyy-MM-dd HH:mm:ss"),
      YYYY_MM_DD("yyyy/MM/dd"),
      ;

      private final SimpleDateFormat sdf;

      Format(String format) {
        this.sdf = new SimpleDateFormat(format);
      }

      public String format(java.util.Date date) {
        return this.sdf.format(date);
      }

      public String format(Timestamp ts) {
        return this.sdf.format(ts);
      }
    }
  }

  public static String secondsToStr(int seconds) {
    if (seconds < 60) {
      if (seconds < 10) {
        return String.format("00:00:0%d", seconds);
      }
      return String.format("00:00:%d", seconds);
    } else if (seconds < 3600) {
      int minutes = seconds / 60;
      seconds = seconds % 60;
      return String.format("00:%02d:%02d", minutes, seconds);
    } else {
      int hours = seconds / 3600;
      int remainingSeconds = seconds % 3600;
      int minutes = remainingSeconds / 60;
      seconds = remainingSeconds % 60;
      return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
  }

  public static double roundToFirstDecimalPlace(double value) {
    BigDecimal bd = new BigDecimal(Double.toString(value));
    bd = bd.setScale(1, RoundingMode.HALF_UP);
    return bd.doubleValue();
  }
}
