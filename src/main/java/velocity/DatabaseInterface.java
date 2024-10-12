package velocity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public interface DatabaseInterface {
    // メソッドの宣言
	Connection getConnection(String customDatabase) throws SQLException, ClassNotFoundException;
    Connection getConnection() throws SQLException, ClassNotFoundException;
    void close_resource(ResultSet[] resultsets, Connection[] conns, PreparedStatement ps);
    void close_resource(ResultSet[] resultsets, Connection conn, PreparedStatement ps);
}
