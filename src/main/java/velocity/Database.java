package velocity;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public class Database
{
    public static Connection conn;
    
    public Database()
    {
    	//
    }

	public static Connection getConnection() throws SQLException, ClassNotFoundException
	{
		if
		(
				
			((String) Config.getConfig().get("MySQL.Host")).isEmpty() || 
			((int) Config.getConfig().get("MySQL.Port"))==0 || 
			((String) Config.getConfig().get("MySQL.Database")).isEmpty() || 
			((String) Config.getConfig().get("MySQL.User")).isEmpty() || 
			((String) Config.getConfig().get("MySQL.Password")).isEmpty()
		)
		{
			return null;
		}
		
        if (conn != null && !conn.isClosed()) return conn;
        
        synchronized (Database.class) {
            if (conn != null && !conn.isClosed()) return conn;
            
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection
            		(
            			"jdbc:mysql://" + ((String) Config.getConfig().get("MySQL.Host") + ":" + 
            			(int) Config.getConfig().get("MySQL.Port") + "/" + 
            			(String) Config.getConfig().get("MySQL.Database")), 
            			((String) Config.getConfig().get("MySQL.User")), 
            			((String) Config.getConfig().get("MySQL.Password"))
            		);
            return conn;
        }
    }
	
	public static void close_resorce(ResultSet[] resultsets,Connection conn, PreparedStatement ps)
	{
		if(Objects.nonNull(resultsets))
		{
			for (ResultSet resultSet : resultsets)
			{
			    if (Objects.nonNull(resultSet))
			    {
			    	try
			    	{
	                    resultSet.close();
	                }
			    	catch (SQLException e)
			    	{
	                    e.printStackTrace();
	                }
			    }
			}
		}
		
		if(Objects.nonNull(conn))
		{
			try
			{
                ps.close();
            }
			catch (SQLException e)
			{
                e.printStackTrace();
            }
		}
		
		if(Objects.nonNull(ps))
		{
			try
			{
                conn.close();
            }
			catch (SQLException e)
			{
                e.printStackTrace();
            }
		}
	}
}