package velocity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import com.google.inject.Inject;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

public class PlayerUtil
{
	private final ProxyServer server;
	private final DatabaseInterface db;
	private Connection conn = null;
	private PreparedStatement ps = null;
	private ResultSet playerlist = null, dbuuid = null;
	private ResultSet[] resultsets = {playerlist, dbuuid};
	private List<String> Players = new CopyOnWriteArrayList<>();
	private boolean isLoaded = false;
	
	@Inject
	public PlayerUtil(ProxyServer server, DatabaseInterface db)
	{
		this.server = server;
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
	
	public Optional<Player> getPlayerByName(String playerName)
	{
        return server.getAllPlayers().stream()
                .filter(player -> player.getUsername().equalsIgnoreCase(playerName))
                .findFirst();
    }
	
	public String getPlayerUUIDByName(String playerName)
	{
		try
		{
			conn = db.getConnection();
			String sql = "SELECT uuid FROM minecraft WHERE name=? ORDER BY id DESC LIMIT 1;";
			ps = conn.prepareStatement(sql);
			ps.setString(1, playerName);
			dbuuid = ps.executeQuery();
			if(dbuuid.next())
			{
				return dbuuid.getString("uuid");
			}
			else
			{
				return null;
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}
}
