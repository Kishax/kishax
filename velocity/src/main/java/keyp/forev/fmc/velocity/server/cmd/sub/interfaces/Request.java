package keyp.forev.fmc.velocity.server.cmd.sub.interfaces;

import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;

public interface Request {
  void execute(@NotNull CommandSource source, String[] args);
  void execute2(Player player, String targetServerName);
  String getExecPath(String serverName);
  Map<String, String> paternFinderMapForReq(String buttonMessage);
}
