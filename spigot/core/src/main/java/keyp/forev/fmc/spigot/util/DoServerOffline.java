package keyp.forev.fmc.spigot.util;

import java.sql.Connection;
import java.sql.SQLException;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;

import keyp.forev.fmc.common.Database;
import keyp.forev.fmc.common.SocketSwitch;

public class DoServerOffline {
	private final Logger logger;
	private final Database db;
	private final Provider<SocketSwitch> sswProvider;
	private final String thisServerName;
	@Inject
	public DoServerOffline (Logger logger, Database db, Provider<SocketSwitch> sswProvider, SpigotServerHomeDir shd) {
		this.logger = logger;
		this.db = db;
		this.sswProvider = sswProvider;
		this.thisServerName = shd.getServerName();
	}
	
	public void UpdateDatabase() {
		try (Connection conn = db.getConnection()) {
			db.updateLog(conn, "UPDATE status SET online=?, socketport=?, player_list=?, current_players=? WHERE name=?;", new Object[] {false, 0, null, 0, thisServerName});
			SocketSwitch ssw = sswProvider.get();
			ssw.sendSpigotServer(conn, "MineStatusSync");
			ssw.stopSocketServer();
		} catch (SQLException | ClassNotFoundException e2) {
			logger.error( "A SQLException | ClassNotFoundException error occurred: {}", e2.getMessage());
            for (StackTraceElement element : e2.getStackTrace()) {
                logger.error(element.toString());
            }
		}
	}
}
