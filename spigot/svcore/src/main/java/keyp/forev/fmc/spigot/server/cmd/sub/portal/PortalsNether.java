package keyp.forev.fmc.spigot.server.cmd.sub.portal;

import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.google.inject.Inject;

import keyp.forev.fmc.spigot.util.config.PortalsConfig;
import net.md_5.bungee.api.ChatColor;

public class PortalsNether {
    private final PortalsConfig psConfig;
    @Inject
    public PortalsNether(PortalsConfig psConfig) {
        this.psConfig = psConfig;
    }

    public void execute(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "ポータルUUIDを指定してください。");
            return;
        }
        String portalUUID = args[2];
        List<Map<?, ?>> portals = psConfig.getListMap("portals");
        if (portals != null) {
            for (Map<?, ?> portal : portals) {
                Object uuid = portal.get("uuid");
                if (uuid instanceof String configUUID && configUUID.equals(portalUUID)) {
                    List<?> corner1List = (List<?>) portal.get("corner1");
                    List<?> corner2List = (List<?>) portal.get("corner2");
                    if (corner1List != null && corner2List != null &&
                        corner1List.size() >= 3 && corner2List.size() >= 3 &&
                        corner1List.get(0) != null && corner1List.get(1) != null && corner1List.get(2) != null &&
                        corner2List.get(0) != null && corner2List.get(1) != null && corner2List.get(2) != null) {
                        double corner1X = ((Number) corner1List.get(0)).doubleValue();
                        double corner1Y = ((Number) corner1List.get(1)).doubleValue();
                        double corner1Z = ((Number) corner1List.get(2)).doubleValue();
                        double corner2X = ((Number) corner2List.get(0)).doubleValue();
                        double corner2Y = ((Number) corner2List.get(1)).doubleValue();
                        double corner2Z = ((Number) corner2List.get(2)).doubleValue();
                        // x, y, zのいずれかが一致している場合に作成する
                        boolean zE = corner1Z == corner2Z,
                            xE = corner1X == corner2X,
                            yE = corner1Y == corner2Y;
                        if (yE) {
                            sender.sendMessage(ChatColor.RED + "ポータルの高さが足りません。\nポータルのx,zのいずれかが一致する必要があります。");
                            return;
                        }
                        if (xE && zE) {
                            sender.sendMessage(ChatColor.RED + "ポータルの幅が足りません。\nポータルのx,zのいずれかが一致する必要があります。");
                            return;
                        }
                        if (xE) {
                            double minY = Math.min(corner1Y, corner2Y);
                            double maxY = Math.max(corner1Y, corner2Y);
                            double minZ = Math.min(corner1Z, corner2Z);
                            double maxZ = Math.max(corner1Z, corner2Z);
                            for (double y = minY; y <= maxY; y++) {
                                for (double z = minZ; z <= maxZ; z++) {
                                    if (y == minY || y == maxY || z == minZ || z == maxZ) {
                                        continue;
                                    }
                                    Block block = ((Player) sender).getWorld().getBlockAt((int) corner1X, (int) y, (int) z);
                                    block.setType(Material.NETHER_PORTAL);
                                }
                            }
                            sender.sendMessage(ChatColor.GREEN + "ネザーポータルを作成しました。");
                            return;
                        }
                        if (zE) {
                            double minX = Math.min(corner1X, corner2X);
                            double maxX = Math.max(corner1X, corner2X);
                            double minY = Math.min(corner1Y, corner2Y);
                            double maxY = Math.max(corner1Y, corner2Y);
                            for (double x = minX; x <= maxX; x++) {
                                for (double y = minY; y <= maxY; y++) {
                                    if (x == minX || x == maxX || y == minY || y == maxY) {
                                        continue;
                                    }
                                    Block block = ((Player) sender).getWorld().getBlockAt((int) x, (int) y, (int) corner1Z);
                                    block.setType(Material.NETHER_PORTAL);
                               }
                            }
                            sender.sendMessage(ChatColor.GREEN + "ネザーポータルを作成しました。");
                            return;
                        }
                    }
                }
            }
        }
    }
}
