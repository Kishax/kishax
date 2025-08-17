package net.kishax.mc.velocity.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import net.kishax.mc.common.database.Database;
import net.kishax.mc.velocity.util.config.VelocityConfig;

@Singleton
public class SettingsSyncService {
  private final VelocityConfig config;
  private final Database db;
  private final Logger logger;

  @Inject
  public SettingsSyncService(VelocityConfig config, Database db, Logger logger) {
    this.config = config;
    this.db = db;
    this.logger = logger;
  }

  public void syncSettingsToDatabase() {
    Map<String, Object> settingsMap = config.getStringObjectMap("Settings");
    if (settingsMap == null || settingsMap.isEmpty()) {
      logger.info("No Settings configuration found in config.yml");
      return;
    }

    try (Connection conn = db.getConnection()) {
      String query = "INSERT INTO settings (name, value) VALUES (?, ?) " +
                     "ON DUPLICATE KEY UPDATE value = VALUES(value)";
      
      for (Map.Entry<String, Object> entry : settingsMap.entrySet()) {
        String key = entry.getKey();
        String value = entry.getValue() != null ? entry.getValue().toString() : "";
        
        try (PreparedStatement ps = conn.prepareStatement(query)) {
          ps.setString(1, key);
          ps.setString(2, value);
          ps.executeUpdate();
          logger.debug("Synced setting: {} = {}", key, value);
        }
      }
      
      logger.info("Successfully synced {} settings to database", settingsMap.size());
    } catch (SQLException | ClassNotFoundException e) {
      logger.error("Failed to sync settings to database: {}", e.getMessage());
    }
  }
}