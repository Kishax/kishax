package common;

import java.util.Objects;
import java.util.logging.Level;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin
{
    private static Main plugin;
	
    @Override
    public void onEnable()
    {
        plugin = this;

        try
        {
            if (isVelocity())
            {
            	// Velocityプラグインは自動的にインスタンス化されるので、ここでは何もしない
            }
            /*else if(isBungeeCord())
            {
            	new bungee.Main().onEnable();
            }*/
            else
            {
                new spigot.Main(this).onEnable();
            }
        }
        catch (Exception e)
        {
            plugin.getLogger().log(Level.SEVERE, "An Exception error occurred: {0}", e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) 
            {
                plugin.getLogger().severe(element.toString());
            }
        }
    }

    @Override
    public void onDisable()
    {
    	try
        {
            if (isVelocity())
            {
                //
            }
            /*else if(isBungeeCord())
            {
                new bungee.Main().onDisable();
            }*/
            else
            {
            	new spigot.Main(this).onDisable();
            }
        }
    	catch (Exception e)
        {
            plugin.getLogger().log(Level.SEVERE, "An Exception error occurred: {0}", e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) 
            {
                plugin.getLogger().severe(element.toString());
            }
        }
    }
    
    /*private boolean isBungeeCord()
    {
        return Objects.nonNull(getClass().getClassLoader().getResource("net/md_5/bungee/api/plugin/Plugin.class"));
    }*/

    private boolean isVelocity()
    {
        return Objects.nonNull(getClass().getClassLoader().getResource("com/velocitypowered/api/plugin/Plugin.class"));
    }

    public static Main getInstance()
    {
        return plugin;
    }
}
