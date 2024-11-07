package spigot_command;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.slf4j.Logger;

import com.google.inject.Inject;

import common.Database;
import common.FMCSettings;
import common.Luckperms;
import net.md_5.bungee.api.ChatColor;
import spigot.ImageMap;

public class Confirm {
    private final common.Main plugin;
    private final Logger logger;
    private final Database db;
    private final Luckperms lp;
    private final ImageMap im;
    @Inject
    public Confirm(common.Main plugin, Logger logger, Database db, Luckperms lp, ImageMap im) {
        this.plugin = plugin;
        this.logger = logger;
        this.db = db;
        this.lp = lp;
        this.im = im;
    }

    public void execute(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
        if (sender instanceof Player player) {
            String playerName = player.getName();
            int permLevel = lp.getPermLevel(playerName);
            if (permLevel < 1) {
                try (Connection conn = db.getConnection()) {
                    // server=?, confirm=?, name=?で一致するレコードがあるか取得
                    // あれば、mapIdより、givemapするが、
                    // 渡す予定のmapがそのサーバーの起動中にロードされたかはわからない
                    
                    Map<String, Object> memberMap = db.getMemberMap(conn, player.getName());
                    if (!memberMap.isEmpty()) {
                        if (memberMap.get("id") instanceof Integer id) {
                            String confirmUrl = FMCSettings.CONFIRM_URL.getValue() + "?id=" + id;
                            player.sendMessage(ChatColor.GREEN + "WEB認証のQRコードを生成します。");
                            String[] imageArgs = {"fmc", "image", "createqr", confirmUrl};
                            im.executeImageMapForConfirm(player, imageArgs);
                        }
                    }
                } catch (SQLException | ClassNotFoundException e2) {
                    player.sendMessage(ChatColor.RED + "WEB認証のQRコード生成に失敗しました。");
                    logger.error("A SQLException error occurred: " + e2.getMessage());
                    for (StackTraceElement element : e2.getStackTrace()) {
                        logger.error(element.toString());
                    }
                }
            }
        } else {
            if (sender != null) {
                sender.sendMessage("プレイヤーからのみ実行可能です。");
            }
        }
    }
}
