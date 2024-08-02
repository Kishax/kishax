package spigot;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

import spigot_command.FMCCommand;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class Main
{
	private static Injector injector = null;
	
	public Connection conn = null;
	public PreparedStatement ps = null;
	public static Main instance;
	public final common.Main plugin;
	private Database db = null;
	private SocketSwitch ssw = null; 
	private AutoShutdown as = null;
	private Rcon rcon = null;
	
	public Main(common.Main plugin)
	{
		this.plugin = plugin;
	}
	
	public void onEnable()
    {
		instance = this;
		
		// Guice インジェクターを作成
        injector = Guice.createInjector(new SpigotModule(plugin, this));
        
		plugin.getLogger().info("Detected Spigot platform.");
		
		this.ssw = getInjector().getInstance(SocketSwitch.class);
		this.db = getInjector().getInstance(Database.class);
		this.as = getInjector().getInstance(AutoShutdown.class);
		this.rcon = getInjector().getInstance(Rcon.class);
		
		as.startCheckForPlayers();
		
	    plugin.saveDefaultConfig();
		
    	plugin.getServer().getPluginManager().registerEvents(getInjector().getInstance(EventListener.class), plugin);
        
    	plugin.getCommand("fmc").setExecutor(getInjector().getInstance(FMCCommand.class));
        
    	if(plugin.getConfig().getBoolean("MCVC.Mode",false))
		{
			rcon.startMCVC();
		}
    	
    	getInjector().getInstance(DoServerOnline.class).UpdateDatabase();
    	
    	plugin.getLogger().info("プラグインが有効になりました。");
    }
    
	public static Injector getInjector()
    {
        return injector;
    }
	
    public void onDisable()
    {
    	if(plugin.getConfig().getBoolean("MCVC.Mode",false))
		{
    		rcon.stopMCVC();
		}
        
        as.stopCheckForPlayers();
        
    	ssw.stopSocketServer();
    	
    	plugin.getLogger().info("Socket Server stopping...");
    	
    	getInjector().getInstance(DoServerOffline.class).UpdateDatabase();
    	
    	plugin.getLogger().info("プラグインが無効になりました。");
    }
}
