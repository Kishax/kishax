package net.kishax.mc.spigot.server;

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
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

import com.google.inject.Inject;

import net.kishax.mc.common.database.Database;
import net.kishax.mc.spigot.server.cmd.sub.Book;

public class ItemFrames {
  private final JavaPlugin plugin;
  private final Logger logger;
  private final Database db;
  private final Book book;
  private final ImageMap im;
  private static final String OLD_KEY_SUFFIX = "custom_image";
  private static final String OLD_LARGE_KEY_SUFFIX = "custom_large_image";
  public static final String NEW_KEY_SUFFIX = ImageMap.PERSISTANT_KEY;
  public static final String NEW_LARGE_KEY_SUFFIX = ImageMap.LARGE_PERSISTANT_KEY;
  private static final String OLD_PLUGIN_ID = "fmc-plugin"; // old plugin id
  private static final String MIGRATED_KEY_NAME = "migrated";

  @Inject
  public ItemFrames(JavaPlugin plugin, Logger logger, Database db, Book book, ImageMap im) {
    this.plugin = plugin;
    this.logger = logger;
    this.db = db;
    this.book = book;
    this.im = im;
  }

  @Deprecated
  public void loadWorldsItemFrames() {
    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
      Bukkit.getScheduler().runTask(plugin, () -> {
        try (Connection conn = db.getConnection()) {
          for (World world : Bukkit.getWorlds()) {
            logger.info("Loading item frames in the world: {}", world.getName());
            for (ItemFrame itemFrame : world.getEntitiesByClass(ItemFrame.class)) {
              replaceImageMap(conn, itemFrame);
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

  @Deprecated
  public void replaceImageMap(Connection conn, ItemFrame itemFrame) throws SQLException, ClassNotFoundException {
    ItemStack item = itemFrame.getItem();
    if (item.getType() == org.bukkit.Material.FILLED_MAP && item.getItemMeta() instanceof MapMeta mapMeta) {
      MapView mapView = mapMeta.getMapView();
      if (mapView != null) {
        PersistentDataContainer container = mapMeta.getPersistentDataContainer();
        NamespacedKey oldKey = new NamespacedKey(OLD_PLUGIN_ID, OLD_KEY_SUFFIX);
        NamespacedKey oldLargeKey = new NamespacedKey(OLD_PLUGIN_ID,
            OLD_LARGE_KEY_SUFFIX);
        NamespacedKey newKey = new NamespacedKey(plugin, NEW_KEY_SUFFIX);
        NamespacedKey newLargeKey = new NamespacedKey(plugin, NEW_LARGE_KEY_SUFFIX);
        NamespacedKey migratedKey = new NamespacedKey(plugin, MIGRATED_KEY_NAME);

        // if you would like to detect migrated map, use under
        // if (container.has(migratedKey, PersistentDataType.BOOLEAN)
        // && container.get(migratedKey, PersistentDataType.BOOLEAN)) {
        // // logger.info(" Map ID {}: Already migrated.", mapView.getId());
        // return;
        // }

        if (container.has(oldKey, PersistentDataType.STRING) &&
            !container.has(newKey, PersistentDataType.STRING)) {
          logger.info(" Map ID {}: Old small key found.", mapView.getId());
          String imageData = container.get(oldKey, PersistentDataType.STRING);
          logger.info(" Map ID {}: Migrating small data...", mapView.getId());
          container.set(newKey, PersistentDataType.STRING, imageData);
          container.remove(oldKey);
          container.set(migratedKey, PersistentDataType.BOOLEAN, true);
          item.setItemMeta(mapMeta);
          im.loadAndSetImage(conn, mapView.getId(), item, mapMeta, mapView);
          itemFrame.setItem(item);

          logger.info(" Map ID {}: Small data migrated.", mapView.getId());
          logger.info(" Container Keys: {}", container.getKeys());
          return;
        }

        if (container.has(oldLargeKey, PersistentDataType.STRING)
            && !container.has(newLargeKey, PersistentDataType.STRING)) {
          logger.info(" Map ID {}: Old large key found.", mapView.getId());
          String imageData = container.get(oldLargeKey, PersistentDataType.STRING);
          logger.info(" Map ID {}: Migrating large data...", mapView.getId());
          container.set(newLargeKey, PersistentDataType.STRING, imageData);
          container.remove(oldLargeKey);
          container.set(migratedKey, PersistentDataType.BOOLEAN, true);
          item.setItemMeta(mapMeta); // 以下、loadAndSetImageTileメソッドで実行されるのでここでは不必要
          im.loadAndSetImageTile(conn, mapView.getId(), item, mapMeta, mapView);
          itemFrame.setItem(item);

          logger.info(" Map ID {}: Large data migrated.", mapView.getId());
          logger.info(" Container Keys: {}", container.getKeys());
          return;
        }

        if (container.has(newKey, PersistentDataType.STRING)) {
          im.loadAndSetImage(conn, mapView.getId(), item, mapMeta, mapView);
        } else if (container.has(newLargeKey, PersistentDataType.STRING)) {
          im.loadAndSetImageTile(conn, mapView.getId(), item, mapMeta, mapView);
        }
      }
    } else if (item.getType() == org.bukkit.Material.WRITTEN_BOOK &&
        item.getItemMeta() instanceof BookMeta meta) {
      if (meta.getPersistentDataContainer().has(new NamespacedKey(plugin,
          Book.PERSISTANT_KEY),
          PersistentDataType.STRING)) {
        item.setItemMeta(book.setBookItemMeta((BookMeta) meta));
        logger.info("Updated book item frame: {}", itemFrame.getLocation());
      }
    }
  }
}
