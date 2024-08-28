package spigot;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;
import java.util.logging.Level;

import com.google.inject.Inject;

public class SocketSwitch {

    public String sendmsg;
    
    private Thread clientThread;
    private Thread socketThread;
    private volatile boolean running = true;
    private ServerSocket serverSocket;
    public final common.Main plugin;
    
    @Inject
	public SocketSwitch(common.Main plugin) {
		this.plugin = plugin;
	}
	
	//Client side
	public void startSocketClient(String sendmsg) {
	    if (plugin.getConfig().getInt("Socket.Client_Port") == 0) {
	        plugin.getLogger().info("Client Socket is canceled for config value not given");
	        return;
	    }

	    plugin.getLogger().info("Client Socket is Available");

	    clientThread = new Thread(() -> {
	        sendMessage(sendmsg);
	    });

	    clientThread.start();
	}

	private void sendMessage(String sendmsg) {
	    String hostname = "localhost";

	    try (
	    	Socket socket = new Socket(hostname, plugin.getConfig().getInt("Socket.Client_Port"));
	    	BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
	    ) {
	    	writer.write(sendmsg + "\n");
	    	writer.flush();
	    } catch (Exception e) {
	        plugin.getLogger().log(Level.SEVERE, "An Exception error occurred: {0}", e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                plugin.getLogger().severe(element.toString());
            }
	    }
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
    
    //Server side
    public void startSocketServer() {
		if (Objects.isNull(plugin.getConfig().getInt("Socket.Server_Port")) || plugin.getConfig().getInt("Socket.Server_Port") ==0) {
			plugin.getLogger().info("Socket Server is canceled for config value not given");
			return;
		}

		plugin.getLogger().info("Server Socket is Available");
        socketThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(plugin.getConfig().getInt("Socket.Server_Port"));
                plugin.getLogger().log(Level.INFO, "Socket Server is listening on port {0}", plugin.getConfig().getInt("Socket.Server_Port"));
                
                while (running) {
                    try {
                        Socket socket = serverSocket.accept();
                        if (!running) {
                            socket.close();
                            break;
                        }

                        plugin.getLogger().info("New client connected"); 
                        new SocketServerThread(socket, plugin).start();
                    } catch (IOException e) {
                        if (running) {
                            plugin.getLogger().log(Level.SEVERE, "An InterruptedException error occurred: {0}", e.getMessage());
                            for (StackTraceElement element : e.getStackTrace()) {
                                plugin.getLogger().severe(element.toString());
                            }
                        }
                    }
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "An IOException error occurred: {0}", e.getMessage());
                for (StackTraceElement element : e.getStackTrace()) {
                    plugin.getLogger().severe(element.toString());
                }
            } finally {
                try {
                    if (serverSocket != null && !serverSocket.isClosed()) {
                        serverSocket.close();
                    }
                } catch (IOException e) {
                    plugin.getLogger().log(Level.SEVERE, "An IOException error occurred: {0}", e.getMessage());
                    for (StackTraceElement element : e.getStackTrace()) {
                        plugin.getLogger().severe(element.toString());
                    }
                }
            }
        });

        socketThread.start();
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
                plugin.getLogger().severe(element.toString());
            }
        }

        try
        {
            if (socketThread != null && socketThread.isAlive()) {
                socketThread.join(1000); // 1秒以内にスレッドの終了を待つ
                if (socketThread.isAlive()) {
                    socketThread.interrupt(); // 強制的にスレッドを停止
                }
            }
        } catch (InterruptedException e) {
            plugin.getLogger().log(Level.SEVERE, "An IOException error occurred: {0}", e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                plugin.getLogger().severe(element.toString());
            }
        }
    }
}
