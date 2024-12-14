package keyp.forev.fmc.spigot.server.menu.interfaces;

import org.bukkit.event.inventory.InventoryClickEvent;

@FunctionalInterface
public interface MenuEventRunnable {
    void run(InventoryClickEvent event);
}
