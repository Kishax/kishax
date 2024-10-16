package spigot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.google.inject.Inject;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;

public class WandListener implements Listener {
    public static boolean isMakePortal = false;
    private final common.Main plugin;
    private final Map<Player, Location> firstCorner = new HashMap<>();
    private final PortalsConfig psConfig;

    @Inject
    public WandListener(common.Main plugin, PortalsConfig psConfig) {
        this.plugin = plugin;
        this.psConfig = psConfig;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (plugin.getConfig().getBoolean("Portals.Wand", false)) {
            Player player = event.getPlayer();
            ItemStack item = event.getItem();

            if (item != null && item.getType() == Material.STONE_AXE && event.getHand() == EquipmentSlot.HAND) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.hasDisplayName() && meta.getDisplayName().equals(ChatColor.GREEN + "Portal Wand")) {
                    Block block = event.getClickedBlock();
                    if (block == null) {
                        plugin.getLogger().warning("Block is null");
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
                        newPortal.put("corner1", Arrays.asList(corner1.getX(), corner1.getY(), corner1.getZ()));
                        newPortal.put("corner2", Arrays.asList(corner2.getX(), corner2.getY(), corner2.getZ()));
                        portals.add(newPortal);

                        portalsConfig.set("portals", portals);
                        psConfig.savePortalsConfig();

                        psConfig.reloadPortalsConfig(); // 追加: 設定を再読み込み
                        isMakePortal = true;

                        player.sendMessage(ChatColor.GREEN + "2番目のコーナーを選択しました。\nポータルUUID: "+portalUUID+"\n"+ChatColor.AQUA+"("+clickedBlock.getX()+", "+clickedBlock.getY()+", "+clickedBlock.getZ()+")"+ChatColor.GREEN+"\nポータルが保存されました。");
                        // クリック可能なメッセージを送信
                        BaseComponent[] component = new ComponentBuilder(ChatColor.YELLOW+"FMC COMMANDS LIST").bold(true).underlined(true)
                                .append(ChatColor.WHITE + "もし、取り消す場合は、")
                                .append(ChatColor.GOLD + "ココ")
                                .append(ChatColor.WHITE + "をクリックしてね")
                                .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,"/fmc portal delete " + portalUUID))
                                .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("コンフィグ、リロードします！(クリックしてコピー)")))
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