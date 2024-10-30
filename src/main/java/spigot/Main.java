package spigot;

import java.util.TimeZone;
import java.util.logging.Level;

import org.bukkit.command.PluginCommand;

import com.google.inject.Guice;
import com.google.inject.Injector;

import net.luckperms.api.LuckPermsProvider;
import spigot_command.FMCCommand;

public class Main {
	private static Injector injector = null;
	public final common.Main plugin;
	
	public Main(common.Main plugin) {
		this.plugin = plugin;
	}
	
	public void onEnable() {
		// Guice インジェクターを作成
        injector = Guice.createInjector(new spigot.Module(plugin, this));
		plugin.getLogger().info("Detected Spigot platform.");
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));
		getInjector().getInstance(AutoShutdown.class).startCheckForPlayers();
	    plugin.saveDefaultConfig();
		getInjector().getInstance(PortalsConfig.class).createPortalsConfig();
    	plugin.getServer().getPluginManager().registerEvents(getInjector().getInstance(EventListener.class), plugin);
        plugin.getServer().getPluginManager().registerEvents(getInjector().getInstance(WandListener.class), plugin);
		FMCCommand commandFMC = getInjector().getInstance(FMCCommand.class);
		PluginCommand fmcCmd = plugin.getCommand("fmc");
		if (fmcCmd != null) {
			fmcCmd.setExecutor(commandFMC);
		}
    	if (plugin.getConfig().getBoolean("MCVC.Mode",false)) {
    		getInjector().getInstance(Rcon.class).startMCVC();
		}
		getInjector().getInstance(Luckperms.class).triggerNetworkSync();
		plugin.getLogger().info("luckpermsと連携しました。");
		plugin.getLogger().log(Level.INFO, LuckPermsProvider.get().getPlatform().toString());
		getInjector().getInstance(PlayerUtils.class).loadPlayers();
    	// DoServerOnlineとPortFinderとSocketの処理を統合
		getInjector().getInstance(ServerStatusCache.class).serverStatusCache();
    	plugin.getLogger().info("プラグインが有効になりました。");
    }
    
	public static Injector getInjector() {
        return injector;
    }
	
    public void onDisable() {
		try {
			getInjector().getInstance(DoServerOffline.class).UpdateDatabase();
		} catch (Exception e) {
			plugin.getLogger().log(Level.SEVERE, "An error occurred while updating the database: {0}", e.getMessage());
		}
    	if (plugin.getConfig().getBoolean("MCVC.Mode",false)) {
    		getInjector().getInstance(Rcon.class).stopMCVC();
		}
    	getInjector().getInstance(AutoShutdown.class).stopCheckForPlayers();
    	plugin.getLogger().info("Socket Server stopping...");
    	plugin.getLogger().info("プラグインが無効になりました。");
    }
}
