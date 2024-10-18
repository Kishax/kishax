package velocity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;

public class SocketSwitch {

	private Thread clientThread;
	private Thread socketThread;
	private ServerSocket serverSocket;
	public String sendmsg;
	private volatile boolean running = true;
	private final Config config;
	private final Logger logger;
	private final SocketResponse sr;
	
	@Inject
	public SocketSwitch(Logger logger, Config config, SocketResponse sr) {
		this.logger = logger;
		this.config = config;
		this.sr = sr;
	}
	
	//Client side
	public void startSocketClient(String sendmsg) {
		if (config.getInt("Socket.Client_Port",0)==0) {
			logger.info("Client Socket is canceled for config value not given");
			return;
		}

		logger.info("Client Socket is Available");
        clientThread = new Thread(() -> {
            String hostname = "localhost";
            try (
            	Socket socket = new Socket(hostname, config.getInt("Socket.Client_Port"));
            	DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            	DataInputStream in = new DataInputStream(socket.getInputStream())
            ) {
                // 送信するデータの準備
                ByteArrayDataOutput dataOut = ByteStreams.newDataOutput();
                dataOut.writeUTF(sendmsg); // 例として文字列を送信

                // データの送信
                byte[] dataToSend = dataOut.toByteArray();
                out.writeInt(dataToSend.length); // データの長さを最初に送信
                out.write(dataToSend); // 実際のデータを送信

                // レスポンスの受信
                int length = in.readInt(); // レスポンスの長さを最初に受信
                byte[] responseData = new byte[length];
                in.readFully(responseData); // レスポンスデータを受信

                // 受信したデータの処理
                String response = new String(responseData, "UTF-8");
                logger.info("Server response: " + response);
                
            } catch (Exception e) {
                logger.error("An Exception error occurred: " + e.getMessage());
                for (StackTraceElement element : e.getStackTrace()) {
                    logger.error(element.toString());
                }
            }
        });

        clientThread.start();
    }
	
    public void stopSocketClient() {
        try {
            if (clientThread != null && clientThread.isAlive()) {
                clientThread.interrupt();
                clientThread.join();
            }
        } catch (InterruptedException e) {
            logger.error("An InterruptedException error occurred: " + e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                logger.error(element.toString());
            }
        }
    }
    
    public void startSocketServer() {
		if (config.getInt("Socket.Server_Port",0)==0) {
            logger.info("Socket.Server_Port: "+config.getInt("Socket.Server_Port"));
			logger.info("Server Socket is canceled for config value not given");
			return;
		}

		logger.info("Server Socket is Available");
        socketThread = new Thread(() -> {
            try {
                serverSocket = new ServerSocket(config.getInt("Socket.Server_Port"));
                logger.info("Socket Server is listening on port " + config.getInt("Socket.Server_Port"));

                while (running) {
                    try {
                        Socket socket2 = serverSocket.accept();
                        if (!running) {
                            socket2.close();
                            break;
                        }
                        // logger.info("New client connected()");
                        new SocketServerThread(logger, sr, socket2).start();
                    } catch (IOException e) {
                        if (running) {
                            logger.error("An IOException error occurred: " + e.getMessage());
                            for (StackTraceElement element : e.getStackTrace()) {
                                logger.error(element.toString());
                            }
                        }
                    }
                }
            } catch (IOException e) {
                logger.error("Socket Server socket error");
            } finally {
                try {
                    if (serverSocket != null && !serverSocket.isClosed()) {
                        serverSocket.close();
                    }
                } catch (IOException e) {
                    logger.error("An IOException error occurred: " + e.getMessage());
                    for (StackTraceElement element : e.getStackTrace()) {
                        logger.error(element.toString());
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
            logger.error("An IOException error occurred: " + e.getMessage());
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
            logger.error("An InterruptedException error occurred: " + e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                logger.error(element.toString());
            }
        }
    }
}