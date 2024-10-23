package velocity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class DoServerOffline {
    private final Logger logger;
    private final DatabaseInterface db;
    private final Provider<SocketSwitch> sswProvider;
    @Inject
    public DoServerOffline(Logger logger, DatabaseInterface db, Provider<SocketSwitch> sswProvider) {
        this.logger = logger;
        this.db = db;
        this.sswProvider = sswProvider;
    }

    public void updateDatabase() {
        String query = "UPDATE status SET online=? WHERE name=?;";
        try (Connection conn = db.getConnection();
            PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setBoolean(1,false);
            ps.setString(2, "proxy");
            int rsAffected = ps.executeUpdate();
            if (rsAffected > 0) {
                String query2 = "UPDATE status SET player_list=?, current_players=?;";
                try (PreparedStatement ps2 = conn.prepareStatement(query2)) {
                    ps2.setString(1, null);
                    ps2.setInt(2, 0);
                    int rsAffected2 = ps2.executeUpdate();
                    if (rsAffected2 > 0) {
                        SocketSwitch ssw = sswProvider.get();
                        ssw.sendSpigotServer("MineStatusSync");
                    }
                }
            }
        } catch (SQLException | ClassNotFoundException e2) {
            logger.error("A SQLException | ClassNotFoundException error occurred: " + e2.getMessage());
            for (StackTraceElement element : e2.getStackTrace()) {
                logger.error(element.toString());
            }
        }
    }
}
