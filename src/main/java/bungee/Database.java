package bungee;

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
			Config.getConfig().getString("MySQL.Host").isEmpty() || 
			Config.getConfig().getInt("MySQL.Port")==0 || 
			Config.getConfig().getString("MySQL.Database").isEmpty() || 
			Config.getConfig().getString("MySQL.User").isEmpty() || 
			Config.getConfig().getString("MySQL.Password").isEmpty()
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
            			"jdbc:mysql://" + Config.getConfig().getString("MySQL.Host") + ":" + 
            			Config.getConfig().getInt("MySQL.Port") + "/" + 
            			Config.getConfig().getString("MySQL.Database"), 
            			Config.getConfig().getString("MySQL.User"), 
            			Config.getConfig().getString("MySQL.Password")
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