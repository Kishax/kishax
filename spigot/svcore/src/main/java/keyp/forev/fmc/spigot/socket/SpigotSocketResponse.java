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
import keyp.forev.fmc.common.server.DefaultLuckperms;
import keyp.forev.fmc.common.server.ServerStatusCache;
import keyp.forev.fmc.common.socket.SocketSwitch;
import keyp.forev.fmc.common.socket.interfaces.SocketResponse;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import keyp.forev.fmc.spigot.cmd.sub.Menu;
import keyp.forev.fmc.spigot.events.EventListener;
import keyp.forev.fmc.spigot.server.AutoShutdown;
import keyp.forev.fmc.spigot.server.BroadCast;
import keyp.forev.fmc.spigot.server.Inventory;
import keyp.forev.fmc.spigot.server.SpigotServerHomeDir;
import keyp.forev.fmc.spigot.server.textcomponent.TCUtils;

import org.bukkit.plugin.java.JavaPlugin;

public class SpigotSocketResponse implements SocketResponse {
    private final JavaPlugin plugin;
    private final Logger logger;
    private final Database db;
    private final ServerStatusCache ssc;
    private final Provider<SocketSwitch> sswProvider;
    private final AutoShutdown asd;
    private final Inventory inv;
    private final Menu menu;
    private final DefaultLuckperms lp;
    private final BroadCast bc;
    private final String thisServerName;
    @Inject
    public SpigotSocketResponse(JavaPlugin plugin, Logger logger, Database db, ServerStatusCache ssc, SpigotServerHomeDir shd, Provider<SocketSwitch> sswProvider, AutoShutdown asd, Inventory inv, Menu menu, DefaultLuckperms lp, BroadCast bc) {
        this.plugin = plugin;
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
	                bc.broadCastMessage(ChatColor.RED+"管理者の命令より、"+thisServerName+"サーバーを5秒後に停止します。");
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
                                    TextComponent message2 = new TextComponent("サーバーに入るには");
                                    TextComponent message3 = new TextComponent("ココ");
                                    message3.setBold(true);
                                    message3.setUnderlined(true);
                                    message3.setColor(ChatColor.GOLD);
                                    message3.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/fmc fv " + playerName + " fmcp stp " + extractedServer));
                                    message3.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("クリックでサーバーに入ります。")));
                                    TextComponent message4 = new TextComponent("をクリックしてください。\n");
                                    player.spigot().sendMessage(message2, message3, message4, TCUtils.SETTINGS_ENTER.get());
                                } else {
                                    TextComponent message = new TextComponent("3秒後にインベントリを開きます。\n");
                                    message.setBold(true);
                                    message.setUnderlined(true);
                                    message.setColor(ChatColor.GOLD);
                                    message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/fmc menu server before"));
                                    message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("直近で起動したサーバーインベントリを開きます。")));
                                    player.spigot().sendMessage(message, TCUtils.SETTINGS_ENTER.get());
                                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                                        menu.openServerInventoryFromOnlineServerInventory(player, extractedServer, 1);
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