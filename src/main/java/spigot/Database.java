package spigot;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.logging.Level;

import com.google.inject.Inject;

public class Database {

    public static Connection conn;
    private final common.Main plugin;
    
    @Inject
    public Database(common.Main plugin) {
    	this.plugin = plugin;
    }
    
	public Connection getConnection() throws SQLException, ClassNotFoundException {
		String host = plugin.getConfig().getString("MySQL.Host", "");
		int port = plugin.getConfig().getInt("MySQL.Port", 0);
		String database = plugin.getConfig().getString("MySQL.Database", "");
		String user = plugin.getConfig().getString("MySQL.User", "");
		String password = plugin.getConfig().getString("MySQL.Password", "");
		if (
			(host != null && host.isEmpty()) || 
			port == 0 || 
			(database != null && database.isEmpty()) || 
			(user != null && user.isEmpty()) || 
			(password != null && password.isEmpty())
		) {
			return null;
		}
		
        if (Objects.nonNull(conn) && !conn.isClosed()) return conn;
        
        synchronized (Database.class) {
            if (Objects.nonNull(conn) && !conn.isClosed()) return conn;
            
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database, user, password);
            return conn;
        }
    }
	
	public void close_resorce(ResultSet[] resultsets,Connection conn, PreparedStatement ps) {
		if (Objects.nonNull(resultsets)) {
			for (ResultSet resultSet : resultsets) {
			    if (Objects.nonNull(resultSet)) {
			    	try {
	                    resultSet.close();
	                } catch (SQLException e) {
	                    plugin.getLogger().log(Level.SEVERE, "A SQLException error occurred: {0}", e.getMessage());
						for (StackTraceElement element : e.getStackTrace()) {
							plugin.getLogger().severe(element.toString());
						}
	                }
			    }
			}
		}
		
		if (Objects.nonNull(conn)) {
			try {
                ps.close();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "A SQLException error occurred: {0}", e.getMessage());
				for (StackTraceElement element : e.getStackTrace()) {
					plugin.getLogger().severe(element.toString());
				}
            }
		}
		
		if (Objects.nonNull(ps)) {
			try {
                conn.close();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "A SQLException error occurred: {0}", e.getMessage());
				for (StackTraceElement element : e.getStackTrace()) {
					plugin.getLogger().severe(element.toString());
				}
            }
		}
	}
}