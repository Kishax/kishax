package keyp.forev.fmc.velocity.cmd;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import keyp.forev.fmc.common.Database;
import keyp.forev.fmc.common.Luckperms;
import keyp.forev.fmc.common.PlayerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import keyp.forev.fmc.velocity.discord.DiscordInterface;
import keyp.forev.fmc.velocity.discord.EmojiManager;
import keyp.forev.fmc.velocity.discord.MessageEditorInterface;
import keyp.forev.fmc.velocity.util.BroadCast;
import keyp.forev.fmc.velocity.util.Config;
import keyp.forev.fmc.velocity.util.DoServerOnline;

public class Request implements RequestInterface {
	public static Map<String, Boolean> PlayerReqFlags = new HashMap<>();
	private final ProxyServer server;
	private final Config config;
	private final Logger logger;
	private final Database db;
	private final BroadCast bc;
	private final DiscordInterface discord;
	private final MessageEditorInterface discordME;
	private final EmojiManager emoji;
	private final Luckperms lp;
	private final PlayerUtils pu;
	private final DoServerOnline dso;
	private String currentServerName = null;
	
	@Inject
	public Request (ProxyServer server, Logger logger, Config config, Database db, BroadCast bc, DiscordInterface discord, MessageEditorInterface discordME, EmojiManager emoji, Luckperms lp, PlayerUtils pu, DoServerOnline dso) {
		this.server = server;
		this.logger = logger;
		this.config = config;
		this.db = db;
		this.bc = bc;
		this.discord = discord;
		this.discordME = discordME;
		this.emoji = emoji;
		this.lp = lp;
		this.pu = pu;
		this.dso = dso;
	}

	@Override
	public void execute(@NotNull CommandSource source,String[] args) {
		if (args.length < 2) {
			source.sendMessage(Component.text("引数が足りません。").color(NamedTextColor.RED));
			return;
		}
		String targetServerName = args[1];
		if (!(source instanceof Player)) {
			source.sendMessage(Component.text("このコマンドはプレイヤーのみが実行できます。").color(NamedTextColor.RED));
			return;
		}
		Player player = (Player) source;
		String playerName = player.getUsername(),
			playerUUID = player.getUniqueId().toString();
		if (args.length == 1 || targetServerName == null || targetServerName.isEmpty()) {
			player.sendMessage(Component.text("サーバー名を入力してください。").color(NamedTextColor.RED));
			return;
		}
		player.getCurrentServer().ifPresent(serverConnection -> {
			RegisteredServer registeredServer = serverConnection.getServer();
			currentServerName = registeredServer.getServerInfo().getName();
		});
		boolean containsServer = server.getAllServers().stream()
			.anyMatch(registeredServer -> registeredServer.getServerInfo().getName().equalsIgnoreCase(targetServerName));
		if (!containsServer) {
			player.sendMessage(Component.text("サーバー名が違います。").color(NamedTextColor.RED));
			return;
		}
		int permLevel = lp.getPermLevel(playerName);
		if (permLevel < 1) {
			player.sendMessage(Component.text("権限がありません。").color(NamedTextColor.RED));
			return;
		}
		String query = "SELECT * FROM members WHERE uuid=?;";
		try (Connection conn = db.getConnection();
			PreparedStatement ps = conn.prepareStatement(query)) {
			ps.setString(1,player.getUniqueId().toString());
			try (ResultSet minecrafts = ps.executeQuery()) {
				if (minecrafts.next()) {
					Timestamp beforeReqTime = minecrafts.getTimestamp("req");
					if (beforeReqTime != null) {
						long now_timestamp = Instant.now().getEpochSecond();
						long req_timestamp = beforeReqTime.getTime() / 1000L;
						long req_sa = now_timestamp-req_timestamp;
						long req_sa_minute = req_sa/60;
						if (req_sa_minute <= config.getInt("Interval.Request",0)) {
							player.sendMessage(Component.text("リクエストは"+config.getInt("Interval.Request",0)+"分に1回までです。").color(NamedTextColor.RED));
							return;
						}
					}
				}
			}
			Map<String, Map<String, Object>> statusMap = dso.loadStatusTable(conn);
			statusMap.entrySet().stream()
				.filter(entry -> entry.getKey() instanceof String && entry.getKey().equals(targetServerName))
				.forEach(entry -> {
					Map<String, Object> serverInfo = entry.getValue();
					if (serverInfo.get("online") instanceof Boolean online && online) {
						player.sendMessage(Component.text(targetServerName+"サーバーは起動中です。").color(NamedTextColor.RED));
						logger.info(targetServerName+"サーバーは起動中です。");
						return;
					}
					if (serverInfo.get("exec") instanceof String) {
						if (permLevel < 3 && serverInfo.get("enter") instanceof Boolean enter && !enter) {
							player.sendMessage(Component.text("許可されていません。").color(NamedTextColor.RED));
							return;
						}
						if (serverInfo.get("memory") instanceof Integer memory) {
							int currentUsedMemory = dso.getCurrentUsedMemory(statusMap),
								maxMemory = config.getInt("MaxMemory", 0);
							int futureMemory = currentUsedMemory + memory;
							if (maxMemory < futureMemory) {
								String message = "メモリ超過のため、サーバーを起動できません。(" + futureMemory + "GB/" + maxMemory + "GB)";
								player.sendMessage(Component.text(message).color(NamedTextColor.RED));
								logger.info(message);
								return;
							}
							String query2 = "UPDATE members SET req=CURRENT_TIMESTAMP WHERE uuid=?;";
							try (PreparedStatement ps2 = conn.prepareStatement(query2)) {
								ps2.setString(1,player.getUniqueId().toString());
								int rsAffected2 = ps2.executeUpdate();
								if (rsAffected2 > 0) {
									Request.PlayerReqFlags.put(player.getUniqueId().toString(), true); // フラグを設定
									emoji.createOrgetEmojiId(playerName).thenApply(success -> {
										if (success != null && !success.isEmpty()) {
											String playerEmoji = emoji.getEmojiString(playerName, success);
											discord.sendRequestButtonWithMessage(playerEmoji+playerName+"が"+targetServerName+"サーバーの起動リクエストを送信しました。\n起動しますか？\n(管理者のみ実行可能です。)");
											player.sendMessage(Component.text("送信されました。\n管理者が3分以内に対応しますのでしばらくお待ちくださいませ。").color(NamedTextColor.GREEN));
											TextComponent notifyComponent = Component.text()
												.append(Component.text(playerName+"が"+targetServerName+"サーバーの起動リクエストを送信しました。").color(NamedTextColor.AQUA))
												.build();
											bc.sendExceptPlayerMessage(notifyComponent, playerName);
											discordME.AddEmbedSomeMessage("Request", player, targetServerName);
											try (Connection connection = db.getConnection()) {
												db.insertLog(connection, "INSERT INTO log (name,uuid,server,req,reqserver) VALUES (?,?,?,?,?);" , new Object[] {playerName, playerUUID, currentServerName, true, targetServerName});
											} catch (SQLException | ClassNotFoundException e) {
												logger.error("A SQLException | ClassNotFoundException error occurred: {}", e.getMessage());
												for (StackTraceElement element : e.getStackTrace()) {
													logger.error(element.toString());
												}
											}											
											return true;
										} else {
											return false;
										}
									}).thenAccept(result -> {
										if (result) {
											logger.info(playerName+"が"+targetServerName+"サーバーの起動リクエストを送信しました。");
										} else {
											logger.error("Start Error: Emoji is null or empty.");
										}
									}).exceptionally(ex -> {
										logger.error("Start Error: " + ex.getMessage());
										return null;
									});
								}
							} catch (SQLException e) {
								logger.error("A SQLException error occurred: " + e.getMessage());
								for (StackTraceElement element : e.getStackTrace()) {
									logger.error(element.toString());
								}
							}
						} else {
							player.sendMessage(Component.text("メモリが設定されていないため、サーバーを起動できません。").color(NamedTextColor.RED));
						}
					} else {
						player.sendMessage(Component.text("実行パスが設定されていないため、サーバーを起動できません。").color(NamedTextColor.RED));
					}
			});
		} catch (SQLException | ClassNotFoundException e) {
			logger.error("A SQLException | ClassNotFoundException error occurred: " + e.getMessage());
			for (StackTraceElement element : e.getStackTrace()) {
				logger.error(element.toString());
			}
		}
	}

    @Override
	public String getExecPath(String serverName) {
		String query = "SELECT * FROM status WHERE name=?;";
		try (Connection conn = db.getConnection();
			PreparedStatement ps = conn.prepareStatement(query)) {
			ps.setString(1, serverName);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return rs.getString("exec");
				}
			}
		} catch (SQLException | ClassNotFoundException e) {
			logger.error("A SQLException | ClassNotFoundException error occurred: " + e.getMessage());
			for (StackTraceElement element : e.getStackTrace()) {
				logger.error(element.toString());
			}
		}
		return null;
	}

    @Override
	public Map<String, String> paternFinderMapForReq(String buttonMessage) {
		String pattern = "<(.*?)>(.*?)が(.*?)サーバーの起動リクエストを送信しました。\n起動しますか？(.*?)";
		Pattern compiledPattern = Pattern.compile(pattern);
		Matcher matcher = compiledPattern.matcher(buttonMessage);
		Map<String, String> resultMap = new HashMap<>();
		if (matcher.find()) {
			Optional<String> reqPlayerName = Optional.ofNullable(matcher.group(2));
			Optional<String> reqServerName = Optional.ofNullable(matcher.group(3));
			if (reqPlayerName.isPresent() && reqServerName.isPresent()) {
				String reqPlayerUUID = pu.getPlayerUUIDByNameFromDB(reqPlayerName.get());
				resultMap.put("playerName", reqPlayerName.get());
				resultMap.put("serverName", reqServerName.get());
				resultMap.put("playerUUID", reqPlayerUUID);
			} else {
				logger.error("必要な情報が見つかりませんでした。");
			}
		} else {
			logger.error("パターンに一致するメッセージが見つかりませんでした。");
		}
		return resultMap;
	}
}