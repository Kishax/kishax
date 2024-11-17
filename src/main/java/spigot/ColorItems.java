package spigot;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ColorItems {
    public static Map<ItemStack, Color> getColorItems() {
        Map<ItemStack, Color> colorItems = new LinkedHashMap<>();
        ItemStack white = new ItemStack(Material.WHITE_WOOL);
        ItemMeta whiteMeta = white.getItemMeta();
        if (whiteMeta != null) {
            whiteMeta.setDisplayName("ホワイト");
            white.setItemMeta(whiteMeta);
        }
        ItemStack orange = new ItemStack(Material.ORANGE_WOOL);
        ItemMeta orangeMeta = orange.getItemMeta();
        if (orangeMeta != null) {
            orangeMeta.setDisplayName("オレンジ");
            orange.setItemMeta(orangeMeta);
        }
        ItemStack magenta = new ItemStack(Material.MAGENTA_WOOL);
        ItemMeta magentaMeta = magenta.getItemMeta();
        if (magentaMeta != null) {
            magentaMeta.setDisplayName("マゼンタ");
            magenta.setItemMeta(magentaMeta);
        }
        ItemStack lightBlue = new ItemStack(Material.LIGHT_BLUE_WOOL);
        ItemMeta lightBlueMeta = lightBlue.getItemMeta();
        if (lightBlueMeta != null) {
            lightBlueMeta.setDisplayName("ライトブルー");
            lightBlue.setItemMeta(lightBlueMeta);
        }
        ItemStack yellow = new ItemStack(Material.YELLOW_WOOL);
        ItemMeta yellowMeta = yellow.getItemMeta();
        if (yellowMeta != null) {
            yellowMeta.setDisplayName("イエロー");
            yellow.setItemMeta(yellowMeta);
        }
        ItemStack lime = new ItemStack(Material.LIME_WOOL);
        ItemMeta limeMeta = lime.getItemMeta();
        if (limeMeta != null) {
            limeMeta.setDisplayName("ライム");
            lime.setItemMeta(limeMeta);
        }
        ItemStack pink = new ItemStack(Material.PINK_WOOL);
        ItemMeta pinkMeta = pink.getItemMeta();
        if (pinkMeta != null) {
            pinkMeta.setDisplayName("ピンク");
            pink.setItemMeta(pinkMeta);
        }
        ItemStack gray = new ItemStack(Material.GRAY_WOOL);
        ItemMeta grayMeta = gray.getItemMeta();
        if (grayMeta != null) {
            grayMeta.setDisplayName("グレー");
            gray.setItemMeta(grayMeta);
        }
        ItemStack lightGray = new ItemStack(Material.LIGHT_GRAY_WOOL);
        ItemMeta lightGrayMeta = lightGray.getItemMeta();
        if (lightGrayMeta != null) {
            lightGrayMeta.setDisplayName("ライトグレー");
            lightGray.setItemMeta(lightGrayMeta);
        }
        ItemStack cyan = new ItemStack(Material.CYAN_WOOL);
        ItemMeta cyanMeta = cyan.getItemMeta();
        if (cyanMeta != null) {
            cyanMeta.setDisplayName("シアン");
            cyan.setItemMeta(cyanMeta);
        }
        ItemStack purple = new ItemStack(Material.PURPLE_WOOL);
        ItemMeta purpleMeta = purple.getItemMeta();
        if (purpleMeta != null) {
            purpleMeta.setDisplayName("パープル");
            purple.setItemMeta(purpleMeta);
        }
        ItemStack blue = new ItemStack(Material.BLUE_WOOL);
        ItemMeta blueMeta = blue.getItemMeta();
        if (blueMeta != null) {
            blueMeta.setDisplayName("ブルー");
            blue.setItemMeta(blueMeta);
        }
        ItemStack brown = new ItemStack(Material.BROWN_WOOL);
        ItemMeta brownMeta = brown.getItemMeta();
        if (brownMeta != null) {
            brownMeta.setDisplayName("ブラウン");
            brown.setItemMeta(brownMeta);
        }
        ItemStack green = new ItemStack(Material.GREEN_WOOL);
        ItemMeta greenMeta = green.getItemMeta();
        if (greenMeta != null) {
            greenMeta.setDisplayName("グリーン");
            green.setItemMeta(greenMeta);
        }
        ItemStack red = new ItemStack(Material.RED_WOOL);
        ItemMeta redMeta = red.getItemMeta();
        if (redMeta != null) {
            redMeta.setDisplayName("レッド");
            red.setItemMeta(redMeta);
        }
        ItemStack black = new ItemStack(Material.BLACK_WOOL);
        ItemMeta blackMeta = black.getItemMeta();
        if (blackMeta != null) {
            blackMeta.setDisplayName("ブラック");
            black.setItemMeta(blackMeta);
        }
        ItemStack transparent = new ItemStack(Material.GLASS);
        ItemMeta transparentMeta = transparent.getItemMeta();
        if (transparentMeta != null) {
            transparentMeta.setDisplayName("透明");
            transparent.setItemMeta(transparentMeta);
        }
        colorItems.put(white, new java.awt.Color(255, 255, 255));
        colorItems.put(orange, new java.awt.Color(255, 165, 0));
        colorItems.put(magenta, new java.awt.Color(255, 0, 255));
        colorItems.put(lightBlue, new java.awt.Color(173, 216, 230));
        colorItems.put(yellow, new java.awt.Color(255, 255, 0));
        colorItems.put(lime, new java.awt.Color(0, 255, 0));
        colorItems.put(pink, new java.awt.Color(255, 192, 203));
        colorItems.put(gray, new java.awt.Color(128, 128, 128));
        colorItems.put(lightGray, new java.awt.Color(211, 211, 211));
        colorItems.put(cyan, new java.awt.Color(0, 255, 255));
        colorItems.put(purple, new java.awt.Color(128, 0, 128));
        colorItems.put(blue, new java.awt.Color(0, 0, 255));
        colorItems.put(brown, new java.awt.Color(165, 42, 42));
        colorItems.put(green, new java.awt.Color(0, 128, 0));
        colorItems.put(red, new java.awt.Color(255, 0, 0));
        colorItems.put(black, new java.awt.Color(0, 0, 0));
        colorItems.put(transparent, new java.awt.Color(0, 0, 0, 0));
        return colorItems;
    }

    public static boolean isTransparent(java.awt.Color color) {
        return color.getAlpha() != 255;
    }

    public static String getColorName(java.awt.Color color) {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        int a = color.getAlpha();
        if (r == 255 && g == 255 && b == 255) {
            return "ホワイト";
        } else if (r == 255 && g == 165 && b == 0) {
            return "オレンジ";
        } else if (r == 255 && g == 0 && b == 255) {
            return "マゼンタ";
        } else if (r == 173 && g == 216 && b == 230) {
            return "ライトブルー";
        } else if (r == 255 && g == 255 && b == 0) {
            return "イエロー";
        } else if (r == 0 && g == 255 && b == 0) {
            return "ライム";
        } else if (r == 255 && g == 192 && b == 203) {
            return "ピンク";
        } else if (r == 128 && g == 128 && b == 128) {
            return "グレー";
        } else if (r == 211 && g == 211 && b == 211) {
            return "ライトグレー";
        } else if (r == 0 && g == 255 && b == 255) {
            return "シアン";
        } else if (r == 128 && g == 0 && b == 128) {
            return "パープル";
        } else if (r == 0 && g == 0 && b == 255) {
            return "ブルー";
        } else if (r == 165 && g == 42 && b == 42) {
            return "ブラウン";
        } else if (r == 0 && g == 128 && b == 0) {
            return "グリーン";
        } else if (r == 255 && g == 0 && b == 0) {
            return "レッド";
        } else if (r == 0 && g == 0 && b == 0 && a == 255) {
            return "ブラック";
        } else if (r == 0 && g == 0 && b == 0 && a == 0) {
            return "透明";
        } else {
            return "カスタム: (" + r + ", " + g + ", " + b + ")";
        }
    }
}
