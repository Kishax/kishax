package net.kishax.mc.velocity.socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Objects;

import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.inject.Injector;

import net.kishax.mc.common.socket.message.Message;

public class VelocitySocketServerThread extends Thread {
  private final Logger logger;
  private final Socket socket;
  private final Injector injector;

  public VelocitySocketServerThread(Logger logger, Socket socket, Injector injector) {
    this.logger = logger;
    this.socket = socket;
    this.injector = injector;
  }

  @Override
  public void run() {
    // logger.info("DEBUG: New socket connection established from: {}", socket.getRemoteSocketAddress());
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));) {
      String line;
      while (Objects.nonNull(line = reader.readLine())) {
        // logger.info("DEBUG: Received socket message: {}", line);
        Gson gson = new Gson();
        try {
          Message message = gson.fromJson(line, Message.class);
          // logger.info("DEBUG: Parsed message successfully, processing...");
          VelocityMessageProcessor msgProcessor = injector.getInstance(VelocityMessageProcessor.class);
          msgProcessor.process(message);
          // logger.info("DEBUG: Message processing completed");
        } catch (Exception e) {
          logger.error("JSON parse error: {}", e.getMessage());
        }
      }
    } catch (Exception e) {
      logger.error("An Exception error occurred: {}", e.getMessage());
      for (StackTraceElement element : e.getStackTrace()) {
        logger.error(element.toString());
      }
    } finally {
      try {
        if (socket != null && !socket.isClosed()) {
          socket.close();
        }
      } catch (IOException e) {
        logger.error("An IOException error occurred: {}", e.getMessage());
        for (StackTraceElement element : e.getStackTrace()) {
          logger.error(element.toString());
        }
      }
    }
  }
}