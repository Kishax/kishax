package spigot_command;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import spigot.Database;

@Singleton
public class ServerStatusCache {

    private static final long CACHE_REFRESH_INTERVAL = 60000;
    private final common.Main plugin;
    private final Database db;
    private Map<String, Map<String, Map<String, String>>> statusMap = new HashMap<>();

    @Inject
    public ServerStatusCache(common.Main plugin, Database db) {
        this.plugin = plugin;
        this.db = db;
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

                String serverType = rs.getString("type");
                newServerStatusMap.computeIfAbsent(serverType, k -> new HashMap<>()).put(rs.getString("name"), rowMap);
            }
            this.statusMap = newServerStatusMap;
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
}