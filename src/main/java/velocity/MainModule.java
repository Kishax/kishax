package velocity;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.velocitypowered.api.proxy.ProxyServer;

import net.luckperms.api.LuckPerms;

import org.slf4j.Logger;
import com.velocitypowered.api.plugin.annotation.DataDirectory;

import java.io.IOException;
import java.nio.file.Path;

public class MainModule extends AbstractModule
{
	private final Main plugin;
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    //private final Config config;
    private final Luckperms lp;
    
    public MainModule(Main plugin, ProxyServer server, Logger logger, Path dataDirectory, /*Config config,*/ Luckperms lpinstance)
    {
    	this.plugin = plugin;
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        //this.config = config;
        this.lp = lpinstance;
    }

    @Override
    protected void configure()
    {
    	bind(Main.class).toInstance(plugin);
        bind(ProxyServer.class).toInstance(server);
        bind(Logger.class).toInstance(logger);
        bind(Path.class).annotatedWith(DataDirectory.class).toInstance(dataDirectory);
        
        // Config インスタンスの作成とバインド
        Config config = new Config(server,logger,dataDirectory);
        try
        {
            config.loadConfig();  // 一度だけロードする
        }
        catch (IOException e)
        {
            logger.error("Error loading config", e);
        }
        bind(Config.class).toInstance(config);
        
        bind(Luckperms.class).toInstance(lp);
        bind(DatabaseInterface.class).to(Database.class);
        bind(PlayerList.class); // Guice に PlayerList のインスタンス化を任せる
    }
}