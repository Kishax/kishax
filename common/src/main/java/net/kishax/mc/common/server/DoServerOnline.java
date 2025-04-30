package net.kishax.mc.common.server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;

import net.kishax.mc.common.database.Database;
import net.kishax.mc.common.server.interfaces.ServerHomeDir;
import net.kishax.mc.common.socket.SocketSwitch;
import net.kishax.mc.common.socket.message.Message;

public class DoServerOnline {
  private final Logger logger;
  private final Provider<SocketSwitch> sswProvider;
  private final ServerHomeDir shd;
  private final Database db;

  @Inject
  public DoServerOnline (Logger logger, Provider<SocketSwitch> sswProvider, ServerHomeDir shd, Database db) {
    this.logger = logger;
    this.sswProvider = sswProvider;
    this.shd = shd;
    this.db = db;
  }

  public void updateDatabase(int socketport) {
    String serverName = shd.getServerName();

    Objects.requireNonNull(serverName);
    try (Connection conn = db.getConnection()) {
      db.updateLog( "UPDATE settings SET value=? WHERE name=?;", new Object[] {serverName, "now_online"});
      SocketSwitch ssw = sswProvider.get();

      Message msg = new Message();
      msg.mc = new Message.Minecraft();
      msg.mc.server = new Message.Minecraft.Server();
      msg.mc.server.action = "START";
      msg.mc.server.name = serverName;
      ssw.sendVelocityServer(conn, msg);
      ssw.sendSpigotServer(conn, msg);

      // refreshManualOnlineServer()を呼び出しているので、MineStatusSyncは不要
      String query = "UPDATE status SET online=?, socketport=? WHERE name=?;";
      try (PreparedStatement ps = conn.prepareStatement(query)) {
        ps.setBoolean(1,true);
        ps.setInt(2, socketport);
        ps.setString(3, serverName);
        int rsAffected = ps.executeUpdate();
        if (rsAffected > 0) {
          logger.info("MySQL Server is connected!");
        }
      }
    } catch (SQLException | ClassNotFoundException e) {
      logger.error("An error occurred while updating the database: " + e.getMessage(), e);
      for (StackTraceElement element : e.getStackTrace()) {
        logger.error(element.toString());
      }
    }
  }
}
