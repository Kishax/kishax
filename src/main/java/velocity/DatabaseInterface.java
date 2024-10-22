package velocity;

import java.sql.Connection;
import java.sql.SQLException;

public interface DatabaseInterface {
    // メソッドの宣言
	Connection getConnection(String customDatabase) throws SQLException, ClassNotFoundException;
    Connection getConnection() throws SQLException, ClassNotFoundException;
}
