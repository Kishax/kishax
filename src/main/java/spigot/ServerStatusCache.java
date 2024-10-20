package spigot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class ServerStatusCache {

    private static final long CACHE_REFRESH_INTERVAL = 60000;
    private final common.Main plugin;
    private final Database db;
    private final PortFinder pf;
    private final DoServerOnline dso;
    private final Provider<SocketSwitch> sswProvider;
    private final ServerHomeDir shd;
    private final AtomicBoolean isFirstRefreshing = new AtomicBoolean(false);
    private Map<String, Map<String, Map<String, String>>> statusMap = new ConcurrentHashMap<>();

    @Inject
    public ServerStatusCache(common.Main plugin, Database db, PortFinder pf, DoServerOnline dso, Provider<SocketSwitch> sswProvider, ServerHomeDir shd) {
        this.plugin = plugin;
        this.db = db;
        this.pf = pf;
        this.dso = dso;
        this.sswProvider = sswProvider;
        this.shd = shd;
    }

    public void serverStatusCache() {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                refreshCache();
            }
        }, 0, CACHE_REFRESH_INTERVAL);
    }
    
    private void refreshCache() {
        SocketSwitch ssw = sswProvider.get();
        Map<String, Map<String, Map<String, String>>> newServerStatusMap = new HashMap<>();
        try (Connection conn = db.getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM status;")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, String> rowMap = new HashMap<>();
                rowMap.put("id", String.valueOf(rs.getInt("id")));
                rowMap.put("name", rs.getString("name"));
                rowMap.put("port", String.valueOf(rs.getInt("port")));
                rowMap.put("online", String.valueOf(rs.getInt("online")));
                rowMap.put("player_list", rs.getString("player_list"));
                rowMap.put("current_players", rs.getString("current_players"));
                rowMap.put("exception", String.valueOf(rs.getInt("exception")));
                rowMap.put("exception2", String.valueOf(rs.getInt("exception2")));
                rowMap.put("type", rs.getString("type"));
                rowMap.put("socketport", String.valueOf(rs.getInt("socketport")));
                rowMap.put("platform", rs.getString("platform"));
                String serverType = rs.getString("type");
                newServerStatusMap.computeIfAbsent(serverType, k -> new HashMap<>()).put(rs.getString("name"), rowMap);
            }

            // サーバーネームをアルファベット順にソート
            Map<String, Map<String, Map<String, String>>> sortedServerStatusMap = new HashMap<>();
            for (Map.Entry<String, Map<String, Map<String, String>>> entry : newServerStatusMap.entrySet()) {
                String serverType = entry.getKey();
                Map<String, Map<String, String>> servers = entry.getValue();

                // サーバーネームをソート
                Map<String, Map<String, String>> sortedServers = servers.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                    ));

                sortedServerStatusMap.put(serverType, sortedServers);
            }

            this.statusMap = sortedServerStatusMap;
            // 初回ループのみ
            if (isFirstRefreshing.compareAndSet(false, true)) {
                plugin.getLogger().info("Server status cache has been initialized.");
                pf.findAvailablePortAsync(statusMap).thenAccept(port -> {
                    dso.UpdateDatabase(port);
                    ssw.startSocketServer(port);
                }).exceptionally(ex -> {
                    plugin.getLogger().log(Level.SEVERE, "ソケット利用可能ポートが見つからなかったため、サーバーをオンラインにできませんでした。", ex.getMessage());
                    for (StackTraceElement element : ex.getStackTrace()) {
                        plugin.getLogger().log(Level.SEVERE, element.toString());
                    }
                    return null;
                });
            }

        } catch (SQLException | ClassNotFoundException e) {
            this.statusMap = null;
            plugin.getLogger().log(Level.SEVERE, "An Exception error occurred: {0}", e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                plugin.getLogger().severe(element.toString());
            }
        }
    }

    public Map<String, Map<String, Map<String, String>>> getStatusMap() {
        return this.statusMap;
    }

    public void setStatusMap(Map<String, Map<String, Map<String, String>>> statusMap) {
        this.statusMap = statusMap;
    }

    public String getServerType(String serverName) {
        Map<String, Map<String, Map<String, String>>> serverStatusMap = getStatusMap();
        for (Map.Entry<String, Map<String, Map<String, String>>> serverEntry : serverStatusMap.entrySet()) {
            String serverType = serverEntry.getKey();
            Map<String, Map<String, String>> serverStatusList = serverEntry.getValue();
            for (Map.Entry<String, Map<String, String>> serverDataEntry : serverStatusList.entrySet()) {
                String name = serverDataEntry.getKey();
                if (name.equals(serverName)) {
                    return serverType;
                }
            }
        }
        return null;
    }
}