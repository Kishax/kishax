package common;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;

public class Database {
    private final String host, user, defaultDatabase, password;
    private final int port;
    private final Logger logger;
    public Database(Logger logger, String host, String user, String defaultDatabase, String password, int port) {
        this.logger = logger;
        this.host = host;
        this.user = user;
        this.defaultDatabase = defaultDatabase;
        this.password = password;
        this.port = port;
    }
    
    public synchronized Connection getConnection() throws SQLException, ClassNotFoundException {
		return getConnection(null);
	}
    
	public synchronized Connection getConnection(String customDatabase) throws SQLException, ClassNotFoundException {
        String database = customDatabase != null && !customDatabase.isEmpty() ? customDatabase : defaultDatabase;
        try {
            synchronized (Database.class) {
                //if (Objects.nonNull(conn2) && !conn2.isClosed()) return conn2;
                Class.forName("com.mysql.cj.jdbc.Driver");
                return DriverManager.getConnection ("jdbc:mysql://" + host + ":" + port + "/" + database +"?autoReconnect=true&useSSL=false", user, password);
            }
        } catch (SQLException e) {
            logger.error( "Failed to establish database connection.\n{}", e);
            for (StackTraceElement element : e.getStackTrace()) {
                logger.error(element.toString());
            }
            throw e;
        }
    }

    public String createQueryPart(Set<String> keySet) {
        return keySet.stream()
                     .map(key -> key + " = ?")
                     .collect(Collectors.joining(", "));
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

    public boolean isMaintenance(Connection conn) {
		String query = "SELECT online FROM status WHERE name=?;";
		try (PreparedStatement ps = conn.prepareStatement(query)) {
			ps.setString(1, "maintenance");
			try (var rs = ps.executeQuery()) {
				if (rs.next()) {
					return rs.getBoolean("online");
				}
			}
		} catch (SQLException e) {
			logger.error("A SQLException error occurred: " + e.getMessage());
			for (StackTraceElement element : e.getStackTrace()) {
				logger.error(element.toString());
			}
		}
		return false;
	}

    public int countPlaceholders(String query) {
        int count = 0;
        for (char c : query.toCharArray()) {
            if (c == '?') {
                count++;
            }
        }
        return count;
    }

    private void setPreparedStatementValues(PreparedStatement ps, Object[] args) throws SQLException {
        for (int i = 0; i < args.length; i++) {
            ps.setObject(i + 1, args[i]);
        }
    }

    @SuppressWarnings("unused")
    private Class<?> getTypes(Object value) {
		return value.getClass();
	}
}