package f5.si.kishax.mc.spigot.server;

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
import org.slf4j.Logger;

import com.google.inject.Inject;

import f5.si.kishax.mc.common.database.Database;
import f5.si.kishax.mc.spigot.server.cmd.sub.Book;

import org.bukkit.plugin.java.JavaPlugin;

public class ItemFrames {
  private final JavaPlugin plugin;
  private final Logger logger;
  private final Database db;
  private final Book book;
  private final ImageMap im;
  private static final String OLD_KEY_SUFFIX = "custom_image";
  private static final String OLD_LARGE_KEY_SUFFIX = "custom_large_image";
  public static final String NEW_KEY_SUFFIX = ImageMap.PERSISTANT_KEY; // "custom_image"
  public static final String NEW_LARGE_KEY_SUFFIX = ImageMap.LARGE_PERSISTANT_KEY; // "custom_large_image"
  private static final String OLD_PLUGIN_ID = "fmc-plugin"; // 古いプラグインの ID

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
        // 古いプラグインの NamespacedKey を作成 (プラグインインスタンスではなく、文字列で指定)
        NamespacedKey oldKey = new NamespacedKey(OLD_PLUGIN_ID, OLD_KEY_SUFFIX);
        NamespacedKey oldLargeKey = new NamespacedKey(OLD_PLUGIN_ID, OLD_LARGE_KEY_SUFFIX);
        NamespacedKey newKey = new NamespacedKey(plugin, NEW_KEY_SUFFIX);
        NamespacedKey newLargeKey = new NamespacedKey(plugin, NEW_LARGE_KEY_SUFFIX);

        logger.info("Checking for old key: {}", oldKey.toString()); // ログ出力も修正

        // 古いキーが存在する場合は、新しいキーにデータを移行
        if (container.has(oldKey, PersistentDataType.STRING) && !container.has(newKey, PersistentDataType.STRING)) {
          logger.info("Old key found for map ID: {}", mapView.getId());
          String imageData = container.get(oldKey, PersistentDataType.STRING);
          logger.info("Retrieved old data: {}", imageData);
          container.set(newKey, PersistentDataType.STRING, imageData);
          logger.info("Set new key with data.");
          container.remove(oldKey);
          logger.info("Removed old key.");
          itemFrame.setItem(item);
          logger.info("Updated item in frame.");
          im.loadAndSetImage(conn, mapView.getId(), item, mapMeta, mapView);
          logger.info("Loaded image with new key.");
          return; // 移行処理が終わったら、以降の処理は不要
        } else {
          logger.info("Old key not found for map ID: {}", mapView.getId());
        }

        // 新しいキーが存在する場合は、通常通りロード
        if (container.has(newKey, PersistentDataType.STRING)) {
          im.loadAndSetImage(conn, mapView.getId(), item, mapMeta, mapView);
        } else if (container.has(newLargeKey, PersistentDataType.STRING)) {
          im.loadAndSetImageTile(conn, mapView.getId(), item, mapMeta, mapView);
        }
      }
    } else if (item.getType() == org.bukkit.Material.WRITTEN_BOOK && item.getItemMeta() instanceof BookMeta meta) {
      if (meta.getPersistentDataContainer().has(new NamespacedKey(plugin, Book.PERSISTANT_KEY),
          PersistentDataType.STRING)) {
        item.setItemMeta(book.setBookItemMeta((BookMeta) meta));
        logger.info("Updated book item frame: {}", itemFrame.getLocation());
      }
    }
  }
}
