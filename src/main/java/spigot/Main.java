package spigot;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import spigot_command.FMCCommand;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

public class Main
{
	public FileConfiguration config;
	public Connection conn = null;
	public PreparedStatement ps = null;
	public common.Main plugin;
	public SocketSwitch ssw;
	public static Main instance;
	private volatile boolean isRconActive = false;
	private Thread rconMonitorThread;
	
	public Main(common.Main plugin)
	{
		this.plugin = plugin;
	}
	
	public void onEnable()
    {
		instance = this;
		
		plugin.getLogger().info("Detected Spigot platform.");
		
	    ssw = new SocketSwitch(plugin);
		
	    plugin.saveDefaultConfig();
		
    	this.config = plugin.getConfig();
    	new Config(config);
    	
    	plugin.getServer().getPluginManager().registerEvents(new EventListener(plugin,ssw), plugin);
        
    	plugin.getCommand("fmc").setExecutor(new FMCCommand(plugin));
        
        try
		{
        	// "plugins"ディレクトリの親ディレクトリを取得
            File dataFolder = plugin.getDataFolder();
            String grandDir = getParentDir(dataFolder);
            
			conn = Database.getConnection();
			
			if(Objects.nonNull(conn) && Objects.nonNull(grandDir))
			{
				// サーバーをオンラインに
				ssw.startSocketClient(grandDir+"サーバーが起動しました。");
				plugin.getLogger().info(grandDir+"サーバーが起動しました。");
				String sql = "UPDATE mine_status SET online=? WHERE name=?;";
				ps = conn.prepareStatement(sql);
				ps.setBoolean(1,true);
				ps.setString(2, grandDir);
				ps.executeUpdate();
				
				plugin.getLogger().info("MySQL Server is connected!");
				
				plugin.getLogger().info("プラグインが有効になりました。");
				
		        
				if(config.getBoolean("MCVC.Mode",false))
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
			}
			else plugin.getLogger().info("MySQL Server is canceled for config value not given");
		}
		catch (SQLException | ClassNotFoundException e)
		{
			e.printStackTrace();
		}
        finally
        {
        	Database.close_resorce(null, conn, ps);
        }
        
    }
    
	private void onRconActivated(String RCON_HOST, int RCON_PORT, String RCON_PASS)
	{
        // RCONが有効になった後の処理
        plugin.getLogger().info("RCON is active. Performing specific actions...");
        // ここにRCONが有効になった後の特定の処理を記述
        plugin.getLogger().info("RCON is active.");
        // RCONが有効な場合の処理
        if(!config.getString("MCVC.EXE_Path","").isEmpty())
		{
			// EXEファイルのパスを指定
	        String exeFilePath = config.getString("MCVC.EXE_Path");
	        String Host = config.getString("MCVC.Host","localhost");
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
            while (!isRconActive && !Thread.currentThread().isInterrupted())
            {
                if (checkRconRunning(RCON_HOST, RCON_PORT))
                {
                    isRconActive = true;
                    
                    onRconActivated(RCON_HOST, RCON_PORT, RCON_PASS);
                    plugin.getServer().getScheduler().runTask(plugin, () ->
                    {
                        // RCONが有効になった後の処理をメインスレッドで実行
                    	onRconActivated(RCON_HOST, RCON_PORT, RCON_PASS);
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
	
	private String getParentDir(File dataFolder)
	{
		// サーバーのホームディレクトリを取得
        File serverHomeDirectory = dataFolder.getParentFile();
        
        // ホームディレクトリ名を取得
        String homeDirectoryPath = serverHomeDirectory.getAbsolutePath();
        
        // Fileオブジェクトを作成
        File file = new File(homeDirectoryPath);
        
        // "plugins"ディレクトリの親ディレクトリを取得
        File parentDir = file.getParentFile();
        
        // 親ディレクトリが存在するか確認
        if (Objects.nonNull(parentDir))
        {
            // 親ディレクトリの名前を取得
        	return parentDir.getName();
        }
        return null;
	}
	
	public static Main getMaininstance()
	{
		return instance;
	}
	
	public SocketSwitch getSocket()
	{
		return ssw;
	}
	
    public void onDisable()
    {
    	try
		{
    		conn = Database.getConnection();
			// サーバーをオフラインに
			if(Objects.nonNull(conn))
			{
				String sql = "UPDATE mine_status SET "+Config.config.getString("Server")+"=? WHERE id=1;";
				ps = conn.prepareStatement(sql);
				ps.setBoolean(1,false);
				ps.executeUpdate();
			}
		}
		catch (SQLException | ClassNotFoundException e2)
		{
			e2.printStackTrace();
		}
    	
    	// プラグイン無効化時にスレッドを停止
        if (Objects.nonNull(rconMonitorThread) && rconMonitorThread.isAlive())
        {
            rconMonitorThread.interrupt();
        }
        
    	ssw.stopSocketServer();
    	
    	plugin.getLogger().info("Socket Server stopping...");
    	plugin.getLogger().info("プラグインが無効になりました。");
    }
}
