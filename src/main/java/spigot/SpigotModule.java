package spigot;


import com.google.inject.AbstractModule;

public class SpigotModule extends AbstractModule
{
	private final common.Main plugin;
	private final Main main;
	
	public SpigotModule(common.Main plugin, Main main)
    {
		this.plugin = plugin;
		this.main = main;
    }
	
	@Override
    protected void configure()
    {
		bind(common.Main.class).toInstance(plugin);
		bind(Main.class).toInstance(main);
		bind(SocketSwitch.class);
		bind(Database.class);
		bind(ServerHomeDir.class);
		bind(DoServerOnline.class);
		bind(DoServerOffline.class);
    }
}
