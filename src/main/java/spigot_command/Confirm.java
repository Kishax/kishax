package spigot_command;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.slf4j.Logger;

import com.google.inject.Inject;

import common.Database;
import common.FMCSettings;
import common.Luckperms;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.chat.ComponentSerializer;
import spigot.ImageMap;
import spigot.ServerHomeDir;

public class Confirm {
    public static final Set<Player> confirmMap = new HashSet<>();
    private final Logger logger;
    private final Database db;
    private final Luckperms lp;
    private final ImageMap im;
    private final String thisServerName;
    @Inject
    public Confirm(Logger logger, Database db, Luckperms lp, ImageMap im, ServerHomeDir shd) {
        this.logger = logger;
        this.db = db;
        this.lp = lp;
        this.im = im;
        this.thisServerName = shd.getServerName();
    }

    public void execute(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
        if (sender instanceof Player player) {
            if (!Confirm.confirmMap.contains(player)) {
                String playerName = player.getName(),
                    playerUUID = player.getUniqueId().toString();
                int permLevel = lp.getPermLevel(playerName);
                if (permLevel < 1) {
                    try (Connection conn = db.getConnection()) {
                        int ifMapId = checkExistConfirmMap(conn, new Object[] {thisServerName, true, playerName});
                        if (ifMapId == -1) {
                            Map<String, Object> memberMap = db.getMemberMap(conn, player.getName());
                            if (!memberMap.isEmpty()) {
                                if (memberMap.get("id") instanceof Integer id) {
                                    String confirmUrl = FMCSettings.CONFIRM_URL.getValue() + "?id=" + id;
                                    player.sendMessage(ChatColor.GREEN + "WEB認証のQRコードを生成します。");
                                    String[] imageArgs = {"fmc", "image", "createqr", confirmUrl};
                                    im.executeImageMapForConfirm(player, imageArgs);
                                    Random rnd = new Random();
                                    int ranum = 100000 + rnd.nextInt(900000);
                                    String ranumstr = Integer.toString(ranum);
									int rsAffected3 = updateSecret2(conn, new Object[] {ranum, playerUUID});
									if (rsAffected3 > 0) {
                                        sendConfirmationMessage(player, ranumstr, confirmUrl);
									} else {
										player.sendMessage(ChatColor.RED + "エラーが発生しました。");
									}
                                }
                            }
                        } else {
                            im.giveMapToPlayer(player, ifMapId);
                        }
                    } catch (SQLException | ClassNotFoundException e2) {
                        player.sendMessage(ChatColor.RED + "WEB認証のQRコード生成に失敗しました。");
                        logger.error("A SQLException error occurred: " + e2.getMessage());
                        for (StackTraceElement element : e2.getStackTrace()) {
                            logger.error(element.toString());
                        }
                    }
                } else {
                    player.sendMessage(ChatColor.GREEN + "WEB認証済みプレイヤーが通過しました！");
                }
                Confirm.confirmMap.add(player);
            }
        } else {
            if (sender != null) {
                sender.sendMessage("プレイヤーからのみ実行可能です。");
            }
        }
    }

    private int updateSecret2(Connection conn, Object[] args) throws SQLException {
        String query = "UPDATE members SET secret2=? WHERE uuid=?;";
        PreparedStatement ps = conn.prepareStatement(query);
        for (int i = 0; i < args.length; i++) {
            ps.setObject(i + 1, args[i]);
        }
        return ps.executeUpdate();
    }

    private void sendConfirmationMessage(Player player, String ranumstr, String conrimUrl) {
        TextComponent message = Component.text("サーバーに参加するには、FMCアカウントとあなたのMinecraftでのUUIDをリンクさせる必要があります。以下、")
            .color(NamedTextColor.WHITE);
        Component webAuth = Component.text("WEB認証")
            .color(NamedTextColor.LIGHT_PURPLE)
            .decorate(TextDecoration.BOLD, TextDecoration.UNDERLINED)
            .clickEvent(ClickEvent.openUrl(conrimUrl))
            .hoverEvent(HoverEvent.showText(Component.text("クリックしてWEB認証ページを開く")));
        TextComponent instruction = Component.text("より、手続きを進めてください。")
            .color(NamedTextColor.WHITE);
        TextComponent authCode = Component.text("\n\n認証コードは ")
            .color(NamedTextColor.WHITE);
        TextComponent code = Component.text(ranumstr)
            .color(NamedTextColor.BLUE)
            .clickEvent(ClickEvent.copyToClipboard(ranumstr))
            .hoverEvent(HoverEvent.showText(Component.text("クリックしてコピー")));
        TextComponent endMessage = Component.text(" です。")
            .color(NamedTextColor.WHITE);
        Component regenerate = Component.text("\n\n認証コードの再生成")
            .color(NamedTextColor.LIGHT_PURPLE)
            .decorate(TextDecoration.BOLD, TextDecoration.UNDERLINED)
            .clickEvent(ClickEvent.runCommand("/fmcp retry"))
            .hoverEvent(HoverEvent.showText(Component.text("クリックして認証コードを再生成します。")));
        message = message.append(webAuth)
            .append(instruction)
            .append(authCode)
            .append(code)
            .append(endMessage)
            .append(regenerate);
        player.spigot().sendMessage(new net.md_5.bungee.api.chat.TextComponent(ComponentSerializer.toString(message)));
    }
    
    private int checkExistConfirmMap(Connection conn, Object[] args) throws SQLException {
        String query = "SELECT * FROM images WHERE server=? AND confirm=? AND name=?";
        PreparedStatement ps = conn.prepareStatement(query);
        for (int i = 0; i < args.length; i++) {
            ps.setObject(i + 1, args[i]);
        }
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("mapid");
            } else {
                return -1;
            }
        }
    }
}
