package keyp.forev.fmc.common.socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Objects;

import org.slf4j.Logger;

import keyp.forev.fmc.common.socket.interfaces.SocketResponse;

public class SocketServerThread extends Thread {
    public Logger logger;
    private final Socket socket;
    private final SocketResponse response;
    
    public SocketServerThread(Logger logger, Socket socket, SocketResponse response) {
        this.logger = logger;
        this.socket = socket;
        this.response = response;
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));) {
        	StringBuilder receivedMessageBuilder = new StringBuilder();
            String line;
            while (Objects.nonNull(line = reader.readLine())) {
                receivedMessageBuilder.append(line).append("\n");
            }
            String receivedMessage = receivedMessageBuilder.toString();
            	// プラットフォームによって、receivedMessageを処理するメソッドを分ける
            response.resaction(receivedMessage);
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