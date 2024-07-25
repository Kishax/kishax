package spigot;

import org.bukkit.configuration.file.FileConfiguration;

import net.md_5.bungee.api.ChatColor;
import spigot_command.FMCCommand;

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
	
	public Main(common.Main plugin)
	{
		this.plugin = plugin;
	}
	
	public void onEnable()
    {
		this.plugin.getLogger().info("Detected Spigot platform.");
		
	    ssw = new SocketSwitch(this.plugin);
		
	    this.plugin.saveDefaultConfig();
		
    	FileConfiguration config = this.plugin.getConfig();
    	new Config(config);
    	
    	this.plugin.getServer().getPluginManager().registerEvents(new EventListener(this.plugin,ssw), this.plugin);
        
    	this.plugin.getCommand("fmc").setExecutor(new FMCCommand(this.plugin));
        
        try
		{
			conn = Database.getConnection();
			// サーバーをオンラインに
			if(Objects.nonNull(conn))
			{
				// サーバーをオンラインに
				String sql = "UPDATE mine_status SET "+Config.config.getString("Server")+"=? WHERE id=1;";
				ps = conn.prepareStatement(sql);
				ps.setBoolean(1,true);
				ps.executeUpdate();
				
				this.plugin.getLogger().info("MySQL Server is connected!");
			}
			else this.plugin.getLogger().info("MySQL Server is canceled for config value not given");
		}
		catch (SQLException | ClassNotFoundException e)
		{
			e.printStackTrace();
		}
        finally
        {
        	Database.close_resorce(null, conn, ps);
        }
        
        //startSocketServer();
        
	    ssw.startSocketClient(ChatColor.GREEN+Config.config.getString("Server")+"サーバーが起動しました。");
	    
	    this.plugin.getLogger().info("プラグインが有効になりました。");
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
    	
    	this.plugin.getLogger().info("Socket Server stopping...");
    	this.plugin.getLogger().info("プラグインが無効になりました。");
    }
}
