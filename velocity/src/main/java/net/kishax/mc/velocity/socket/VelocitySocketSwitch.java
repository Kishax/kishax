package net.kishax.mc.velocity.socket;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Injector;

import net.kishax.mc.common.socket.SocketSwitch;

public class VelocitySocketSwitch extends SocketSwitch {
  private final Logger logger;
  private final Injector injector;

  @Inject
  public VelocitySocketSwitch(Logger logger, Injector injector) {
    super(logger, injector);
    this.logger = logger;
    this.injector = injector;
  }

  @Override
  public void startSocketServer(int port) {
    Thread socketThread = new Thread(() -> {
      try {
        ServerSocket serverSocket = new ServerSocket(port);
        logger.info("Velocity Socket Server is listening on port {}", port);
        boolean running = true;
        while (running) {
          try {
            Socket socket2 = serverSocket.accept();
            if (!running) {
              socket2.close();
              break;
            }

            new VelocitySocketServerThread(logger, socket2, injector).start();
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
      }
    });
    socketThread.start();
  }
}
