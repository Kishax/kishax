package net.kishax.mc.common.database.interfaces;

public interface DatabaseInfo {
  String getHost();

  String getUser();

  String getPassword();

  String getDefaultDatabase();

  int getPort();

  boolean check();
}
