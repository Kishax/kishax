package net.kishax.mc.velocity.auth;

/**
 * MC認証レベルを表すenum
 * kishax-awsのAuthLevelと対応
 */
public enum AuthLevel {
  MC_UNAUTHENTICATED("a", "MC未認証"),
  MC_AUTHENTICATED_TRYING("b", "MC認証中（確認待ち）"),
  MC_AUTHENTICATED_UNLINKED("c", "MC認証クリア＋Kishaxアカウント未連携"),
  MC_AUTHENTICATED_LINKED("d", "MC認証クリア＋Kishaxアカウント連携済み"),
  MC_AUTHENTICATED_PRODUCT("e", "MC認証クリア＋プロダクト購入済み");

  private final String code;
  private final String description;

  AuthLevel(String code, String description) {
    this.code = code;
    this.description = description;
  }

  public String getCode() {
    return code;
  }

  public String getDescription() {
    return description;
  }

  public static AuthLevel fromCode(String code) {
    for (AuthLevel level : values()) {
      if (level.code.equals(code)) {
        return level;
      }
    }
    throw new IllegalArgumentException("Unknown auth level code: " + code);
  }

  /**
   * 文字列コードから変換
   */
  public static AuthLevel fromString(String authLevelStr) {
    if (authLevelStr == null) {
      return MC_UNAUTHENTICATED;
    }

    return switch (authLevelStr) {
      case "MC_UNAUTHENTICATED" -> MC_UNAUTHENTICATED;
      case "MC_AUTHENTICATED_TRYING" -> MC_AUTHENTICATED_TRYING;
      case "MC_AUTHENTICATED_UNLINKED" -> MC_AUTHENTICATED_UNLINKED;
      case "MC_AUTHENTICATED_LINKED" -> MC_AUTHENTICATED_LINKED;
      case "MC_AUTHENTICATED_PRODUCT" -> MC_AUTHENTICATED_PRODUCT;
      default -> MC_UNAUTHENTICATED;
    };
  }
}
