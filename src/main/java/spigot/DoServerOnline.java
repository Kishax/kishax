package spigot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;
import java.util.logging.Level;

import com.google.inject.Inject;

public class DoServerOnline {
	public Connection conn = null;
	public PreparedStatement ps = null;
	
	private final common.Main plugin;
	private final SocketSwitch ssw;
	private final ServerHomeDir shd;
	private final Database db;
	
	@Inject
	public DoServerOnline (
		common.Main plugin, SocketSwitch ssw, ServerHomeDir shd,
		Database db
	) {
		this.plugin = plugin;
		this.ssw = ssw;
		this.shd = shd;
		this.db = db;
	}
	
	public void UpdateDatabase() {
		try {
        	// "plugins"ディレクトリの親ディレクトリを取得
            String serverName = shd.getServerName();
            
			conn = db.getConnection();
			
			if(Objects.nonNull(conn) && Objects.nonNull(serverName)) {
				// サーバーをオンラインに
				ssw.startSocketClient(serverName+"サーバーが起動しました。");
				plugin.getLogger().info(String.format("""
					%sサーバーが起動しました。""", serverName));
				
				String sql = "UPDATE status SET online=? WHERE name=?;";
				ps = conn.prepareStatement(sql);
				ps.setBoolean(1,true);
				ps.setString(2, serverName);
				ps.executeUpdate();
				
				plugin.getLogger().info("MySQL Server is connected!");
			} else plugin.getLogger().info("MySQL Server is canceled for config value not given");
		} catch (SQLException | ClassNotFoundException e) {
			plugin.getLogger().log(Level.SEVERE, "A sendWebhookMessage error occurred: {0}", e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                plugin.getLogger().severe(element.toString());
            }
		} finally {
        	db.close_resource(null, conn, ps);
        }
	}
}
