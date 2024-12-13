package keyp.forev.fmc.spigot.server;

import java.sql.Connection;
import java.sql.SQLException;

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

import keyp.forev.fmc.common.database.Database;
import keyp.forev.fmc.spigot.server.cmd.sub.Book;

import org.bukkit.plugin.java.JavaPlugin;

public class FMCItemFrame {
    private final JavaPlugin plugin;
    private final Logger logger;
    private final Database db;
    private final Book book;
    private final ImageMap im;
    @Inject
    public FMCItemFrame(JavaPlugin plugin, Logger logger, Database db, Book book, ImageMap im) {
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
                    for (World world : Bukkit.getWorlds()) {
                        logger.info("Loading item frames in the world: {}", world.getName());
                        for (ItemFrame itemFrame : world.getEntitiesByClass(ItemFrame.class)) {
                            ItemStack item = itemFrame.getItem();
                            switch (item.getType()) {
                                case FILLED_MAP -> {
                                    MapMeta mapMeta = (MapMeta) item.getItemMeta();
                                    if (mapMeta != null && mapMeta.hasMapView()) {
                                        MapView mapView = mapMeta.getMapView();
                                        if (mapView != null) {
                                            int mapId = mapView.getId();
                                            if (mapMeta.getPersistentDataContainer().has(new NamespacedKey(plugin, ImageMap.PERSISTANT_KEY), PersistentDataType.STRING)) {
                                                im.loadAndSetImage(conn, mapId, item, mapMeta, mapView);
                                            } else if (mapMeta.getPersistentDataContainer().has(new NamespacedKey(plugin, ImageMap.LARGE_PERSISTANT_KEY), PersistentDataType.STRING)) {
                                                im.loadAndSetImageTile(conn, mapId, item, mapMeta, mapView);
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
