package spigot;

import org.bukkit.configuration.file.FileConfiguration;

import spigot_command.FMCCommand;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;

public class Main
{
	public FileConfiguration config;
	public Connection conn = null;
	public PreparedStatement ps = null;
	public common.Main plugin;
	public SocketSwitch ssw;
	public static Main instance;
	
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
		
    	FileConfiguration config = plugin.getConfig();
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
        
	    plugin.getLogger().info("プラグインが有効になりました。");
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
    	
    	ssw.stopSocketServer();
    	
    	plugin.getLogger().info("Socket Server stopping...");
    	plugin.getLogger().info("プラグインが無効になりました。");
    }
}
