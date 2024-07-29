package velocity;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;

import net.luckperms.api.LuckPermsProvider;
import velocity_command.FMCCommand;
import velocity_command.Hub;

import java.nio.file.Path;

import org.slf4j.Logger;

public class Main
{
	private static Injector injector = null;
	
	private final ProxyServer server;
	private final Logger logger;
	private final Path dataDirectory;
	// Guice注入後、取得するインスタンス(フィールド)郡
	public SocketSwitch ssw = null;
	private Luckperms lp = null;
	private DoServerOnline doOnline = null;
	
    @Inject
    public Main(ProxyServer serverinstance, Logger logger, @DataDirectory Path dataDirectory)
    {
        this.server = serverinstance;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        
        // Guiceに依存性を自動で解決させ、インスタンスを生成してもらう。
        // Guice インジェクターの作成は onProxyInitialization メソッドで行う
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent e)
    {
    	logger.info("Detected Velocity platform.");
    	
        // Guice インジェクターを作成
        injector = Guice.createInjector(new MainModule(this, server, logger, dataDirectory, LuckPermsProvider.get()));
        
        // 依存性が解決された@Injectを使用するクラスのインスタンスを取得
        lp = getInjector().getInstance(Luckperms.class);
    	ssw = getInjector().getInstance(SocketSwitch.class);
    	doOnline = getInjector().getInstance(DoServerOnline.class);
    			
    	doOnline.UpdateDatabase();
    	
    	server.getEventManager().register(this, getInjector().getInstance(EventListener.class));
    	
 		lp.triggerNetworkSync();
 		logger.info("luckpermsと連携しました。");
 		
 		getInjector().getInstance(PlayerList.class).loadPlayers(); // プレイヤーリストをアップデート
    	
    	CommandManager commandManager = server.getCommandManager();
        commandManager.register("fmcp", getInjector().getInstance(FMCCommand.class));
        commandManager.register("hub", getInjector().getInstance(Hub.class));
        
		// Client side
	    ssw.startSocketClient("Hello!\nStart Server!!");
	    // Server side
	    ssw.startSocketServer();
	    ssw.startBufferedSocketServer();
	    
	    logger.info("プラグインが有効になりました。");
    }
    
    public static Injector getInjector()
    {
        return injector;
    }
    
    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent e)
    {
    	ssw.stopSocketClient();
		logger.info( "Client Socket Stopping..." );
		ssw.stopSocketServer();
		ssw.stopBufferedSocketServer();
    	logger.info("Socket Server stopping...");
    	logger.info("Buffered Socket Server stopping...");
		logger.info( "プラグインが無効になりました。" );
    }
}
