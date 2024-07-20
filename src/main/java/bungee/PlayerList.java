package bungee;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PlayerList
{
	public Connection conn = null;
	public PreparedStatement ps;
	public ResultSet playerlist = null;
	public ResultSet[] resultsets = {playerlist};
	public static List<String> Players = new ArrayList<>();
	
	public PlayerList()
	{
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
		}
		catch(SQLException | ClassNotFoundException e)
		{
			e.printStackTrace();
		}
		finally
		{
			Database.close_resorce(resultsets, conn, null);
		}
	}
	
	public static List<String> getPlayerList()
	{
		return PlayerList.Players;
	}
}
