package velocity;

import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.io.InputStream;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class Config
{
	private Main plugin;
	private ProxyServer server;
    private static Config instance;
    private static Map<String, Object> config;
    private final Logger logger;
    private final Path dataDirectory;

    @Inject
    public Config(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory)
    {
    	this.plugin = Main.getInstance();
    	this.server = this.plugin.getServer();
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        instance = this;
    }

    public static Config getInstance()
    {
        return instance;
    }

    public void onProxyInitialization(ProxyInitializeEvent event)
    {
        try
        {
            loadConfig();
        }
        catch (IOException e)
        {
            logger.error("Error loading configuration", e);
        }
    }

    public void loadConfig() throws IOException
    {
        Path configPath = dataDirectory.resolve("velocity-config.yml");
        if (!Files.exists(configPath))
        {
            try (InputStream in = getClass().getResourceAsStream("/velocity-config.yml"))
            {
                Files.copy(in, configPath);
            }
        }

        // 読み込みと新規内容の追加
        String existingContent = Files.readString(configPath);
        String addContents = "\n\nServers:\n    Hub: \"\"\n    Request_Path: \"\"\n    Memory_Limit: ";
        addContents += "\n\n    Velocity:\n        Memory: ";
        
        // 例: サーバー名を追加する部分
        for (RegisteredServer server : this.server.getAllServers())
        {
        	addContents += "\n    "+server.getServerInfo().getName()+":";
        	addContents += "\n        Memory: ";
        	addContents += "\n        Bat_Path: \"\"";
        }
        
        // 新しい内容を追加してファイルに書き込み
        Files.writeString(configPath, existingContent + addContents);
        
        // Yamlでの読み込み
        Yaml yaml = new Yaml(new Constructor(Map.class, null));
        try 
        (
        		InputStream inputStream = Files.newInputStream(configPath)
        )
        {
            config = yaml.load(inputStream);
        }
    }

    public static Map<String, Object> getConfig()
    {
        return config;
    }

    public void saveConfig() throws IOException
    {
        Path configPath = dataDirectory.resolve("velocity-config.yml");
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);
        try
        (
        	FileWriter writer = new FileWriter(configPath.toFile())
        )
        {
            yaml.dump(config, writer);
        }
    }
    
    @SuppressWarnings("unchecked")
    public List<String> getStringList(String key) {
        Object value = config.get(key);
        if (value instanceof List) {
            return (List<String>) value;
        } else {
            throw new IllegalArgumentException("The value for the key '" + key + "' is not a list.");
        }
    }
}
