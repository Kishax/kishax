package f5.si.kishax.mc.spigot.server.menu.interfaces;

import org.bukkit.entity.Player;

@FunctionalInterface
public interface PlayerRunnable {
  void run(Player player);
}
