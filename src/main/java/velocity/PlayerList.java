package velocity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import com.google.inject.Inject;

public class PlayerList
{
	private final DatabaseInterface db;
	private Connection conn = null;
	private PreparedStatement ps = null;
	private ResultSet playerlist = null;
	private ResultSet[] resultsets = {playerlist};
	private List<String> Players = new CopyOnWriteArrayList<>();
	private boolean isLoaded = false;
	
	@Inject
	public PlayerList(DatabaseInterface db)
	{
		this.db = db;
	}
	
	public synchronized void loadPlayers()
	{
		if (isLoaded) return;
		
		try
		{
			conn = db.getConnection();
			
			if(Objects.isNull(conn))	return;
			
			String sql = "SELECT * FROM minecraft;";
			ps = conn.prepareStatement(sql);
			playerlist = ps.executeQuery();
			
			while(playerlist.next())
			{
				Players.add(playerlist.getString("name"));
			}
			isLoaded = true;
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			db.close_resorce(resultsets, conn, ps);
		}
 	}
	
	public void updatePlayers()
	{
		try
		{
			conn = db.getConnection();
			String sql = "SELECT * FROM minecraft;";
			ps = conn.prepareStatement(sql);
			playerlist = ps.executeQuery();
			
			// Playersリストを初期化
			Players.clear();
					
			while(playerlist.next())
			{
				Players.add(playerlist.getString("name"));
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			db.close_resorce(resultsets, conn, ps);
		}
 	}
	
	public List<String> getPlayerList()
	{
		return Players;
	}
}
