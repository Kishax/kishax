package net.kishax.mc.velocity.socket.message.handlers.web;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import net.kishax.mc.common.database.Database;
import net.kishax.mc.common.server.Luckperms;
import net.kishax.mc.common.settings.PermSettings;
import net.kishax.mc.common.socket.SocketSwitch;
import net.kishax.mc.common.socket.message.Message;
import net.kishax.mc.common.socket.message.handlers.interfaces.web.MinecraftWebConfirmHandler;
import net.kishax.mc.velocity.discord.MessageEditor;
import net.kishax.mc.velocity.util.config.VelocityConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class VelocityMinecraftWebConfirmHandler implements MinecraftWebConfirmHandler {
  private final Logger logger;
  private final ProxyServer server;
  private final Database db;
  private final VelocityConfig config;
  private final Luckperms lp;
  private final MessageEditor discordME;
  private final Provider<SocketSwitch> sswProvider;

  @Inject
  public VelocityMinecraftWebConfirmHandler(Logger logger, ProxyServer server, Database db, VelocityConfig config,
      Luckperms lp, MessageEditor discordME, Provider<SocketSwitch> sswProvider) {
    this.logger = logger;
    this.server = server;
    this.db = db;
    this.config = config;
    this.lp = lp;
    this.discordME = discordME;
    this.sswProvider = sswProvider;
  }

  @Override
  public void handle(Message.Web.MinecraftConfirm confirm) {
    String mineName = confirm.who.name;
    lp.addPermission(mineName, PermSettings.NEW_USER.get());

    Message msg = new Message();
    msg.mc = new Message.Minecraft();
    msg.mc.sync = new Message.Minecraft.Sync();
    msg.mc.sync.content = "MEMBER";

    // spigotsに通知し、serverCacheを更新させる
    try (Connection conn = db.getConnection()) {
      SocketSwitch ssw = sswProvider.get();
      ssw.sendSpigotServer(conn, msg);
    } catch (SQLException | ClassNotFoundException e) {
      logger.error("An error occurred while updating the database: " + e.getMessage(), e);
      for (StackTraceElement element : e.getStackTrace()) {
        logger.error(element.toString());
      }
    }
    Optional<Player> playerOptional = server.getAllPlayers().stream()
        .filter(player -> player.getUsername().equalsIgnoreCase(mineName))
        .findFirst();
    if (playerOptional.isPresent()) {
      Player player = playerOptional.get();
      TextComponent component;
      String DiscordInviteUrl = config.getString("Discord.InviteUrl", "");
      if (!DiscordInviteUrl.isEmpty()) {
        component = Component.text()
            .appendNewline()
            .append(Component.text("WEB認証")
                .color(NamedTextColor.LIGHT_PURPLE)
                .decorate(
                    TextDecoration.BOLD,
                    TextDecoration.UNDERLINED))
            .append(Component.text("が完了しました。")
                .color(NamedTextColor.AQUA))
            .appendNewline()
            .append(Component.text("池に飛び込もう！")
                .color(NamedTextColor.AQUA))
            .appendNewline()
            .appendNewline()
            .append(Component.text("Kishaxサーバーの")
                .color(NamedTextColor.AQUA))
            .append(Component.text("Discord")
                .color(NamedTextColor.BLUE)
                .decorate(
                    TextDecoration.BOLD,
                    TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.openUrl(DiscordInviteUrl))
                .hoverEvent(HoverEvent.showText(Component.text("KishaxサーバーのDiscordへいこう！"))))
            .append(Component.text("には参加しましたか？")
                .color(NamedTextColor.AQUA))
            .appendNewline()
            .append(Component.text("ここでは、個性豊かな色々なメンバーと交流ができます！")
                .color(NamedTextColor.AQUA))
            .appendNewline()
            .append(Component.text("ぜひ、参加してみてください！")
                .color(NamedTextColor.AQUA))
            .build();
        player.sendMessage(component);
      }
    }

    try {
      discordME.AddEmbedSomeMessage("AddMember", mineName);
    } catch (Exception e) {
      logger.error("An exception occurred while executing the AddEmbedSomeMessage method: {}", e.getMessage());
      for (StackTraceElement ste : e.getStackTrace()) {
        logger.error(ste.toString());
      }
    }
  }
}
