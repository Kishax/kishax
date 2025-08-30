package net.kishax.mc.common.settings;

public enum PermSettings {
  NEW_USER("group.new-user"),
  SUB_ADMIN("group.sub-admin"),
  SUPER_ADMIN("group.super-admin"),
  HUB("kishax.proxy.hub"),
  CEND("kishax.proxy.cend"),
  PERM("kishax.proxy.perm"),
  SILENT("kishax.proxy.silent"),
  TPR("kishax.tpr"),
  TELEPORT_REGISTER_POINT("kishax.registerpoint"),
  IMAGEMAP_REGISTER_MAP("kishax.registermap");

  private final String value;

  PermSettings(String key) {
    this.value = key;
  }

  public String get() {
    return this.value;
  }
}
