package keyp.forev.fmc.util;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;

public class TCUtils2 {
    private final String value;
    private final TextComponent message;
    public TCUtils2(String input) {
        this.value = input;
        this.message = new TextComponent();
    }

    public TextComponent getResponseComponent() {
        message.addExtra("\n" + this.value);
        message.setBold(true);
        message.setItalic(true);
        message.setColor(ChatColor.GRAY);
        TextComponent message2 = new TextComponent(" と入力されました。\n");
        message2.setColor(ChatColor.GRAY);
        message2.setItalic(true);
        message.addExtra(message2);
        return message;
    }
}
