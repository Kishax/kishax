package net.kishax.mc.spigot.server.menu;

import org.bukkit.Material;

public class MaterialUtil {
  public static String materialToDatabase(Material material) {
    return material.name();
  }

  public static Material stringToMaterial(String materialName) {
    try {
      return Material.valueOf(materialName);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
