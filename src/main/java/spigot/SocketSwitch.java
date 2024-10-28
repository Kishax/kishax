package spigot;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;

import com.google.inject.Inject;

public class SocketSwitch {

    public final common.Main plugin;
    private final SocketResponse sr;
    private final ServerStatusCache ssc;
    private final String hostname = "localhost";
    private ServerSocket serverSocket;
    private Thread clientThread, socketThread;
    private volatile boolean running = true;
    
    @Inject
	public SocketSwitch(common.Main plugin, SocketResponse sr, PortFinder pf, ServerStatusCache ssc) {
        this.ssc = ssc;
		this.plugin = plugin;
        this.sr = sr;
	}
    
    //Server side
    public void startSocketServer(int port) {
        socketThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                plugin.getLogger().log(Level.INFO, "Socket Server is listening on port {0}", port);
                while (running) {
                    try {
                        Socket socket2 = serverSocket.accept();
                        if (!running) {
                            socket2.close();
                            break;
                        }
                        // logger.info("New client connected()");
                        new SocketServerThread(plugin, sr, socket2).start();
                    } catch (IOException e) {
                        if (running) {
                            plugin.getLogger().log(Level.SEVERE, "An IOException error occurred: {0}", e.getMessage());
                            for (StackTraceElement element : e.getStackTrace()) {
                                plugin.getLogger().log(Level.SEVERE, element.toString());
                            }
                        }
                    }
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "An IOException error occurred: {0}", e.getMessage());
                for (StackTraceElement element : e.getStackTrace()) {
                    plugin.getLogger().log(Level.SEVERE, element.toString());
                }
            } finally {
                try {
                    if (serverSocket != null && !serverSocket.isClosed()) {
                        serverSocket.close();
                    }
                } catch (IOException e) {
                    plugin.getLogger().log(Level.SEVERE, "An IOException error occurred: {0}", e.getMessage());
                    for (StackTraceElement element : e.getStackTrace()) {
                        plugin.getLogger().log(Level.SEVERE, element.toString());
                    }
                }
            }
        });
        socketThread.start();
    }

    public void startSocketClient(int port, String sendmsg) {
	    if (port == 0) return;
	    //plugin.getLogger().info("Client Socket is Available");
	    clientThread = new Thread(() -> {
	        sendMessage(port, sendmsg);
	    });
	    clientThread.start();
	}

	private void sendMessage(int port, String sendmsg) {
	    try (Socket socket = new Socket(hostname, port);
	    	BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));) {
	    	writer.write(sendmsg + "\n");
	    	writer.flush();
	    } catch (Exception e) {
	        plugin.getLogger().log(Level.SEVERE, "An Exception error occurred: {0}", e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                plugin.getLogger().severe(element.toString());
            }
	    }
	}
    
    public void sendSpigotServer(String sendmsg) {
        sendMessageToServer("spigot", sendmsg);
    }

    public void sendVelocityServer(String sendmsg) {
        sendMessageToServer("velocity", sendmsg);
    }
    
    private void sendMessageToServer(String serverType, String sendmsg) {
        ssc.getStatusMap().values().stream()
            .flatMap(serverMap -> serverMap.entrySet().stream())
            .filter(entry -> entry.getValue().get("online") instanceof Boolean online && online)
            .filter(entry -> entry.getValue().get("platform") instanceof String platform && platform.equalsIgnoreCase(serverType))
            .filter(entry -> entry.getValue().get("socketport") instanceof Integer port && port != 0)
            .forEach(entry -> {
                int port = (Integer) entry.getValue().get("socketport");
                plugin.getLogger().log(Level.INFO, "sendSpigotServer: Starting client for server {0} on port {1}", new Object[]{entry.getKey(), port});
                startSocketClient(port, sendmsg);
            });
    }

    public void stopSocketClient() {
        try {
            if (clientThread != null && clientThread.isAlive()) {
                clientThread.interrupt();
                clientThread.join();
            }
        } catch (InterruptedException e) {
            plugin.getLogger().log(Level.SEVERE, "An InterruptedException error occurred: {0}", e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                plugin.getLogger().severe(element.toString());
            }
        }
    }
    
    public void stopSocketServer() {
    	running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close(); // これによりaccept()が解除される
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "An IOException error occurred: {0}", e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                plugin.getLogger().log(Level.SEVERE, element.toString());
            }
        }

        try {
            if (socketThread != null && socketThread.isAlive()) {
                socketThread.join(1000); // 1秒以内にスレッドの終了を待つ
                if (socketThread.isAlive()) {
                    socketThread.interrupt(); // 強制的にスレッドを停止
                }
            }
        } catch (InterruptedException e) {
            plugin.getLogger().log(Level.SEVERE, "An InterruptedException error occurred: {0}", e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                plugin.getLogger().log(Level.SEVERE, element.toString());
            }
        }
    }
}
