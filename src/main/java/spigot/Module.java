package spigot;


import com.google.inject.AbstractModule;

import spigot_command.Menu;
import spigot_command.PortalsDelete;
import spigot_command.ReloadConfig;

public class Module extends AbstractModule {
	private final common.Main plugin;
	private final Main main;
	
	public Module(common.Main plugin, Main main) {
		this.plugin = plugin;
		this.main = main;
    }
	
	@Override
    protected void configure() {
		bind(common.Main.class).toInstance(plugin);
		bind(Main.class).toInstance(main);
		bind(SocketSwitch.class);
		bind(Database.class);
		//bind(Database.class).in(com.google.inject.Scopes.SINGLETON);
		bind(ServerHomeDir.class);
		bind(DoServerOnline.class);
		bind(DoServerOffline.class);
		bind(PortalsConfig.class).in(com.google.inject.Scopes.SINGLETON);
		bind(Menu.class);
		bind(PortalsDelete.class);
		bind(EventListener.class);
		bind(WandListener.class);
		bind(ReloadConfig.class);
		bind(ServerStatusCache.class).in(com.google.inject.Scopes.SINGLETON);
		bind(PortFinder.class);
		bind(PlayerUtils.class);
		bind(AutoShutdown.class);
		bind(Rcon.class);
		bind(Luckperms.class);
		bind(ImageMap.class);
    }
}
