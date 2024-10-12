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
	public Connection getConnection(String customDatabase) throws SQLException, ClassNotFoundException {
		String host = config.getString("MySQL.Host", "");
		int port = config.getInt("MySQL.Port", 0);
		String user = config.getString("MySQL.User", "");
		String password = config.getString("MySQL.Password", "");
		if (customDatabase != null && !customDatabase.isEmpty()) {
			//logger.info("customDatabase: " + customDatabase);
			if ((host != null && host.isEmpty()) || 
				port == 0 || 
				(user != null && user.isEmpty()) || 
				(password != null && password.isEmpty())) {
				return null;
			}
			
			synchronized (Database.class) {
				//if (Objects.nonNull(conn) && !conn.isClosed()) return conn;
				
				Class.forName("com.mysql.cj.jdbc.Driver");
				return DriverManager.getConnection (
							"jdbc:mysql://" + host + ":" + 
							port + "/" + 
							customDatabase +
							"?autoReconnect=true&useSSL=false", 
							user, 
							password
				);
			}
		} else {
			String database = config.getString("MySQL.Database", "");
			if ((host != null && host.isEmpty()) || 
				port == 0 || 
				(database != null && database.isEmpty()) || 
				(user != null && user.isEmpty()) || 
				(password != null && password.isEmpty())) {
				return null;
			}
			
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
    }
	
	@Override
	public Connection getConnection() throws SQLException, ClassNotFoundException {
		return getConnection(null);
	}

    @Override
	public void close_resource(ResultSet[] resultsets, Connection[] conns, PreparedStatement ps) {
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
		
		if (Objects.nonNull(conns)) {
			for (Connection simpleconn : conns) {
			    if (Objects.nonNull(simpleconn)) {
					try {
						simpleconn.close();
					} catch (SQLException e) {
						logger.error("A SQLException error occurred: " + e.getMessage());
						for (StackTraceElement element : e.getStackTrace()) {
							logger.error(element.toString());
						}
					}
				}
			}
		}
		
		if (Objects.nonNull(ps)) {
			try {
                ps.close();
            } catch (SQLException e) {
                logger.error("A SQLException error occurred: " + e.getMessage());
				for (StackTraceElement element : e.getStackTrace()) {
					logger.error(element.toString());
				}
            }
		}
	}

	@Override
	public void close_resource(ResultSet[] resultsets, Connection conn, PreparedStatement ps) {
		Connection[] conns = {conn};
		close_resource(resultsets, conns, ps);
	}
}