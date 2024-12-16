package keyp.forev.fmc.spigot.server.cmd.sub;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

import com.google.inject.Inject;

import keyp.forev.fmc.common.server.Luckperms;
import keyp.forev.fmc.common.settings.FMCSettings;
import keyp.forev.fmc.spigot.server.menu.Menu;
import net.md_5.bungee.api.ChatColor;

public class MenuExecutor {
    public static final List<String> args1 = new ArrayList<>(Arrays.asList("server", "image", "get", "tp"));
    public static final List<String> args2 = new ArrayList<>(Arrays.asList("online","survival","minigame","dev","mod","distributed","others","before"));
    public static final List<String> args2tp = new ArrayList<>(Arrays.asList("point","player", "navi"));
    public static final List<String> args2image = new ArrayList<>(Arrays.asList("list","register"));
    public static final List<String> args3tpsp = new ArrayList<>(Arrays.asList("private","public"));

    private final JavaPlugin plugin;
    private final Luckperms lp;
    private final Menu menu;
    public static final String PERSISTANT_KEY = "fmcmenu";
    @Inject
	public MenuExecutor(JavaPlugin plugin, Logger logger, Luckperms lp, Menu menu) {  
		this.plugin = plugin;
        this.lp = lp;
        this.menu = menu;
	}

    @Deprecated
	public void execute(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
        if (sender instanceof Player player) {
            if (args.length == 1) {
                menu.generalMenu(player, 1);
            } else if (args.length > 1) {
                int permLevel = lp.getPermLevel(player.getName());
                switch (args[1].toLowerCase()) {
                    case "get" -> {
                        boolean hasMenuBook = false;
                        for (ItemStack item : player.getInventory().getContents()) {
                            if (item != null) {
                                switch (item.getType()) {
                                    case ENCHANTED_BOOK -> {
                                        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
                                        if (meta != null && meta.getPersistentDataContainer().has(new NamespacedKey(plugin, keyp.forev.fmc.spigot.server.cmd.sub.MenuExecutor.PERSISTANT_KEY), PersistentDataType.STRING)) {
                                            hasMenuBook = true;
                                        }
                                    }
                                    default -> {
                                    }
                                }
                            }
                        }
                        if (!hasMenuBook) {
                            // エンチャント本を渡す
                            ItemStack menuBook = new ItemStack(Material.ENCHANTED_BOOK);
                            EnchantmentStorageMeta meta = (EnchantmentStorageMeta) menuBook.getItemMeta();
                            if (meta != null) {
                                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, keyp.forev.fmc.spigot.server.cmd.sub.MenuExecutor.PERSISTANT_KEY), PersistentDataType.STRING, "true");
                                meta.setDisplayName(ChatColor.GOLD + "FMC Menu Book");
                                meta.setLore(Arrays.asList(ChatColor.GRAY + "右クリックでメニューを開くことができます。"));
                                menuBook.setItemMeta(meta);
                            }
                            player.getInventory().addItem(menuBook);
                            player.sendMessage(ChatColor.GREEN + "メニューブックを受け取りました。");
                        } else {
                            player.sendMessage(ChatColor.RED + "既にメニューブックを持っています。");
                        }
                    }
                    case "server" -> {
                        if (plugin.getConfig().getBoolean("Menu.Server", false)) {
                            if (permLevel < 1) {
                                player.sendMessage("先にWEB認証を完了させてください。");
                                return;
                            }
                            if (args.length > 2) {
                                String serverType = args[2].toLowerCase();
                                switch (serverType) {
                                    case "online" -> menu.onlineServerMenu(player, 1);
                                    case "survival", "minigame", "mod", "distributed", "others", "dev" -> menu.serverEachTypeMenu(player, serverType, 1);
                                    case "before" -> menu.serverMenu(player, FMCSettings.NOW_ONLINE.getValue(), 1);
                                    default -> sender.sendMessage("Usage: /fmc menu server <survival|minigame|dev|mod|distributed|others>");
                                }
                            } else {
                                menu.serverTypeMenu((Player) sender, 1);
                            }
                        } else {
                            sender.sendMessage(ChatColor.RED + "このサーバーでは、この機能は無効になっています。");
                        }
                    }
                    case "image" -> {
                        if (plugin.getConfig().getBoolean("Menu.ImageMap", false)) {
                            menu.imageMapMenu(player);
                            if (args.length > 2) {
                                switch (args[2].toLowerCase()) {
                                    case "list" -> menu.imageMapListMenu(player, 1);
                                    case "register" -> player.performCommand("registerimagemap");
                                    default -> menu.imageMapMenu(player);
                                }
                            } else {
                                menu.imageMapMenu(player);
                            }
                        } else {
                            sender.sendMessage(ChatColor.RED + "このサーバーでは、この機能は無効になっています。");
                        }
                    }
                    case "tp" -> {
                        if (permLevel < 1) {
                            player.sendMessage("先にWEB認証を完了させてください。");
                            return;
                        }
                        if (args.length > 2) {
                            switch (args[2].toLowerCase()) {
                                case "navi" -> menu.faceIconNaviMenu(player, 1);
                                case "point" -> menu.teleportPointTypeMenu(player, 1);
                                case "player" -> menu.playerTeleportMenu(player, 1);
                                default -> menu.serverTypeMenu(player, 1);
                            }
                        } else if (args.length > 3) {
                            switch (args[2].toLowerCase()) {
                                case "point" -> {
                                    if (args[3].equalsIgnoreCase("private") || args[3].equalsIgnoreCase("public")) {
                                        menu.teleportPointMenu(player, 1, args[3]);
                                    } else {
                                        sender.sendMessage("Usage: /fmc menu tp point <private|public>");
                                    }
                                }
                                default -> menu.serverTypeMenu(player, 1);
                            }
                        } else {
                            menu.serverTypeMenu(player, 1);
                        }
                    }
                }
            } else {
                sender.sendMessage("Usage: /fmc menu <server|image> <server: serverType>");
            }
        } else {
            if (sender != null) {
                sender.sendMessage(ChatColor.RED + "このコマンドはプレイヤーのみ実行可能です。");
            }
        }
	}
}

