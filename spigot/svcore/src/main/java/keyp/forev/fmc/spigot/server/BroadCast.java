package keyp.forev.fmc.spigot.server;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.inject.Inject;

import net.md_5.bungee.api.ChatColor;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;

public class BroadCast {
  private final JavaPlugin plugin;
  private final BukkitAudiences audiences;

  @Inject
  public BroadCast(JavaPlugin plugin, BukkitAudiences audiences) {
    this.plugin = plugin;
    this.audiences = audiences;
  }

  public void broadCastMessage(String message) {
    for (Player player : plugin.getServer().getOnlinePlayers()) {
      player.sendMessage(ChatColor.RED + message);
    }
  }

  public void broadCastMessage(Component component) {
    for (Player player : plugin.getServer().getOnlinePlayers()) {
      audiences.player(player).sendMessage(component);
    }
  }
}
