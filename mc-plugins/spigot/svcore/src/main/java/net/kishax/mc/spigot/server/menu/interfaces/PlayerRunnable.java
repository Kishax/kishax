package net.kishax.mc.spigot.server.menu.interfaces;

import org.bukkit.entity.Player;

@FunctionalInterface
public interface PlayerRunnable {
  void run(Player player);
}
