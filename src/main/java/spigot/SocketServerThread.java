package spigot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Objects;
import java.util.logging.Level;

public class SocketServerThread extends Thread {
    
    public common.Main plugin;
    public SocketResponse sr;
    private final Socket socket;
    
    public SocketServerThread (common.Main plugin, SocketResponse sr, Socket socket) {
        this.plugin = plugin;
        this.sr = sr;
        this.socket = socket;
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
            
            sr.resaction(receivedMessage);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "An Exception error occurred: {0}", e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                plugin.getLogger().log(Level.SEVERE, element.toString());
            }
        } finally {
            try {
                if (socket != null && !socket.isClosed()) {
                	socket.close();
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "An IOException error occurred: {0}", e.getMessage());
                for (StackTraceElement element : e.getStackTrace()) {
                    plugin.getLogger().log(Level.SEVERE, element.toString());
                }
            }
        }
    }
}