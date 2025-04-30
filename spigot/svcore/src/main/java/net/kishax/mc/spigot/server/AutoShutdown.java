package net.kishax.mc.spigot.server;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

import org.bukkit.scheduler.BukkitRunnable;
import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;

import net.kishax.mc.common.database.Database;
import net.kishax.mc.common.server.interfaces.ServerHomeDir;
import net.kishax.mc.common.socket.SocketSwitch;
import net.kishax.mc.common.socket.message.Message;
import net.md_5.bungee.api.ChatColor;

import org.bukkit.plugin.java.JavaPlugin;

public class AutoShutdown {
  private final JavaPlugin plugin;
  private final Logger logger;
  private final Database db;
  private final Provider<SocketSwitch> sswProvider;
  private final BroadCast bc;
  private final String thisServerName;
  private BukkitRunnable task = null;

  @Inject
  public AutoShutdown (JavaPlugin plugin, Logger logger, Database db, Provider<SocketSwitch> sswProvider, ServerHomeDir shd, BroadCast bc) {
    this.plugin = plugin;
    this.logger = logger;
    this.db = db;
    this.sswProvider = sswProvider;
    this.bc = bc;
    this.thisServerName = shd.getServerName();
  }

  public void startCheckForPlayers() {
    if (!plugin.getConfig().getBoolean("AutoStop.Mode", false)) {	
      logger.info("auto-stop is disabled.");
      return;
    }

    plugin.getServer().getConsoleSender().sendMessage(ChatColor.GREEN+"Auto-Stopが有効になりました。");

    long NO_PLAYER_THRESHOLD = plugin.getConfig().getInt("AutoStop.Interval",3) * 60 * 20;

    task = new BukkitRunnable() {
      @Override
      public void run() {
        Message msg = new Message();
        msg.mc = new Message.Minecraft();
        msg.mc.server = new Message.Minecraft.Server();
        msg.mc.server.action = "EMPTY_STOP";
        msg.mc.server.name = thisServerName;

        SocketSwitch ssw = sswProvider.get();
        if (plugin.getServer().getOnlinePlayers().isEmpty()) {
          try (Connection conn = db.getConnection()) {
            ssw.sendVelocityServer(conn, msg);
          } catch (SQLException | ClassNotFoundException e) {
            logger.error("An error occurred while updating the database: " + e.getMessage(), e);
            for (StackTraceElement element : e.getStackTrace()) {
              logger.error(element.toString());
            }
          }
          bc.broadCastMessage(ChatColor.RED+"プレイヤー不在のため、"+thisServerName+"サーバーを5秒後に停止します。");
          countdownAndShutdown(5);
        }
      }
    };
    task.runTaskTimer(plugin, NO_PLAYER_THRESHOLD, NO_PLAYER_THRESHOLD);
  }

  public void countdownAndShutdown(int seconds) {
    new BukkitRunnable() {
      int countdown = seconds;

      @Override
      public void run() {
        if (countdown <= 0) {
          bc.broadCastMessage(ChatColor.RED + "サーバーを停止します。");

          plugin.getServer().shutdown();
          cancel();
          return;
        }

        bc.broadCastMessage(String.valueOf(countdown));
        countdown--;
      }
    }.runTaskTimer(plugin, 0, 20);
  }

  public void stopCheckForPlayers() {
    if (Objects.nonNull(task) && !task.isCancelled()) {
      task.cancel();
    }
  }
}
