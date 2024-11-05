package velocity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.slf4j.Logger;

import com.google.inject.Inject;

import common.Database;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.messaging.MessagingService;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;

public class Luckperms {
	private final Logger logger;
	private final Database db;
	private final LuckPerms lpapi = LuckPermsProvider.get();
	private final PlayerUtils pu;
	
	@Inject
	public Luckperms(Logger logger, Database db, PlayerUtils pu) {
		this.logger = logger;
		this.db = db;
		this.pu = pu;
	}
	
	public void triggerNetworkSync() {
        MessagingService messagingService = lpapi.getMessagingService().orElse(null);
        if (messagingService != null) {
            messagingService.pushUpdate();
            logger.info("LuckPerms network sync triggered.");
        } else {
        	logger.error("Failed to get LuckPerms MessagingService.");
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
			logger.error("An error occurred while updating the database: " + e.getMessage());
			for (StackTraceElement element : e.getStackTrace()) {
				logger.error(element.toString());
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

	public int getPermLevel(String playerName) {
        int permLevel = 0;
        List<String> groups = new ArrayList<>(Arrays.asList("group.new-fmc-user", "group.sub-admin", "group.super-admin"));
        Map<String, Boolean> permMap = hasPermissionWithMap(playerName, groups);
        if (permMap.get("group.super-admin")) {
            permLevel = 3;
        } else if (permMap.get("group.sub-admin")) {
            permLevel = 2;
        } else if (permMap.get("group.new-fmc-user")) {
            permLevel = 1;
        }
        return permLevel;
    }

	public Map<String, Boolean> hasPermissionWithMap(String playerName, List<String> permissions) {
		User user = lpapi.getUserManager().getUser(playerName);
		if (user == null) {
			return permissions.stream().collect(Collectors.toMap(permission -> permission, _ -> false));
		}
		List<String> groups = user.getNodes().stream()
				.filter(node -> node instanceof InheritanceNode)
				.map(node -> ((InheritanceNode) node).getGroupName())
				.collect(Collectors.toList());
		//logger.info("Groups: " + groups);
		Map<String, Boolean> hasPermissionBools = permissions.stream().collect(Collectors.toMap(permission -> permission, _ -> false));
    	CopyOnWriteArrayList<String> permissionsCopy = new CopyOnWriteArrayList<>(permissions);
		permissions.stream()
			.filter(perm -> perm.startsWith("group."))
			.forEach(perm -> {
				String groupName = perm.substring(6);
				if (groups.contains(groupName)) {
					hasPermissionBools.put(perm, true);
					permissionsCopy.remove(perm);
				}
			});
		for (String groupName : groups) {
            checkGroupPermissionWithMap(groupName, permissionsCopy, hasPermissionBools);
        }
		//logger.info("permMap: " + hasPermissionBools);
		return hasPermissionBools;
	}
	
	private void checkGroupPermissionWithMap(String groupName, List<String> permissions, Map<String, Boolean> hasPermissionBools) {
        Group group = lpapi.getGroupManager().getGroup(groupName);
        if (group == null) {
            return;
        }
		group.getNodes().forEach(node -> permissions.forEach(entry -> {
			if (node.getKey().equalsIgnoreCase(entry)) {
				hasPermissionBools.put(entry, true);
			}
		}));
        List<String> subGroups = group.getNodes().stream()
                .filter(node -> node instanceof InheritanceNode)
                .map(node -> ((InheritanceNode) node).getGroupName())
                .collect(Collectors.toList());
        for (String subGroupName : subGroups) {
            checkGroupPermissionWithMap(subGroupName, permissions, hasPermissionBools);
        }
    }

	public boolean hasPermission(String playerName, String permission) {
		return hasPermission(playerName, List.of(permission));
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
		boolean hasGroupPermission = permissions.stream()
				.filter(perm -> perm.startsWith("group."))
				.map(perm -> perm.substring(6))
				.anyMatch(groups::contains);
		if (hasGroupPermission) {
			return true;
		}
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
}