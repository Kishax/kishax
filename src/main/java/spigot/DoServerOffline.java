package spigot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;

import common.Database;

public class DoServerOffline {
	private final Logger logger;
	private final Provider<SocketSwitch> sswProvider;
	private final ServerHomeDir shd;
	private final Database db;
	
	@Inject
	public DoServerOffline (Logger logger, Provider<SocketSwitch> sswProvider, ServerHomeDir shd, Database db) {
		this.logger = logger;
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
			logger.error( "A SQLException | ClassNotFoundException error occurred: {}", e2.getMessage());
            for (StackTraceElement element : e2.getStackTrace()) {
                logger.error(element.toString());
            }
		}
	}
}
