package velocity_command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import velocity.Config;
import velocity.Main;

import com.velocitypowered.api.proxy.Player;


public class Hub implements SimpleCommand
{
	private final Main plugin;
	private final ProxyServer server;
	
    public Hub()
    {
    	this.plugin = Main.getInstance();
    	this.server = this.plugin.getServer();
    }

	@Override
	public void execute(Invocation invocation)
	{
		CommandSource source = invocation.source();
        
        if(((String) Config.getConfig().getOrDefault("Servers.Hub","")).isEmpty())
        {
        	source.sendMessage(Component.text("コンフィグで設定されていません。").color(NamedTextColor.RED));
        	return;
        }
        
        if (!(source instanceof Player))
        {
            source.sendMessage(Component.text("このコマンドはプレイヤーのみが実行できます。").color(NamedTextColor.RED));
            return;
        }

        Player player = (Player) source;
        // Implement the logic to send the player to the hub server
        player.sendMessage(Component.text("Sending you to the hub..."));
        this.server.getServer(((String) Config.getConfig().get("Servers.Hub"))).ifPresent(server -> player.createConnectionRequest(server).fireAndForget());
	}
}
