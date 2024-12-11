package keyp.forev.fmc.spigot.server.interfaces;

import org.bukkit.event.inventory.ClickType;

@FunctionalInterface
public interface MenuRunnable {
    void run(ClickType clickType);
}
