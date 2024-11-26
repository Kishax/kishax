package keyp.forev.fmc.spigot.util;

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
import org.bukkit.configuration.file.FileConfiguration;
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

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import keyp.forev.fmc.spigot.cmd.PortalsWand;
import org.bukkit.plugin.java.JavaPlugin;

public class WandListener implements Listener {
    public static boolean isMakePortal = false;
    private final JavaPlugin plugin;
    private final Logger logger;
    private final Map<Player, Location> firstCorner = new HashMap<>();
    private final PortalsConfig psConfig;

    @Inject
    public WandListener(JavaPlugin plugin, Logger logger, PortalsConfig psConfig) {
        this.plugin = plugin;
        this.logger = logger;
        this.psConfig = psConfig;
    }

    @EventHandler
    @SuppressWarnings("unchecked")
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

                        // portals.yml にポータルを追加
                        FileConfiguration portalsConfig = psConfig.getPortalsConfig();
                        List<Map<String, Object>> portals = (List<Map<String, Object>>) portalsConfig.getList("portals");
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

                        portalsConfig.set("portals", portals);
                        psConfig.savePortalsConfig();

                        psConfig.reloadPortalsConfig(); // 追加: 設定を再読み込み
                        isMakePortal = true;

                        player.sendMessage(ChatColor.GREEN + "2番目のコーナーを選択しました。\nポータルUUID: "+portalUUID+"\n"+ChatColor.AQUA+"("+clickedBlock.getX()+", "+clickedBlock.getY()+", "+clickedBlock.getZ()+")"+ChatColor.GREEN+"\nポータルが保存されました。");
                        // クリック可能なメッセージを送信
                        BaseComponent[] component = new ComponentBuilder()
                                .append(ChatColor.WHITE + "もし、取り消す場合は、")
                                .append(ChatColor.GOLD + "ココ")
                                    .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/fmc portal delete " + portalUUID))
                                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("ポータルを削除")))
                                .append(ChatColor.WHITE + "をクリックしてね")
                                .append("\n" + ChatColor.WHITE + "ポータルの名前を変えるには、")
                                .append(ChatColor.GOLD + "ココ")
                                    .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,"/fmc portal rename " + portalUUID + " "))
                                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("ポータルの名前を変更")))
                                .append(ChatColor.WHITE + "をクリックしてね")
                                .append("\n" + ChatColor.WHITE + "ポータルにネザーゲートを付与するには")
                                .append(ChatColor.GOLD + "ココ")
                                    .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/fmc portal nether " + portalUUID))
                                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("ポータルにネザーゲートを付与")))
                                .append(ChatColor.WHITE + "をクリックしてね")
                                .create();
                        player.spigot().sendMessage(component);
                        firstCorner.remove(player);
                    }
                    event.setCancelled(true);
                }
            }
        }
    }
}