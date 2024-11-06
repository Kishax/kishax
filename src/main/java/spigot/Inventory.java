package spigot;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.slf4j.Logger;

import com.google.inject.Inject;

import common.Database;
import spigot_command.Book;

public class Inventory {
    private final common.Main plugin;
    private final Logger logger;
    private final Database db;
    private final Book book;
    private final ImageMap im;
    @Inject
    public Inventory(common.Main plugin, Logger logger, Database db, Book book, ImageMap im) {
        this.plugin = plugin;
        this.logger = logger;
        this.db = db;
        this.book = book;
        this.im = im;
    }

    public void updatePlayerInventory(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                String playerName = player.getName();
                try (Connection conn = db.getConnection()) {
                    Map<Integer, Map<String, Object>> serverImageInfo = im.getThisServerImages(conn);
                    for (ItemStack item : player.getInventory().getContents()) {
                        if (item != null) {
                            switch (item.getType()) {
                                case WRITTEN_BOOK -> {
                                    BookMeta meta = (BookMeta) item.getItemMeta();
                                    if (meta != null) {
                                        item.setItemMeta(book.setBookItemMeta(meta));
                                        logger.info("Updating book in {}\'s inventory...", playerName);
                                    }
                                }
                                case FILLED_MAP -> {
                                    MapMeta mapMeta = (MapMeta) item.getItemMeta();
                                    if (mapMeta != null && mapMeta.hasMapView()) {
                                        MapView mapView = mapMeta.getMapView();
                                        if (mapView != null) {
                                            int mapId = mapView.getId();
                                            if (serverImageInfo.containsKey(mapId) && !ImageMap.thisServerMapIds.contains(mapId)) {
                                                Map<String, Object> imageInfo = serverImageInfo.get(mapId);
                                                boolean isQr = (boolean) imageInfo.get("isqr");
                                                Date date = (Date) imageInfo.get("date");
                                                String imageUUID = (String) imageInfo.get("imuuid");
                                                String ext = (String) imageInfo.get("ext");
                                                try {
                                                    String fullPath = im.getFullPath(date, imageUUID, ext); // Connectionが必要ない場合はnullを渡す
                                                    logger.info("Replacing image to the No.{} map in {}\\'s inventory...", new Object[] {mapId, playerName});
                                                        BufferedImage image = im.loadImage(fullPath);
                                                        if (image != null) {
                                                            // QRコードならリサイズしない
                                                            image = isQr ? image : im.resizeImage(image, 128, 128);
                                                            mapView.getRenderers().clear();
                                                            mapView.addRenderer(new ImageMapRenderer(logger, image, fullPath));
                                                            mapMeta.setMapView(mapView);
                                                            item.setItemMeta(mapMeta);
                                                        } else {
                                                            logger.error("Failed to load image from path: {}", fullPath);
                                                        }
                                                } catch (IOException e) {
                                                    logger.error("Error loading image: {}" , e);
                                                }
                                            }
                                        }
                                    }
                                }
                                default -> {
                                }
                            }
                        }
                    }
                    logger.info("Updated player's inventory: {}", player.getName());
                } catch (SQLException | ClassNotFoundException e) {
                    logger.error("Error updating player inventory: {}", e.getMessage());
                    for (StackTraceElement element : e.getStackTrace()) {
                        logger.error(element.toString());
                    }
                }
            });
        });
    }

    public void updateOnlinePlayerInventory() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerInventory(player);
        }
    }
}
