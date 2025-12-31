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
  HOME_SERVER_NAME("HOME_SERVER_NAME"),
  HOME_SERVER_IP("HOME_SERVER_IP"),
  INPUT_PERIOD("INPUT_PERIOD"),
  HUB_TELEPORT_TIME("HUB_TELEPORT_TIME"),
  
  // Image Storage Settings
  IMAGE_STORAGE_MODE("IMAGE_STORAGE_MODE"),              // "local" or "s3"
  S3_BUCKET_NAME("S3_BUCKET_NAME"),                      // S3 bucket name
  S3_PREFIX("S3_PREFIX"),                                 // S3 prefix (e.g., "images/")
  S3_REGION("S3_REGION"),                                 // AWS region (e.g., "ap-northeast-1")
  S3_USE_INSTANCE_PROFILE("S3_USE_INSTANCE_PROFILE"),    // "true" or "false"
  S3_CACHE_ENABLED("S3_CACHE_ENABLED"),                  // "true" or "false"
  S3_CACHE_DIRECTORY("S3_CACHE_DIRECTORY"),              // Local cache directory
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

  public boolean getBooleanValue() {
    return this.value != null && "true".equalsIgnoreCase(this.value);
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
