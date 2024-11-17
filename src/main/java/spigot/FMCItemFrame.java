package spigot;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.persistence.PersistentDataType;
import org.slf4j.Logger;

import com.google.inject.Inject;

import common.Database;
import spigot_command.Book;

public class FMCItemFrame {
    private final common.Main plugin;
    private final Logger logger;
    private final Database db;
    private final Book book;
    private final ImageMap im;
    @Inject
    public FMCItemFrame(common.Main plugin, Logger logger, Database db, Book book, ImageMap im) {
        this.plugin = plugin;
        this.logger = logger;
        this.db = db;
        this.book = book;
        this.im = im;
    }

    public void loadWorldsItemFrames() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                try (Connection conn = db.getConnection()) {
                    Map<Integer, Map<String, Object>> serverImageInfo = im.getThisServerImages(conn);
                    for (World world : Bukkit.getWorlds()) {
                        logger.info("Loading item frames in the world: {}", world.getName());
                        for (ItemFrame itemFrame : world.getEntitiesByClass(ItemFrame.class)) {
                            ItemStack item = itemFrame.getItem();
                            switch (item.getType()) {
                                case FILLED_MAP -> {
                                    //logger.info("Loaded map item frame: {}", itemFrame.getLocation());
                                    MapMeta mapMeta = (MapMeta) item.getItemMeta();
                                    if (mapMeta != null && mapMeta.hasMapView()) {
                                        MapView mapView = mapMeta.getMapView();
                                        if (mapView != null) {
                                            if (mapMeta.getPersistentDataContainer().has(new NamespacedKey(plugin, ImageMap.PERSISTANT_KEY), PersistentDataType.STRING)) {
                                                int mapId = mapView.getId();
                                                if (serverImageInfo.containsKey(mapId)) {
                                                    ImageMap.thisServerMapIds.add(mapId);
                                                    Map<String, Object> imageInfo = serverImageInfo.get(mapId);
                                                    boolean isQr = (boolean) imageInfo.get("isqr");
                                                    Date date = (Date) imageInfo.get("date");
                                                    String imageUUID = (String) imageInfo.get("imuuid");
                                                    String ext = (String) imageInfo.get("ext");
                                                    String fullPath = im.getFullPath(date, imageUUID, ext);
                                                    logger.info("Replacing image to the map(No.{})...", new Object[] {mapId});
                                                    try {
                                                        BufferedImage image = im.loadImage(fullPath);
                                                        if (image != null) {
                                                            // QRコードならリサイズしない
                                                            image = isQr ? image : im.resizeImage(image, 128, 128);
                                                            mapView.getRenderers().clear();
                                                            mapView.addRenderer(new ImageMapRenderer(logger, image, fullPath));
                                                            item.setItemMeta(mapMeta);
                                                        } 
                                                    } catch (IOException e) {
                                                        logger.error("マップId {} の画像の読み込みに失敗しました: {}", new Object[] {mapId, fullPath});
                                                        logger.error("An IOException error occurred: {}", e.getMessage());
                                                        for (StackTraceElement element : e.getStackTrace()) {
                                                            logger.error(element.toString());
                                                        }
                                                    }
                                                }
                                            } else if (mapMeta.getPersistentDataContainer().has(new NamespacedKey(plugin, ImageMap.LARGE_PERSISTANT_KEY), PersistentDataType.STRING)) {
                                                int mapId = mapView.getId();
                                                logger.info("Replacing large tile image to the map(No.{})...", new Object[] {mapId});
                                                try {
                                                    BufferedImage tileImage = im.loadTileImage(conn, mapId);
                                                    if (tileImage != null) {
                                                        mapView.getRenderers().clear();
                                                        mapView.addRenderer(new ImageMapRenderer(logger, tileImage, null));
                                                        item.setItemMeta(mapMeta);
                                                    }
                                                } catch (IOException e) {
                                                    logger.error("Failed to load tile image: {}", e.getMessage());
                                                    for (StackTraceElement element : e.getStackTrace()) {
                                                        logger.error(element.toString());
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                case WRITTEN_BOOK -> {
                                    BookMeta meta = (BookMeta) item.getItemMeta();
                                    if (meta != null) {
                                        if (meta.getPersistentDataContainer().has(new NamespacedKey(plugin, Book.PERSISTANT_KEY), PersistentDataType.STRING)) {
                                            item.setItemMeta(book.setBookItemMeta((BookMeta) meta));
                                            logger.info("Updated book item frame: {}", itemFrame.getLocation());
                                        }
                                    }
                                }
                                default -> {
                                }
                            }
                        }
                    }
                    logger.info("Loaded all item frames.");
                } catch (SQLException | ClassNotFoundException e) {
                    logger.error("Error updating item frames: {}", e.getMessage());
                    for (StackTraceElement element : e.getStackTrace()) {
                        logger.error(element.toString());
                    }
                }
            });
        });
    }
}
