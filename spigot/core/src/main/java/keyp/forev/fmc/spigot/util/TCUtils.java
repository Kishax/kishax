package keyp.forev.fmc.spigot.util;

import keyp.forev.fmc.common.FMCSettings;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

public enum TCUtils {
    ASTERISK("asterisk"),
    JAVA_USER("javauser"),
    BEDROCK_USER("bedrockuser"),
    SETTINGS_ENTER("settings_enter"),
    LATER_OPEN_INV_3("later_open_inv_3"),
    LATER_OPEN_INV_5("later_open_inv_5"),
    INPUT_MODE("input_mode"),
    ZERO("0"),
    ONE("1"),
    TWO("2"),
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
            case "later_open_inv_3", "later_open_inv_5" -> {
                String number = key.substring(key.lastIndexOf('_') + 1);
                message.addExtra(number + "秒後にインベントリを開きます。");
                message.setBold(true);
                message.setUnderlined(true);
                message.setColor(ChatColor.GOLD);
            }
            case "input_mode" -> {
                message.addExtra("-------user-input-mode(" + FMCSettings.INPUT_PERIOD.getValue() + "s)-------");
                message.setItalic(true);
                message.setColor(ChatColor.BLUE);
                TextComponent message2 = new TextComponent("\n以下、入力する内容は、チャット欄には表示されません。");
                message2.setItalic(true);
                message2.setColor(ChatColor.GRAY);
                message.addExtra(message2);
            }
            case "0", "1", "2" -> {
                message.addExtra(key);
                message.setBold(true);
                message.setUnderlined(true);
                message.setColor(ChatColor.GOLD);
                message.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, key));
                message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("クリックして入力")));
            }
        }
        this.value = message;
    }

    public TextComponent get() {
        return this.value;
    }
}
