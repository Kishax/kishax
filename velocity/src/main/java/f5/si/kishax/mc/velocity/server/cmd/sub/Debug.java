package f5.si.kishax.mc.velocity.server.cmd.sub;

import java.io.IOException;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;

import f5.si.kishax.mc.velocity.util.config.VelocityConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class Debug {
  private final VelocityConfig config;
  private final Logger logger;

  @Inject
  public Debug(VelocityConfig config, Logger logger) {
    this.config = config;
    this.logger = logger;
  }

  public void execute(@NotNull CommandSource source, String[] args) {
    Map<String, Object> DebugConfig = config.getStringObjectMap("Debug");
    Map<String, Object> DiscordConfig = config.getStringObjectMap("Discord");
    if (DebugConfig == null || DiscordConfig == null) {
      Component errorMessage = Component.text("コンフィグの設定が正しくありません。")
        .appendNewline()
        .append(Component.text("(Debug or Discord) Map is Empty.")
            .color(NamedTextColor.RED));
      source.sendMessage(errorMessage);
      return;
    }

    String value1 = config.getString("Debug.Webhook_URL", null),
      value2 = config.getString("Discord.Webhook_URL", null);

    if (value1 != null && value2 != null) {
      DiscordConfig.put("Webhook_URL", value1);
      DebugConfig.put("Webhook_URL", value2);
    } else {
      source.sendMessage(Component.text("コンフィグの設定が不十分です。(Webhook_URL)").color(NamedTextColor.RED));
    }

    long value3 = config.getLong("Debug.ChannelId", 0),
      value4 = config.getLong("Discord.ChannelId", 0),
      value5 = config.getLong("Debug.ChatChannelId", 0),
      value6 = config.getLong("Discord.ChatChannelId", 0),
      value7 = config.getLong("Debug.AdminChannelId", 0),
      value8 = config.getLong("Discord.AdminChannelId", 0);

    if (value3 != 0 && value4 != 0) {
      DiscordConfig.put("ChannelId", value3);
      DebugConfig.put("ChannelId", value4);
    } else {
      source.sendMessage(Component.text("コンフィグの設定が不十分です。(ChannelId)").color(NamedTextColor.RED));
    }

    if (value5 != 0 && value6 != 0) {
      DiscordConfig.put("ChatChannelId", value5);
      DebugConfig.put("ChatChannelId", value6);
    } else {
      source.sendMessage(Component.text("コンフィグの設定が不十分です。(ChatChannelId)").color(NamedTextColor.RED));
    }

    if (value7 != 0 && value8 != 0) {
      DiscordConfig.put("AdminChannelId", value7);
      DebugConfig.put("AdminChannelId", value8);
    } else {
      source.sendMessage(Component.text("コンフィグの設定が不十分です。(AdminChannelId)").color(NamedTextColor.RED));
    }

    if (config.getBoolean("Debug.Mode", false)) {
      source.sendMessage(Component.text("デバッグモードがOFFになりました。").color(NamedTextColor.GREEN));
      DebugConfig.put("Mode", false);
    } else {
      source.sendMessage(Component.text("デバッグモードがONになりました。").color(NamedTextColor.GREEN));
      DebugConfig.put("Mode", true);
    }

    try {
      config.saveConfig();
    } catch (IOException e) {
      source.sendMessage(Component.text("コンフィグへの保存に失敗しました。").color(NamedTextColor.RED));
      logger.error("An IOException error occurred: " + e.getMessage());
      for (StackTraceElement element : e.getStackTrace()) {
        logger.error(element.toString());
      }
    }
  }
}
