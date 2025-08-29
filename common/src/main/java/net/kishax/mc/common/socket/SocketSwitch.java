package net.kishax.mc.common.socket;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;

import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Injector;

import net.kishax.mc.common.socket.message.Message;
import net.kishax.mc.common.settings.Settings;

public class SocketSwitch {
  private final Logger logger;
  private final Injector injector;
  private final Gson gson = new Gson();
  private ServerSocket serverSocket;
  private Thread clientThread, socketThread;
  private volatile boolean running = true;

  @Inject
  public SocketSwitch(Logger logger, Injector injector) {
    this.logger = logger;
    this.injector = injector;
  }

  public void startSocketServer(int port) {
    socketThread = new Thread(() -> {
      try {
        serverSocket = new ServerSocket(port);
        logger.info("Socket Server is listening on port {}", port);
        while (running) {
          try {
            Socket socket2 = serverSocket.accept();
            if (!running) {
              socket2.close();
              break;
            }

            new SocketServerThread(logger, socket2, injector).start();
          } catch (IOException e) {
            if (running) {
              logger.error("An IOException error occurred: {}", e.getMessage());
              for (StackTraceElement element : e.getStackTrace()) {
                logger.error(element.toString());
              }
            }
          }
        }
      } catch (IOException e) {
        logger.error("An IOException error occurred: {}", e.getMessage());
        for (StackTraceElement element : e.getStackTrace()) {
          logger.error(element.toString());
        }
      } finally {
        try {
          if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
          }
        } catch (IOException e) {
          logger.error("An IOException error occurred: {}", e.getMessage());
          for (StackTraceElement element : e.getStackTrace()) {
            logger.error(element.toString());
          }
        }
      }
    });
    socketThread.start();
  }

  public void startSocketClient(int port, Message msg) {
    if (port == 0)
      return;
    clientThread = new Thread(() -> {
      try (Socket socket = new Socket("localhost", port);
          BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));) {
        writer.write(gson.toJson(msg) + "\n");
        writer.flush();
      } catch (Exception e) {
        logger.error("An Exception error occurred: {}", e.getMessage());
        for (StackTraceElement element : e.getStackTrace()) {
          logger.error(element.toString());
        }
      }
    });
    clientThread.start();
  }

  public void sendSpigotServer(Connection conn, Message msg) throws SQLException, ClassNotFoundException {
    sendMessageToServer(conn, "spigot", msg);
  }

  public void sendVelocityServer(Connection conn, Message msg) throws SQLException, ClassNotFoundException {
    sendMessageToServer(conn, "velocity", msg);
  }

  public void stopSocketClient() {
    try {
      if (clientThread != null && clientThread.isAlive()) {
        clientThread.interrupt();
        clientThread.join();
      }
    } catch (InterruptedException e) {
      logger.error("An InterruptedException error occurred: {}", e.getMessage());
      for (StackTraceElement element : e.getStackTrace()) {
        logger.error(element.toString());
      }
    }
  }

  public void stopSocketServer() {
    running = false;
    try {
      if (serverSocket != null && !serverSocket.isClosed()) {
        serverSocket.close();
      }
    } catch (IOException e) {
      logger.error("An IOException error occurred: {}", e.getMessage());
      for (StackTraceElement element : e.getStackTrace()) {
        logger.error(element.toString());
      }
    }

    try {
      if (socketThread != null && socketThread.isAlive()) {
        socketThread.join(1000);
        if (socketThread.isAlive()) {
          socketThread.interrupt();
        }
      }
    } catch (InterruptedException e) {
      logger.error("An InterruptedException error occurred: {}", e.getMessage());
      for (StackTraceElement element : e.getStackTrace()) {
        logger.error(element.toString());
      }
    }
  }

  public boolean sendSpecificServer(Connection conn, Message msg) throws SQLException {
    String serverName = msg.mc.server.name;

    try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM status")) {
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          String serverDBName = rs.getString("name");
          int port = rs.getInt("socketport");
          boolean online = rs.getBoolean("online");
          if (port == 0) {
            continue;
          }
          if (online && serverDBName.equalsIgnoreCase(serverName)) {
            startSocketClient(port, msg);
            return true;
          }
        }
      }
    }
    return false;
  }

  private void sendMessageToServer(Connection conn, String serverType, Message msg)
      throws SQLException, ClassNotFoundException {
    try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM status;")) {
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          String platform = rs.getString("platform");
          int port = rs.getInt("socketport");
          boolean online = rs.getBoolean("online");
          if (port == 0) {
            continue;
          }
          if (online && platform.equalsIgnoreCase(serverType)) {
            startSocketClient(port, msg);
          }
        }
      }
    }
  }

  /**
   * Web側（SQS経由でAPI Gateway）にメッセージを送信
   */
  public void sendToWeb(Message msg) {
    Thread webSendThread = new Thread(() -> {
      try {
        String apiGatewayUrl = Settings.API_GATEWAY_URL.getValue();
        if (apiGatewayUrl == null || apiGatewayUrl.isEmpty()) {
          logger.warn("API Gateway URL is not configured. Skipping web message send.");
          return;
        }

        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

        String jsonMessage = gson.toJson(msg);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(apiGatewayUrl))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonMessage))
            .timeout(Duration.ofSeconds(30))
            .build();

        HttpResponse<String> response = client.send(request, 
            HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
          logger.info("Successfully sent message to Web via API Gateway");
        } else {
          logger.warn("Failed to send message to Web. Status code: {}, Response: {}", 
              response.statusCode(), response.body());
        }

      } catch (Exception e) {
        logger.error("Error sending message to Web via API Gateway: {}", e.getMessage());
        for (StackTraceElement element : e.getStackTrace()) {
          logger.error(element.toString());
        }
      }
    });
    webSendThread.start();
  }
}
