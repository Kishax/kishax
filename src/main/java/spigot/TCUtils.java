package spigot;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;

public enum TCUtils {
    ASTERISK("asterisk"),
    JAVA_USER("javauser"),
    BEDROCK_USER("bedrockuser"),
    SETTINGS_ENTER("settings_enter"),
    LATER_OPEN_INV("later_open_inv"),
    ;
	private final TextComponent value;
	TCUtils(String key) {
        TextComponent message = new TextComponent();
        switch (key) {
            case "asterisk" -> {
                message.addExtra("\n※\s");
                message.setColor(ChatColor.GRAY);
                message.setItalic(true);
            }
            case "javauser" -> {
                message.addExtra("Java版ユーザー");
                message.setColor(ChatColor.GRAY);
                message.setItalic(true);
                message.setUnderlined(true);
            }
            case "bedrockuser" -> {
                message.addExtra("Bedrock版ユーザー");
                message.setColor(ChatColor.GRAY);
                message.setItalic(true);
                message.setUnderlined(true);
            }
            case "settings_enter" -> {
                message.addExtra("この機能はいつでもメニュー>設定より変更できます。");
                message.setColor(ChatColor.GRAY);
                message.setItalic(true);
            }
            case "later_open_inv" -> {
                message.addExtra("3秒後にインベントリを開きます。");
                message.setBold(true);
                message.setUnderlined(true);
                message.setColor(ChatColor.GOLD);
            }
        }
        this.value = message;
    }

    public TextComponent get() {
        return this.value;
    }
}
