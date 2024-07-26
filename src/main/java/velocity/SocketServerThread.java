package velocity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

public class SocketServerThread extends Thread
{
    private Socket socket;
    public Main plugin;
    public SocketResponse sr;
    
    public SocketServerThread(Socket socket, Main plugin, SocketResponse sr)
    {
        this.socket = socket;
        this.plugin = plugin;
        this.sr = sr;
    }

    public void run()
    {
        try
        (
    		DataInputStream in = new DataInputStream(socket.getInputStream());
    		DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        )
        {
            // クライアントからのデータを受信
            int length = in.readInt(); // データの長さを最初に受信
            byte[] data = new byte[length];
            in.readFully(data); // データを受信

            // 受信したデータの処理
            ByteArrayDataInput dataIn = ByteStreams.newDataInput(data);
            String receivedMessage = dataIn.readUTF();
            sr.resaction(receivedMessage);
            
            // レスポンスの準備
            ByteArrayDataOutput dataOut = ByteStreams.newDataOutput();
            sr.sendresponse(receivedMessage,dataOut);
            dataOut.writeUTF("Hello, Client!"); // 例として文字列を返す

            // レスポンスの送信
            byte[] responseData = dataOut.toByteArray();
            out.writeInt(responseData.length); // レスポンスの長さを最初に送信
            out.write(responseData); // 実際のレスポンスデータを送信
        }
        catch (Exception e)
        {
            e.printStackTrace();
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
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }
}