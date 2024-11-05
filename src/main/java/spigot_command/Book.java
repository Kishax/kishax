package spigot_command;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataType;

import com.google.inject.Inject;

import net.md_5.bungee.api.ChatColor;

public class Book {
    private final common.Main plugin;
    @Inject
    public Book(common.Main plugin) {
        this.plugin = plugin;
    }

    private void giveRuleBook(Player player) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "custom_book"), PersistentDataType.STRING, "true");
            String rulebook = common.FMCSettings.RULEBOOK_CONTENT.getValue();
            if (rulebook != null) {
                List<String> pages = new ArrayList<>(List.of(rulebook.split("%%")));
                // もし、pagesの各ラインに##が含まれていたら、それをタイトルとして扱う
                // もし、pagesの各ラインに、By 〇〇が含まれていたら、それを著者として扱う
                // そしてそれらを、Listから除外する
                // また、各ラインの&を§に変換する
                // また、§の後に、空白があれば、そのあとは黒文字にする。
                
                for (int i = 0; i < pages.size(); i++) {
                    pages.set(i, formatText(pages.get(i)));
                }
                meta.setPages(pages);
            }
            meta.setTitle("ルールガイド");
            meta.setAuthor("サーバー管理者");
            meta.addPage(
                ChatColor.BOLD + "ルールガイド\n\n" +
                ChatColor.RESET + "1. 荒らし行為は禁止です。\n" +
                "2. 他のプレイヤーに対する敬意を持ちましょう。\n" +
                "3. チート行為は禁止です。\n" +
                "4. サーバーのルールを守りましょう。\n" +
                "5. 楽しんでください！"
            );
            meta.addPage(
                ChatColor.BOLD + "追加ルール\n\n" +
                ChatColor.RESET + "1. 建築物の破壊は禁止です。\n" +
                "2. 他のプレイヤーのアイテムを盗まないでください。\n" +
                "3. サーバーのイベントに参加しましょう。\n" +
                "4. サーバーの管理者の指示に従ってください。\n" +
                "5. サーバーのチャットを荒らさないでください。"
            );
            book.setItemMeta(meta);
        }
        // プレイヤーに本を渡す
        player.getInventory().addItem(book);
        player.sendMessage(ChatColor.GREEN + "ルールガイドを渡しました！");
    }

    private String formatText(String text) {
        return text.replace("&", "§");
    }
}
