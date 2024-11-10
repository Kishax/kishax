package common;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;

import spigot.ImageMap;

public class Database {
    public static Database staticInstance;
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
    
    // guiceでバインドしないクラスからDBにアクセスするため、staticメソッドに変更
    public static synchronized Database getInstance() {
        return staticInstance;
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

    public void updateLog(String query, Object[] args) throws SQLException, ClassNotFoundException {
        insertLog(null, query, args);
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

    public void defineImageColumnNamesList(Connection conn, String tableName) throws SQLException {
        List<String> columnNames = new ArrayList<>();
        DatabaseMetaData metaData = conn.getMetaData();
        try (ResultSet rs = metaData.getColumns(null, null, tableName, null)) {
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                columnNames.add(columnName);
            }
        }
        switch (tableName) {
            case "images" -> {
                ImageMap.imagesColumnsList = columnNames;
            }
        }
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