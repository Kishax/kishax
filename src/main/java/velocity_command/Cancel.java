package velocity_command;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import velocity.Config;
import velocity.Main;

public class Cancel
{
	private final Main plugin;
	private final ProxyServer server;
	private final Config config;
	
	@Inject
	public Cancel(Main plugin,ProxyServer server, Config config)
	{
		this.plugin = plugin;
		this.server = server;
		this.config = config;
	}
	
    public void execute(CommandSource source,String[] args)
    {
        source.sendMessage(Component.text("キャンセルしました。").color(NamedTextColor.WHITE));
        return;
    }
}
