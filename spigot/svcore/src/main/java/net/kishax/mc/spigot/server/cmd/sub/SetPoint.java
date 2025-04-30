package net.kishax.mc.spigot.server.cmd.sub;

import java.sql.Connection;
import java.sql.SQLException;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.slf4j.Logger;

import com.google.inject.Inject;

import net.kishax.mc.common.database.Database;
import net.kishax.mc.common.server.Luckperms;
import net.kishax.mc.common.server.interfaces.ServerHomeDir;
import net.md_5.bungee.api.ChatColor;

public class SetPoint {
  private final Logger logger;
  private final Database db;
  private final Luckperms lp;
  private final String thisServerName;

  @Inject
  public SetPoint(Logger logger, Database db, Luckperms lp, ServerHomeDir shd) {
    this.logger = logger;
    this.db = db;
    this.lp = lp;
    this.thisServerName = shd.getServerName();
  }

  public void execute(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
    if (sender instanceof Player player) {
      String playerName = player.getName();
      if (lp.getPermLevel(playerName) < 2)  {
        player.sendMessage(ChatColor.RED + "権限がありません。");
        return;
      }
      if (args.length < 1) {
        player.sendMessage(ChatColor.RED + "引数が不足しています。");
        return;
      }
      String type = args[1];
      switch (type) {
        case "load", "room", "hub" -> {
          Location loc = player.getLocation();
          if (loc == null) {
            player.sendMessage(ChatColor.RED + "座標の取得に失敗しました。");
            return;
          }
          double x = loc.getX();
          double y = loc.getY();
          double z = loc.getZ();
          float yaw = loc.getYaw();
          float pitch = loc.getPitch();
          String worldName = null;
          if (loc.getWorld() instanceof World) {
            World playerWorld = loc.getWorld();
            worldName = playerWorld.getName();
          }
          player.sendMessage("""
            座標を取得しました。
            X: %s
            Y: %s
            Z: %s
            Yaw: %s
            Pitch: %s
            World: %s
          """.formatted(x, y, z, yaw, pitch, worldName));

          try (Connection conn = db.getConnection()) {
            db.updateLog(conn, "UPDATE coords SET x = ?, y = ?, z = ?, yaw = ?, pitch = ?, world = ?, server = ? WHERE name = ?", new Object[] {x, y, z, yaw, pitch, worldName, thisServerName, type});
          } catch (SQLException | ClassNotFoundException e) {
            player.sendMessage(ChatColor.RED + "座標の保存に失敗しました。");
            logger.error("A SQLException | ClassNotFoundException error occurred: " + e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
              logger.error(element.toString());
            }
          }
        }
        default -> {
          player.sendMessage(ChatColor.RED + "引数が不正です。");
        }
      }
    } else {
      if (sender != null) {
        sender.sendMessage("プレイヤーのみ実行可能です。");
      }
    }
  }
}
