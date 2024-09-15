package velocity;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.ProxyServer;

public class Database implements DatabaseInterface {

	private final Config config;
	private final Logger logger;
    private Connection conn = null;
    public ResultSet mine_status = null;
	public ResultSet[] resultsets = {mine_status};
    
    @Inject
    public Database (
		Main plugin, ProxyServer server, Logger logger, 
		Config config, ConsoleCommandSource console
	) {
		this.logger = logger;
    	this.config = config;
    }

    @Override
	public Connection getConnection() throws SQLException, ClassNotFoundException {
		String host = config.getString("MySQL.Host", "");
		int port = config.getInt("MySQL.Port", 0);
		String database = config.getString("MySQL.Database", "");
		String user = config.getString("MySQL.User", "");
		String password = config.getString("MySQL.Password", "");
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
            conn = DriverManager.getConnection (
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
	
    @Override
	public void close_resorce(ResultSet[] resultsets,Connection conn, PreparedStatement ps) {
		if (Objects.nonNull(resultsets)) {
			for (ResultSet resultSet : resultsets) {
			    if (Objects.nonNull(resultSet)) {
			    	try {
	                    resultSet.close();
	                } catch (SQLException e) {
						logger.error("A mysql close-resource error occurred: " + e.getMessage());
						for (StackTraceElement element : e.getStackTrace()) {
							logger.error(element.toString());
						}
	                }
			    }
			}
		}
		
		if (Objects.nonNull(conn)) {
			try {
                ps.close();
            } catch (SQLException e) {
                logger.error("A SQLException error occurred: " + e.getMessage());
				for (StackTraceElement element : e.getStackTrace()) {
					logger.error(element.toString());
				}
            }
		}
		
		if (Objects.nonNull(ps)) {
			try {
                conn.close();
            } catch (SQLException e) {
                logger.error("A SQLException error occurred: " + e.getMessage());
				for (StackTraceElement element : e.getStackTrace()) {
					logger.error(element.toString());
				}
            }
		}
	}
}