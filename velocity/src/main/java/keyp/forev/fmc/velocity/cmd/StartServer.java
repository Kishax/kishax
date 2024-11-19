package keyp.forev.fmc.velocity.cmd;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import common.src.main.java.keyp.forev.fmc.main.Database;
import common.src.main.java.keyp.forev.fmc.main.Luckperms;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import velocity.src.main.java.keyp.forev.fmc.core.discord.MessageEditorInterface;
import velocity.src.main.java.keyp.forev.fmc.core.main.BroadCast;
import velocity.src.main.java.keyp.forev.fmc.core.main.Config;
import velocity.src.main.java.keyp.forev.fmc.core.main.DoServerOnline;

public class StartServer {

	private final ProxyServer server;
	private final Config config;
	private final Logger logger;
	private final Database db;
	private final ConsoleCommandSource console;
	private final BroadCast bc;
	private final DoServerOnline dso;
	private final MessageEditorInterface discordME;
	private final Luckperms lp;
	private String currentServerName = null;
	
	@Inject
	public StartServer (ProxyServer server, Logger logger, Config config, Database db, ConsoleCommandSource console, MessageEditorInterface discordME, BroadCast bc, DoServerOnline dso, Luckperms lp) {
		this.server = server;
		this.logger = logger;
		this.config = config;
		this.db = db;
		this.console = console;
		this.bc = bc;
		this.discordME = discordME;
		this.dso = dso;
		this.lp = lp;
	}
	
	public void execute(@NotNull CommandSource source, String[] args) {
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
		if (permLevel < 2) {
			player.sendMessage(Component.text("権限がありません。").color(NamedTextColor.RED));
			return;
		}
		String query = "SELECT * FROM members WHERE uuid=?;";
		try (Connection conn = db.getConnection();
			PreparedStatement ps = conn.prepareStatement(query)) {
			ps.setString(1, player.getUniqueId().toString());
			try (ResultSet minecrafts = ps.executeQuery()) {
				if (minecrafts.next()) {
					Timestamp beforeStartTime = minecrafts.getTimestamp("st");
					if (beforeStartTime != null) {
						long now_timestamp = Instant.now().getEpochSecond();
						long st_timestamp = beforeStartTime.getTime() / 1000L;
						long sa = now_timestamp-st_timestamp;
						long sa_minute = sa/60;
						if (sa_minute <= config.getInt("Interval.Start_Server",0)) {
							player.sendMessage(Component.text("サーバーの起動間隔は"+config.getInt("Interval.Start_Server",0)+"分以上は空けてください。").color(NamedTextColor.RED));
							return;
						}
					}
					Map<String, Map<String, Object>> statusMap = dso.loadStatusTable(conn);
					statusMap.entrySet().stream()
						.filter(entry -> entry.getKey() instanceof String serverName && serverName.equals(targetServerName))
						.forEach(entry -> {
							Map<String, Object> serverInfo = entry.getValue();
							if (serverInfo.get("online") instanceof Boolean online && online) {
								player.sendMessage(Component.text(targetServerName+"サーバーは起動中です。").color(NamedTextColor.RED));
								logger.info(targetServerName+"サーバーは起動中です。");
								return;
							}
							if (serverInfo.get("exec") instanceof String execPath) {
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
									String query3 = "UPDATE members SET st=CURRENT_TIMESTAMP WHERE uuid=?;";
									try (PreparedStatement ps3 = conn.prepareStatement(query3)) {
										ps3.setString(1, player.getUniqueId().toString());
										int rsAffected3 = ps3.executeUpdate();
										if (rsAffected3 > 0) {
											try {
												ProcessBuilder processBuilder = new ProcessBuilder(execPath);
												processBuilder.start();
												String query4 = "UPDATE status SET online=? WHERE name=?;";
												try (PreparedStatement ps4 = conn.prepareStatement(query4)) {
													ps4.setBoolean(1, true);
													ps4.setString(2, targetServerName);
													int rsAffected4 = ps4.executeUpdate();
													if (rsAffected4 > 0) {
														try (Connection connection = db.getConnection()) {
															db.insertLog(connection, "INSERT INTO log (name, uuid, server, sss, status) VALUES (?, ?, ?, ?, ?);", new Object[] {playerName, playerUUID, currentServerName, true, "start"});
														} catch (SQLException | ClassNotFoundException e) {
															logger.error("A SQLException | ClassNotFoundException error occurred: {}", e.getMessage());
															for (StackTraceElement element : e.getStackTrace()) {
																logger.error(element.toString());
															}
														}
														discordME.AddEmbedSomeMessage("Start", player, targetServerName);
														TextComponent component = Component.text()
																	.append(Component.text("WEB認証...PASS\nアドミン認証...PASS\n\nALL CORRECT\n").color(NamedTextColor.GREEN))
																	.append(Component.text(targetServerName+"サーバーがまもなく起動します。").color(NamedTextColor.GREEN))
																	.build();
														player.sendMessage(component);
														TextComponent notifyComponent = Component.text()
															.append(Component.text(player.getUsername()+"が"+targetServerName+"サーバーを起動しました。\nまもなく"+targetServerName+"サーバーが起動します。\n自動転送を希望する方は、自動転送エリアの上でお待ちくださいませ。").color(NamedTextColor.AQUA))
															.build();
														bc.sendExceptPlayerMessage(notifyComponent, player.getUsername());
														console.sendMessage(Component.text(targetServerName+"サーバーがまもなく起動します。").color(NamedTextColor.GREEN));
													}
												}
											} catch (IOException e) {
												player.sendMessage(Component.text("実行時にエラーが発生しました。").color(NamedTextColor.RED));
												logger.error(NamedTextColor.RED+"実行時にエラーが発生しました。");
											}
										} else {
											player.sendMessage(Component.text("内部エラーが発生しました。\nサーバーを起動できません。").color(NamedTextColor.RED));
											logger.error(NamedTextColor.RED+"内部エラーが発生しました。\nサーバーを起動できません。");
										}
									} catch (SQLException e) {
										logger.error("An SQLException error occurred: " + e.getMessage());
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
				} else {
					// MySQLサーバーにプレイヤー情報が登録されてなかった場合
					logger.info(playerName + "のプレイヤー情報がデータベースに登録されていません。");
					player.sendMessage(Component.text(playerName + "のプレイヤー情報がデータベースに登録されていません。").color(NamedTextColor.RED));
				}
			}
		} catch (SQLException | ClassNotFoundException e) {
			logger.error("An SQLException | ClassNotFoundException error occurred: " + e.getMessage());
			for (StackTraceElement element : e.getStackTrace()) {
				logger.error(element.toString());
			}
		}
	}
}