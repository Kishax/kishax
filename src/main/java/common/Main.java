package common;

import java.util.Objects;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin
{
    private static Main instance;
	
    @Override
    public void onEnable()
    {
        instance = this;

        try
        {
            if (isVelocity())
            {
            	// Velocityプラグインは自動的にインスタンス化されるので、ここでは何もしない
            }
            else if(isBungeeCord())
            {
            	new bungee.Main().onEnable();
            }
            else
            {
                new spigot.Main(this).onEnable();
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
            if (isVelocity())
            {
                //
            }
            else if(isBungeeCord())
            {
            	// Call BungeeCord-specific initialization
                new bungee.Main().onDisable();
            }
            else
            {
            	new spigot.Main(this).onDisable();
            }
        }
    	catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    
    private boolean isBungeeCord()
    {
        return Objects.nonNull(getClass().getClassLoader().getResource("net/md_5/bungee/api/plugin/Plugin.class"));
    }

    private boolean isVelocity()
    {
        return Objects.nonNull(getClass().getClassLoader().getResource("com/velocitypowered/api/plugin/Plugin.class"));
    }

    public static Main getInstance()
    {
        return instance;
    }
}
