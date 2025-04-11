package f5.si.kishax.mc.spigot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.TimeZone;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.inject.Guice;
import com.google.inject.Injector;

import f5.si.kishax.mc.common.database.Database;
import f5.si.kishax.mc.common.server.DoServerOffline;
import f5.si.kishax.mc.common.server.Luckperms;
import f5.si.kishax.mc.common.server.ServerStatusCache;
import f5.si.kishax.mc.common.server.interfaces.ServerHomeDir;
import f5.si.kishax.mc.common.util.PlayerUtils;
import f5.si.kishax.mc.spigot.module.Module;
import f5.si.kishax.mc.spigot.server.AutoShutdown;
import f5.si.kishax.mc.spigot.server.ImageMap;
import f5.si.kishax.mc.spigot.server.cmd.main.Command;
import f5.si.kishax.mc.spigot.server.cmd.sub.imagemap.Q;
import f5.si.kishax.mc.spigot.server.cmd.sub.imagemap.RegisterImageMap;
import f5.si.kishax.mc.spigot.server.cmd.sub.teleport.Navi;
import f5.si.kishax.mc.spigot.server.cmd.sub.teleport.RegisterTeleportPoint;
import f5.si.kishax.mc.spigot.server.cmd.sub.teleport.TeleportAccept;
import f5.si.kishax.mc.spigot.server.cmd.sub.teleport.TeleportBack;
import f5.si.kishax.mc.spigot.server.cmd.sub.teleport.TeleportRequest;
import f5.si.kishax.mc.spigot.server.events.EventListener;
import f5.si.kishax.mc.spigot.server.events.WandListener;
import net.luckperms.api.LuckPermsProvider;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;

public class Main extends JavaPlugin {
  private static Injector injector = null;
  private BukkitAudiences audiences;
  public static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("kishax");

  @Override
  public void onEnable() {
    logger.info("detected spigot platform.");
    this.audiences = BukkitAudiences.create(this);
    injector = Guice.createInjector(new Module(this, logger, audiences));
    TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));
    Database db = getInjector().getInstance(Database.class);
    saveDefaultConfig();
    try (Connection conn = db.getConnection()) {
      ifHubThenUpdate(conn);
      ImageMap.imagesColumnsList = db.defineImageColumnNamesList(conn, "images");
    } catch (SQLException | ClassNotFoundException e) {
      logger.error("An error occurred while updating the database: {}", e.getMessage());
    }
    getInjector().getInstance(AutoShutdown.class).startCheckForPlayers();
    getServer().getPluginManager().registerEvents(getInjector().getInstance(EventListener.class), this);
    getServer().getPluginManager().registerEvents(getInjector().getInstance(WandListener.class), this);

    registerCommand();

    getInjector().getInstance(Luckperms.class).triggerNetworkSync();
    logger.info("linking with LuckPerms...");
    logger.info(LuckPermsProvider.get().getPlatform().toString());
    getInjector().getInstance(PlayerUtils.class).loadPlayers();
    // DoServerOnlineとPortFinderとSocketの処理を統合
    getInjector().getInstance(ServerStatusCache.class).serverStatusCache();
    logger.info("plugin has been enabled.");
  }

  public static Injector getInjector() {
    return injector;
  }

  @Override
  public void onDisable() {
    if (audiences != null) {
      audiences.close();
    }
    try {
      getInjector().getInstance(DoServerOffline.class).updateDatabase();
    } catch (Exception e) {
      logger.error( "An error occurred while updating the database: {}", e.getMessage());
    }
    getInjector().getInstance(AutoShutdown.class).stopCheckForPlayers();
    logger.info("Socket Server stopping...");
    logger.info("プラグインが無効になりました。");
  }

  private void registerCommand() {
    PluginCommand kishaxCmd = getCommand("kishax"),
      qCmd = getCommand("q"),
      tprCmd = getCommand("tpr"),
      tprmCmd = getCommand("tprm"),
      tprmaCmd = getCommand("tprma"),
      tpraCmd = getCommand("tpra"),
      tprdCmd = getCommand("tprd"),
      tprmdCmd = getCommand("tprmd"),
      registerpointCmd = getCommand("registerpoint"),
      registerimagemapCmd = getCommand("registerimagemap"),
      backCmd = getCommand("back"),
      nvCmd = getCommand("nv");

    if (kishaxCmd != null) {
      kishaxCmd.setExecutor(getInjector().getInstance(Command.class));
    }

    if (qCmd != null) {
      qCmd.setExecutor(getInjector().getInstance(Q.class));
    }

    if (tprCmd != null && tprmCmd != null && tpraCmd != null && tprmaCmd != null && tprdCmd != null && tprmdCmd != null) {
      tprCmd.setExecutor(getInjector().getInstance(TeleportRequest.class));
      tprmCmd.setExecutor(getInjector().getInstance(TeleportRequest.class));
      tpraCmd.setExecutor(getInjector().getInstance(TeleportAccept.class));
      tprmaCmd.setExecutor(getInjector().getInstance(TeleportAccept.class));
      tprdCmd.setExecutor(getInjector().getInstance(TeleportAccept.class));
      tprmdCmd.setExecutor(getInjector().getInstance(TeleportAccept.class));
    }

    if (registerpointCmd != null) {
      registerpointCmd.setExecutor(getInjector().getInstance(RegisterTeleportPoint.class));
    }

    if (backCmd != null) {
      backCmd.setExecutor(getInjector().getInstance(TeleportBack.class));
    }

    if (nvCmd != null) {
      nvCmd.setExecutor(getInjector().getInstance(Navi.class));
    }

    if (registerimagemapCmd != null) {
      registerimagemapCmd.setExecutor(getInjector().getInstance(RegisterImageMap.class));
    }
  }

  private void ifHubThenUpdate(Connection conn) throws SQLException, ClassNotFoundException {
    String thisServerName = getInjector().getInstance(ServerHomeDir.class).getServerName();
    String query = "SELECT hub FROM status WHERE name=? LIMIT 1;";

    PreparedStatement ps = conn.prepareStatement(query);
    ps.setString(1, thisServerName);
    ResultSet rs = ps.executeQuery();
    if (rs.next()) {
      if (rs.getBoolean("hub")) {
        EventListener.isHub.set(true);
      }
    }
  }
}
