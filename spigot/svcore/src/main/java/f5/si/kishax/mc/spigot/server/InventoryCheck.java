package f5.si.kishax.mc.spigot.server;

import java.sql.Connection;
import java.sql.SQLException;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.persistence.PersistentDataType;
import org.slf4j.Logger;

import com.google.inject.Inject;

import f5.si.kishax.mc.common.database.Database;
import f5.si.kishax.mc.spigot.server.cmd.sub.Book;
import f5.si.kishax.mc.spigot.server.menu.Type;

import org.bukkit.plugin.java.JavaPlugin;

public class InventoryCheck {
  private final JavaPlugin plugin;
  private final Logger logger;
  private final Database db;
  private final Book book;
  private final ImageMap im;

  @Inject
  public InventoryCheck(JavaPlugin plugin, Logger logger, Database db, Book book, ImageMap im) {
    this.plugin = plugin;
    this.logger = logger;
    this.db = db;
    this.book = book;
    this.im = im;
  }

  @Deprecated
  public void updatePlayerInventory(Player player) {
    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
      Bukkit.getScheduler().runTask(plugin, () -> {
        String playerName = player.getName();
        //logger.info("Updating player's inventory: {}", player.getName());
        boolean hasMenuBook = false;
        try (Connection conn = db.getConnection()) {
          for (ItemStack item : player.getInventory().getContents()) {
            if (item != null) {
              ItemMeta meta = item.getItemMeta();
              if (meta != null) {
                switch (item.getType()) {
                  case WRITTEN_BOOK -> {
                    BookMeta bookMeta = (BookMeta) meta;
                    if (meta != null) {
                      item.setItemMeta(book.setBookItemMeta(bookMeta));
                      logger.info("Updating book in {}\'s inventory...", playerName);
                    }
                  }
                  case ENCHANTED_BOOK -> {
                    if (meta != null) {
                      if (meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "kishaxmenu"), PersistentDataType.STRING)) {
                        logger.info("Removed old-Menu-Book from {}\'s inventory ", playerName);
                        player.getInventory().remove(item);
                      }
                      if (meta.getPersistentDataContainer().has(new NamespacedKey(plugin, Type.GENERAL.getPersistantKey()), PersistentDataType.STRING)) {
                        hasMenuBook = true;
                      }
                    }
                  }
                  case FILLED_MAP -> {
                    MapMeta mapMeta = (MapMeta) item.getItemMeta();
                    if (mapMeta != null && mapMeta.hasMapView()) {
                      MapView mapView = mapMeta.getMapView();
                      if (mapView != null) {
                        int mapId = mapView.getId();
                        if (!ImageMap.thisServerMapIds.contains(mapId)) {
                          if (mapMeta.getPersistentDataContainer().has(new NamespacedKey(plugin, ImageMap.PERSISTANT_KEY), PersistentDataType.STRING)) {
                            im.loadAndSetImage(conn, mapId, item, mapMeta, mapView);
                          } else if (mapMeta.getPersistentDataContainer().has(new NamespacedKey(plugin, ImageMap.LARGE_PERSISTANT_KEY), PersistentDataType.STRING)) {
                            im.loadAndSetImageTile(conn, mapId, item, mapMeta, mapView);
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
          }
          logger.info("Updated player's inventory: {}", player.getName());
        } catch (SQLException | ClassNotFoundException e) {
          logger.error("Error updating player inventory: {}", e.getMessage());
          for (StackTraceElement element : e.getStackTrace()) {
            logger.error(element.toString());
          }
        }
        if (!hasMenuBook) {
          player.performCommand("kishax menu get");
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
