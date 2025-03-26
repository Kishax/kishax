package keyp.forev.fmc.common.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum PatternUtil {
  URL("^(https?)://[\\w.-]+(?:\\.[\\w.-]+)+[/\\w\\-._~:/?#[\\\\]@!$&'()*+,;=.]*$");

  private final String patternText;
  private final Pattern pattern;

  PatternUtil(String patternText) {
    this.patternText = patternText;
    this.pattern = Pattern.compile(patternText);
  }

  public String get() {
    return this.patternText;
  }

  public boolean check(String text) {
    if (text == null || text.isEmpty()) {
      return false; // nullまたは空文字列は無効
    }

    Matcher matcher = pattern.matcher(text);
    return matcher.matches();
  }
}
