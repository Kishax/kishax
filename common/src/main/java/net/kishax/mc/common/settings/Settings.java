package net.kishax.mc.common.settings;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.kishax.mc.common.database.Database;

public enum Settings {
  MAX_IMAGE_TILES("MAX_IMAGE_TILES"),
  LARGE_IMAGE_LIMIT_TIMES("LARGE_IMAGE_LIMIT_TIMES"),
  IMAGE_LIMIT_TIMES("IMAGE_LIMIT_TIMES"),
  IMAGE_FOLDER("IMAGE_FOLDER"),
  CONFIRM_URL("CONFIRM_URL"),
  API_GATEWAY_URL("API_GATEWAY_URL"),
  HOME_SERVER_NAME("HOME_SERVER_NAME"),
  HOME_SERVER_IP("HOME_SERVER_IP"),
  INPUT_PERIOD("INPUT_PERIOD"),
  HUB_TELEPORT_TIME("HUB_TELEPORT_TIME"),
  ;

  private final Database db = Database.getInstance();
  private final String columnKey;
  private final String value;

  Settings(String key) {
    this.columnKey = key;
    this.value = loadValueFromDatabase(key);
  }

  public String getValue() {
    return this.value;
  }

  public int getIntValue() {
    return this.value != null ? Integer.parseInt(this.value) : 0;
  }

  public String getColumnKey() {
    return this.columnKey;
  }

  private String loadValueFromDatabase(String key) {
    if (db == null) {
      throw new IllegalStateException("Database has not been set");
    }
    String dbValue = null;
    try (Connection connForSettings = db.getConnection()) {
      String query = "SELECT value FROM settings WHERE name = ?";
      PreparedStatement ps = connForSettings.prepareStatement(query);
      ps.setString(1, key);
      ResultSet rs = ps.executeQuery();
      if (rs.next()) {
        dbValue = rs.getString("value");
      }
    } catch (SQLException | ClassNotFoundException e) {
    }
    return dbValue;
  }
}
