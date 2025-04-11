package f5.si.kishax.mc.spigot.server.cmd.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.google.inject.Inject;

import f5.si.kishax.mc.common.server.Luckperms;
import f5.si.kishax.mc.common.settings.Settings;
import f5.si.kishax.mc.spigot.settings.Coords;
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
        player.teleport(Coords.ROOM_POINT.getLocation());
      } else {
        final int hubTpTime = Settings.HUB_TELEPORT_TIME.getIntValue();
        player.sendMessage(ChatColor.GREEN + "WEB認証...PASS\n\nALL CORRECT");
        if (hubTpTime == 0) {
          player.teleport(Coords.HUB_POINT.getLocation());
        } else {
          player.sendMessage(ChatColor.GREEN + (hubTpTime + "秒後にハブに移動します。"));
          new BukkitRunnable() {
            int countdown = hubTpTime;
            @Override
            public void run() {
              if (countdown <= 0) {
                player.teleport(Coords.HUB_POINT.getLocation());
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
