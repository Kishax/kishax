package spigot_command;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataType;
import org.slf4j.Logger;

import com.google.inject.Inject;

import net.md_5.bungee.api.ChatColor;

public class Book {
    private static final String PERSISTANT_KEY = "custom_book";
    private final common.Main plugin;
    private final Logger logger;
    @Inject
    public Book(common.Main plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }

    public void checkPlayerInventory(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() == Material.WRITTEN_BOOK) {
                    BookMeta meta = (BookMeta) item.getItemMeta();
                    if (meta != null) {
                        item.setItemMeta(setBookItemMeta(meta));
                        logger.info("Updated book in player's inventory: {}", player.getName());
                    }
                }
            }
        });
    }

    public void loadAllPlayersBooks() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            checkPlayerInventory(player);
        }
    }

    public void loadAllBooks() {
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (World world : Bukkit.getWorlds()) {
                logger.info("Loading book item frames in the world: {}", world.getName());
                for (ItemFrame itemFrame : world.getEntitiesByClass(ItemFrame.class)) {
                    ItemStack item = itemFrame.getItem();
                    if (item.getType() == Material.WRITTEN_BOOK) {
                        BookMeta meta = (BookMeta) item.getItemMeta();
                        if (meta != null) {
                            if (meta.getPersistentDataContainer().has(new NamespacedKey(plugin, PERSISTANT_KEY), PersistentDataType.STRING)) {
                                item.setItemMeta(setBookItemMeta((BookMeta) meta));
                                logger.info("Updated book item frame: {}", itemFrame.getLocation());
                            }
                        }
                    }
                }
            }
            logger.info("Loaded all books.");
        });
    }

    public void giveRuleBook(Player player) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta != null) {
            book.setItemMeta(setBookItemMeta(meta));
        }
        player.getInventory().addItem(book);
        player.sendMessage(ChatColor.GREEN + "ルールガイドを渡しました！");
    }

    private BookMeta setBookItemMeta(BookMeta meta) {
        String rulebook = common.FMCSettings.RULEBOOK_CONTENT.getValue();
        if (rulebook != null) {
            List<String> pages = new ArrayList<>(List.of(rulebook.split("%%")));
            String title = "ルールガイド";
            String author = "サーバー管理者";
            for (int i = 0; i < pages.size(); i++) {
                List<String> lines = new ArrayList<>(List.of(rulebook.split("\n")));
                for (int j = 0; j < lines.size(); j++) {
                    String line = lines.get(j);
                    if (line.startsWith("##")) {
                        title = line.substring(2).trim();
                        lines.remove(j);
                        j--; // インデックスを調整
                    } else if (line.startsWith("By ")) {
                        author = line.substring(3).trim();
                        lines.remove(j);
                        j--; // インデックスを調整
                    } else {
                        lines.set(j, formatText(line));
                    }
                }
                //pages.set(k + 1, String.join("\n", eachLine));
                //meta.setPage(k + 1, String.join("\n", eachLine));
                /*if (k < pages.size()) {
                    pages.set(k, String.join("\n", eachLine));
                } else {
                    pages.add(String.join("\n", eachLine));
                }*/
                pages.set(i, String.join("\n", lines));
            }
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, PERSISTANT_KEY), PersistentDataType.STRING, "true");
            meta.setPages(pages);
            meta.setTitle(title);
            meta.setAuthor(author);
        }
        return meta;
    }

    private String formatText(String text) {
        text = text.replace("&", "§");
        text = text.replaceAll("§\\s+", "§0");
        return text;
    }
}
