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
import keyp.forev.fmc.common.socket.message.Message;
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
		String cmds = String.join(" ", args);
        String targetPlayerName = args[1];
        Message msg = new Message();
        msg.mc = new Message.Minecraft();
        msg.mc.cmd = new Message.Minecraft.Command();
        msg.mc.cmd.forward = new Message.Minecraft.Command.Forward();
        msg.mc.cmd.forward.cmd = cmds;
        msg.mc.cmd.forward.target = targetPlayerName;
        msg.mc.cmd.forward.who = new Message.Minecraft.Who();

		if (sender instanceof Player player) {
            String playerName = player.getName();

			int permLevel = lp.getPermLevel(playerName);
			if (!(permLevel >= 3 || playerName.equals(targetPlayerName))) {
				player.sendMessage(ChatColor.RED + "権限がありません。");
				return;
			}

            msg.mc.cmd.forward.who.name = playerName;
            msg.mc.cmd.forward.who.system = false;
		} else {
		    msg.mc.cmd.forward.who.system = true; // コンソールから打った場合
		}

		SocketSwitch ssw = sswProvider.get();
		try (Connection conn = db.getConnection()) {
			ssw.sendVelocityServer(conn, msg);
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

	public void executeProxyCommand(Player player, String cmds) {
		String playerName = player.getName();

        Message msg = new Message();
        msg.mc = new Message.Minecraft();
        msg.mc.cmd = new Message.Minecraft.Command();
        msg.mc.cmd.forward = new Message.Minecraft.Command.Forward();
        msg.mc.cmd.forward.cmd = cmds;
        msg.mc.cmd.forward.target = playerName;
        msg.mc.cmd.forward.who = new Message.Minecraft.Who();
        msg.mc.cmd.forward.who.name = playerName;
        msg.mc.cmd.forward.who.system = false;

		SocketSwitch ssw = sswProvider.get();
		try (Connection conn = db.getConnection()) {
			ssw.sendVelocityServer(conn, msg);
		} catch (SQLException | ClassNotFoundException e) {
			player.sendMessage(ChatColor.RED + "データベースに接続できませんでした。");
			logger.error("An error occurred while updating the database: " + e.getMessage(), e);
			for (StackTraceElement element : e.getStackTrace()) {
				logger.error(element.toString());
			}
		}
	}
}
