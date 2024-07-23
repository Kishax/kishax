package velocity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

public class BufferedSocketServerThread extends Thread
{
    private Socket socket;
    public Main plugin;
    public BufferedSocketServerThread(Socket socket, Main plugin)
    {
        this.socket = socket;
        this.plugin = plugin;
    }

    public void run()
    {
        try (
        		BufferedReader reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
        	)
        {
        	//String receivedMessage = reader.readLine();
        	StringBuilder receivedMessageBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null)
            {
                receivedMessageBuilder.append(line).append("\n");
            }
            
            String receivedMessage = receivedMessageBuilder.toString();
            this.plugin.resaction(receivedMessage);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                if (this.socket != null && !this.socket.isClosed())
                {
                	this.socket.close();
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }
}