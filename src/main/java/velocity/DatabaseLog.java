package velocity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.slf4j.Logger;

import com.google.inject.Inject;

public class DatabaseLog {
    private final Logger logger;
    private final DatabaseInterface db;
    @Inject
    public DatabaseLog(Logger logger, DatabaseInterface db) {
        this.logger = logger;
        this.db = db;
    }

    private void setPreparedStatementValues(PreparedStatement ps, Object[] args) throws SQLException {
        for (int i = 0; i < args.length; i++) {
            ps.setObject(i + 1, args[i]);
        }
    }

    public void insertLog(String query, Object[] args) {
        insertLog(null, query, args);
    }

    public void insertLog(Connection conn, String query, Object[] args) {
        try (Connection connection = (conn != null) ? conn : db.getConnection()) {
            if (args.length < db.countPlaceholders(query)) return;
            PreparedStatement ps = connection.prepareStatement(query);
            setPreparedStatementValues(ps, args);
            ps.executeUpdate();
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("A SQLException | ClassNotFoundException error occurred: " + e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                logger.error(element.toString());
            }
        }
    }
}
