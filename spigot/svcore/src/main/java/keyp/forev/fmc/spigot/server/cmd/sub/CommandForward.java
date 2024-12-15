package keyp.forev.fmc.spigot.server.cmd.sub;

import java.sql.Connection;
import java.sql.SQLException;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;

import keyp.forev.fmc.common.database.Database;
import keyp.forev.fmc.common.server.Luckperms;
import keyp.forev.fmc.common.socket.SocketSwitch;
import net.md_5.bungee.api.ChatColor;

public class CommandForward {
	private final Database db;
	private final Logger logger;
	private final Provider<SocketSwitch> sswProvider;
	private final Luckperms lp;
	@Inject
	public CommandForward(Database db, Logger logger, Provider<SocketSwitch> sswProvider, Luckperms lp) {
		this.db = db;
		this.logger = logger;
		this.sswProvider = sswProvider;
		this.lp = lp;
	}
	
	public void execute(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
		// fv <player> fmcp <command>
		//String playerName = player.getName();
		//int permLevel = lp.getPermLevel(playerName);

		String allcmd = String.join(" ", args);
		if (sender instanceof Player player) {
			allcmd = player.getName() + " " + allcmd;
			if (!permCheck(player, allcmd)) {
				player.sendMessage(ChatColor.RED + "権限がありません。");
				return;
			}
		} else {
			allcmd = "? " + allcmd;
		}

		if (!patternCheck(allcmd)) {
			sender.sendMessage(ChatColor.RED + "コマンドのパターンが違います。");
			return;
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

	private boolean patternCheck(String res) {
		String pattern = "(\\S+) fv (\\S+) (.+)";
		java.util.regex.Pattern r = java.util.regex.Pattern.compile(pattern);
		java.util.regex.Matcher m = r.matcher(res);
		if (m.find()) {
			return true;
		}
		return false;
	}

	private boolean permCheck(Player player, String res) {
		String pattern = "(\\S+) fv (\\S+) (.+)";
		java.util.regex.Pattern r = java.util.regex.Pattern.compile(pattern);
		java.util.regex.Matcher m = r.matcher(res);
		if (m.find()) {
			String execplayerName = m.group(1);
			String targetPlayerName = m.group(2);
			//String command = m.group(3);
			int permLevel = lp.getPermLevel(player.getName());
			if (permLevel >= 3) {
				return true;
			} else {
				return execplayerName.equals(targetPlayerName);
			}
		}

		return true;
	}
}
