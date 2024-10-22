package velocity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.messaging.MessagingService;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;

public class Luckperms {
	private final DatabaseInterface db;
	private final Provider<LuckPerms> lpapiProvider;
	private final Logger logger;
	private final PlayerUtil pu;
	
	@Inject
	public Luckperms(DatabaseInterface db, Provider<LuckPerms> lpapiProvider, Logger logger, PlayerUtil pu) {
		this.db = db;
		this.lpapiProvider = lpapiProvider;
		this.logger = logger;
		this.pu = pu;
	}
	
	public void triggerNetworkSync() {
		LuckPerms lpapi = lpapiProvider.get();
        MessagingService messagingService = lpapi.getMessagingService().orElse(null);
		
        if (messagingService != null) {
            messagingService.pushUpdate();
            logger.info("LuckPerms network sync triggered.");
        } else {
        	logger.error("Failed to get LuckPerms MessagingService.");
        }
    }

	public void addPermission(String playerName, String permission) {
		LuckPerms lpapi = lpapiProvider.get();
		User user = lpapi.getUserManager().getUser(playerName);
		if (user != null) {
			Node node = Node.builder(permission).build();
			user.data().add(node);
			lpapi.getUserManager().saveUser(user);
			// キャッシュをクリア
			//user.getCachedData().invalidate();
			triggerNetworkSync();
		}
	}

	public List<String> getPlayersWithPermission(String permission) {
		pu.loadPlayers();
		List<String> playersWithPermission = new ArrayList<>();
		try (Connection conn = db.getConnection("fmc_lp");
				PreparedStatement ps = conn.prepareStatement("SELECT * FROM lp_user_permissions WHERE permission = ?")) {
			ps.setString(1, permission);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					playersWithPermission.add(rs.getString("uuid"));
				}
				return pu.getPlayerNamesListFromUUIDs(playersWithPermission);
			}
		} catch (SQLException | ClassNotFoundException e) {
			logger.error("An error occurred while updating the database: " + e.getMessage(), e);
			for (StackTraceElement element : e.getStackTrace()) {
				logger.error(element.toString());
			}
			return null;
		}
	}

	public void removePermission(String playerName, String permission) {
		LuckPerms lpapi = lpapiProvider.get();
		User user = lpapi.getUserManager().getUser(playerName);
		if (user != null) {
			Node node = Node.builder(permission).build();
			user.data().remove(node);
			lpapi.getUserManager().saveUser(user);
			// キャッシュをクリア
			//user.getCachedData().invalidate();
			triggerNetworkSync();
		}
	}

	public boolean hasPermission(String playerName, String permission) {
		List<String> permissions = Arrays.asList(permission);
		return hasPermission(playerName, permissions);
	}

	public boolean hasPermission(String playerName, List<String> permission) {
		try (Connection conn = db.getConnection("fmc_lp");
				PreparedStatement ps = conn.prepareStatement("SELECT * FROM lp_user_permissions WHERE uuid = ? AND permission = ?")) {
			ps.setString(1, pu.getPlayerUUIDByNameFromDB(playerName));
			for (String perm : permission) {
				ps.setString(2, perm);
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						return true;
					}
				}
			}
			return false;
		} catch (SQLException | ClassNotFoundException e) {
			logger.error("An error occurred while updating the database: " + e.getMessage(), e);
			for (StackTraceElement element : e.getStackTrace()) {
				logger.error(element.toString());
			}
			return false;
		}
	}
}