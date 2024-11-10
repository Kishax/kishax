package spigot;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.entity.Player;
import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;

import common.Database;
import common.Luckperms;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import spigot_command.Menu;

public class SocketResponse {
    private final common.Main plugin;
    private final Logger logger;
    private final Database db;
    private final ServerStatusCache ssc;
    private final Provider<SocketSwitch> sswProvider;
    private final AutoShutdown asd;
    private final Inventory inv;
    private final Menu menu;
    private final Luckperms lp;
    private final String thisServerName;
    @Inject
    public SocketResponse(common.Main plugin, Logger logger, Database db, ServerStatusCache ssc, ServerHomeDir shd, Provider<SocketSwitch> sswProvider, AutoShutdown asd, Inventory inv, Menu menu, Luckperms lp) {
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
    }

    public void resaction(String res) {
    	if (res != null) {
            res = res.replace("\n", "").replace("\r", "");
            if (res.startsWith("proxy->")) {
                if (res.contains("stop")) {
                    SocketSwitch ssw = sswProvider.get();
	            	ssw.sendVelocityServer("管理者の命令より、"+thisServerName+"サーバーを停止させます。");
	                plugin.getServer().broadcastMessage(ChatColor.RED+"管理者の命令より、"+thisServerName+"サーバーを5秒後に停止します。");
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
                                    TextComponent message = new TextComponent("サーバーインベントリを開きます。\n");
                                    message.setBold(true);
                                    message.setUnderlined(true);
                                    message.setColor(ChatColor.GOLD);
                                    message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/fmc menu server before"));
                                    message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("直近で起動したサーバーインベントリを開きます。")));
                                    player.spigot().sendMessage(message, TCUtils.SETTINGS_ENTER.get());
                                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                                        menu.openServerInventoryFromOnlineServerInventory(player, extractedServer, 1);
                                    });
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