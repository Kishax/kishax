package keyp.forev.fmc.spigot.socket;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.entity.Player;
import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;

import keyp.forev.fmc.common.database.Database;
import keyp.forev.fmc.common.server.Luckperms;
import keyp.forev.fmc.common.server.ServerStatusCache;
import keyp.forev.fmc.common.socket.SocketSwitch;
import keyp.forev.fmc.common.socket.interfaces.SocketResponse;
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

public class SpigotSocketResponse implements SocketResponse {
    private final JavaPlugin plugin;
    private final BukkitAudiences audiences;
    private final Logger logger;
    private final Database db;
    private final ServerStatusCache ssc;
    private final Provider<SocketSwitch> sswProvider;
    private final AutoShutdown asd;
    private final InventoryCheck inv;
    private final Menu menu;
    private final Luckperms lp;
    private final BroadCast bc;
    private final String thisServerName;
    @Inject
    public SpigotSocketResponse(JavaPlugin plugin, BukkitAudiences audiences, Logger logger, Database db, ServerStatusCache ssc, ServerHomeDir shd, Provider<SocketSwitch> sswProvider, AutoShutdown asd, InventoryCheck inv, Menu menu, Luckperms lp, BroadCast bc) {
        this.plugin = plugin;
        this.audiences = audiences;
        this.logger = logger;
        this.db = db;
        this.ssc = ssc;
        this.sswProvider = sswProvider;
        this.asd = asd;
        this.inv = inv;
        this.menu = menu;
        this.thisServerName = shd.getServerName();
        this.lp = lp;
        this.bc = bc;
    }

    @Override
    public void resaction(String res) {
    	if (res != null) {
            res = res.replace("\n", "").replace("\r", "");
            if (res.startsWith("proxy->")) {
                if (res.contains("stop")) {
                    try (Connection conn = db.getConnection()) {
                        SocketSwitch ssw = sswProvider.get();
	            	    ssw.sendVelocityServer(conn, "管理者の命令より、"+thisServerName+"サーバーを停止させます。");
                    } catch (SQLException | ClassNotFoundException e) {
                        logger.error("An error occurred while updating the database: " + e.getMessage(), e);
                        for (StackTraceElement element : e.getStackTrace()) {
                            logger.error(element.toString());
                        }
                    }
                    Component message = Component.text("管理者の命令より、"+thisServerName+"サーバーを停止させます。").color(NamedTextColor.RED);
	                bc.broadCastMessage(message);
	                asd.countdownAndShutdown(5);
                }
            } else if (res.contains("起動")) {
                logger.info("{}", res);
                String pattern = "(.*?)サーバーが起動しました。";
                Pattern compiledPattern = Pattern.compile(pattern);
                Matcher matcher = compiledPattern.matcher(res);
                if (matcher.find()) {
                    String extractedServer = matcher.group(1);
                    if (EventListener.isHub.get() && !extractedServer.equals(thisServerName)) {
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
                                        .clickEvent(ClickEvent.runCommand("/fmc fv " + playerName + " fmcp stp " + extractedServer))
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
                                        menu.serverMenuFromOnlineServerMenu(player, extractedServer);
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
                    ssc.refreshManualOnlineServer(extractedServer);
                }
            } else if (res.contains("PHP")) {
                if (res.contains("uuid")) {
                    ssc.refreshMemberInfo();
                }
            } else if (res.contains("MineStatusSync")) {
                ssc.refreshCache();
            } else if (res.contains("RulebookSync")) {
                inv.updateOnlinePlayerInventory();
            }
        }
    }
}