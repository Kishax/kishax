package net.kishax.mc.common.settings;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.kishax.mc.common.database.Database;

public enum Settings {
  MAX_IMAGE_TILES("maximagetiles"),
  LARGE_IMAGE_LIMIT_TIMES("largeimageuploadlimittimes"),
  IMAGE_LIMIT_TIMES("imageuploadlimittimes"),
  DISCORD_IMAGE_LIMIT_TIMES("discordimageuploadlimittimes"),
  IMAGE_FOLDER("image_folder"),
  RULEBOOK_CONTENT("rulebook"),
  CONFIRM_URL("confirm_url"),
  HOME_SERVER_NAME("home_server_name"),
  INPUT_PERIOD("input_period"),
  HUB_TELEPORT_TIME("hubteleporttime"),
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
    } catch (SQLException | ClassNotFoundException e) {}
    return dbValue;
  }
}
