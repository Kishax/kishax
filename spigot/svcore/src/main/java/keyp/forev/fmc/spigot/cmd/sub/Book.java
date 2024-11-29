package keyp.forev.fmc.spigot.cmd.sub;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataType;

import com.google.inject.Inject;

import keyp.forev.fmc.common.settings.FMCSettings;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public class Book {
    public static final String PERSISTANT_KEY = "custom_book";
    private final JavaPlugin plugin;
    @Inject
    public Book(JavaPlugin plugin) {
        this.plugin = plugin;
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

    public BookMeta setBookItemMeta(BookMeta meta) {
        String rulebook = FMCSettings.RULEBOOK_CONTENT.getValue();
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
