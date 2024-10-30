package velocity_command;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import velocity.Config;
import velocity.DatabaseInterface;
import velocity.DoServerOnline;
import velocity.Luckperms;

public class ServerTeleport {
    private final Logger logger;
    private final ProxyServer server;
    private final DatabaseInterface db;
    private final Luckperms lp;
    private final DoServerOnline dso;
    @Inject
    public ServerTeleport(Logger logger, ProxyServer server, Config config, DatabaseInterface db, Luckperms lp, DoServerOnline dso) {
        this.logger = logger;
		this.server = server;
		this.db = db;
        this.lp = lp;
        this.dso = dso;
	}
    
    public void execute(@NotNull CommandSource source,String[] args) {
        if (args.length < 2) {
			source.sendMessage(Component.text("引数が足りません。").color(NamedTextColor.RED));
			return;
		}
        String targetServerName = args[1];
        if (!(source instanceof Player)) {
            source.sendMessage(Component.text("このコマンドはプレイヤーのみが実行できます。").color(NamedTextColor.RED));
            return;
        }
        Player player = (Player) source;
        String playerName = player.getUsername();
        if (args.length == 1 || targetServerName == null || args[1].isEmpty()) {
            player.sendMessage(Component.text("サーバー名を入力してください。").color(NamedTextColor.RED));
            return;
        }
		boolean containsServer = server.getAllServers().stream()
			.anyMatch(registeredServer -> registeredServer.getServerInfo().getName().equalsIgnoreCase(targetServerName));
		if (!containsServer) {
            player.sendMessage(Component.text("サーバー名が違います。").color(NamedTextColor.RED));
            return;
        }
        int permLevel = lp.getPermLevel(playerName);
		if (permLevel < 1) {
			player.sendMessage(Component.text("権限がありません。").color(NamedTextColor.RED));
			return;
		}
        try (Connection conn = db.getConnection()) {
            Map<String, Map<String, Object>> statusMap = dso.loadStatusTable(conn);
			statusMap.entrySet().stream()
				.filter(entry -> entry.getKey() instanceof String serverName && serverName.equals(targetServerName))
				.forEach(entry -> {
					Map<String, Object> serverInfo = entry.getValue();
					if (serverInfo.get("enter") instanceof Boolean enter && !enter) {
						player.sendMessage(Component.text("許可されていません。").color(NamedTextColor.RED));
						return;
					}
					if (serverInfo.get("online") instanceof Boolean online && !online) {
						player.sendMessage(Component.text(targetServerName+"サーバーは現在オフラインです。").color(NamedTextColor.RED));
						logger.info(targetServerName+"サーバーは現在オフラインです。");
						return;
					}
                    server.getServer(targetServerName).ifPresent(registeredServer -> player.createConnectionRequest(registeredServer).fireAndForget());
                });
        } catch (SQLException | ClassNotFoundException e) {
			logger.error("A SQLException | ClassNotFoundException error occurred: " + e.getMessage());
			for (StackTraceElement element : e.getStackTrace()) {
				logger.error(element.toString());
			}
		}
    }
}
