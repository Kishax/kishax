package spigot;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.logging.Level;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton 
public class Database {

    private Connection conn;
	private Connection conn2;
    private final common.Main plugin;
    
    @Inject
    public Database(common.Main plugin) {
    	this.plugin = plugin;
    }
    
	public synchronized Connection getConnection(String customDatabase) throws SQLException, ClassNotFoundException {
        String host = plugin.getConfig().getString("MySQL.Host", "");
        int port = plugin.getConfig().getInt("MySQL.Port", 0);
        String user = plugin.getConfig().getString("MySQL.User", "");
        String password = plugin.getConfig().getString("MySQL.Password", "");
        if (customDatabase != null && !customDatabase.isEmpty()) {
            if ((host != null && host.isEmpty()) || 
                port == 0 || 
                (user != null && user.isEmpty()) || 
                (password != null && password.isEmpty())) {
                return null;
            }
            
            synchronized (Database.class) {
                //if (Objects.nonNull(conn2) && !conn2.isClosed()) return conn2;
                
                Class.forName("com.mysql.cj.jdbc.Driver");
                this.conn2 = DriverManager.getConnection (
                            "jdbc:mysql://" + host + ":" + 
                            port + "/" + 
                            customDatabase +
                            "?autoReconnect=true&useSSL=false", 
                            user, 
                            password
                );
				return conn2;
            }
        } else {
            String database = plugin.getConfig().getString("MySQL.Database", "");
            if ((host != null && host.isEmpty()) || 
                port == 0 || 
                (database != null && database.isEmpty()) || 
                (user != null && user.isEmpty()) || 
                (password != null && password.isEmpty())) {
                return null;
            }
            
            synchronized (Database.class) {
                //if (Objects.nonNull(conn) && !conn.isClosed()) return conn;
                
                Class.forName("com.mysql.cj.jdbc.Driver");
                this.conn = DriverManager.getConnection (
                            "jdbc:mysql://" + host + ":" + 
                            port + "/" + 
                            database +
                            "?autoReconnect=true&useSSL=false", 
                            user, 
                            password
                        );

                return conn;
            }
        }
    }
	
	public Connection getConnection() throws SQLException, ClassNotFoundException {
		return getConnection(null);
	}

	public void close_resource(ResultSet[] resultsets, Connection[] conns, PreparedStatement ps) {
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
		
		if (Objects.nonNull(conns)) {
			for (Connection simpleconn : conns) {
			    if (Objects.nonNull(simpleconn)) {
					try {
						simpleconn.close();
					} catch (SQLException e) {
						plugin.getLogger().log(Level.SEVERE, "A SQLException error occurred: {0}", e.getMessage());
						for (StackTraceElement element : e.getStackTrace()) {
							plugin.getLogger().severe(element.toString());
						}
					}
				}
			}
		}
		
		if (Objects.nonNull(ps)) {
			try {
                ps.close();
            } catch (SQLException e) {
				plugin.getLogger().log(Level.SEVERE, "A SQLException error occurred: {0}", e.getMessage());
				for (StackTraceElement element : e.getStackTrace()) {
					plugin.getLogger().severe(element.toString());
				}
            }
		}
	}

	public void close_resource(ResultSet[] resultsets, Connection conn, PreparedStatement ps) {
		Connection[] conns = {conn};
		close_resource(resultsets, conns, ps);
	}
}