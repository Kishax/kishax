package spigot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.google.inject.Inject;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.messaging.MessagingService;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;

public class Luckperms {
	private final common.Main plugin;
	private final Database db;
	private final LuckPerms lpapi = LuckPermsProvider.get();
	private final PlayerUtil pu;
	
	@Inject
	public Luckperms(common.Main plugin, Database db, PlayerUtil pu) {
		this.plugin = plugin;
		this.db = db;
		this.pu = pu;
	}
	
	public void triggerNetworkSync() {
        MessagingService messagingService = lpapi.getMessagingService().orElse(null);
        if (messagingService != null) {
            messagingService.pushUpdate();
            plugin.getLogger().log(Level.INFO, "LuckPerms network sync triggered.");
        } else {
        	plugin.getLogger().log(Level.SEVERE, "Failed to get LuckPerms MessagingService.");
        }
    }

	public void addPermission(String playerName, String permission) {
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
			plugin.getLogger().log(Level.SEVERE, "An error occurred while updating the database: {0}", e.getMessage());
			for (StackTraceElement element : e.getStackTrace()) {
				plugin.getLogger().log(Level.SEVERE, element.toString());
			}
			return null;
		}
	}

	public void removePermission(String playerName, String permission) {
		User user = lpapi.getUserManager().getUser(playerName);
		if (user != null) {
			Node node = Node.builder(permission).build();
			user.data().remove(node);
			lpapi.getUserManager().saveUser(user);
			//user.getCachedData().invalidate(); // キャッシュをクリア
			triggerNetworkSync();
		}
	}

	public List<Boolean> hasPermissionReturnBools(String playerName, List<String> permissions) {
		User user = lpapi.getUserManager().getUser(playerName);
		if (user == null) {
			return null;
		}
		List<String> groups = user.getNodes().stream()
				.filter(node -> node instanceof InheritanceNode)
				.map(node -> ((InheritanceNode) node).getGroupName())
				.collect(Collectors.toList());
		plugin.getLogger().log(Level.INFO, "Groups: {0}", groups);
		List<Boolean> hasPermission = new ArrayList<>();
		for (String groupName : groups) {
            if (acheckGroupPermission(groupName, permissions)) {
                return true;
            }
        }
	}
	
	private boolean acheckGroupPermission(String groupName, List<String> permissions) {
		Map<String, Boolean> hasPermission = new HashMap<>();
        Group group = lpapi.getGroupManager().getGroup(groupName);
        if (group == null) {
            return false;
        }
		group.getNodes().forEach(node -> permissions.forEach(entry -> {
			if (node.getKey().equalsIgnoreCase(entry)) {
				hasPermission.put(entry, true);
			}
		});
		
			
        if (hasPermission) {
            return true;
        }
        List<String> subGroups = group.getNodes().stream()
                .filter(node -> node instanceof InheritanceNode)
                .map(node -> ((InheritanceNode) node).getGroupName())
                .collect(Collectors.toList());
        for (String subGroupName : subGroups) {
            if (checkGroupPermission(subGroupName, permissions)) {
                return true;
            }
        }
        return false;
    }

	public boolean hasPermission(String playerName, List<String> permissions) {
        User user = lpapi.getUserManager().getUser(playerName);
        if (user == null) {
            return false;
        }
        List<String> groups = user.getNodes().stream()
                .filter(node -> node instanceof InheritanceNode)
                .map(node -> ((InheritanceNode) node).getGroupName())
                .collect(Collectors.toList());
		plugin.getLogger().log(Level.INFO, "Groups: {0}", groups);
        for (String groupName : groups) {
            if (checkGroupPermission(groupName, permissions)) {
                return true;
            }
        }
        return false;
    }

    private boolean checkGroupPermission(String groupName, List<String> permissions) {
        Group group = lpapi.getGroupManager().getGroup(groupName);
        if (group == null) {
            return false;
        }
        boolean hasPermission = group.getNodes().stream()
                .anyMatch(node -> permissions.stream()
					.anyMatch(permission -> node.getKey().equalsIgnoreCase(permission)));
        if (hasPermission) {
            return true;
        }
        List<String> subGroups = group.getNodes().stream()
                .filter(node -> node instanceof InheritanceNode)
                .map(node -> ((InheritanceNode) node).getGroupName())
                .collect(Collectors.toList());
        for (String subGroupName : subGroups) {
            if (checkGroupPermission(subGroupName, permissions)) {
                return true;
            }
        }
        return false;
    }

	/*public boolean hasPermission(String playerName, List<String> permission) {
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
			plugin.getLogger().log(Level.SEVERE, "An error occurred while updating the database: {0}", e.getMessage());
			for (StackTraceElement element : e.getStackTrace()) {
				plugin.getLogger().log(Level.SEVERE, element.toString());
			}
			return false;
		}
	}*/
}