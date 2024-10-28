package velocity;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.inject.Inject;

public class Database implements DatabaseInterface {

	private final Config config;
    
    @Inject
    public Database (Config config) {
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
				//if (Objects.nonNull(conn) && !conn.isClosed()) return conn;
				Class.forName("com.mysql.cj.jdbc.Driver");
				return DriverManager.getConnection (
							"jdbc:mysql://" + host + ":" + 
							port + "/" + 
							database +
							"?autoReconnect=true&useSSL=false", 
							user, 
							password
						);
			}
		}
    }
	
	@Override
	public Connection getConnection() throws SQLException, ClassNotFoundException {
		return getConnection(null);
	}

	@Override
    public String createPlaceholders(int count) {
        return String.join(", ", java.util.Collections.nCopies(count, "?"));
    }

	@Override
    public String createQueryPart(Set<String> keySet) {
        return keySet.stream()
                     .map(key -> key + " = ?")
                     .collect(Collectors.joining(", "));
    }

	@Override
    public void setPreparedStatementValue(PreparedStatement ps, int parameterIndex, Object value) throws SQLException {
        if (value == null) {
            ps.setNull(parameterIndex, java.sql.Types.NULL);
		} else if (value instanceof Integer i) {
			ps.setInt(parameterIndex, i);
		} else if (value instanceof Long l) {
			ps.setLong(parameterIndex, l);
		} else if (value instanceof Boolean b) {
			ps.setBoolean(parameterIndex, b);
		} else if (value instanceof String s) {
			ps.setString(parameterIndex, s);
		} else if (value instanceof Double d) {
			ps.setDouble(parameterIndex, d);
		} else if (value instanceof Float f) {
			ps.setFloat(parameterIndex, f);
		} else if (value instanceof java.sql.Date d) {
			ps.setDate(parameterIndex, d);
		} else if (value instanceof java.sql.Timestamp t) {
			ps.setTimestamp(parameterIndex, t);
        } else {
            throw new SQLException("Unsupported data type: " + value.getClass().getName());
        }
    }

	@Override
	public Class<?> getTypes(Object value) {
		return value.getClass();
	}
}