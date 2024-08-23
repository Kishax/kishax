package spigot;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.logging.Level;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

public class SocketServerThread extends Thread 
{
    private final Socket socket;
    public final common.Main plugin;
    
    public SocketServerThread(Socket socket, common.Main plugin) 
    {
        this.socket = socket;
        this.plugin = plugin;
    }

    @Override
    public void run() 
    {
        try
        (	
        	DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream())
        )
        {
            // クライアントからのデータを受信
            int length = in.readInt(); // データの長さを最初に受信
            byte[] data = new byte[length];
            in.readFully(data); // データを受信

            // 受信したデータの処理
            ByteArrayDataInput dataIn = ByteStreams.newDataInput(data);
            String receivedMessage = dataIn.readUTF();
            //System.out.println("Received: " + receivedMessage);
            this.plugin.getLogger().log(Level.INFO, "Received: {0}", receivedMessage);

            // レスポンスの準備
            ByteArrayDataOutput dataOut = ByteStreams.newDataOutput();
            dataOut.writeUTF("Hello, Client!"); // 例として文字列を返す

            // レスポンスの送信
            byte[] responseData = dataOut.toByteArray();
            out.writeInt(responseData.length); // レスポンスの長さを最初に送信
            out.write(responseData); // 実際のレスポンスデータを送信

        } 
        catch (Exception e) 
        {
            plugin.getLogger().log(Level.SEVERE, "An Exception error occurred: {0}", e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) 
            {
                plugin.getLogger().severe(element.toString());
            }
        } 
        finally 
        {
            try 
            {
                if (socket != null && !socket.isClosed()) 
                {
                    socket.close();
                }
            } 
            catch (IOException e) 
            {
                plugin.getLogger().log(Level.SEVERE, "An IOException error occurred: {0}", e.getMessage());
                for (StackTraceElement element : e.getStackTrace()) 
                {
                    plugin.getLogger().severe(element.toString());
                }
            }
        }
    }
}

