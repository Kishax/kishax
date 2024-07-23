package common;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import spigot.Config;
import spigot.Database;
import spigot.EventListener;
import spigot.SocketSwitch;
import spigot_command.FMCCommand;

import com.velocitypowered.api.proxy.ProxyServer;

public class Main extends JavaPlugin
{

    private static Main instance;
    private Connection conn = null;
    private PreparedStatement ps = null;
    private SocketSwitch ssw;
	
    @Override
    public void onEnable()
    {
        instance = this;

        try
        {
            if (isBungeeCord())
            {
                new bungee.Main().onEnable();
            }
            else if(isVelocity())
            {
            	new velocity.Main((ProxyServer) this).onProxyInitialization(null);
            }
            else
            {
                new spigot.Main(this);
        		getLogger().info("Detected Spigot platform.");
        		
        	    ssw = new SocketSwitch(this);
        		
        		saveDefaultConfig();
        		
            	FileConfiguration config = getConfig();
            	new Config(config);
            	
            	getServer().getPluginManager().registerEvents(new EventListener(this,ssw), this);
                
            	getCommand("fmc").setExecutor(new FMCCommand(this));
                
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
        				
        				getLogger().info("MySQL Server is connected!");
        			}
        			else getLogger().info("MySQL Server is canceled for config value not given");
        		}
        		catch (SQLException e)
        		{
        			e.printStackTrace();
        		}
                finally
                {
                	Database.close_resorce(null, conn, ps);
                }
                
                getLogger().info("プラグインが有効になりました。");
                
                //startSocketServer();
                
        	    ssw.startSocketClient(ChatColor.GREEN+Config.config.getString("Server")+"サーバーが起動しました。");
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable()
    {
    	try
        {
            if (isBungeeCord())
            {
                // Call BungeeCord-specific initialization
                new bungee.Main().onDisable();
            }
            else if(isVelocity())
            {
            	System.out.println("Velocity plugin has been disabled.");
            }
            else
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
            	
            	getLogger().info("Socket Server stopping...");
            	getLogger().info("プラグインが無効になりました。");
            }
        }
    	catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private boolean isBungeeCord()
    {
        try
        {
            Class.forName("net.md_5.bungee.api.plugin.Plugin");
            return true;
        }
        catch (ClassNotFoundException e)
        {
            return false;
        }
    }
    
    private boolean isVelocity() {
        try {
            Class.forName("com.velocitypowered.api.plugin.Plugin");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static Main getInstance()
    {
        return instance;
    }
}
