package velocity_command;

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

import discord.MessageEditorInterface;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import velocity.DatabaseInterface;
import velocity.PlayerDisconnect;

public class Maintenance {

	public static boolean isMente;
	public Connection conn = null, connLp = null;
	public Connection[] conns = {conn, connLp};
	public ResultSet ismente = null, issuperadmin = null;
	public ResultSet[] resultsets = {ismente, issuperadmin};
	public PreparedStatement ps = null;
	public static List<String> args1 = new ArrayList<>(Arrays.asList("switch","status"));
	public static List<String> args2 = new ArrayList<>(Arrays.asList("discord"));
	public static List<String> args3 = new ArrayList<>(Arrays.asList("true","false"));
	private final DatabaseInterface db;
	private final PlayerDisconnect pd;
	private final MessageEditorInterface discordME;
	private final Logger logger;
	private Component component = null;
	
	@Inject
	public Maintenance (
		Logger logger, DatabaseInterface db, PlayerDisconnect pd, 
		MessageEditorInterface discordME
	) {
		this.logger = logger;
		this.db = db;
		this.pd = pd;
		this.discordME = discordME;
	}

	public void execute(@NotNull CommandSource source, String[] args) {
		try {
			conn = db.getConnection();
			connLp = db.getConnection("fmc_lp");
			String sql = "SELECT online FROM status WHERE name=?;";
			ps = conn.prepareStatement(sql);
			ps.setString(1, "Maintenance");
			ismente = ps.executeQuery();
			
			sql = "SELECT uuid FROM lp_user_permissions WHERE permission=?;";
			ps = connLp.prepareStatement(sql);
			ps.setString(1, "group.super-admin");
			issuperadmin = ps.executeQuery();
			
			List<String> superadminUUIDs = new ArrayList<>();
			while(issuperadmin.next()) {
				superadminUUIDs.add(issuperadmin.getString("uuid"));
			}
			
			switch (args.length) {
	        	case 0, 1 -> source.sendMessage(Component.text("usage: /fmcp　maintenance <switch|status> <discord> <true|false>").color(NamedTextColor.GREEN));
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
							
						default -> source.sendMessage(Component.text("usage: /fmcp　maintenance <switch|status> <discord> <true|false>").color(NamedTextColor.GREEN));
					}
                }
	            	
	        	case 3 -> {
					// 以下はパーミッションが所持していることが確認されている上で、permというコマンドを使っているので、確認の必要なし
					//if(args[0].toLowerCase().equalsIgnoreCase("perm"))
					if(!(args1.contains(args[1].toLowerCase()))) {
						source.sendMessage(Component.text("第2引数が不正です。\n").color(NamedTextColor.RED).append(Component.text("usage: /fmcp　maintenance <switch|status> <discord> <true|false>").color(NamedTextColor.GREEN)));
						break;
					}
					
					if(!(args2.contains(args[2].toLowerCase()))) {
						source.sendMessage(Component.text("第3引数が不正です。\n").color(NamedTextColor.RED).append(Component.text("usage: /fmcp　maintenance <switch|status> <discord> <true|false>").color(NamedTextColor.GREEN)));
						break;
					}
					
					source.sendMessage(Component.text("discord通知をtrueにするかfalseにするかを決定してください。\n").color(NamedTextColor.RED).append(Component.text("usage: /fmcp　maintenance <switch|status> <discord> <true|false>").color(NamedTextColor.GREEN)));
				}
        			
	        	case 4 -> {
					if (!(args1.contains(args[1].toLowerCase()))) {
						source.sendMessage(Component.text("第2引数が不正です。\n").color(NamedTextColor.RED).append(Component.text("usage: /fmcp　maintenance <switch|status> <discord> <true|false>").color(NamedTextColor.GREEN)));
						break;
					}
					
					if (!(args2.contains(args[2].toLowerCase()))) {
						source.sendMessage(Component.text("第3引数が不正です。\n").color(NamedTextColor.RED).append(Component.text("usage: /fmcp　maintenance <switch|status> <discord> <true|false>").color(NamedTextColor.GREEN)));
						break;
					}
					
					if (!(args3.contains(args[3].toLowerCase()))) {
						source.sendMessage(Component.text("第4引数が不正です。\n").color(NamedTextColor.RED).append(Component.text("usage: /fmcp　maintenance <switch|status> <discord> <true|false>").color(NamedTextColor.GREEN)));
						break;
					}
					
					switch (args[3].toLowerCase()) {
						case "true"-> {
							// Discord通知をする
							if (ismente.next()) {
								if (ismente.getBoolean("online")) {
									Maintenance.isMente = false; // フラグをtrueに
									discordME.AddEmbedSomeMessage("MenteOff");
									
									// メンテナンスモードが有効の場合
									sql = "UPDATE status SET online=? WHERE name=?;";
									ps = conn.prepareStatement(sql);
									ps.setBoolean(1, false);
									ps.setString(2, "Maintenance");
									ps.executeUpdate();
									source.sendMessage(Component.text("メンテナンスモードが無効になりました。").color(NamedTextColor.GREEN));
								} else {
									Maintenance.isMente = true; // フラグをtrueに
									discordME.AddEmbedSomeMessage("MenteOn");
									
									// メンテナンスモードが無効の場合
									sql = "UPDATE status SET online=? WHERE name=?;";
									ps = conn.prepareStatement(sql);
									ps.setBoolean(1, true);
									ps.setString(2, "Maintenance");
									ps.executeUpdate();
									pd.menteDisconnect(superadminUUIDs);
								}
							}
						}

						case "false"-> {
							// Discord通知をしない
							if (ismente.next()) {
								if (ismente.getBoolean("online")) {
									// メンテナンスモードが有効の場合
									sql = "UPDATE status SET online=? WHERE name=?;";
									ps = conn.prepareStatement(sql);
									ps.setBoolean(1, false);
									ps.setString(2, "Maintenance");
									ps.executeUpdate();
									source.sendMessage(Component.text("メンテナンスモードが無効になりました。").color(NamedTextColor.GREEN));
								} else {
									// メンテナンスモードが無効の場合
									sql = "UPDATE status SET online=? WHERE name=?;";
									ps = conn.prepareStatement(sql);
									ps.setBoolean(1, true);
									ps.setString(2, "Maintenance");
									ps.executeUpdate();
									
									pd.menteDisconnect(superadminUUIDs);
								}
							}
						}
					}
				}
                default -> source.sendMessage(Component.text("usage: /fmcp　maintenance <switch|status> <discord> <true|false>").color(NamedTextColor.GREEN));
	        }
		} catch (ClassNotFoundException | SQLException e) {
            logger.error("A ClassNotFoundException | SQLException error occurred: " + e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) 
            {
                logger.error(element.toString());
            }
        } finally {
			db.close_resource(resultsets, conns, ps);
		}
	}
}
