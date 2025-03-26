package keyp.forev.fmc.spigot.server.cmd.sub.teleport;

// this is depended on BetterNavi plugin!
// https://github.com/ThomasVerschoor/BetterNav

import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import com.google.inject.Inject;

public class Navi implements TabExecutor {
  @Inject
  public Navi() {
  }

  @Override
  public boolean onCommand(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
    if (sender instanceof Player player) {
      player.performCommand("fmc menu tp navi");
    } else {
      if (sender != null) {
        sender.sendMessage("プレイヤーからのみ実行可能です。");
      }
    }
    return true;
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
    return Collections.emptyList();
  }
}
