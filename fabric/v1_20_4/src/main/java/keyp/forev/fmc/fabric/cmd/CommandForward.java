package keyp.forev.fmc.fabric.cmd;

import java.sql.Connection;
import java.sql.SQLException;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import keyp.forev.fmc.common.Database;
import keyp.forev.fmc.common.SocketSwitch;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

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
	
	public void execute(CommandContext<ServerCommandSource> context) {
		// fv <player> fmcp <command>
		ServerCommandSource source = context.getSource();
		String playerName = StringArgumentType.getString(context, "player"),
			proxyCmd = StringArgumentType.getString(context, "proxy_cmds");
		StringBuilder allcmd = new StringBuilder();
		Entity entity = source.getEntity();
		if (entity instanceof PlayerEntity player) {
			allcmd.append(player.getName().getString()).append(" fv ").append(playerName).append(" ").append(proxyCmd);
		} else {
			allcmd.append("? fv ").append(playerName).append(" ").append(proxyCmd);
		}
		SocketSwitch ssw = sswProvider.get();
		try (Connection conn = db.getConnection()) {
			ssw.sendVelocityServer(conn, allcmd.toString());
		} catch (SQLException | ClassNotFoundException e) {
			if (entity != null) {
				entity.sendMessage(Text.literal("データベースに接続できませんでした。").formatted(Formatting.RED));
			}
			logger.error("An error occurred while updating the database: " + e.getMessage(), e);
			for (StackTraceElement element : e.getStackTrace()) {
				logger.error(element.toString());
			}
		}
	}

	public void executeProxyCommand(PlayerEntity player, String allcmd) {
		// fmcp <command>
		String playerName = player.getName().toString();
		allcmd = playerName + " fv " + playerName + " " + allcmd;
		SocketSwitch ssw = sswProvider.get();
		try (Connection conn = db.getConnection()) {
			ssw.sendVelocityServer(conn, allcmd);
		} catch (SQLException | ClassNotFoundException e) {
			player.sendMessage(Text.literal("データベースに接続できませんでした。").formatted(Formatting.RED));
			logger.error("An error occurred while updating the database: " + e.getMessage(), e);
			for (StackTraceElement element : e.getStackTrace()) {
				logger.error(element.toString());
			}
		}
	}
}
