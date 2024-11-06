package spigot_command;

import java.sql.Connection;
import java.sql.SQLException;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.slf4j.Logger;

import com.google.inject.Inject;

import common.Database;
import net.md_5.bungee.api.ChatColor;
import spigot.Luckperms;
import spigot.ServerHomeDir;

public class SetPoint {
    private final Logger logger;
    private final Database db;
    private final Luckperms lp;
    private final ServerHomeDir shd;
    @Inject
    public SetPoint(Logger logger, Database db, Luckperms lp, ServerHomeDir shd) {
        this.logger = logger;
        this.db = db;
        this.lp = lp;
        this.shd = shd;
    }

    public void execute(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
        if (sender instanceof Player player) {
            // /fmc setpoint <type>
            String playerName = player.getName();
            if (lp.getPermLevel(playerName) < 2)  {
                player.sendMessage(ChatColor.RED + "権限がありません。");
                return;
            }
            if (args.length < 1) {
                player.sendMessage(ChatColor.RED + "引数が不足しています。");
                return;
            }
            String type = args[1];
            switch (type) {
                case "load", "room", "hub" -> {
                    Location location = player.getLocation();
                    String serverName = shd.getServerName();
                    if (location != null) {
                        double x = location.getX();
                        double y = location.getY();
                        double z = location.getZ();
                        float yaw = location.getYaw();
                        float pitch = location.getPitch();
                        String world = null;
                        if (location.getWorld() instanceof World playerWorld) {
                            world = playerWorld.getName();
                        }
                        player.sendMessage("""
                            座標を取得しました。
                            X: %s
                            Y: %s
                            Z: %s
                            Yaw: %s
                            Pitch: %s
                            World: %s
                            """.formatted(x, y, z, yaw, pitch, world));
                        try (Connection conn = db.getConnection()) {
                            db.updateLog(conn, "UPDATE coords SET x = ?, y = ?, z = ?, yaw = ?, pitch = ?, world = ?, server = ? WHERE name = ?", new Object[] {x, y, z, yaw, pitch, world, serverName, type});
                        } catch (SQLException | ClassNotFoundException e) {
                            player.sendMessage(ChatColor.RED + "座標の保存に失敗しました。");
                            logger.error("A SQLException | ClassNotFoundException error occurred: " + e.getMessage());
                            for (StackTraceElement element : e.getStackTrace()) {
                                logger.error(element.toString());
                            }
                        }
                    } else {
                        player.sendMessage(ChatColor.RED + "座標が取得できませんでした。");
                    }
                }
                default -> {
                    player.sendMessage(ChatColor.RED + "引数が不正です。");
                }
            }
        } else {
            if (sender != null) {
                sender.sendMessage("プレイヤーのみ実行可能です。");
            }
        }
    }
}
