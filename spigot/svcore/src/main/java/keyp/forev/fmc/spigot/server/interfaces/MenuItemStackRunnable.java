package keyp.forev.fmc.spigot.server.interfaces;

import org.bukkit.inventory.ItemStack;

@FunctionalInterface
public interface MenuItemStackRunnable {
    void run(ItemStack item);
}
