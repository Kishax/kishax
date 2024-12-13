package keyp.forev.fmc.forge.server.cmd.sub;

import java.sql.Connection;
import java.sql.SQLException;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import keyp.forev.fmc.common.database.Database;
import keyp.forev.fmc.common.socket.SocketSwitch;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

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
	
	public int execute(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		String playerName = StringArgumentType.getString(context, "player");
		String args = StringArgumentType.getString(context, "proxy_cmds");
		StringBuilder allcmd = new StringBuilder(); // コマンドを組み立てる
		Entity entity = source.getEntity();
		if (entity instanceof ServerPlayer) {
		    allcmd.append(source.getPlayer()).append(" fv ").append(playerName).append(" ").append(args); // コマンドを打ったプレイヤー名をallcmdに乗せる
		} else {
		    allcmd.append("? fv ").append(playerName).append(" ").append(args); // コンソールから打った場合
		}
		SocketSwitch ssw = sswProvider.get();
		try (Connection conn = db.getConnection()) {
			ssw.sendVelocityServer(conn, allcmd.toString());
		} catch (SQLException | ClassNotFoundException e) {
			source.sendFailure(Component.literal(ChatFormatting.RED + "データベースに接続できませんでした。"));
			logger.error("An error occurred while updating the database: " + e.getMessage(), e);
			for (StackTraceElement element : e.getStackTrace()) {
				logger.error(element.toString());
			}
		}
		return 0;
	}
	
	public void executeProxyCommand(ServerPlayer player, String allcmd) {
		// fmcp <command>
		String playerName = player.getName().toString();
		allcmd = playerName + " fv " + playerName + " " + allcmd;
		SocketSwitch ssw = sswProvider.get();
		try (Connection conn = db.getConnection()) {
			ssw.sendVelocityServer(conn, allcmd);
		} catch (SQLException | ClassNotFoundException e) {
			player.sendSystemMessage(Component.literal(ChatFormatting.RED + "データベースに接続できませんでした。"));
			logger.error("An error occurred while updating the database: " + e.getMessage(), e);
			for (StackTraceElement element : e.getStackTrace()) {
				logger.error(element.toString());
			}
		}
	}
}
