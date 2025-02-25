package keyp.forev.fmc.common.server;

import java.sql.Connection;
import java.sql.SQLException;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;

import keyp.forev.fmc.common.database.Database;
import keyp.forev.fmc.common.server.interfaces.ServerHomeDir;
import keyp.forev.fmc.common.socket.message.Message;
import keyp.forev.fmc.common.socket.SocketSwitch;

public class DoServerOffline {
	private final Logger logger;
	private final Database db;
	private final Provider<SocketSwitch> sswProvider;
	private final String thisServerName;
	@Inject
	public DoServerOffline(Logger logger, Database db, Provider<SocketSwitch> sswProvider, ServerHomeDir shd) {
		this.logger = logger;
		this.db = db;
		this.sswProvider = sswProvider;
		this.thisServerName = shd.getServerName();
	}
	
	public void updateDatabase() {
		try (Connection conn = db.getConnection()) {
            Message msg = new Message();
            msg.mc = new Message.Minecraft();
            msg.mc.server = new Message.Minecraft.Server();
            msg.mc.server.action = "STOP";
            msg.mc.server.name = thisServerName;

			SocketSwitch ssw = sswProvider.get();
			ssw.sendVelocityServer(conn, msg);
			db.updateLog(conn, "UPDATE status SET online=?, socketport=?, player_list=?, current_players=? WHERE name=?;", new Object[] {false, 0, null, 0, thisServerName});

            Message msg2 = new Message();
            msg2.mc = new Message.Minecraft();
            msg2.mc.sync = new Message.Minecraft.Sync();
            msg2.mc.sync.content = "STATUS";

			ssw.sendSpigotServer(conn, msg2);
			ssw.stopSocketServer();
		} catch (SQLException | ClassNotFoundException e2) {
			logger.error( "A SQLException | ClassNotFoundException error occurred: {}", e2.getMessage());
			for (StackTraceElement element : e2.getStackTrace()) {
				logger.error(element.toString());
			}
		}
	}
}
