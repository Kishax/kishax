package net.kishax.mc.fabric.server.cmd.sub;

import java.sql.Connection;
import java.sql.SQLException;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.kishax.mc.common.database.Database;
import net.kishax.mc.common.socket.SocketSwitch;
import net.kishax.mc.common.socket.message.Message;
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
    ServerCommandSource source = context.getSource();
    String targetPlayerName = StringArgumentType.getString(context, "player");
    String cmds = StringArgumentType.getString(context, "proxy_cmds");
    Entity entity = source.getEntity();

    Message msg = new Message();
    msg.mc = new Message.Minecraft();
    msg.mc.cmd = new Message.Minecraft.Command();
    msg.mc.cmd.forward = new Message.Minecraft.Command.Forward();
    msg.mc.cmd.forward.cmd = cmds;
    msg.mc.cmd.forward.target = targetPlayerName;
    msg.mc.cmd.forward.who = new Message.Minecraft.Who();

    if (entity instanceof PlayerEntity player) {
      msg.mc.cmd.forward.who.name = player.getName().getString();
      msg.mc.cmd.forward.who.system = false;
    } else {
      msg.mc.cmd.forward.who.system = true; // コンソールから打った場合
    }

    SocketSwitch ssw = sswProvider.get();
    try (Connection conn = db.getConnection()) {
      ssw.sendVelocityServer(conn, msg);
    } catch (SQLException | ClassNotFoundException e) {
      source.sendError(Text.literal("データベースに接続できませんでした。").formatted(Formatting.RED));
      logger.error("An error occurred while updating the database: " + e.getMessage(), e);
      for (StackTraceElement element : e.getStackTrace()) {
        logger.error(element.toString());
      }
    }
  }
}
