package spigot;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import com.google.inject.Inject;

public class Rcon
{
	private final common.Main plugin;
	private volatile boolean isRconActive = false;
	private Thread rconMonitorThread;
	
	@Inject
	public Rcon(common.Main plugin)
	{
		this.plugin = plugin;
	}
	
	public void startMCVC()
	{
		File propertiesFile = new File(plugin.getServer().getWorldContainer(), "server.properties");
        Properties properties = new Properties();

        try (FileInputStream fis = new FileInputStream(propertiesFile))
        {
            properties.load(fis);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return;
        }

        boolean isRconEnabled = Boolean.parseBoolean(properties.getProperty("enable-rcon", "false"));
        String rconPassword = properties.getProperty("rcon.password", "");
        int rconPort = Integer.parseInt(properties.getProperty("rcon.port", "0"));

        /*plugin.getLogger().info("RCON Enabled: " + isRconEnabled);
        plugin.getLogger().info("RCON Password: " + rconPassword);
        plugin.getLogger().info("RCON Port: " + rconPort);*/
        
        if(isRconEnabled)
        {
        	if(rconPassword.isEmpty() || rconPort == 0)
        	{
        		plugin.getLogger().info("Server.Properties.Rconの設定が不十分のため、MCVCを開始できません。");
        	}
        	else
        	{
        		// RCONの状態を監視するスレッドを開始
                rconMonitorThread = new Thread(() -> monitorRcon("localhost", rconPort, rconPassword));
                rconMonitorThread.start();
        	}
        }
        else
        {
        	plugin.getLogger().info("Server.Properties.Rconの設定が不十分のため、MCVCを開始できません。");
        }
	}
	
	private void onRconActivated(String RCON_HOST, int RCON_PORT, String RCON_PASS)
	{
        // RCONが有効になった後の処理
        plugin.getLogger().info("RCON is active. Performing specific actions...");
        // ここにRCONが有効になった後の特定の処理を記述
        plugin.getLogger().info("RCON is active.");
        // RCONが有効な場合の処理
        if(!plugin.getConfig().getString("MCVC.EXE_Path","").isEmpty())
		{
			// EXEファイルのパスを指定
	        String exeFilePath = plugin.getConfig().getString("MCVC.EXE_Path");
	        String Host = plugin.getConfig().getString("MCVC.Host","localhost");
			// コマンドを定義
			List<String> commands = Arrays.asList
				(
				    exeFilePath, Host, String.valueOf(RCON_PORT), RCON_PASS
				);
	        /*List<String> commands = Arrays.asList
	        	(
	        		exeFilePath, ":loop && "+exeFilePath+" " + Host + " " + String.valueOf(rconPort) + " " + rconPassword + " && timeout /t 5 && goto loop"
	        	);*/

	        // ProcessBuilderを作成
	        ProcessBuilder processBuilder = new ProcessBuilder(commands);

	        // 標準出力と標準エラーを現在のプロセスにリダイレクト
	        processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
	        processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
	        // プロセスを開始
	        try
	        {
				processBuilder.start();
			}
	        catch (IOException e)
	        {
				e.printStackTrace();
			}
		}
		else
		{
			plugin.getLogger().info("MCVCを有効にするには、mcvc.exeの絶対パスをconfigに書く必要があります。");
		}
    }
	
	private void monitorRcon(String RCON_HOST, int RCON_PORT, String RCON_PASS)
	{
	    try
	    {
	        while (!Thread.currentThread().isInterrupted())
	        {
	            if (!isRconActive && checkRconRunning(RCON_HOST, RCON_PORT))
	            {
	                isRconActive = true;
	                
	                plugin.getServer().getScheduler().runTask(plugin, () ->
	                {
	                	// RCONが有効になった後の処理をメインスレッドで実行
		            	if (isRconActive)
		            	{ // メインスレッドでチェック
		            		plugin.getLogger().info("Running onRconActivated method.");
		            		onRconActivated(RCON_HOST, RCON_PORT, RCON_PASS);
		                    isRconActive = false; // フラグをリセット
		                }
	                });
	                break;
	            }
	
		        // 一定の待ち時間（例えば5秒）を設ける
		        try
		        {
		            Thread.sleep(5000);
		        }
		        catch (InterruptedException e)
		        {
		            // スレッドが中断された場合は終了
		            Thread.currentThread().interrupt();
		        }
	        }
	    }
		finally
		{
		    // スレッド終了時の処理
		    plugin.getLogger().info("RCON monitor thread stopping.");
		}
	}
	  
	private boolean checkRconRunning(String host, int port)
	{
	    try (Socket socket = new Socket(host, port))
	    {
	        return true; // RCONが動作中
	    }
	    catch (IOException e)
	    {
	        return false; // RCONが動作していない
	    }
	}
	
	public void stopMCVC()
	{
		// プラグイン無効化時にスレッドを停止
        if (Objects.nonNull(rconMonitorThread) && rconMonitorThread.isAlive())
        {
            rconMonitorThread.interrupt();
        }
	}
}
