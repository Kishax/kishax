package spigot_command;

import java.sql.Connection;
import java.sql.SQLException;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;

import common.Database;
import common.SocketSwitch;
import net.md_5.bungee.api.ChatColor;

public class CommandForward {
	private final Database db;
	private final Logger logger;
	private final Provider<SocketSwitch> sswProvider;
	@Inject
	public CommandForward(Database db, Logger logger, Provider<SocketSwitch> sswProvider) {
		this.db = db;
		this.logger = logger;
		this.sswProvider = sswProvider;
	}
	
	public void execute(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
		// fv <player> fmcp <command>
		String allcmd = String.join(" ", args);
		if (sender instanceof Player player) {
			allcmd = player.getName() + " " + allcmd; 
		} else {
			allcmd = "? " + allcmd;
		}
		SocketSwitch ssw = sswProvider.get();
		try (Connection conn = db.getConnection()) {
			ssw.sendVelocityServer(conn, allcmd);
		} catch (SQLException | ClassNotFoundException e) {
			if (sender != null) {
				sender.sendMessage(ChatColor.RED + "データベースに接続できませんでした。");
			}
			logger.error("An error occurred while updating the database: " + e.getMessage(), e);
			for (StackTraceElement element : e.getStackTrace()) {
				logger.error(element.toString());
			}
		}
	}

	public void executeProxyCommand(Player player, String allcmd) {
		// fmcp <command>
		String playerName = player.getName();
		allcmd = playerName + " fv " + playerName + " " + allcmd;
		SocketSwitch ssw = sswProvider.get();
		try (Connection conn = db.getConnection()) {
			ssw.sendVelocityServer(conn, allcmd);
		} catch (SQLException | ClassNotFoundException e) {
			player.sendMessage(ChatColor.RED + "データベースに接続できませんでした。");
			logger.error("An error occurred while updating the database: " + e.getMessage(), e);
			for (StackTraceElement element : e.getStackTrace()) {
				logger.error(element.toString());
			}
		}
	}
}
