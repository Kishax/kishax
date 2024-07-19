package spigot;

import org.bukkit.configuration.file.FileConfiguration;

public class Config
{
    public static FileConfiguration config;
    
    public Config(FileConfiguration config)
    {
    	Config.config = config;
    }
}
