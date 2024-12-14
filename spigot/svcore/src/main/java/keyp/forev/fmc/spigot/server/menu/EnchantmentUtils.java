package keyp.forev.fmc.spigot.server.menu;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class EnchantmentUtils {

    /**
     * ItemStackにエンチャントを付与する
     *
     * @param item ItemStackオブジェクト
     * @param enchantment 付与するエンチャント
     * @param level エンチャントのレベル
     * @return エンチャント付与に成功すればtrue、失敗すればfalse
     */
    public static boolean addEnchantment(ItemStack item, Enchantment enchantment, int level) {
        if (item == null || enchantment == null) {
            return false;
        }

        try {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.addEnchant(enchantment, level, true); // true = 安全性を無視して付与
                item.setItemMeta(meta);
                return true;
            }
        } catch (IllegalArgumentException e) {
            // 例えばエンチャントがサポートされていない場合など
            e.printStackTrace();
        }
        return false;
    }

    /**
     * ItemStackがエンチャントされているかを調べる
     *
     * @param item ItemStackオブジェクト
     * @return エンチャントされていればtrue、そうでなければfalse
     */
    public static boolean isEnchanted(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().hasEnchants();
    }

    /**
     * アイテムに見た目だけエンチャントを付ける
     *
     * @param item ItemStackオブジェクト
     * @return エフェクト付与に成功すればtrue、失敗すればfalse
     */
    public static boolean addEnchantmentEffect(ItemStack item) {
        if (item == null) {
            return false;
        }

        try {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                // ダミーのエンチャントを追加
                meta.addEnchant(Enchantment.LUCK, 1, true); // レベルは1でOK
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS); // エンチャント表示を非表示に
                item.setItemMeta(meta);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}

