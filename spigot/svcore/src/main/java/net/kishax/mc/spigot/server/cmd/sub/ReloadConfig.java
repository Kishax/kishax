package net.kishax.mc.spigot.server.cmd.sub;

import net.kishax.mc.spigot.util.config.PortalsConfig;
import net.md_5.bungee.api.ChatColor;

import org.slf4j.Logger;
import org.bukkit.command.CommandSender;

import com.google.inject.Inject;

public class ReloadConfig {
  private final Logger logger;
  private final PortalsConfig psConfig;

  @Inject
  public ReloadConfig(Logger logger, PortalsConfig psConfig) {
    this.logger = logger;
    this.psConfig = psConfig;
  }

  public void execute(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
    try {
      psConfig.loadConfig();
      sender.sendMessage(ChatColor.GREEN+"コンフィグをリロードしました。");
    } catch (Exception e) {
      sender.sendMessage(ChatColor.RED+"コンフィグのリロードに失敗しました。");
      logger.error("An error occurred while reloading the config: "+e.getMessage());
      for (StackTraceElement element : e.getStackTrace()) {
        logger.error(element.toString());
      }
    }
  }
}
