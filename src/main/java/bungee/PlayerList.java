package bungee;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class PlayerList
{
	public static Connection conn = null;
	public static PreparedStatement ps = null;
	public static ResultSet playerlist = null;
	public static ResultSet[] resultsets = {playerlist};
	public static List<String> Players = new CopyOnWriteArrayList<>();
	public static boolean isLoaded = false;
	
	public PlayerList()
	{
		//
	}
	
	public static synchronized void loadPlayers()
	{
		if (isLoaded) return;
		
		try
		{
			conn = Database.getConnection();
			String sql = "SELECT * FROM minecraft;";
			ps = conn.prepareStatement(sql);
			playerlist = ps.executeQuery();
			
			while(playerlist.next())
			{
				Players.add(playerlist.getString("name"));
			}
			isLoaded = true;
		}
		catch(SQLException | ClassNotFoundException e)
		{
			e.printStackTrace();
		}
		finally
		{
			Database.close_resorce(resultsets, conn, ps);
		}
 	}
	
	public static void updatePlayers()
	{
		try
		{
			conn = Database.getConnection();
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
		catch(SQLException | ClassNotFoundException e)
		{
			e.printStackTrace();
		}
		finally
		{
			Database.close_resorce(resultsets, conn, ps);
		}
 	}
	
	public static List<String> getPlayerList()
	{
		return PlayerList.Players;
	}
}
