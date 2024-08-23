package velocity_command;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import velocity.Config;
import velocity.DatabaseInterface;
import velocity.Main;

public class ServerTeleport
{
    private final ProxyServer server;
    private final DatabaseInterface db;
    
    public Connection conn = null;
    public ResultSet minecrafts = null;
    public ResultSet[] resultsets = {minecrafts};
    public PreparedStatement ps = null;
    
    @Inject
    public ServerTeleport(Main plugin,ProxyServer server, Config config, DatabaseInterface db)
	{
		this.server = server;
		this.db = db;
	}
    
    public void execute(@NotNull CommandSource source,String[] args)
    {
        if (!(source instanceof Player))
        {
            source.sendMessage(Component.text("このコマンドはプレイヤーのみが実行できます。").color(NamedTextColor.RED));
            return;
        }

        Player player = (Player) source;

        if (args.length == 1 || Objects.isNull(args[1]) || args[1].isEmpty())
        {
            player.sendMessage(Component.text("サーバー名を入力してください。").color(NamedTextColor.RED));
            return;
        }

        String targetServerName = args[1];
        boolean containsServer = false;
        for (RegisteredServer server : server.getAllServers())
        {
            if (server.getServerInfo().getName().equalsIgnoreCase(targetServerName))
            {
                containsServer = true;
                break;
            }
        }

        if (!containsServer)
        {
            player.sendMessage(Component.text("サーバー名が違います。").color(NamedTextColor.RED));
            return;
        }

        try
        {
            conn = db.getConnection();
            String sql = "SELECT * FROM minecraft WHERE uuid=?;";
            ps = conn.prepareStatement(sql);
            ps.setString(1, player.getUniqueId().toString());
            minecrafts = ps.executeQuery();
            if (minecrafts.next())
            {
                long nowTimestamp = Instant.now().getEpochSecond();
                Timestamp sstTimeGet = minecrafts.getTimestamp("sst");
                long sstTimestamp = sstTimeGet.getTime() / 1000L;
                long ssSa = nowTimestamp - sstTimestamp;
                long ssSaMinute = ssSa / 60;
                if (ssSaMinute > 3)
                {
                    player.sendMessage(Component.text("セッションが無効です。").color(NamedTextColor.RED));
                    return;
                }
            }
            else
            {
                player.sendMessage(Component.text("このサーバーは、データベースに登録されていません。").color(NamedTextColor.RED));
                return;
            }
        }
        catch (SQLException | ClassNotFoundException e)
        {
            e.printStackTrace();
        }
        finally
        {
            db.close_resorce(resultsets, conn, ps);
        }

        server.getServer(targetServerName).ifPresent(server -> player.createConnectionRequest(server).fireAndForget());
    }
}
