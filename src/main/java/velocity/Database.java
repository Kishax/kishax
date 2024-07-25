package velocity;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import org.slf4j.Logger;

import com.google.inject.Inject;

public class Database implements DatabaseInterface
{
	private final Config config;
    private Connection conn = null;
    private PreparedStatement ps = null;
    private boolean firstonline = false;
    private final Logger logger;
    
    @Inject
    public Database(Logger logger, Config config)
    {
    	this.logger = logger;
    	this.config = config;
    }

    @Inject
    @Override
	public void DoServerOnline()
	{
    	if(!(firstonline))
    	{
    		firstonline = true;
    		return;
    	}
    		
		try
		{
			conn = getConnection();
			if(Objects.nonNull(conn))
			{
				String sql = "UPDATE mine_status SET Proxi=? WHERE id=1;";
				ps = conn.prepareStatement(sql);
				ps.setBoolean(1,true);
				ps.executeUpdate();
				logger.info("MySQL Server is connected!");
			}
			else logger.info("MySQL Server is canceled for config value not given");
		}
		catch (ClassNotFoundException | SQLException e1)
		{
			e1.printStackTrace();
		}
		finally
		{
			close_resorce(null,conn,ps);
		}
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