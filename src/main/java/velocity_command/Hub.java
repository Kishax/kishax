package velocity_command;

import java.util.Objects;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import velocity.Config;


public class Hub implements SimpleCommand {

	private final ProxyServer server;
	private final Config config;
	
	@Inject
    public Hub(ProxyServer server, Config config) {
		this.server = server;
		this.config = config;
	}

	@Override
	public void execute(Invocation invocation) {
		CommandSource source = invocation.source();
        
        if (config.getString("Servers.Hub","").isEmpty()) {
        	source.sendMessage(Component.text("コンフィグで設定されていません。").color(NamedTextColor.RED));
        	return;
        }
        
        if (!(source instanceof Player)) {
			Objects.requireNonNull(source);
            source.sendMessage(Component.text("このコマンドはプレイヤーのみが実行できます。").color(NamedTextColor.RED));
            return;
        }

        Player player = (Player) source;
        // Implement the logic to send the player to the hub server
        player.sendMessage(Component.text("Sending you to the hub..."));
        this.server.getServer(config.getString("Servers.Hub")).ifPresent(presentServer -> player.createConnectionRequest(presentServer).fireAndForget());
	}
}
