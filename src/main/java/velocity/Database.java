package velocity;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.ProxyServer;

public class Database implements DatabaseInterface
{
	private final Config config;
	private final Logger logger;
    private Connection conn = null;
    public ResultSet mine_status = null;
	public ResultSet[] resultsets = {mine_status};
    
    @Inject
    public Database
	(
		Main plugin, ProxyServer server, Logger logger, 
		Config config, ConsoleCommandSource console
	)
    {
		this.logger = logger;
    	this.config = config;
    }

    @Override
	public Connection getConnection() throws SQLException, ClassNotFoundException
	{
		// Map<String, Object> mysqlConfig = (Map<String, Object>) config.getConfig().get("MySQL");
		if
		(
			config.getString("MySQL.Host","").isEmpty() || 
			config.getInt("MySQL.Port",0)==0 || 
			config.getString("MySQL.Database","").isEmpty() || 
			config.getString("MySQL.User","").isEmpty() || 
			config.getString("MySQL.Password","").isEmpty()
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
            			"jdbc:mysql://" + config.getString("MySQL.Host") + ":" + 
            			config.getInt("MySQL.Port") + "/" + 
            			config.getString("MySQL.Database"), 
            			config.getString("MySQL.User"), 
            			config.getString("MySQL.Password")
            		);
            return conn;
        }
    }
	
    @Override
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
						logger.error("A mysql close-resource error occurred: " + e.getMessage());
						for (StackTraceElement element : e.getStackTrace()) 
						{
							logger.error(element.toString());
						}
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
                logger.error("A SQLException error occurred: " + e.getMessage());
				for (StackTraceElement element : e.getStackTrace()) 
				{
					logger.error(element.toString());
				}
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
                logger.error("A SQLException error occurred: " + e.getMessage());
				for (StackTraceElement element : e.getStackTrace()) 
				{
					logger.error(element.toString());
				}
            }
		}
	}
}