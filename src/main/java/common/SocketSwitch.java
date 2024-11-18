package common;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;

import com.google.inject.Inject;


public class SocketSwitch {
    private final Logger logger;
    private final String hostname = "localhost";
    private ServerSocket serverSocket;
    private Thread clientThread, socketThread;
    private volatile boolean running = true;
    
    @Inject
	public SocketSwitch(Logger logger) {
        this.logger = logger;
	}
    
    //Server side
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
                        // logger.info("New client connected()");
                        new SocketServerThread(logger, socket2).start();
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

    public void startSocketClient(int port, String sendmsg) {
	    if (port == 0) return;
	    //logger.info("Client Socket is Available");
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
	        logger.error("An Exception error occurred: {}", e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                logger.error(element.toString());
            }
	    }
	}
    
    public void sendSpigotServer(Connection conn, String sendmsg) throws SQLException, ClassNotFoundException {
        sendMessageToServer(conn, "spigot", sendmsg);
    }

    public void sendVelocityServer(Connection conn, String sendmsg) throws SQLException, ClassNotFoundException {
        sendMessageToServer(conn, "velocity", sendmsg);
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
                serverSocket.close(); // これによりaccept()が解除される
            }
        } catch (IOException e) {
            logger.error("An IOException error occurred: {}", e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                logger.error(element.toString());
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
            logger.error("An InterruptedException error occurred: {}", e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                logger.error(element.toString());
            }
        }
    }

    public boolean sendSpecificServer(Connection conn, String serverName, String msg) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM status")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String serverDBName = rs.getString("name");
                    int port = rs.getInt("socketport");
                    boolean online = rs.getBoolean("online");
                    if (port == 0) {
                        //logger.info("sendSpecificServer: Server " + serverName + " has no socketport");
                        continue;
                    }
                    if (online && serverDBName.equalsIgnoreCase(serverName)) {
                        //logger.info("sendSpecificServer: Starting client for server " + serverName + " on port " + port);
                        startSocketClient(port, msg);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void sendMessageToServer(Connection conn, String serverType, String sendmsg) throws SQLException, ClassNotFoundException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM status;")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    //String serverName = rs.getString("name");
                    String platform = rs.getString("platform");
                    int port = rs.getInt("socketport");
                    boolean online = rs.getBoolean("online");
                    if (port == 0) {
                        //logger.info("sendSpigotServer: Server " + serverName + " has no socketport");
                        continue;
                    }
                    if (online && platform.equalsIgnoreCase(serverType)) {
                        //logger.info("sendSpigotServer: Starting client for server " + serverName + " on port " + port);
                        startSocketClient(port, sendmsg);
                    }
                }
            }
        }
    }
}
