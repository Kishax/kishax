package keyp.forev.fmc.common.database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.slf4j.Logger;

import keyp.forev.fmc.common.database.interfaces.DatabaseInfo;
import keyp.forev.fmc.common.util.JavaUtils;
import com.google.inject.Inject;

public class Database {
    private final Logger logger;
    private final DatabaseInfo dInfo;
    private static Database staticInstance;
    private final AtomicBoolean firstConnection = new AtomicBoolean(false);
    @Inject
    public Database(Logger logger, DatabaseInfo dInfo) {
        this.logger = logger;
        this.dInfo = dInfo;
        Database.staticInstance = this;
    }
    
    public static synchronized Database getInstance() {
        return Database.staticInstance;
    }

    public synchronized Connection getConnection() throws SQLException, ClassNotFoundException {
		return getConnection(null);
	}
    
	public synchronized Connection getConnection(String customDatabase) throws SQLException, ClassNotFoundException {
        if (!dInfo.check()) {
            if (firstConnection.compareAndSet(false, true)) {
                logger.error("Database information is not set.");
            }
            return null;
        }
        String database = customDatabase != null && !customDatabase.isEmpty() ? customDatabase : dInfo.getDefaultDatabase();
        try {
            synchronized (Database.class) {
                Class.forName("com.mysql.cj.jdbc.Driver");
                return DriverManager.getConnection("jdbc:mysql://" + dInfo.getHost() + ":" + dInfo.getPort() + "/" + database +"?autoReconnect=true&useSSL=false", dInfo.getUser(), dInfo.getPassword());
            }
        } catch (SQLException e) {
            logger.error( "Failed to establish database connection.\n{}", e);
            for (StackTraceElement element : e.getStackTrace()) {
                logger.error(element.toString());
            }
            throw e;
        }
    }

    public Set<Object> getSet(Connection conn, String query, Object[] args) throws SQLException, ClassNotFoundException {
        PreparedStatement ps = conn.prepareStatement(query);
        setPreparedStatementValues(ps, args);
        ResultSet rs = ps.executeQuery();
        return getResultSetAsSet(rs);
    }

    public Set<String> getStringSet(Connection conn, String query, Object[] args) throws SQLException, ClassNotFoundException {
        Set<Object> set = getSet(conn, query, args);
        return set.stream().map(Object::toString).collect(Collectors.toSet());
    }

    public String createQueryPart(Set<String> keySet) {
        return keySet.stream()
                     .map(key -> key + " = ?")
                     .collect(Collectors.joining(", "));
    }

    public void insertLog(String query, Object[] args) throws SQLException, ClassNotFoundException {
        insertLog(null, query, args);
    }

    public Object insertLogAndGetColumnValue(int columnNum, Connection conn, String query, Object[] args) throws SQLException, ClassNotFoundException {
        Connection connection = (conn != null && !conn.isClosed()) ? conn : getConnection();
        if (args.length < countPlaceholders(query)) return null;
        PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        setPreparedStatementValues(ps, args);
        ps.executeUpdate();
        try (ResultSet rs = ps.getGeneratedKeys()) {
            if (rs.next()) {
                return rs.getObject(columnNum);
            }
        }
        return null;
    }

    public void insertLog(Connection conn, String query, Object[] args) throws SQLException, ClassNotFoundException {
        Connection connection = (conn != null && !conn.isClosed()) ? conn : getConnection();
        if (args.length < countPlaceholders(query)) return;
        PreparedStatement ps = connection.prepareStatement(query);
        setPreparedStatementValues(ps, args);
        ps.executeUpdate();
    }
    
    public void deleteLog(Connection conn, String query, Object[] args) throws SQLException, ClassNotFoundException {
        insertLog(conn, query, args);
    }

    public void updateLog(Connection conn, String query, Object[] args) throws SQLException, ClassNotFoundException {
        insertLog(conn, query, args);
    }

    public void updateLog(String query, Object[] args) throws SQLException, ClassNotFoundException {
        insertLog(null, query, args);
    }

    public void setPreparedStatementValue(PreparedStatement ps, int parameterIndex, Object value) throws SQLException {
        if (value == null) {
            ps.setNull(parameterIndex, java.sql.Types.NULL);
		} else if (value instanceof Integer) {
			ps.setInt(parameterIndex, (Integer) value);
		} else if (value instanceof Long) {
			ps.setLong(parameterIndex, (Long) value);
		} else if (value instanceof Boolean) {
			ps.setBoolean(parameterIndex, (Boolean) value);
		} else if (value instanceof String) {
			ps.setString(parameterIndex, (String) value);
		} else if (value instanceof Double) {
			ps.setDouble(parameterIndex, (Double) value);
		} else if (value instanceof Float) {
			ps.setFloat(parameterIndex, (Float) value);
		} else if (value instanceof java.sql.Date) {
			ps.setDate(parameterIndex, (java.sql.Date) value);
		} else if (value instanceof java.sql.Timestamp) {
			ps.setTimestamp(parameterIndex, (java.sql.Timestamp) value);
        } else {
            throw new SQLException("Unsupported data type: " + value.getClass().getName());
        }
    }

    public List<String> getServersList(boolean isOnline) {
        List<String> servers = new ArrayList<>();
        String query = "SELECT * FROM status WHERE online=? AND exception!=1 AND exception2!=1;";
        try (Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(query)) {
            if (isOnline) {
                ps.setBoolean(1, true);
            } else {
                ps.setBoolean(1, false);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    servers.add(rs.getString("name"));
                }
            }
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("A SQLException | ClassNotFoundException error occurred: " + e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                logger.error(element.toString());
            }
        }
        return servers;
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

    public List<String> defineImageColumnNamesList(Connection conn, String tableName) throws SQLException {
        List<String> columnNames = new ArrayList<>();
        DatabaseMetaData metaData = conn.getMetaData();
        try (ResultSet rs = metaData.getColumns(null, null, tableName, null)) {
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                columnNames.add(columnName);
            }
        }
        return columnNames;
    }
    
    public void updateMemberToggle(Connection conn, String columnName, boolean value, String key) throws SQLException {
        String query = "UPDATE members SET " + columnName + " = ? WHERE ";
        if (JavaUtils.isUUID(key)) {
            query += "uuid = ?";
        } else {
            query += "name = ?";
        }
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setBoolean(1, value);
            ps.setString(2, key);
            ps.executeUpdate();
        }
    }
    
    public Map<String, Object> getMemberMap(Connection conn, String key) throws SQLException {
        Map<String, Object> rowMap = new HashMap<>();
        String query = "SELECT * FROM members WHERE ";
        if (JavaUtils.isUUID(key)) {
            query += "uuid = ?";
        } else {
            query += "name = ?";
        }
        PreparedStatement ps = conn.prepareStatement(query);
        ps.setString(1, key);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            int columnCount = rs.getMetaData().getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = rs.getMetaData().getColumnName(i);
                rowMap.put(columnName, rs.getObject(columnName));
            }
        }
        return rowMap;
    }
    
    public int getPlayerTime(Connection conn, String playerUUID, int logId) throws SQLException{
        // 現在の時刻と最後のログの時刻の差を計算するメソッド
		String query = "SELECT * FROM log WHERE uuid=? AND `join`=? AND `id`=? ORDER BY id DESC LIMIT 1;";
		try (PreparedStatement ps = conn.prepareStatement(query)) {
    		ps.setString(1, playerUUID);
    		ps.setBoolean(2, true);
            ps.setInt(3, logId);
    		try (ResultSet bj_logs = ps.executeQuery()) {
				if (bj_logs.next()) {
					long now_timestamp = Instant.now().getEpochSecond();
					Timestamp bj_time = bj_logs.getTimestamp("time");
					long bj_timestamp = bj_time.getTime() / 1000L;
					long bj_sa = now_timestamp-bj_timestamp;
					return (int) bj_sa;
				}
			}
    	}
		return 0;
	}

    private Set<Object> getResultSetAsSet(ResultSet rs) throws SQLException {
        Set<Object> set = new java.util.HashSet<>();
        while (rs.next()) {
            set.add(rs.getObject(1));
        }
        return set;
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