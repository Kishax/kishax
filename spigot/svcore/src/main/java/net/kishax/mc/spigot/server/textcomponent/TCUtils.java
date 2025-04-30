package net.kishax.mc.spigot.server.textcomponent;

import net.kishax.mc.common.settings.Settings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

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

  private final String key;
  TCUtils(String key) {
    this.key = key;
  }

  public Component get() {
    switch (key) {
      case "asterisk" -> {
        return Component.text("※")
          .appendNewline()
          .color(NamedTextColor.GRAY)
          .decorate(TextDecoration.ITALIC);
        }
      case "javauser" -> {
        return Component.text("Java版ユーザー")
          .color(NamedTextColor.GRAY)
          .decorate(
            TextDecoration.ITALIC,
            TextDecoration.UNDERLINED);
      }
      case "bedrockuser" -> {
        return Component.text("Bedrock版ユーザー")
          .color(NamedTextColor.GRAY)
          .decorate(
            TextDecoration.ITALIC,
            TextDecoration.UNDERLINED);
          }
      case "settings_enter" -> {
        return Component.text("この機能はいつでもメニュー>設定より変更できます。")
          .color(NamedTextColor.GRAY)
          .decorate(TextDecoration.ITALIC);
      }
      case "later_open_inv_3", "later_open_inv_5" -> {
        String seconds = key.substring(key.lastIndexOf('_') + 1);
        return Component.text(seconds + "秒後にインベントリを開きます。")
          .color(NamedTextColor.GOLD)
          .decorate(
            TextDecoration.BOLD,
            TextDecoration.UNDERLINED);
        }
      case "input_mode" -> {
        Component t1 = Component.text("-------user-input-mode(" + Settings.INPUT_PERIOD.getValue() + "s)-------")
          .color(NamedTextColor.BLUE)
          .decorate(TextDecoration.ITALIC);
        Component t2 = Component.newline()
          .append(Component.text("以下、入力する内容は、チャット欄には表示されません。"))
          .color(NamedTextColor.GRAY)
          .decorate(TextDecoration.ITALIC);
        return t1.append(t2);
      }
      case "0", "1", "2" -> {
        return Component.text(key)
          .color(NamedTextColor.GOLD)
          .decorate(
            TextDecoration.BOLD,
            TextDecoration.ITALIC)
          .hoverEvent(HoverEvent.showText(Component.text("クリックして入力")))
          .clickEvent(ClickEvent.suggestCommand(key));
      }
      default -> throw new IllegalArgumentException("Unexpected value: " + key);
    }
  }
}
