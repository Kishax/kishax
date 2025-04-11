package f5.si.kishax.mc.spigot.server.cmd.sub.teleport;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import com.google.inject.Inject;
import com.google.inject.Provider;

import f5.si.kishax.mc.common.database.Database;
import f5.si.kishax.mc.common.server.Luckperms;
import f5.si.kishax.mc.common.socket.SocketSwitch;
import f5.si.kishax.mc.common.socket.message.Message;
import f5.si.kishax.mc.spigot.server.events.EventListener;
import f5.si.kishax.mc.spigot.settings.Coords;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatColor;
import org.slf4j.Logger;

public class TeleportBack implements TabExecutor {
  private final BukkitAudiences audiences;
  private final Logger logger;
  private final Database db;
  private final Luckperms lp;
  private final Provider<SocketSwitch> sswProvider;

  @Inject
  public TeleportBack(BukkitAudiences audiences, Logger logger, Database db, Luckperms lp, Provider<SocketSwitch> sswProvider) {
    this.audiences = audiences;
    this.logger = logger;
    this.db = db;
    this.lp = lp;
    this.sswProvider = sswProvider;
  }

  @Override
  public boolean onCommand(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
    if (sender instanceof Player player) {
      String playerName = player.getName();
      int permLevel = lp.getPermLevel(playerName);
      if (permLevel < 1) {
        player.sendMessage(ChatColor.RED + "まだWEB認証が完了していません。");
        player.teleport(Coords.ROOM_POINT.getLocation());
      } else {
        if (EventListener.playerBeforeLocationMap.containsKey(player)) {
          Message msg = new Message();
          msg.mc = new Message.Minecraft();
          msg.mc.cmd = new Message.Minecraft.Command();
          msg.mc.cmd.teleport = new Message.Minecraft.Command.Teleport();
          msg.mc.cmd.teleport.point = new Message.Minecraft.Command.Teleport.Point();
          msg.mc.cmd.teleport.point.who = new Message.Minecraft.Who();
          msg.mc.cmd.teleport.point.who.name = playerName;
          msg.mc.cmd.teleport.point.back = true;

          SocketSwitch ssw = sswProvider.get();
          try (Connection conn = db.getConnection()) {
            ssw.sendVelocityServer(conn, msg);
          } catch (SQLException | ClassNotFoundException e) {
            logger.info("An error occurred at Menu#teleportPointMenu: {}", e);
          }

          Component message = Component.text("テレポート前の座標に戻りました。")
            .color(NamedTextColor.GREEN)
            .decorate(TextDecoration.BOLD);

          audiences.player(player).sendMessage(message);

          player.teleport(EventListener.playerBeforeLocationMap.get(player));
          EventListener.playerBeforeLocationMap.remove(player);
        } else {
          Component message = Component.text("テレポート前の座標がありません。")
            .color(NamedTextColor.RED)
            .decorate(TextDecoration.BOLD);

          audiences.player(player).sendMessage(message);
        }
      }
    } else {
      if (sender != null) {
        sender.sendMessage("プレイヤーからのみ実行可能です。");
      }
    }
    return true;
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
    return Collections.emptyList();
  }
}
