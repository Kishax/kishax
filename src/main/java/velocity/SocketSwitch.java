package velocity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;

public class SocketSwitch
{
	private Thread clientThread;
	private Thread socketThread;
	private Thread bufferedsocketThread;
	private ServerSocket serverSocket;
	private ServerSocket bufferedserverSocket;
	public String sendmsg;
	private volatile boolean running = true;
	private volatile boolean bufferedrunning = true;
	private final Main plugin;
	private final Config config;
	private final Logger logger;
	
	@Inject
	public SocketSwitch(Main plugin,Logger logger,Config config)
	{
		this.plugin = plugin;
		this.logger = logger;
		this.config = config;
	}
	
	//Client side
	public void startSocketClient(String sendmsg)
	{
		if(config.getInt("Socket.Client_Port",0)==0)
		{
			logger.info("Client Socket is canceled for config value not given");
			return;
		}
		logger.info("Client Socket is Available");
        clientThread = new Thread(() ->
        {
        	
            String hostname = "localhost";
            
            try
            (
            	Socket socket = new Socket(hostname, config.getInt("Socket.Client_Port"));
            	DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            	DataInputStream in = new DataInputStream(socket.getInputStream())
            )
            {

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
                
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        });

        clientThread.start();
    }

	
    public void stopSocketClient()
    {
        try
        {
            if (clientThread != null && clientThread.isAlive())
            {
                clientThread.interrupt();
                clientThread.join();
            }
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }
    
    //Server side
    public void startSocketServer()
    {
		if(config.getInt("Socket.Server_Port",0)==0)
		{
			logger.info("Server Socket is canceled for config value not given");
			return;
		}
		logger.info("Server Socket is Available");
        socketThread = new Thread(() ->
        {
            try
            {
                serverSocket = new ServerSocket(config.getInt("Socket.Server_Port"));
                logger.info("Socket Server is listening on port " + config.getInt("Socket.Server_Port"));

                while (running)
                {
                    try
                    {
                        Socket socket = serverSocket.accept();
                        if (!running)
                        {
                            socket.close();
                            break;
                        }
                        logger.info("New client connected");
                        new SocketServerThread(socket,plugin).start();
                    }
                    catch (Exception e)
                    {
                        if (running)
                        {
                            logger.error("Error accepting client connection");
                            e.printStackTrace();
                        }
                    }
                }
            }
            catch (Exception e)
            {
                logger.error("Socket Server socket error");
                e.printStackTrace();
            }
            finally
            {
                try
                {
                    if (serverSocket != null && !serverSocket.isClosed())
                    {
                        serverSocket.close();
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });

        socketThread.start();
    }

    public void startBufferedSocketServer()
    {
		if(config.getInt("Socket.Buffered_Server_Port",0)==0)
		{
			logger.info("Buffered Server Socket is canceled for config value not given");
			return;
		}
		logger.info("Buffered Server Socket is Available");
        bufferedsocketThread = new Thread(() ->
        {
            try
            {
                bufferedserverSocket = new ServerSocket(config.getInt("Socket.Buffered_Server_Port"));
                logger.info("Buffered Socket Server is listening on port " + config.getInt("Socket.Buffered_Server_Port"));

                while (bufferedrunning)
                {
                    try
                    {
                        Socket socket2 = bufferedserverSocket.accept();
                        if (!bufferedrunning)
                        {
                            socket2.close();
                            break;
                        }
                        logger.info("New client connected");
                        new BufferedSocketServerThread(socket2,plugin).start();
                    }
                    catch (Exception e)
                    {
                        if (bufferedrunning)
                        {
                            logger.error("Error accepting client connection");
                            e.printStackTrace();
                        }
                    }
                }
            }
            catch (Exception e)
            {
                logger.error("Socket Server socket error");
                e.printStackTrace();
            }
            finally
            {
                try
                {
                    if (bufferedserverSocket != null && !bufferedserverSocket.isClosed())
                    {
                        bufferedserverSocket.close();
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });

        bufferedsocketThread.start();
    }
    
    public void stopBufferedSocketServer()
    {
    	bufferedrunning = false;
        try
        {
            if (bufferedserverSocket != null && !bufferedserverSocket.isClosed())
            {
                bufferedserverSocket.close(); // これによりaccept()が解除される
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        try
        {
            if (bufferedsocketThread != null && bufferedsocketThread.isAlive())
            {
                bufferedsocketThread.join(1000); // 1秒以内にスレッドの終了を待つ
                if (bufferedsocketThread.isAlive())
                {
                    bufferedsocketThread.interrupt(); // 強制的にスレッドを停止
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    
    public void stopSocketServer()
    {
        running = false;
        try
        {
            if (serverSocket != null && !serverSocket.isClosed())
            {
                serverSocket.close(); // これによりaccept()が解除される
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        try
        {
            if (socketThread != null && socketThread.isAlive())
            {
                socketThread.join(1000); // 1秒以内にスレッドの終了を待つ
                if (socketThread.isAlive())
                {
                    socketThread.interrupt(); // 強制的にスレッドを停止
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}