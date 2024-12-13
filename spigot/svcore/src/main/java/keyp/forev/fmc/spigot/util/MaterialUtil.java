package keyp.forev.fmc.spigot.util;

import org.bukkit.Material;

public class MaterialUtil {
    public static String materialToDatabase(Material material) {
        return material.name(); // Material名を取得
    }

    public static Material databaseToMaterial(String materialName) {
        try {
            return Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
