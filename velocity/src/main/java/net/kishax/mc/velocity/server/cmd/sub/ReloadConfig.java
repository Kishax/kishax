package net.kishax.mc.velocity.server.cmd.sub;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;

import net.kishax.mc.common.database.Database;
import net.kishax.mc.velocity.server.DoServerOnline;
import net.kishax.mc.velocity.util.config.VelocityConfig;
import net.kyori.adventure.text.Component;

public class ReloadConfig {
  private final Database db;
  private final VelocityConfig config;
  private final Logger logger;
  private final DoServerOnline dso;

  @Inject
  public ReloadConfig(Database db, VelocityConfig config, Logger logger, DoServerOnline dso) {
    this.db = db;
    this.config = config;
    this.logger = logger;
    this.dso = dso;
  }

  public void execute(@NotNull CommandSource source, String[] args) {
    try (Connection conn = db.getConnection()) {
      if (config instanceof VelocityConfig) {
        ((VelocityConfig) config).loadConfig();
      } else {
        logger.error("Config is not an instance of Config.");
      }
      dso.updateAndSyncDatabase(true);
    } catch (IOException | SQLException | ClassNotFoundException e1) {
      logger.error("An IOException | SQLException | ClassNotFoundException error occurred: " + e1.getMessage());
      for (StackTraceElement element : e1.getStackTrace()) {
        logger.error(element.toString());
      }
    }
    source.sendMessage(Component.text("コンフィグをリロードしました。"));
  }
}
