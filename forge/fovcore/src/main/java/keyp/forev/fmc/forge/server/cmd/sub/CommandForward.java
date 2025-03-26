package keyp.forev.fmc.forge.server.cmd.sub;

import java.sql.Connection;
import java.sql.SQLException;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import keyp.forev.fmc.common.database.Database;
import keyp.forev.fmc.common.socket.message.Message;
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

    if (entity instanceof ServerPlayer) {
      msg.mc.cmd.forward.who.name = source.getTextName();
      msg.mc.cmd.forward.who.system = false;
    } else {
      msg.mc.cmd.forward.who.system = true; // コンソールから打った場合
    }

    SocketSwitch ssw = sswProvider.get();
    try (Connection conn = db.getConnection()) {
      ssw.sendVelocityServer(conn, msg);
    } catch (SQLException | ClassNotFoundException e) {
      source.sendFailure(Component.literal(ChatFormatting.RED + "データベースに接続できませんでした。"));
      logger.error("An error occurred while updating the database: " + e.getMessage(), e);
      for (StackTraceElement element : e.getStackTrace()) {
        logger.error(element.toString());
      }
    }
    return 0;
  }
}
