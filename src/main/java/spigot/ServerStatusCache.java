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
    private final AtomicBoolean isFirstRefreshing = new AtomicBoolean(false);
    private Map<String, Map<String, Map<String, Object>>> statusMap = new ConcurrentHashMap<>();
    private Map<String, Map<String, String>> memberMap = new ConcurrentHashMap<>();

    @Inject
    public ServerStatusCache(common.Main plugin, Database db, PortFinder pf, DoServerOnline dso, Provider<SocketSwitch> sswProvider) {
        this.plugin = plugin;
        this.db = db;
        this.pf = pf;
        this.dso = dso;
        this.sswProvider = sswProvider;
    }

    public void serverStatusCache() {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                refreshCache();
                refreshMemberInfo();
            }
        }, 0, CACHE_REFRESH_INTERVAL);
    }
    
    public void refreshCache() {
        SocketSwitch ssw = sswProvider.get();
        Map<String, Map<String, Map<String, Object>>> newServerStatusMap = new HashMap<>();
        String query = "SELECT * FROM status;";
        try (Connection conn = db.getConnection();
            PreparedStatement ps = conn.prepareStatement(query)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> rowMap = new HashMap<>();
                    String serverName = rs.getString("name"),
                        serverType = rs.getString("type");

                    int columnCount = rs.getMetaData().getColumnCount();
					for (int i = 1; i <= columnCount; i++) {
						String columnName = rs.getMetaData().getColumnName(i);
						if (!columnName.equals("name") || !columnName.equals("type")) {
							rowMap.put(columnName, rs.getObject(columnName));
						}
					}
                    newServerStatusMap.computeIfAbsent(serverType, k -> new HashMap<>()).put(serverName, rowMap);
                }
    
                // サーバーネームをアルファベット順にソート
                Map<String, Map<String, Map<String, Object>>> sortedServerStatusMap = new HashMap<>();
                for (Map.Entry<String, Map<String, Map<String, Object>>> entry : newServerStatusMap.entrySet()) {
                    String serverType = entry.getKey();
                    Map<String, Map<String, Object>> servers = entry.getValue();
    
                    // サーバーネームをソート
                    Map<String, Map<String, Object>> sortedServers = servers.entrySet().stream()
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
            }
        } catch (SQLException | ClassNotFoundException e) {
            this.statusMap = null;
            plugin.getLogger().log(Level.SEVERE, "An Exception error occurred: {0}", e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                plugin.getLogger().severe(element.toString());
            }
        }
    }

    public Map<String, Map<String, Map<String, Object>>> getStatusMap() {
        return this.statusMap;
    }

    public void setStatusMap(Map<String, Map<String, Map<String, Object>>> statusMap) {
        this.statusMap = statusMap;
    }

    public String getServerType(String serverName) {
        return getStatusMap().entrySet().stream()
            .filter(entry -> entry.getValue().containsKey(serverName))
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(null);
    }

    public void refreshMemberInfo() {
        try (Connection conn = db.getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM members;")) {
            try (ResultSet rs = ps.executeQuery()) {
                Map<String, Map<String, String>> newMemberMap = new HashMap<>();
                while (rs.next()) {
                    String uuid = rs.getString("uuid");
                    String name = rs.getString("name");
                    newMemberMap.put(name, new HashMap<String, String>() {{
                        put("uuid", uuid);
                    }});
                }
                this.memberMap = newMemberMap;
            }
        } catch (SQLException | ClassNotFoundException e) {
            plugin.getLogger().log(Level.SEVERE, "An Exception error occurred: {0}", e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                plugin.getLogger().severe(element.toString());
            }
        }
    }

    public Map<String, Map<String, String>> getMemberMap() {
        return this.memberMap;
    }
}