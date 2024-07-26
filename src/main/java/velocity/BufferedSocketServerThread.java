package velocity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Objects;

import org.slf4j.Logger;

import com.google.inject.Inject;

public class BufferedSocketServerThread extends Thread
{
    private Socket socket;
    public Main plugin;
    public Logger logger;
    public SocketResponse sr;
    
    public BufferedSocketServerThread(Socket socket, Main plugin, Logger logger, SocketResponse sr)
    {
        this.socket = socket;
        this.plugin = plugin;
        this.logger = logger;
        this.sr = sr;
    }

    public void run()
    {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));)
        {
        	StringBuilder receivedMessageBuilder = new StringBuilder();
            String line;
            while (Objects.nonNull(line = reader.readLine()))
            {
                receivedMessageBuilder.append(line).append("\n");
            }
            
            String receivedMessage = receivedMessageBuilder.toString();
            
            sr.resaction(receivedMessage);
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