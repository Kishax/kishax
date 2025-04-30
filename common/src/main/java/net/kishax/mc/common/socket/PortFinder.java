package net.kishax.mc.common.socket;

import java.net.ServerSocket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;

import com.google.inject.Inject;

import net.kishax.mc.common.database.Database;

public class PortFinder {
  public static int foundPort;
  private final Database db;
  private final Logger logger;
  private final int startPort;
  private final int endPort;

  @Inject
  public PortFinder(Logger logger, Database db) {
    this.logger = logger;
    this.db = db;
    this.startPort = 6000;
    this.endPort = 6999;
  }

  public CompletableFuture<Integer> findAvailablePortAsync() {
    return CompletableFuture.supplyAsync(() -> {
      List<Integer> portRange = IntStream.rangeClosed(startPort, endPort).boxed().collect(Collectors.toList());
      List<Integer> usingPorts = getUsingPorts();
      portRange.removeAll(usingPorts);
      for (int port : portRange) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
          serverSocket.close();
          return port;
        } catch (Exception e) {
          logger.info("Port {} is already in use, so skip and tried next one", port);
        }
      }
      throw new RuntimeException("No available port found in the range " + startPort + " to " + endPort);
    });
  }

  private List<Integer> getUsingPorts() {
    List<Integer> usingPorts = new ArrayList<>();
    try (Connection conn = db.getConnection()) {
      String sql = "SELECT socketport FROM status;";
      try (PreparedStatement ps = conn.prepareStatement(sql);
          ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          Object portObj = rs.getObject("socketport");
          if (portObj instanceof Integer) {
            usingPorts.add((Integer) portObj);
          }
        }
      }
    } catch (SQLException | ClassNotFoundException e) {
      logger.error("An error occurred while getting using ports: {}", e.getMessage());
      for (StackTraceElement element : e.getStackTrace()) {
        logger.error(element.toString());
      }
    }
    return usingPorts;
  }
}
