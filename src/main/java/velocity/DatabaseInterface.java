package velocity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public interface DatabaseInterface
{
    // メソッドの宣言
	Connection getConnection() throws SQLException, ClassNotFoundException;
    void DoServerOnline();
    void close_resorce(ResultSet[] resultsets, Connection conn, PreparedStatement ps);
}
