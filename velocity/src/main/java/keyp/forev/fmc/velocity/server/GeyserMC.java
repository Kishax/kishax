package keyp.forev.fmc.velocity.server;

import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.util.LinkedPlayer;

import com.google.inject.Inject;
import com.velocitypowered.api.proxy.Player;

public class GeyserMC {
  private final FloodgateApi fg = FloodgateApi.getInstance();

  @Inject
  public GeyserMC() {
  }

  public boolean isGeyserPlayer(Player player) {
    // Floodgate APIを使用してGeyserプレイヤーかどうかをチェック
    return fg.isFloodgatePlayer(player.getUniqueId());
  }

  public String getGeyserPlayerXuid(Player player) {
    if (isGeyserPlayer(player)) {
      FloodgatePlayer floodgatePlayer = fg.getPlayer(player.getUniqueId());
      if (floodgatePlayer != null) {
        return floodgatePlayer.getXuid();
      }
    }
    return null;
  }

  public LinkedPlayer getGeyserLinkedPlayer(Player player) {
    if (isGeyserPlayer(player)) {
      FloodgatePlayer floodgatePlayer = fg.getPlayer(player.getUniqueId());
      if (floodgatePlayer != null) {
        return floodgatePlayer.getLinkedPlayer();
      }
    }
    return null;
  }
}
