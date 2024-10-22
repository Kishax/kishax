package velocity;

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
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Inject;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerInfo;

public class PlayerUtil {
	private final ProxyServer server;
	private final DatabaseInterface db;
	private final Logger logger;
	private final List<String> Players = new CopyOnWriteArrayList<>();
	private boolean isLoaded = false;
	
	@Inject
	public PlayerUtil(ProxyServer server, Logger logger, DatabaseInterface db) {
		this.server = server;
		this.logger = logger;
		this.db = db;
	}
	
	public synchronized void loadPlayers() {
		if (isLoaded) return;
		String query = "SELECT * FROM members;";
		try (Connection conn = db.getConnection();
			 PreparedStatement ps = conn.prepareStatement(query)) {
			try (ResultSet playerlist = ps.executeQuery()) {
				while(playerlist.next()) {
					Players.add(playerlist.getString("name"));
				}
				isLoaded = true;
			}
		} catch (ClassNotFoundException | SQLException e) {
			logger.error("A ClassNotFoundException | SQLException error occurred: " + e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                logger.error(element.toString());
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
			logger.error("A ClassNotFoundException | SQLException error occurred: " + e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                logger.error(element.toString());
            }
		}
 	}
	
	public List<String> getPlayerList() {
		return Players;
	}
	
	public Optional<Player> getPlayerByName(String playerName) {
        return server.getAllPlayers().stream()
                .filter(player -> player.getUsername().equalsIgnoreCase(playerName))
                .findFirst();
    }
	
	public Player getPlayerFromUUID(String uuidString) {
		try {
            // StringをUUIDに変換
            UUID uuid = UUID.fromString(uuidString);
            
            // UUIDからプレイヤーを取得
            Optional<Player> player = server.getPlayer(uuid);
            return player.orElse(null); // プレイヤーが見つからない場合はnullを返す
        } catch (IllegalArgumentException e) {
            // UUIDの形式が正しくない場合の処理
            System.out.println("UUIDの形式が無効です: " + uuidString);
            return null;
        }
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
			logger.error("A ClassNotFoundException | SQLException error occurred: " + e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                logger.error(element.toString());
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
			logger.error("A ClassNotFoundException | SQLException error occurred: " + e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                logger.error(element.toString());
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
			logger.error("A ClassNotFoundException | SQLException error occurred: " + e.getMessage());
			for (StackTraceElement element : e.getStackTrace()) {
				logger.error(element.toString());
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
			logger.error("A ClassNotFoundException | SQLException error occurred: " + e.getMessage());
			for (StackTraceElement element : e.getStackTrace()) {
				logger.error(element.toString());
			}
		}
		return null;
	}

	public int getPlayerTime(Player player, ServerInfo serverInfo) {
		String query = "SELECT * FROM log WHERE uuid=? AND `join`=? ORDER BY id DESC LIMIT 1;";
		try (Connection conn = db.getConnection();
			PreparedStatement ps = conn.prepareStatement(query)) {
    		// calc playtime
    		ps.setString(1, player.getUniqueId().toString());
    		ps.setBoolean(2, true);
    		try (ResultSet bj_logs = ps.executeQuery()) {
				if (bj_logs.next()) {
					long now_timestamp = Instant.now().getEpochSecond();
					Timestamp bj_time = bj_logs.getTimestamp("time");
					long bj_timestamp = bj_time.getTime() / 1000L;
					long bj_sa = now_timestamp-bj_timestamp;
					return (int) bj_sa;
				}
			}
    	} catch (SQLException | ClassNotFoundException e1) {
            logger.error("A ClassNotFoundException | SQLException error occurred: " + e1.getMessage());
            for (StackTraceElement element : e1.getStackTrace()) {
                logger.error(element.toString());
            }
        }
		return 0;
	}
	
	public String secondsToStr(int seconds) {
        if (seconds < 60) {
            if (seconds < 10) {
                return String.format("00:00:0%d", seconds);
            }
            return String.format("00:00:%d", seconds);
        } else if (seconds < 3600) {
            int minutes = seconds / 60;
            seconds = seconds % 60;
            return String.format("00:%02d:%02d", minutes, seconds);
        } else {
            int hours = seconds / 3600;
            int remainingSeconds = seconds % 3600;
            int minutes = remainingSeconds / 60;
            seconds = remainingSeconds % 60;
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }
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
            	logger.error("GETリクエストに失敗しました。HTTPエラーコード: " + response.statusCode());
                return null;
            }
        } catch (JsonSyntaxException | IOException | InterruptedException | URISyntaxException e) {
            logger.error("A JsonSyntaxException | IOException | InterruptedException | URISyntaxException error occurred: " + e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                logger.error(element.toString());
            }
            return null;
        }
    }
}
