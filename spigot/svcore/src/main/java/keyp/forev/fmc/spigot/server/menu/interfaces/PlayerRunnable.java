package keyp.forev.fmc.spigot.server.menu.interfaces;

import org.bukkit.entity.Player;

@FunctionalInterface
public interface PlayerRunnable {
    void run(Player player);
}
