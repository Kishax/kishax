package spigot;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Inject;

public class PlayerUtil {
	private final common.Main plugin;
	private final Database db;
	private final List<String> Players = new CopyOnWriteArrayList<>();
	private boolean isLoaded = false;
	
	@Inject
	public PlayerUtil(common.Main plugin, Database db) {
		this.plugin = plugin;
		this.db = db;
	}
	
	public synchronized void loadPlayers() {
		if (isLoaded) return;
		String query = "SELECT * FROM members;";
		try (Connection conn = db.getConnection();
			 PreparedStatement ps = conn.prepareStatement(query)) {
			try (ResultSet playerlist = ps.executeQuery()) {
				while (playerlist.next()) {
					Players.add(playerlist.getString("name"));
				}
				isLoaded = true;
			}
		} catch (ClassNotFoundException | SQLException e) {
			plugin.getLogger().log(Level.SEVERE, "A ClassNotFoundException | SQLException error occurred: {0}", e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                plugin.getLogger().log(Level.SEVERE, element.toString());
            }
		}
 	}
	
	public void updatePlayers() {
		String query = "SELECT * FROM members;";
		try (Connection conn = db.getConnection();
			 PreparedStatement ps = conn.prepareStatement(query)) {
			try (ResultSet playerlist = ps.executeQuery()) {
				// Playersリストを初期化
				Players.clear();
				while (playerlist.next()) {
					Players.add(playerlist.getString("name"));
				}
			}
		} catch (ClassNotFoundException | SQLException e) {
			plugin.getLogger().log(Level.SEVERE, "A ClassNotFoundException | SQLException error occurred: {0}", e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                plugin.getLogger().log(Level.SEVERE, element.toString());
            }
		}
 	}
	
	public List<String> getPlayerList() {
		return Players;
	}
	
	public String getPlayerUUIDByNameFromDB(String playerName) {
		String query = "SELECT uuid FROM members WHERE name=? ORDER BY id DESC LIMIT 1;";
		try (Connection conn = db.getConnection();
			 PreparedStatement ps = conn.prepareStatement(query)) {
			ps.setString(1, playerName);
			try (ResultSet dbuuid = ps.executeQuery()) {
				if (dbuuid.next()) {
					return dbuuid.getString("uuid");
				}
			}
		} catch (ClassNotFoundException | SQLException e) {
			plugin.getLogger().log(Level.SEVERE, "A ClassNotFoundException | SQLException error occurred: {0}", e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                plugin.getLogger().log(Level.SEVERE, element.toString());
            }
		}
		return null;
	}
	
	public List<String> getPlayerNamesListFromUUIDs(List<String> playerUUIDs) {
		List<String> playerNames = new ArrayList<>();
		Map<String, String> playerUUIDToNameMap = new HashMap<>();
		try (Connection conn = db.getConnection();
			 PreparedStatement ps = conn.prepareStatement("SELECT name, uuid FROM members;")) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					playerUUIDToNameMap.put(rs.getString("uuid"), rs.getString("name"));
				}
			}
			for (String playerUUID : playerUUIDs) {
				playerNames.add(playerUUIDToNameMap.get(playerUUID));
			}
		} catch (SQLException | ClassNotFoundException e) {
			plugin.getLogger().log(Level.SEVERE, "A ClassNotFoundException | SQLException error occurred: {0}", e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                plugin.getLogger().log(Level.SEVERE, element.toString());
            }
		}
		return playerNames;
	}

	public String getPlayerNameByUUIDFromDB(String playerUUID) {
		try (Connection conn = db.getConnection();
			 PreparedStatement ps = conn.prepareStatement("SELECT name FROM members WHERE uuid=? ORDER BY id DESC LIMIT 1;")) {
			ps.setString(1, playerUUID);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return rs.getString("name");
				}
			}
		} catch (SQLException | ClassNotFoundException e) {
			plugin.getLogger().log(Level.SEVERE, "A ClassNotFoundException | SQLException error occurred: {0}", e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                plugin.getLogger().log(Level.SEVERE, element.toString());
            }
		}
		return null;
	}
	
	public UUID getPlayerNameByUUIDFromDB(UUID playerUUID) {
		try (Connection conn = db.getConnection();
			 PreparedStatement ps = conn.prepareStatement("SELECT name FROM members WHERE uuid=? ORDER BY id DESC LIMIT 1;")) {
			ps.setString(1, playerUUID.toString());
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return UUID.fromString(rs.getString("name"));
				}
			}
		} catch (SQLException | ClassNotFoundException e) {
			plugin.getLogger().log(Level.SEVERE, "A ClassNotFoundException | SQLException error occurred: {0}", e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                plugin.getLogger().log(Level.SEVERE, element.toString());
            }
		}
		return null;
	}
	
	public String getPlayerNameFromUUID(UUID uuid) {
        String uuidString = uuid.toString().replace("-", "");
        String urlString = "https://sessionserver.mojang.com/session/minecraft/profile/" + uuidString;
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(urlString))
                    .header("User-Agent", "Mozilla/5.0")
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                // JSONレスポンスを解析
                Gson gson = new Gson();
                JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
                return jsonResponse.get("name").getAsString();
            } else {
            	plugin.getLogger().log(Level.SEVERE, "GETリクエストに失敗しました。HTTPエラーコード: {0}", response.statusCode());
                return null;
            }
        } catch (JsonSyntaxException | IOException | InterruptedException | URISyntaxException e) {
            plugin.getLogger().log(Level.SEVERE, "A JsonSyntaxException | IOException | InterruptedException | URISyntaxException error occurred: {0}", e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                plugin.getLogger().log(Level.SEVERE, element.toString());
            }
            return null;
        }
    }
}
