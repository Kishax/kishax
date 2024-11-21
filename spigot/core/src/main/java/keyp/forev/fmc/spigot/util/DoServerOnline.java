package keyp.forev.fmc.spigot.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;

import keyp.forev.fmc.common.Database;
import keyp.forev.fmc.common.SocketSwitch;

public class DoServerOnline {
	private final Logger logger;
	private final Provider<SocketSwitch> sswProvider;
	private final SpigotServerHomeDir shd;
	private final Database db;
	
	@Inject
	public DoServerOnline (Logger logger, Provider<SocketSwitch> sswProvider, SpigotServerHomeDir shd, Database db) {
		this.logger = logger;
		this.sswProvider = sswProvider;
		this.shd = shd;
		this.db = db;
	}
	
	public void UpdateDatabase(int socketport) {
		// "plugins"ディレクトリの親ディレクトリを取得
		String serverName = shd.getServerName();
		Objects.requireNonNull(serverName);
		try (Connection conn = db.getConnection()) {
			db.updateLog( "UPDATE settings SET value=? WHERE name=?;", new Object[] {serverName, "now_online"});
			SocketSwitch ssw = sswProvider.get();
			ssw.sendVelocityServer(conn, serverName+"サーバーが起動しました。");
			ssw.sendSpigotServer(conn, serverName+"サーバーが起動しました。");
			// refreshManualOnlineServer()を呼び出しているので、MineStatusSyncは不要
			String query = "UPDATE status SET online=?, socketport=? WHERE name=?;";
			try (PreparedStatement ps = conn.prepareStatement(query)) {
				ps.setBoolean(1,true);
				ps.setInt(2, socketport);
				ps.setString(3, serverName);
				int rsAffected = ps.executeUpdate();
				if (rsAffected > 0) {
					logger.info("MySQL Server is connected!");
				}
			}
		} catch (SQLException | ClassNotFoundException e) {
			logger.error("An error occurred while updating the database: " + e.getMessage(), e);
			for (StackTraceElement element : e.getStackTrace()) {
				logger.error(element.toString());
			}
		}
	}
}
