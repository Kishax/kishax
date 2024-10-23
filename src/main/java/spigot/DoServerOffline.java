package spigot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class DoServerOffline {
	private final common.Main plugin;
	private final Provider<SocketSwitch> sswProvider;
	private final ServerHomeDir shd;
	private final Database db;
	
	@Inject
	public DoServerOffline (common.Main plugin, Provider<SocketSwitch> sswProvider, ServerHomeDir shd, Database db) {
		this.plugin = plugin;
		this.sswProvider = sswProvider;
		this.shd = shd;
		this.db = db;
	}
	
	public void UpdateDatabase() {
		SocketSwitch ssw = sswProvider.get();
		ssw.sendSpigotServer("MineStatusSync");
		String query = "UPDATE status SET online=?, socketport=? WHERE name=?;";
		try (Connection conn = db.getConnection();
			PreparedStatement ps = conn.prepareStatement(query)) {
			// "plugins"ディレクトリの親ディレクトリを取得
            String serverName = shd.getServerName();
			ps.setBoolean(1,false);
			ps.setInt(2, 0);
			ps.setString(3, serverName);
			int rsAffected = ps.executeUpdate();
			if (rsAffected > 0) {
				ssw.stopSocketServer();
			}
		} catch (SQLException | ClassNotFoundException e2) {
			plugin.getLogger().log(Level.SEVERE, "A SQLException | ClassNotFoundException error occurred: {0}", e2.getMessage());
            for (StackTraceElement element : e2.getStackTrace()) {
                plugin.getLogger().severe(element.toString());
            }
		}
	}
}
