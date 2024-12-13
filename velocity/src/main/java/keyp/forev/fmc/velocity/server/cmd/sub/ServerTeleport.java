package keyp.forev.fmc.velocity.server.cmd.sub;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import keyp.forev.fmc.common.database.Database;
import keyp.forev.fmc.common.server.Luckperms;
import keyp.forev.fmc.velocity.server.DoServerOnline;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ServerTeleport implements SimpleCommand {
    private final Logger logger;
    private final ProxyServer server;
    private final Database db;
    private final Luckperms lp;
    private final DoServerOnline dso;
    private final String[] subcommands = {"stp"};
    @Inject
    public ServerTeleport(Logger logger, ProxyServer server, Database db, Luckperms lp, DoServerOnline dso) {
        this.logger = logger;
		this.server = server;
		this.db = db;
        this.lp = lp;
        this.dso = dso;
	}
    
    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (source instanceof Player player) {
            String[] args = invocation.arguments();
            if (args.length < 1) {
                source.sendMessage(Component.text("引数が足りません。").color(NamedTextColor.RED));
                return;
            }
            String targetServerName = args[0];
            if (targetServerName == null || targetServerName.isEmpty()) {
                player.sendMessage(Component.text("サーバー名を入力してください。").color(NamedTextColor.RED));
                return;
            }
            serverTeleport(player, targetServerName);
        } else {
            if (source != null) {
                source.sendMessage(Component.text("このコマンドはプレイヤーのみが実行できます。").color(NamedTextColor.RED));
            }
        }
    }

    public void execute2(Invocation invocation) {
        CommandSource source = invocation.source();
        if (source instanceof Player player) {
            String[] args = invocation.arguments();
            if (args.length < 2) {
                source.sendMessage(Component.text("引数が足りません。").color(NamedTextColor.RED));
                return;
            }
            String targetServerName = args[1];
            if (args.length == 1 || targetServerName == null || targetServerName.isEmpty()) {
                player.sendMessage(Component.text("サーバー名を入力してください。").color(NamedTextColor.RED));
                return;
            }
            serverTeleport(player, targetServerName);
        } else {
            if (source != null) {
                source.sendMessage(Component.text("このコマンドはプレイヤーのみが実行できます。").color(NamedTextColor.RED));
            }
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();
        List<String> ret = new ArrayList<>();
        switch (args.length) {
        	case 0, 1 -> {
                for (String subcmd : subcommands) {
                    if (!source.hasPermission("fmc.proxy." + subcmd)) continue;
                    for (String onlineServer : db.getServersList(true)) {
                        ret.add(onlineServer);
                    }
                }
                return ret;
            }
		}
		return Collections.emptyList();
	}

    private void serverTeleport(Player player, String targetServerName) {
        boolean containsServer = server.getAllServers().stream()
            .anyMatch(registeredServer -> registeredServer.getServerInfo().getName().equalsIgnoreCase(targetServerName));
        if (!containsServer) {
            player.sendMessage(Component.text("サーバー名が違います。").color(NamedTextColor.RED));
            return;
        }
        int permLevel = lp.getPermLevel(player.getUsername());
        if (permLevel < 1) {
            player.sendMessage(Component.text("権限がありません。").color(NamedTextColor.RED));
            return;
        }
        try (Connection conn = db.getConnection()) {
            Map<String, Map<String, Object>> statusMap = dso.loadStatusTable(conn);
            statusMap.entrySet().stream()
                .filter(entry -> entry.getKey() instanceof String && entry.getKey().equals(targetServerName))
                .forEach(entry -> {
                    Map<String, Object> serverInfo = entry.getValue();
                    if (permLevel < 2 && serverInfo.get("enter") instanceof Boolean enter && !enter) {
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
