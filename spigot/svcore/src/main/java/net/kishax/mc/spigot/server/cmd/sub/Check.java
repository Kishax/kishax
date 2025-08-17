package net.kishax.mc.spigot.server.cmd.sub;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.google.inject.Inject;

import net.kishax.mc.common.server.Luckperms;
import net.kishax.mc.common.settings.Settings;
import net.kishax.mc.spigot.settings.Coords;
import net.md_5.bungee.api.ChatColor;

import org.bukkit.plugin.java.JavaPlugin;

public class Check {
  private final JavaPlugin plugin;
  private final Luckperms lp;

  @Inject
  public Check(JavaPlugin plugin, Luckperms lp) {
    this.plugin = plugin;
    this.lp = lp;
  }

  public void execute(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
    if (sender instanceof Player player) {
      String playerName = player.getName();
      int permLevel = lp.getPermLevel(playerName);
      if (permLevel < 1) {
        player.sendMessage(ChatColor.RED + "まだWEB認証が完了していません。");
        Location roomLocation = Coords.ROOM_POINT.getLocation();
        if (roomLocation != null) {
          player.teleport(roomLocation);
        } else {
          Coords.ROOM_POINT.saveLocation(player.getLocation());
          player.sendMessage(ChatColor.YELLOW + "ルームポイントを現在の座標に設定しました。");
        }
      } else {
        final int hubTpTime = Settings.HUB_TELEPORT_TIME.getIntValue();
        player.sendMessage(ChatColor.GREEN + "WEB認証...PASS\n\nALL CORRECT");
        if (hubTpTime == 0) {
          Location hubLocation = Coords.HUB_POINT.getLocation();
          if (hubLocation != null) {
            player.teleport(hubLocation);
          } else {
            Coords.HUB_POINT.saveLocation(player.getLocation());
            player.sendMessage(ChatColor.YELLOW + "ハブポイントを現在の座標に設定しました。");
          }
        } else {
          player.sendMessage(ChatColor.GREEN + (hubTpTime + "秒後にハブに移動します。"));
          new BukkitRunnable() {
            int countdown = hubTpTime;

            @Override
            public void run() {
              if (countdown <= 0) {
                Location hubLocation = Coords.HUB_POINT.getLocation();
                if (hubLocation != null) {
                  player.teleport(hubLocation);
                } else {
                  Coords.HUB_POINT.saveLocation(player.getLocation());
                  player.sendMessage(ChatColor.YELLOW + "ハブポイントを現在の座標に設定しました。");
                }
                cancel();
                return;
              }
              player.sendMessage(ChatColor.AQUA + String.valueOf(countdown));
              countdown--;
            }
          }.runTaskTimer(plugin, 0, 20);
        }
      }
    } else {
      if (sender != null) {
        sender.sendMessage("プレイヤーからのみ実行可能です。");
      }
    }
  }
}
