package keyp.forev.fmc.spigot.socket.message.handlers.minecraft.server;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

import org.bukkit.entity.Player;
import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;

import keyp.forev.fmc.common.database.Database;
import keyp.forev.fmc.common.server.Luckperms;
import keyp.forev.fmc.common.server.ServerStatusCache;
import keyp.forev.fmc.common.socket.SocketSwitch;
import keyp.forev.fmc.common.socket.message.Message;
import keyp.forev.fmc.common.socket.message.handlers.interfaces.minecraft.ServerActionHandler;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import keyp.forev.fmc.spigot.server.AutoShutdown;
import keyp.forev.fmc.spigot.server.BroadCast;
import keyp.forev.fmc.spigot.server.InventoryCheck;
import keyp.forev.fmc.spigot.server.events.EventListener;
import keyp.forev.fmc.spigot.server.menu.Menu;
import keyp.forev.fmc.common.server.interfaces.ServerHomeDir;
import keyp.forev.fmc.spigot.server.textcomponent.TCUtils;

import org.bukkit.plugin.java.JavaPlugin;

public class SpigotServerActionHandler implements ServerActionHandler {
    private final JavaPlugin plugin;
    private final BukkitAudiences audiences;
    private final Logger logger;
    private final Database db;
    private final ServerStatusCache ssc;
    private final Provider<SocketSwitch> sswProvider;
    private final AutoShutdown asd;
    private final Menu menu;
    private final Luckperms lp;
    private final BroadCast bc;
    private final String thisServerName;

    @Inject
    public SpigotServerActionHandler(JavaPlugin plugin, BukkitAudiences audiences, Logger logger, Database db, ServerStatusCache ssc, ServerHomeDir shd, Provider<SocketSwitch> sswProvider, AutoShutdown asd, Menu menu, Luckperms lp, BroadCast bc) {
        this.plugin = plugin;
        this.audiences = audiences;
        this.logger = logger;
        this.db = db;
        this.ssc = ssc;
        this.sswProvider = sswProvider;
        this.asd = asd;
        this.menu = menu;
        this.thisServerName = shd.getServerName();
        this.lp = lp;
        this.bc = bc;
    }

    @Override
    public void handle(Message.Minecraft.Server mserver) {
        String serverName = mserver.name;

        switch (mserver.action) {
            case "START" -> {
                logger.info(mserver.name + "サーバーが起動しました。");

                if (EventListener.isHub.get() && !serverName.equals(thisServerName)) {
                    try (Connection conn = db.getConnection()) {
                        Set<String> chatTypePlayers = db.getStringSet(conn, "SELECT name FROM members WHERE hubinv = ?;", new Object[] {false});
                        for (Player player : plugin.getServer().getOnlinePlayers()) {
                            String playerName = player.getName();
                            if (lp.getPermLevel(playerName) < 1) continue;
                            if (chatTypePlayers.contains(player.getName())) {
                                Component here = Component.text("ココ")
                                    .color(NamedTextColor.GOLD)
                                    .decorate(
                                        TextDecoration.BOLD,
                                        TextDecoration.UNDERLINED)
                                    .clickEvent(ClickEvent.runCommand("/fmc fv " + playerName + " fmcp stp " + serverName))
                                    .hoverEvent(HoverEvent.showText(Component.text("クリックでサーバーに入ります。")));

                                TextComponent messages = Component.text()
                                    .append(Component.text("サーバーに入るには"))
                                    .append(here)
                                    .append(Component.text("をクリックしてください。"))
                                    .appendNewline()
                                    .append(TCUtils.SETTINGS_ENTER.get())
                                    .build();
                                
                                audiences.player(player).sendMessage(messages);
                            } else {
                                TextComponent messages = Component.text()
                                    .append(TCUtils.LATER_OPEN_INV_3.get())
                                    .appendNewline()
                                    .append(TCUtils.SETTINGS_ENTER.get())
                                    .build();

                                audiences.player(player).sendMessage(messages);
                                
                                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                                    menu.serverMenuFromOnlineServerMenu(player, serverName);
                                }, 60L);
                            }
                        }
                    } catch (SQLException | ClassNotFoundException e) {
                        logger.error("A SQLException | ClassNotFoundException occurred: ", e.getMessage());
                        for (StackTraceElement element : e.getStackTrace()) {
                            logger.error(element.toString());
                        }
                    }
                }

                ssc.refreshManualOnlineServer(serverName);
            }
            case "ADMIN_STOP" -> {
                Message msg = new Message();
                msg.mc = new Message.Minecraft();
                msg.mc.server = new Message.Minecraft.Server();
                msg.mc.server.action = "ADMIN_STOP";
                msg.mc.server.name = thisServerName;

                try (Connection conn = db.getConnection()) {
                    SocketSwitch ssw = sswProvider.get();
                    ssw.sendVelocityServer(conn, msg);
                } catch (SQLException | ClassNotFoundException e) {
                    logger.error("An error occurred while updating the database: " + e.getMessage(), e);
                    for (StackTraceElement element : e.getStackTrace()) {
                        logger.error(element.toString());
                    }
                }

                Component message = Component.text("管理者の命令より、" + thisServerName + "サーバーを停止させます。").color(NamedTextColor.RED);
                bc.broadCastMessage(message);
                asd.countdownAndShutdown(5);
            }
        }
    }
}

