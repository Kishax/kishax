package keyp.forev.fmc.velocity.cmd.sub;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import keyp.forev.fmc.common.database.Database;
import keyp.forev.fmc.velocity.discord.MessageEditor;
import keyp.forev.fmc.velocity.server.PlayerDisconnect;

public class Maintenance {
	public static boolean isMente;
	public static List<String> args1 = new ArrayList<>(Arrays.asList("switch", "status", "add", "remove", "list"));
	public static List<String> args2 = new ArrayList<>(Arrays.asList("discord"));
	public static List<String> args3 = new ArrayList<>(Arrays.asList("true", "false"));
	private final Database db;
	private final PlayerDisconnect pd;
	private final MessageEditor discordME;
	private final Logger logger;
	private Component component = null;
	
	@Inject
	public Maintenance (Logger logger, Database db, PlayerDisconnect pd, MessageEditor discordME) {
		this.logger = logger;
		this.db = db;
		this.pd = pd;
		this.discordME = discordME;
	}

	public void execute(@NotNull CommandSource source, String[] args) {
		String query = "SELECT online FROM status WHERE name=?;";
		String query2 = "SELECT uuid FROM lp_user_permissions WHERE permission=?;";
		try (Connection conn = db.getConnection(); 
			Connection connLp = db.getConnection("fmc_lp");
			PreparedStatement ps = conn.prepareStatement(query);
			PreparedStatement ps2 = connLp.prepareStatement(query2)) {
			ps.setString(1, "maintenance");
			ps2.setString(1, "group.super-admin");
			try (ResultSet ismente = ps.executeQuery();
				ResultSet issuperadmin = ps2.executeQuery()) {
				List<String> superadminUUIDs = new ArrayList<>();
				while(issuperadmin.next()) {
					superadminUUIDs.add(issuperadmin.getString("uuid"));
				}
				switch (args.length) {
					case 0, 1 -> source.sendMessage(Component.text("usage: /fmcp maintenance <switch|status|list|add|remove> <player|discord> <true|false>").color(NamedTextColor.GREEN));
					case 2 -> {
						switch(args[1].toLowerCase()) {
							case "status" -> {
								if (ismente.next()) {
									if(ismente.getBoolean("online")) {
										component = Component.text("現在メンテナンス中です。").color(NamedTextColor.GREEN);
									} else {
										component = Component.text("現在メンテナンス中ではありません。").color(NamedTextColor.GREEN);
									}
								}
	
								source.sendMessage(component);
							}
							case "switch" -> source.sendMessage(Component.text("usage: /fmcp maintenance switch discord <true|false>").color(NamedTextColor.GREEN));
							case "add", "remove" -> source.sendMessage(Component.text("usage: /fmcp maintenance <add|remove> <player>").color(NamedTextColor.GREEN));
							case "list" -> {
								source.sendMessage(Component.text("スーパーアドミンに加え、サーバー参加許可メンバ一覧(メンテナンス中)").color(NamedTextColor.WHITE));
								List<String> menteAllowMembers = getMenteAllowMembers(conn);
								if (menteAllowMembers.isEmpty()) {
									source.sendMessage(Component.text("メンテナンスモードの許可メンバーはいません。").color(NamedTextColor.RED));
								} else {
									menteAllowMembers.forEach(member -> {
										source.sendMessage(Component.text("- " + member).color(NamedTextColor.GOLD));
									});
								}
							}
							default -> source.sendMessage(Component.text("usage: /fmcp maintenance <switch|status|list|add|remove> <player|discord> <true|false>").color(NamedTextColor.GREEN));
						}
					}
					case 3 -> {
						switch (args[1].toLowerCase()) {
							case "add" -> {
								if (getMenteAllowMembers().contains(args[2])) {
									source.sendMessage(Component.text("既に追加されています。").color(NamedTextColor.RED));
									break;
								}
								updateMenteMembers(args[2], true);
								source.sendMessage(Component.text("追加しました。").color(NamedTextColor.GREEN));
							}
							case "remove" -> {
								if (!getMenteAllowMembers().contains(args[2])) {
									source.sendMessage(Component.text("追加されていません。").color(NamedTextColor.RED));
									break;
								}
								updateMenteMembers(args[2], false);
								source.sendMessage(Component.text("削除しました。").color(NamedTextColor.GREEN));
							}
							case "switch" -> {
								if(!(args1.contains(args[1].toLowerCase()))) {
									source.sendMessage(Component.text("第2引数が不正です。").color(NamedTextColor.RED).append(Component.text("usage: /fmcp maintenance <switch|status> <discord> <true|false>").color(NamedTextColor.GREEN)));
									break;
								}
								if(!(args2.contains(args[2].toLowerCase()))) {
									source.sendMessage(Component.text("第3引数が不正です。").color(NamedTextColor.RED).append(Component.text("usage: /fmcp maintenance <switch|status> <discord> <true|false>").color(NamedTextColor.GREEN)));
									break;
								}
								source.sendMessage(Component.text("discord通知をtrueにするかfalseにするかを決定してください。").color(NamedTextColor.RED).append(Component.text("usage: /fmcp maintenance <switch|status> <discord> <true|false>").color(NamedTextColor.GREEN)));
							}
							default -> source.sendMessage(Component.text("usage: /fmcp maintenance <switch|status|list|add|remove> <player|discord> <true|false>").color(NamedTextColor.GREEN));
						}
					}
					case 4 -> {
						switch (args[1].toLowerCase()) {
							case "switch" -> {
								if (!(args1.contains(args[1].toLowerCase()))) {
									source.sendMessage(Component.text("第2引数が不正です。").color(NamedTextColor.RED).append(Component.text("usage: /fmcp maintenance <switch|status> <discord> <true|false>").color(NamedTextColor.GREEN)));
									break;
								}
								if (!(args2.contains(args[2].toLowerCase()))) {
									source.sendMessage(Component.text("第3引数が不正です。").color(NamedTextColor.RED).append(Component.text("usage: /fmcp maintenance <switch|status> <discord> <true|false>").color(NamedTextColor.GREEN)));
									break;
								}
								if (!(args3.contains(args[3].toLowerCase()))) {
									source.sendMessage(Component.text("第4引数が不正です。").color(NamedTextColor.RED).append(Component.text("usage: /fmcp maintenance <switch|status> <discord> <true|false>").color(NamedTextColor.GREEN)));
									break;
								}
								if (!(args[3].equals("true") || args[3].equals("false"))) {
									source.sendMessage(Component.text("trueかfalseを入力してください。").color(NamedTextColor.RED).append(Component.text("usage: /fmcp maintenance <switch|status> <discord> <true|false>").color(NamedTextColor.GREEN)));
									break;
								}
		
								boolean isDiscord = Boolean.parseBoolean(args[3]);
								if (ismente.next()) {
									if (ismente.getBoolean("online")) {
										Maintenance.isMente = false; // フラグをtrueに
										if (isDiscord) {
											try {
												discordME.AddEmbedSomeMessage("MenteOff");
											} catch (Exception e) {
												logger.error("An exception occurred while executing the AddEmbedSomeMessage method: {}", e.getMessage());
												for (StackTraceElement ste : e.getStackTrace()) {
													logger.error(ste.toString());
												}
											}
										}
										// メンテナンスモードが有効の場合
										String query3 = "UPDATE status SET online=? WHERE name=?;";
										try (PreparedStatement ps3 = conn.prepareStatement(query3)) {
											ps3.setBoolean(1, false);
											ps3.setString(2, "maintenance");
											int rsAffected3 = ps3.executeUpdate();
											if (rsAffected3 > 0) {
												source.sendMessage(Component.text("メンテナンスモードが無効になりました。").color(NamedTextColor.GREEN));
											}
										}
									} else {
										Maintenance.isMente = true; // フラグをtrueに
										if (isDiscord) {
											try {
												discordME.AddEmbedSomeMessage("MenteOn");
											} catch (Exception e) {
												logger.error("An exception occurred while executing the AddEmbedSomeMessage method: {}", e.getMessage());
												for (StackTraceElement ste : e.getStackTrace()) {
													logger.error(ste.toString());
												}
											}
										}
										// メンテナンスモードが無効の場合
										String query3 = "UPDATE status SET online=? WHERE name=?;";
										try (PreparedStatement ps3 = conn.prepareStatement(query3)) {
											ps3.setBoolean(1, true);
											ps3.setString(2, "maintenance");
											int rsAffected3 = ps3.executeUpdate();
											if (rsAffected3 > 0) {
												pd.menteDisconnect(superadminUUIDs);
											}
										}
									}
								}
							}
						}
					}
				}
			}
		} catch (ClassNotFoundException | SQLException e) {
            logger.error("A ClassNotFoundException | SQLException error occurred: " + e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) 
            {
                logger.error(element.toString());
            }
        }
	}
	
	public List<String> getMenteNotAllowMembers(Connection conn) {
		return getMenteMembers(conn, false);
	}

	public List<String> getMenteNotAllowMembers() {
		return getMenteMembers(null, false);
	}

	public List<String> getMenteAllowMembers() {
		return getMenteMembers(null, true);
	}

	public List<String> getMenteAllowMembers(Connection conn) {
		return getMenteMembers(conn, true);
	}

	private List<String> getMenteMembers(Connection conn, boolean mententer) {
		List<String> members = new ArrayList<>();
		try (Connection connection = (conn != null && !conn.isClosed()) ? conn : db.getConnection();
			PreparedStatement ps3 = connection.prepareStatement("SELECT * FROM members WHERE mententer=?;")) {
			ps3.setBoolean(1, mententer);
			try (ResultSet rs = ps3.executeQuery()) {
				while (rs.next()) {
					members.add(rs.getString("name"));
				}
			}
		} catch (SQLException | ClassNotFoundException e) {
			logger.error("An SQLException error occurred: " + e.getMessage());
			for (StackTraceElement element : e.getStackTrace()) {
				logger.error(element.toString());
			}
		}
		return members;
	}

	public void updateMenteMembers(String name, boolean mententer) {
		updateMenteMembers(null, name, mententer);
	}
	
	public void updateMenteMembers(Connection conn, String name, boolean mententer) {
		String query = "UPDATE members SET mententer=? WHERE name=?;";
		try (Connection connection = (conn != null && !conn.isClosed()) ? conn : db.getConnection();
			PreparedStatement ps = connection.prepareStatement(query)) {
			ps.setBoolean(1, mententer);
			ps.setString(2, name);
			int rsAffected = ps.executeUpdate();
			if (rsAffected > 0) {
				logger.info("メンテナンスモードの許可メンバーを更新しました。");
			}
		} catch (SQLException | ClassNotFoundException e) {
			logger.error("An SQLException error occurred: " + e.getMessage());
			for (StackTraceElement element : e.getStackTrace()) {
				logger.error(element.toString());
			}
		}
	}
}
