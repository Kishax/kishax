package velocity_command;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Random;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentBuilder;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import velocity.Config;
import velocity.DatabaseInterface;
import velocity.Luckperms;

public class SetServer {

	private final ProxyServer server;
	private final Config config;
	private final Logger logger;
	private final DatabaseInterface db;
	private final Luckperms lp;

	@Inject
	public SetServer(ProxyServer server, Logger logger, Config config, DatabaseInterface db, Luckperms lp) {
		this.server = server;
		this.logger = logger;
		this.config = config;
		this.db = db;
		this.lp = lp;
	}
	
	public void execute(@NotNull CommandSource source,String[] args) {
		if (source instanceof Player player) {
            // プレイヤーがコマンドを実行した場合の処理
			String playerName = player.getUsername();
            if (args.length == 1 || Objects.isNull(args[1]) || args[1].isEmpty()) {
            	player.sendMessage(Component.text(NamedTextColor.RED+"サーバー名を入力してください。"));
            	return;
            }
            
            String targetServerName = args[1];
            boolean containsServer = false;
            for (RegisteredServer registeredServer : server.getAllServers()) {
            	if (registeredServer.getServerInfo().getName().equalsIgnoreCase(targetServerName)) {
            		containsServer = true;
            		break;
            	}
            }

            if (!containsServer) {
            	player.sendMessage(Component.text(NamedTextColor.RED+"サーバー名が違います。"));
            	logger.info(NamedTextColor.RED+"サーバー名が違います。");
            	return;
            }
			String query = "SELECT * FROM members WHERE uuid=?;";
			String query2 = "SELECT * FROM status;";
            try (Connection conn = db.getConnection();
				PreparedStatement ps = conn.prepareStatement(query);
				PreparedStatement ps2 = conn.prepareStatement(query2)) {
    			ps.setString(1,player.getUniqueId().toString());
    			try (ResultSet minecrafts = ps.executeQuery();
					ResultSet mine_status = ps2.executeQuery()) {
					boolean isTable = false; // targetServerNameがtableにあるかどうかのフラグ
					int sum_memory = 0; //メモリの確認
					if (!minecrafts.next()) {
						// MySQLサーバーにプレイヤー情報が登録されてなかった場合
						logger.info(player.getUsername()+" のプレイヤー情報がデータベースに登録されていません。");
						player.sendMessage(Component.text(player.getUsername()+"のプレイヤー情報がデータベースに登録されていません。").color(NamedTextColor.RED));
						return;
					}
					while (mine_status.next()) {
						if (!targetServerName.equalsIgnoreCase(mine_status.getString("name"))) {
							if (mine_status.getBoolean("online")) {
								sum_memory = sum_memory + config.getInt("Servers."+mine_status.getString("name")+".memory", 0);
							}
							continue;
						}
						isTable = true;
						// サーバーが配布ワールドを含むかどうか
						if (config.getBoolean("Servers."+targetServerName+".distributed.mode", false)) {
							String distributedUrl = config.getString("Servers."+targetServerName+".distributed.url", "None");
							TextComponent component = Component.text()
										.append(Component.text(targetServerName+"サーバーは、配布ワールドを含みます。").color(NamedTextColor.GOLD).decorate(TextDecoration.UNDERLINED))
										.append(Component.text("\n\n配布元: ").color(NamedTextColor.WHITE))
										.append(Component.text(distributedUrl).color(NamedTextColor.GRAY).decorate(TextDecoration.UNDERLINED))
											.clickEvent(ClickEvent.openUrl(distributedUrl))
										.append(Component.text("\n"))
										.build();
								
							player.sendMessage(component);
						}

						// サーバーがモッドサーバーかどうか
						if (config.getBoolean("Servers."+targetServerName+".modded.mode", false)) {
							String moddedUrl = config.getString("Servers."+targetServerName+".modded.listUrl", "None");
							String loaderType = config.getString("Servers."+targetServerName+".modded.loaderType", "None");
							String loaderUrl = config.getString("Servers."+targetServerName+".modded.loaderUrl", "None");
							ComponentBuilder<TextComponent, ?> builder = Component.text()
								.append(Component.text(targetServerName+"サーバーは、MODサーバーです。").color(NamedTextColor.GOLD).decorate(TextDecoration.UNDERLINED));
							
							if (loaderUrl == null || loaderUrl.isEmpty()) {
								builder
									.append(Component.text("\n\nMODローダー: ").color(NamedTextColor.WHITE))
									.append(Component.text(loaderType).color(NamedTextColor.WHITE).decorate(TextDecoration.UNDERLINED))
									.append(Component.text("\n\nMOD一覧: ").color(NamedTextColor.WHITE))
									.append(Component.text(moddedUrl).color(NamedTextColor.GRAY).decorate(TextDecoration.UNDERLINED)
										.clickEvent(ClickEvent.openUrl(moddedUrl)))
									.append(Component.text("\n"));
							} else {
								builder
									.append(Component.text("\n\nMODローダー: ").color(NamedTextColor.WHITE))
									.append(Component.text(loaderType).color(NamedTextColor.GRAY).decorate(TextDecoration.UNDERLINED)
										.clickEvent(ClickEvent.openUrl(loaderUrl)))
									.append(Component.text("\n\nMOD一覧: ").color(NamedTextColor.WHITE))
									.append(Component.text(moddedUrl).color(NamedTextColor.GRAY).decorate(TextDecoration.UNDERLINED)
										.clickEvent(ClickEvent.openUrl(moddedUrl)))
									.append(Component.text("\n"));
							}

							player.sendMessage(builder.build());
						}
						if (mine_status.getBoolean("online")) {
							// オンライン
							if (minecrafts.getBoolean("confirm")) {
								// fmcアカウントを持っている /stpが使用可能
								// /stpで用いるセッションタイム(現在時刻)(sst)をデータベースに
								LocalDateTime now = LocalDateTime.now();
								DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
								String formattedDateTime = now.format(formatter);
								String query3 = "UPDATE members SET sst=? WHERE uuid=?;";
								try (PreparedStatement ps3 = conn.prepareStatement(query3)) {
									ps3.setString(1,formattedDateTime);
									ps3.setString(2,player.getUniqueId().toString());
									int rsAffected3 = ps3.executeUpdate();
									if (rsAffected3 > 0) {
										//logger.info("rsAffected3: "+rsAffected3); // debug
										TextComponent component = Component.text()
											.append(Component.text(targetServerName+"サーバーは現在").color(NamedTextColor.WHITE))
											.append(Component.text("オンライン").color(NamedTextColor.AQUA))
											.append(Component.text("です。\nサーバーに入りますか？\n").color(NamedTextColor.WHITE))
											.append(Component.text("YES")
													.color(NamedTextColor.GOLD)
													.clickEvent(ClickEvent.runCommand("/fmcp stp "+targetServerName))
													.hoverEvent(HoverEvent.showText(Component.text("(クリックして)"+targetServerName+"サーバーに入ります。"))))
											.append(Component.text(" or ").color(NamedTextColor.GOLD))
											.append(Component.text("NO").color(NamedTextColor.GOLD)
													.clickEvent(ClickEvent.runCommand("/fmcp cancel"))
													.hoverEvent(HoverEvent.showText(Component.text("(クリックして)キャンセルします。"))))
											.build();
										player.sendMessage(component);
									} else {
										player.sendMessage(Component.text("エラーが発生しました。").color(NamedTextColor.RED));
									}
								}
							} else {
								// fmcアカウントを持ってない
								// 6桁の乱数を生成
								Random rnd = new Random();
								int ranum = 100000 + rnd.nextInt(900000);
								String ranumstr = Integer.toString(ranum);
								String query3 = "UPDATE members SET secret2=? WHERE uuid=?;";
								try (PreparedStatement ps3 = conn.prepareStatement(query3)) {
									ps3.setInt(1,ranum);
									ps3.setString(2,player.getUniqueId().toString());
									int rsAffected3 = ps3.executeUpdate();
									if (rsAffected3 > 0) {
										TextComponent component = Component.text()
											.append(Component.text(targetServerName+"サーバーは現在").color(NamedTextColor.WHITE))
											.append(Component.text("オンライン").color(NamedTextColor.AQUA))
											.append(Component.text("です。\nサーバーに参加するには、FMCアカウントとあなたのMinecraftでのUUIDをリンクさせる必要があります。以下、").color(NamedTextColor.WHITE))
											.append(Component.text("UUID認証").color(NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD, TextDecoration.UNDERLINED))
											.append(Component.text("より、手続きを進めてください。").color(NamedTextColor.WHITE))
											.append(Component.text("\nUUID認証\n\n").color(NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD, TextDecoration.UNDERLINED))
											.append(Component.text("https://keypforev.ddns.net/minecraft/uuid_check.php").color(NamedTextColor.GRAY).decorate(TextDecoration.UNDERLINED))
												.clickEvent(ClickEvent.openUrl("https://keypforev.ddns.net/minecraft/uuid_check.php?n="+minecrafts.getInt("id")))
											.append(Component.text("\n\n認証コードは ").color(NamedTextColor.WHITE))
											.append(Component.text(ranumstr).color(NamedTextColor.BLUE)
												.clickEvent(ClickEvent.copyToClipboard(ranumstr))
												.hoverEvent(HoverEvent.showText(Component.text("(クリックして)コピー"))))
											.append(Component.text(" です。").color(NamedTextColor.WHITE)
											.append(Component.text("\n\n認証コードの再生成").color(NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD, TextDecoration.UNDERLINED))
												.clickEvent(ClickEvent.runCommand("/fmcp retry"))
												.hoverEvent(HoverEvent.showText(Component.text("(クリックして)認証コードを再生成します。"))))
											.build();
											player.sendMessage(component);
									} else {
										player.sendMessage(Component.text("エラーが発生しました。").color(NamedTextColor.RED));
									}
								}
							}
							break;
						} else {
							// オフライン
							int permLevel = lp.getPermLevel(playerName);
							if (permLevel >= 1) {
								// 上のwhile文で進んだカーソルの次から最後までの行まで回す
								while (mine_status.next()) {
									// ここ、for文でmine_statusテーブルを回す必要あるかも
									if (mine_status.getBoolean("online")) {
										sum_memory = sum_memory + config.getInt("Servers."+targetServerName+".memory",0);
									}
								}

								// 最後に、起動/起動リクエストしたいサーバーのメモリも足す
								sum_memory = sum_memory + config.getInt("Servers."+targetServerName+".memory",0);
								
								if (!(sum_memory<=config.getInt("Memory_Limit",0))) {
									TextComponent component = Component.text()
												.append(Component.text(targetServerName+"サーバーは現在").color(NamedTextColor.WHITE))
												.append(Component.text("オフライン").color(NamedTextColor.BLUE))
												.append(Component.text("です。").color(NamedTextColor.WHITE))
												.append(Component.text("\nメモリ超過のため、サーバーを起動できません。("+sum_memory+"GB/"+config.getInt("Memory_Limit",0)+"GB)").color(NamedTextColor.RED))
												.build();
									
									player.sendMessage(component);
									return;
								}
								
								// fmcアカウントを持っている
								if (permLevel >= 2) {
									// adminである /startが使用可能
									// /startで用いるセッションタイム(現在時刻)(sst)をデータベースに
									LocalDateTime now = LocalDateTime.now();
									DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
									String formattedDateTime = now.format(formatter);
									
									String query3 = "UPDATE members SET sst=? WHERE uuid=?;";
									try (PreparedStatement ps3 = conn.prepareStatement(query3)) {
										ps3.setString(1,formattedDateTime);
										ps3.setString(2,player.getUniqueId().toString());
										int rsAffected3 = ps3.executeUpdate();
										//logger.info("rsAffected3: "+rsAffected3); // debug
										if (rsAffected3 > 0) {
											TextComponent component = Component.text()
												.append(Component.text(targetServerName+"サーバーは現在").color(NamedTextColor.WHITE))
												.append(Component.text("オフライン").color(NamedTextColor.BLUE))
												.append(Component.text("です。\nサーバーを起動しますか？").color(NamedTextColor.WHITE))
												.append(Component.text("\nYES").color(NamedTextColor.GOLD)
													.clickEvent(ClickEvent.runCommand("/fmcp start "+targetServerName))
													.hoverEvent(HoverEvent.showText(Component.text("(クリックして)"+targetServerName+"サーバーを起動します。"))))
												.append(Component.text(" or ").color(NamedTextColor.GOLD))
												.append(Component.text("NO").color(NamedTextColor.GOLD)
													.clickEvent(ClickEvent.runCommand("/fmcp cancel"))
													.hoverEvent(HoverEvent.showText(Component.text("(クリックして)キャンセルします。"))))
												.build();
									
											player.sendMessage(component);
										}
									}
								} else {
									// adminでない /reqが使用可能
									// /reqで用いるセッションタイム(現在時刻)(sst)をデータベースに
									LocalDateTime now = LocalDateTime.now();
									DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
									String formattedDateTime = now.format(formatter);
									
									String query3 = "UPDATE members SET sst=? WHERE uuid=?;";
									try (PreparedStatement ps3 = conn.prepareStatement(query3)) {
										ps3.setString(1,formattedDateTime);
										ps3.setString(2,player.getUniqueId().toString());
										int rsAffected3 = ps3.executeUpdate();
										if (rsAffected3 > 0) {
											TextComponent component = Component.text()
												.append(Component.text(targetServerName+"サーバーは現在").color(NamedTextColor.WHITE))
												.append(Component.text("オフライン").color(NamedTextColor.BLUE))
												.append(Component.text("です。\n管理者に、サーバー起動のリクエストを送信できます。\n結果は3分以内に返ってきます。\n送信しますか？").color(NamedTextColor.WHITE))
												.append(Component.text("\nYES").color(NamedTextColor.GOLD)
													.clickEvent(ClickEvent.runCommand("/fmcp req "+targetServerName))
													.hoverEvent(HoverEvent.showText(Component.text("(クリックして)"+targetServerName+"サーバー起動リクエストを送信する。"))))
												.append(Component.text(" or ").color(NamedTextColor.GOLD))
												.append(Component.text("NO").color(NamedTextColor.GOLD)
													.clickEvent(ClickEvent.runCommand("/fmcp cancel"))
													.hoverEvent(HoverEvent.showText(Component.text("(クリックして)キャンセルします。"))))
												.build();
											player.sendMessage(component);
										}
									}
								}
							} else {
								// fmcアカウントを持ってない
								// 6桁の乱数を生成
								Random rnd = new Random();
								int ranum = 100000 + rnd.nextInt(900000);
								String ranumstr = Integer.toString(ranum);
								
								String query3 = "UPDATE members SET secret2=? WHERE uuid=?;";
								try (PreparedStatement ps3 = conn.prepareStatement(query3)) {
									ps3.setInt(1,ranum);
									ps3.setString(2,player.getUniqueId().toString());
									int rsAffected3 = ps3.executeUpdate();
									if (rsAffected3 > 0) {
										TextComponent component = Component.text()
										.append(Component.text(targetServerName+"サーバーは現在").color(NamedTextColor.WHITE))
										.append(Component.text("オフライン").color(NamedTextColor.BLUE))
										.append(Component.text("です。\nサーバーを起動して、参加するには、FMCアカウントとあなたのMinecraftでのUUIDをリンクさせる必要があります。以下、").color(NamedTextColor.WHITE))
										.append(Component.text("UUID認証").color(NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD, TextDecoration.UNDERLINED))
										.append(Component.text("より、手続きを進めてください。").color(NamedTextColor.WHITE))
										.append(Component.text("\nUUID認証\n\n").color(NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD, TextDecoration.UNDERLINED))
										.append(Component.text("https://keypforev.ddns.net/minecraft/uuid_check.php").color(NamedTextColor.GRAY).decorate(TextDecoration.UNDERLINED))
											.clickEvent(ClickEvent.openUrl("https://keypforev.ddns.net/minecraft/uuid_check.php?n="+minecrafts.getInt("id")))
										.append(Component.text("\n\n認証コードは ").color(NamedTextColor.WHITE))
										.append(Component.text(ranumstr).color(NamedTextColor.BLUE)
											.clickEvent(ClickEvent.copyToClipboard(ranumstr))
											.hoverEvent(HoverEvent.showText(Component.text("(クリックして)コピー"))))
										.append(Component.text(" です。").color(NamedTextColor.WHITE)
										.append(Component.text("\n\n認証コードの再生成").color(NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD, TextDecoration.UNDERLINED))
											.clickEvent(ClickEvent.runCommand("/fmcp retry"))
											.hoverEvent(HoverEvent.showText(Component.text("(クリックして)認証コードを再生成します。"))))
										.build();
								
										player.sendMessage(component);
									}
								}
							}
						}
						break; // テーブルでサーバーが見つかったら、while-loopを抜ける
					}
					if(!isTable) {
						// MySQLサーバーにサーバーが登録されてなかった場合
						logger.info("このサーバーは、データベースに登録されていません。");
						player.sendMessage(Component.text("このサーバーは、データベースに登録されていません。").color(NamedTextColor.RED));
					}
				}
            } catch (SQLException | ClassNotFoundException e) {
            	logger.error("A SQLException | ClassNotFoundException error occurred: " + e.getMessage());
				for (StackTraceElement element : e.getStackTrace()) {
					logger.error(element.toString());
				}
            }
        } else {
			source.sendMessage(Component.text(NamedTextColor.RED+"このコマンドはプレイヤーのみが実行できます。"));
		}
	}
}