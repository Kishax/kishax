package spigot;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;

import com.google.inject.Inject;

public class SocketSwitch
{
    public String sendmsg;
    
    private Thread clientThread;
    private Thread socketThread;
    private volatile boolean running = true;
    private ServerSocket serverSocket;
    public final common.Main plugin;
    
    @Inject
	public SocketSwitch(common.Main plugin)
	{
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

	private void sendMessage(String sendmsg)
	{
	    String hostname = "localhost";

	    try
	    (
	    	Socket socket = new Socket(hostname, plugin.getConfig().getInt("Socket.Client_Port"));
	    	BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
	    )
	    {
	    	writer.write(sendmsg + "\n");
	    	writer.flush();
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
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
		if(Objects.isNull(plugin.getConfig().getInt("Socket.Server_Port")) || plugin.getConfig().getInt("Socket.Server_Port") ==0)
		{
			plugin.getLogger().info("Socket Server is canceled for config value not given");
			return;
		}
		plugin.getLogger().info("Server Socket is Available");
        socketThread = new Thread(() ->
        {
            try
            {
                serverSocket = new ServerSocket(plugin.getConfig().getInt("Socket.Server_Port"));
                plugin.getLogger().info("Socket Server is listening on port " + plugin.getConfig().getInt("Socket.Server_Port"));

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
                        plugin.getLogger().info("New client connected");
                        new SocketServerThread(socket, plugin).start();
                    }
                    catch (Exception e)
                    {
                        if (running) 
                        {
                            plugin.getLogger().severe("Error accepting client connection");
                            e.printStackTrace();
                        }
                    }
                }
            }
            catch (Exception e)
            {
                plugin.getLogger().severe("Socket Server socket error");
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
