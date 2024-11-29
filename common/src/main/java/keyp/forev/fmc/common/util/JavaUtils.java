package keyp.forev.fmc.common.util;

import java.util.regex.Pattern;

public class JavaUtils {
    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    public static boolean isUUID(String str) {
        return UUID_PATTERN.matcher(str).matches();
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
}
