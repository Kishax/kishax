package f5.si.kishax.mc.velocity.server.cmd.sub;

import java.util.Objects;

import com.google.inject.Inject;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class CommandForwarder {
  private final ProxyServer server;
  private final ConsoleCommandSource console;

  @Inject
  public CommandForwarder (ProxyServer server, ConsoleCommandSource console) {
    this.server = server;
    this.console = console;
  }

  public void forwardCommand(String execPlayer, String command, String targetPlayer) {
    // ターゲットプレイヤーを取得
    Player player = server.getPlayer(targetPlayer).orElse(null);
    if (Objects.nonNull(player)) {
      // プレイヤーが存在すればコマンドを実行
      //logger.info("Forwarding command to " + targetPlayer + ": " + command);
      server.getCommandManager().executeAsync(player, command);
    } else {
      Component errorMessage = Component.text("プレイヤー " + targetPlayer + " は見つかりませんでした。").color(NamedTextColor.RED); 
      // /kishax fv を打ったプレイヤー名を取得して、そいつにsendメッセージしないといけない
      if (execPlayer.equals("?")) {
        // コンソールからコマンドを実行した場合
        console.sendMessage(errorMessage);
      } else {
        // プレイヤーがコマンドを実行した場合
        // 実行プレイヤーを取得
        Player execplayer = server.getPlayer(execPlayer).orElse(null);
        if (Objects.nonNull(execplayer)) {
          execplayer.sendMessage(errorMessage);
        }
      }
    }
  }
}

