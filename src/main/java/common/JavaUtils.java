package common;

import java.util.regex.Pattern;

public class JavaUtils {
    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    public static boolean isUUID(String str) {
        return UUID_PATTERN.matcher(str).matches();
    }
}
