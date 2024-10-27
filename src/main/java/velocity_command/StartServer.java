package velocity_command;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import discord.MessageEditorInterface;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import velocity.BroadCast;
import velocity.Config;
import velocity.DatabaseInterface;

public class StartServer {

	private final ProxyServer server;
	private final Config config;
	private final Logger logger;
	private final DatabaseInterface db;
	private final ConsoleCommandSource console;
	private final BroadCast bc;
	private final MessageEditorInterface discordME;
	private String currentServerName = null;
	
	@Inject
	public StartServer (
		ProxyServer server, Logger logger, Config config,
		DatabaseInterface db, ConsoleCommandSource console, MessageEditorInterface discordME,
		BroadCast bc
	) {
		this.server = server;
		this.logger = logger;
		this.config = config;
		this.db = db;
		this.console = console;
		this.bc = bc;
		this.discordME = discordME;
	}
	
	public void execute(@NotNull CommandSource source,String[] args) {
		String targetServerName = args[1];
		if (source instanceof Player player) {
			if (args.length == 1 || Objects.isNull(targetServerName) || targetServerName.isEmpty()) {
				player.sendMessage(Component.text("サーバー名を入力してください。").color(NamedTextColor.RED));
				return;
			}
			
			// プレイヤーの現在のサーバーを取得
	        player.getCurrentServer().ifPresent(serverConnection -> {
	            RegisteredServer registeredServer = serverConnection.getServer();
	            currentServerName = registeredServer.getServerInfo().getName();
	        });
			
			boolean containsServer = false;
			for (RegisteredServer registeredServer : server.getAllServers()) {
				if (registeredServer.getServerInfo().getName().equalsIgnoreCase(targetServerName)) {
					containsServer = true;
					break;
				}
			}

			if (!containsServer) {
		        player.sendMessage(Component.text("サーバー名が違います。").color(NamedTextColor.RED));
			} else {
				String query = "SELECT * FROM members WHERE uuid=?;",
					query2 = "SELECT * FROM status WHERE name=?;";
				try (Connection conn = db.getConnection();
					PreparedStatement ps = conn.prepareStatement(query);
					PreparedStatement ps2 = conn.prepareStatement(query2)) {
	    			ps.setString(1,player.getUniqueId().toString());
					ps2.setString(1,targetServerName);
					try (ResultSet minecrafts = ps.executeQuery();
						ResultSet mine_status = ps2.executeQuery()) {
						if (minecrafts.next()) {
							//初参加のプレイヤーのsst,req,stカラムはnull値を返すので
							if (Objects.nonNull(minecrafts.getTimestamp("sst")) && Objects.nonNull(minecrafts.getTimestamp("st"))) {
								long now_timestamp = Instant.now().getEpochSecond();
								
								// /ssで発行したセッションタイムをみる
								Timestamp sst_timeget = minecrafts.getTimestamp("sst");
								long sst_timestamp = sst_timeget.getTime() / 1000L;
								
								long ss_sa = now_timestamp-sst_timestamp;
								long ss_sa_minute = ss_sa/60;
								if (ss_sa_minute>=config.getInt("Interval.Session",3)) {
									player.sendMessage(Component.text("セッションが無効です。").color(NamedTextColor.RED));
									return;
								}
								
								// /startを実行したときに発行したセッションタイムをみる
								Timestamp st_timeget = minecrafts.getTimestamp("st");
								long st_timestamp = st_timeget.getTime() / 1000L;
								
								long sa = now_timestamp-st_timestamp;
								long sa_minute = sa/60;
								
								if (sa_minute<=config.getInt("Interval.Start_Server",0)) {
									player.sendMessage(Component.text("サーバーの起動間隔は"+config.getInt("Interval.Start_Server",0)+"分以上は空けてください。").color(NamedTextColor.RED));
									return;
								}
							}
							
							if (mine_status.next()) {
								if (mine_status.getBoolean("online")) {
									player.sendMessage(Component.text(targetServerName+"サーバーは起動中です。").color(NamedTextColor.RED));
									logger.info(NamedTextColor.RED+targetServerName+"サーバーは起動中です。");
								} else {
									if (config.getString("Servers."+targetServerName+".exec","").isEmpty() || !config.getBoolean("Servers."+args[1]+".entry", false)) {
										player.sendMessage(Component.text("許可されていません。").color(NamedTextColor.RED));
										return;
									}
									
									// 全サーバーにプレイヤーがサーバーを起動したことを通知
									TextComponent notifyComponent = Component.text()
											.append(Component.text(player.getUsername()+"が"+targetServerName+"サーバーを起動しました。\nまもなく"+targetServerName+"サーバーが起動します。").color(NamedTextColor.AQUA))
											.build();
									bc.sendExceptPlayerMessage(notifyComponent, player.getUsername());
									
									// stにセッションタイムを入れる
									String query3 = "UPDATE members SET st=CURRENT_TIMESTAMP WHERE uuid=?;";
									try (PreparedStatement ps3 = conn.prepareStatement(query3)) {
										ps3.setString(1,player.getUniqueId().toString());
										int rsAffected3 = ps3.executeUpdate();
										if (rsAffected3 > 0) {
											// バッチファイルのパスを指定
											try {
												String execFilePath = config.getString("Servers."+targetServerName+".exec");
												ProcessBuilder processBuilder = new ProcessBuilder(execFilePath);
												processBuilder.start();

												String query4 = "UPDATE status SET online=? WHERE name=?;",
												query5 = "INSERT INTO log (name, uuid, server, sss, status) VALUES (?, ?, ?, ?, ?);";
											try (PreparedStatement ps4 = conn.prepareStatement(query4);
												PreparedStatement ps5 = conn.prepareStatement(query5)) {
												ps4.setBoolean(1, true);
												ps4.setString(2, targetServerName);
												
												ps5.setString(1, player.getUsername());
												ps5.setString(2, player.getUniqueId().toString());
												ps5.setString(3, currentServerName);
												ps5.setBoolean(4, true);
												ps5.setString(5, "start");
												int rsAffected4 = ps4.executeUpdate(),
													rsAffected5 = ps5.executeUpdate();
												if (rsAffected4 > 0 && rsAffected5 > 0) {
													discordME.AddEmbedSomeMessage("Start", player, targetServerName);
													TextComponent component = Component.text()
																.append(Component.text("UUID認証...PASS\n\nアドミン認証...PASS\n\nALL CORRECT\n\n").color(NamedTextColor.GREEN))
																.append(Component.text(targetServerName+"サーバーがまもなく起動します。").color(NamedTextColor.GREEN))
																.build();
													player.sendMessage(component);
													console.sendMessage(Component.text(targetServerName+"サーバーがまもなく起動します。").color(NamedTextColor.GREEN));
												}
											}
											} catch (IOException e) {
												player.sendMessage(Component.text("内部エラーが発生しました。\n実行パスが範囲外です。").color(NamedTextColor.RED));
												logger.error(NamedTextColor.RED+"内部エラーが発生しました。\n実行パスが範囲外です。");
											}
										} else {
											player.sendMessage(Component.text("内部エラーが発生しました。\nサーバーを起動できません。").color(NamedTextColor.RED));
											logger.error(NamedTextColor.RED+"内部エラーが発生しました。\nサーバーを起動できません。");
										}
									}
								}
							}
						} else {
							// MySQLサーバーにプレイヤー情報が登録されてなかった場合
							logger.info(NamedTextColor.RED+"あなたのプレイヤー情報がデータベースに登録されていません。");
							player.sendMessage(Component.text(player.getUsername()+"のプレイヤー情報がデータベースに登録されていません。").color(NamedTextColor.RED));
						}
					}
		        } catch (SQLException | ClassNotFoundException e) {
		            logger.error("An IOException | SQLException | ClassNotFoundException error occurred: " + e.getMessage());
					for (StackTraceElement element : e.getStackTrace()) {
						logger.error(element.toString());
					}
		        }
			}
		} else {
			source.sendMessage(Component.text("このコマンドはプレイヤーのみが実行できます。").color(NamedTextColor.RED));
		}
	}
}