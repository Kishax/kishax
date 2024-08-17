package spigot;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import com.google.inject.Inject;

public class Database
{
    public static Connection conn;
    private final common.Main plugin;
    
    @Inject
    public Database(common.Main plugin)
    {
    	this.plugin = plugin;
    }
    
	public Connection getConnection() throws SQLException, ClassNotFoundException
	{
		if
		(
			plugin.getConfig().getString("MySQL.Host", "").isEmpty() || 
			plugin.getConfig().getInt("MySQL.Port", 0) == 0 || 
			plugin.getConfig().getString("MySQL.Database", "").isEmpty() || 
			plugin.getConfig().getString("MySQL.User", "").isEmpty() || 
			plugin.getConfig().getString("MySQL.Password", "").isEmpty()
		)
		{
			return null;
		}
		
        if (Objects.nonNull(conn) && !conn.isClosed()) return conn;
        
        synchronized (Database.class) {
            if (Objects.nonNull(conn) && !conn.isClosed()) return conn;
            
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection
            		(
            			"jdbc:mysql://" + plugin.getConfig().getString("MySQL.Host") + ":" + 
            			plugin.getConfig().getInt("MySQL.Port") + "/" + 
            			plugin.getConfig().getString("MySQL.Database"),
            			plugin.getConfig().getString("MySQL.User"),
            			plugin.getConfig().getString("MySQL.Password")
            		);
            return conn;
        }
    }
	
	public void close_resorce(ResultSet[] resultsets,Connection conn, PreparedStatement ps)
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