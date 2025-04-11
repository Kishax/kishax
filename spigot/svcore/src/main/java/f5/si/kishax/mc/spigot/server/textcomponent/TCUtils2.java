package f5.si.kishax.mc.spigot.server.textcomponent;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class TCUtils2 {
  public static Component getResponseComponent(String input) {
    Component message1 = Component.newline()
      .append(Component.text(input))
      .color(NamedTextColor.GRAY)
      .decorate(
        TextDecoration.BOLD,
        TextDecoration.ITALIC);

    Component message2 = Component.text(" と入力されました。")
      .appendNewline()
      .color(NamedTextColor.GRAY)
      .decorate(TextDecoration.ITALIC);

    return message1.append(message2);
  }
}
