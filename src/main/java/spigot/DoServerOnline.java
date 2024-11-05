package spigot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;

import common.Database;

public class DoServerOnline {
	private final common.Main plugin;
	private final Logger logger;
	private final Provider<SocketSwitch> sswProvider;
	private final ServerHomeDir shd;
	private final Database db;
	
	@Inject
	public DoServerOnline (common.Main plugin, Logger logger, Provider<SocketSwitch> sswProvider, ServerHomeDir shd, Database db) {
		this.plugin = plugin;
		this.logger = logger;
		this.sswProvider = sswProvider;
		this.shd = shd;
		this.db = db;
	}
	
	public void UpdateDatabase(int socketport) {
		// "plugins"ディレクトリの親ディレクトリを取得
		String serverName = shd.getServerName();
		Objects.requireNonNull(serverName);

		// サーバーをオンラインに
		SocketSwitch ssw = sswProvider.get();
		// 他のサーバーに通知
		ssw.sendVelocityServer(serverName+"サーバーが起動しました。");
		ssw.sendSpigotServer(serverName+"サーバーが起動しました。");
		// ServerStatusCache初回ループと各サーバーのSocketResponseクラスで、
		// refreshManualOnlineServer()を呼び出しているので、MineStatusSyncは不要
		String query = "UPDATE status SET online=?, socketport=? WHERE name=?;";
		try (Connection conn = db.getConnection();
			PreparedStatement ps = conn != null && !conn.isClosed() ? conn.prepareStatement(query) : db.getConnection().prepareStatement(query)) {
			ps.setBoolean(1,true);
			ps.setInt(2, socketport);
			ps.setString(3, serverName);
			int rsAffected = ps.executeUpdate();
			if (rsAffected > 0) {
				logger.info("MySQL Server is connected!");
			}
		} catch (SQLException | ClassNotFoundException e) {
			logger.error("An error occurred while updating the database: " + e.getMessage(), e);
			for (StackTraceElement element : e.getStackTrace()) {
				plugin.getLogger().severe(element.toString());
			}
		}
	}
}
