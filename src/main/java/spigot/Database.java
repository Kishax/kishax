package spigot;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;

import com.google.inject.Inject;

public class Database {
    private final common.Main plugin;
    
    @Inject
    public Database(common.Main plugin) {
    	this.plugin = plugin;
    }
    
    public synchronized Connection getConnection() throws SQLException, ClassNotFoundException {
		return getConnection(null);
	}
    
	public synchronized Connection getConnection(String customDatabase) throws SQLException, ClassNotFoundException {
        String host = plugin.getConfig().getString("MySQL.Host", "");
        int port = plugin.getConfig().getInt("MySQL.Port", 0);
        String user = plugin.getConfig().getString("MySQL.User", "");
        String password = plugin.getConfig().getString("MySQL.Password", "");
        String database = customDatabase != null && !customDatabase.isEmpty() ? customDatabase : plugin.getConfig().getString("MySQL.Database", "");
        if ((host != null && host.isEmpty()) || 
            port == 0 || 
            (database != null && database.isEmpty()) || 
            (user != null && user.isEmpty()) || 
            (password != null && password.isEmpty())) {
            return null;
        }
        try {
            synchronized (Database.class) {
                //if (Objects.nonNull(conn2) && !conn2.isClosed()) return conn2;
                Class.forName("com.mysql.cj.jdbc.Driver");
                return DriverManager.getConnection ("jdbc:mysql://" + host + ":" + port + "/" + database +"?autoReconnect=true&useSSL=false", user, password);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to establish database connection.", e);
            for (StackTraceElement element : e.getStackTrace()) {
                plugin.getLogger().severe(element.toString());
            }
            throw e;
        }
    }

	private int countPlaceholders(String query) {
        int count = 0;
        for (char c : query.toCharArray()) {
            if (c == '?') {
                count++;
            }
        }
        return count;
    }

    public void insertLog(String query, Object[] args) throws SQLException, ClassNotFoundException {
        insertLog(null, query, args);
    }

    public void insertLog(Connection conn, String query, Object[] args) throws SQLException, ClassNotFoundException {
        Connection connection = (conn != null && !conn.isClosed()) ? conn : getConnection();
        if (args.length < countPlaceholders(query)) return;
        PreparedStatement ps = connection.prepareStatement(query);
        setPreparedStatementValues(ps, args);
        ps.executeUpdate();
    }
    
    public void updateLog(Connection conn, String query, Object[] args) throws SQLException, ClassNotFoundException {
        insertLog(conn, query, args);
    }

    private void setPreparedStatementValues(PreparedStatement ps, Object[] args) throws SQLException {
        for (int i = 0; i < args.length; i++) {
            ps.setObject(i + 1, args[i]);
        }
    }
}