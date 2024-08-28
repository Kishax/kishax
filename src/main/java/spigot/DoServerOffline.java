package spigot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;
import java.util.logging.Level;

import com.google.inject.Inject;

public class DoServerOffline {

	public Connection conn = null;
	public PreparedStatement ps = null;
	
	private final common.Main plugin;
	private final SocketSwitch ssw;
	private final ServerHomeDir shd;
	private final Database db;
	
	@Inject
	public DoServerOffline (
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
			// サーバーをオフラインに
			if (Objects.nonNull(conn)) {
				String sql = "UPDATE mine_status SET online=? WHERE name=?;";
				ps = conn.prepareStatement(sql);
				ps.setBoolean(1,false);
				ps.setString(2, serverName);
				ps.executeUpdate();
			}
			
			ssw.stopSocketServer();
		} catch (SQLException | ClassNotFoundException e2) {
			plugin.getLogger().log(Level.SEVERE, "A SQLException | ClassNotFoundException error occurred: {0}", e2.getMessage());
            for (StackTraceElement element : e2.getStackTrace()) {
                plugin.getLogger().severe(element.toString());
            }
		} finally {
        	db.close_resorce(null, conn, ps);
        }
	}
}
