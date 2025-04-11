package f5.si.kishax.mc.spigot.server.events;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.slf4j.Logger;

import com.google.inject.Inject;

import f5.si.kishax.mc.spigot.server.cmd.sub.portal.PortalsWand;
import f5.si.kishax.mc.spigot.util.config.PortalsConfig;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.ChatColor;

import org.bukkit.plugin.java.JavaPlugin;

public class WandListener implements Listener {
  public static boolean isMakePortal = false;
  private final JavaPlugin plugin;
  private final BukkitAudiences audiences;
  private final Logger logger;
  private final Map<Player, Location> firstCorner = new HashMap<>();
  private final PortalsConfig psConfig;

  @Inject
  public WandListener(JavaPlugin plugin, BukkitAudiences audiences, Logger logger, PortalsConfig psConfig) {
    this.plugin = plugin;
    this.audiences = audiences;
    this.logger = logger;
    this.psConfig = psConfig;
  }

  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (plugin.getConfig().getBoolean("Portals.Wand", false)) {
      Player player = event.getPlayer();
      ItemStack item = event.getItem();
      if (item != null && item.getType() == Material.STONE_AXE && event.getHand() == EquipmentSlot.HAND) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.getPersistentDataContainer().has(new NamespacedKey(plugin, PortalsWand.PERSISTANT_KEY), PersistentDataType.STRING)) {
          Block block = event.getClickedBlock();
          if (block == null) {
            logger.error("Block is null");
            return;
          }
          Location clickedBlock = block.getLocation();

          if (!firstCorner.containsKey(player)) {
            firstCorner.put(player, clickedBlock);
            player.sendMessage(ChatColor.GREEN + "1番目のコーナーを選択しました。\n"+ChatColor.AQUA+"("+clickedBlock.getX()+", "+clickedBlock.getY()+", "+clickedBlock.getZ()+")"+ChatColor.GREEN+"\n2番目のコーナーを右クリックで選択してください。");
          } else {
            Location corner1 = firstCorner.get(player);
            Location corner2 = clickedBlock;
            List<Map<?, ?>> portals = psConfig.getListMap("portals");
            if (portals == null) {
              portals = new ArrayList<>();
            }
            Map<String, Object> newPortal = new HashMap<>();
            String portalUUID = UUID.randomUUID().toString();
            newPortal.put("name", portalUUID);
            newPortal.put("uuid", portalUUID);
            newPortal.put("corner1", Arrays.asList(corner1.getX(), corner1.getY(), corner1.getZ()));
            newPortal.put("corner2", Arrays.asList(corner2.getX(), corner2.getY(), corner2.getZ()));
            portals.add(newPortal);
            try {
              psConfig.replaceValue("portals", portals);Component.text();
              isMakePortal = true;

              double x = clickedBlock.getX();
              double y = clickedBlock.getY();
              double z = clickedBlock.getZ();

              TextComponent messages = Component.text()
                .append(Component.text("2番目のコーナーを選択しました。").color(NamedTextColor.GREEN))
                .appendNewline()
                .append(Component.text("ポータルUUID: " + portalUUID))
                .appendNewline()
                .append(Component.text("(" + x + ", " + y + ", " + z + ")"))
                .append(Component.text("ポータルが保存されました。").color(NamedTextColor.GREEN))
                .appendNewline()
                .append(Component.text("もし、取り消す場合は、"))
                .append(Component.text("ココ").color(NamedTextColor.GOLD).hoverEvent(HoverEvent.showText(Component.text("ポータルを削除"))).clickEvent(ClickEvent.runCommand("/kishax portal delete " + portalUUID)))
                .append(Component.text("をクリックしてね。"))
                .appendNewline()
                .append(Component.text("ポータルの名前を変えるには、"))
                .append(Component.text("ココ").color(NamedTextColor.GOLD).hoverEvent(HoverEvent.showText(Component.text("ポータルの名前を変更"))).clickEvent(ClickEvent.suggestCommand("/kishax portal rename " + portalUUID + " ")))
                .append(Component.text("をクリックしてね。"))
                .appendNewline()
                .append(Component.text("ポータルにネザーゲートを付与するには"))
                .append(Component.text("ココ").color(NamedTextColor.GOLD).hoverEvent(HoverEvent.showText(Component.text("ポータルにネザーゲートを付与"))).clickEvent(ClickEvent.runCommand("/kishax portal nether " + portalUUID)))
                .append(Component.text("をクリックしてね。"))
                .build();

              audiences.player(player).sendMessage(messages);
              firstCorner.remove(player);
            } catch (Exception e) {
              player.sendMessage(ChatColor.RED + "ポータルの保存に失敗しました。");
              logger.error("An error occurred while saving the portal: "+e.getMessage());
              for (StackTraceElement element : e.getStackTrace()) {
                logger.error(element.toString());
              }
            }
          }
          event.setCancelled(true);
        }
      }
    }
  }
}
