package spigot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;
import java.util.logging.Level;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class DoServerOnline {
	private final common.Main plugin;
	private final Provider<SocketSwitch> sswProvider;
	private final ServerHomeDir shd;
	private final Database db;
	
	@Inject
	public DoServerOnline (
		common.Main plugin, Provider<SocketSwitch> sswProvider, ServerHomeDir shd,
		Database db
	) {
		this.plugin = plugin;
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
		plugin.getLogger().info(String.format("""
			%sサーバーが起動しました。""", serverName));
		
		String sql = "UPDATE status SET online=?,socketport=? WHERE name=?;";
		try (Connection conn = db.getConnection();
			PreparedStatement ps = conn.prepareStatement(sql)) {
			if (conn.isClosed()) {
				plugin.getLogger().severe("Connection is closed.");
				return;
			}
			ps.setBoolean(1,true);
			ps.setInt(2, socketport);
			ps.setString(3, serverName);
			ps.executeUpdate();
		
			plugin.getLogger().info("MySQL Server is connected!");
		} catch (SQLException | ClassNotFoundException e) {
			plugin.getLogger().log(Level.SEVERE, "An error occurred while updating the database: " + e.getMessage(), e);
			for (StackTraceElement element : e.getStackTrace()) {
				plugin.getLogger().severe(element.toString());
			}
		}
	}
}
