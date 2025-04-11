package f5.si.kishax.mc.common.socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Objects;

import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.inject.Injector;

import f5.si.kishax.mc.common.socket.message.Message;
import f5.si.kishax.mc.common.socket.message.MessageProcessor;

public class SocketServerThread extends Thread {
  private final Logger logger;
  private final Socket socket;
  private final Injector injector;

  public SocketServerThread(Logger logger, Socket socket, Injector injector) {
    this.logger = logger;
    this.socket = socket;
    this.injector = injector;
  }

  @Override
  public void run() {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));) {
      String line;
      while (Objects.nonNull(line = reader.readLine())) {
        Gson gson = new Gson();
        try {
          Message message = gson.fromJson(line, Message.class);
          MessageProcessor msgProcessor = injector.getInstance(MessageProcessor.class);
          msgProcessor.process(message);
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
