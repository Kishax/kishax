package bungee;

import net.md_5.bungee.api.config.ServerInfo;
import com.google.common.io.ByteStreams;
import net.md_5.bungee.config.*;
import java.io.*;

public class Config
{
    public static Configuration config;
    private static File file;
    private Main plugin;
    public Config(String name, Main plugin)
    {
    	this.plugin = plugin;
        Config.file = new File(plugin.getDataFolder(), name);
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdir();
    
        if (!Config.file.exists())
        {
            try
            {
                Config.file.createNewFile();
                try (final InputStream is = plugin.getResourceAsStream(name);
                     final OutputStream os = new FileOutputStream(Config.file))
                {
                    // 既存のファイル内容を読み込む
                    String existingContent = new String(ByteStreams.toByteArray(is), "UTF-8");

                    String addcontents = "";
                    addcontents += "\n\nServers:";
                    addcontents += "\n    Hub: \"\"";
                    addcontents += "\n    Request_Path: \"\"";
                    addcontents += "\n    Memory_Limit: ";
                    addcontents += "\n\n    BungeeCord:";
                	addcontents += "\n        Memory: ";
                    for (ServerInfo serverInfo: this.plugin.getProxy().getServers().values())
                    {
                    	addcontents += "\n    "+serverInfo.getName().toString()+":";
                    	addcontents += "\n        Memory: ";
                    	addcontents += "\n        Bat_Path: \"\"";
                    }
                    // 新しい行を追加
                    String newContent = existingContent + addcontents;
                    
                    // 新しい内容をファイルに書き込む
                    ByteStreams.copy(new ByteArrayInputStream(newContent.getBytes("UTF-8")), os);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        
        try
        {
            Config.config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(Config.file);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    
    public static void save()
    {
        try
        {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(Config.config, Config.file);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    
    public static Configuration getConfig()
    {
        return Config.config;
    }
}