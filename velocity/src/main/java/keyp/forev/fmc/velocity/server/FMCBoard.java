package keyp.forev.fmc.velocity.server;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import de.timongcraft.veloboard.VeloBoard;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import keyp.forev.fmc.velocity.Main;

@Singleton
public class FMCBoard {
	private final Main plugin;
    private final ProxyServer server;
    private final Map<UUID, VeloBoard> boards = new HashMap<>();
    @Inject
    public FMCBoard(Main plugin, ProxyServer server) {
    	this.plugin = plugin;
    	this.server = server;
    }

    public void updateScheduler() {
        server.getScheduler().buildTask(plugin, () -> {
            for (VeloBoard board : getBoards().values()) {
                updateBoard(board);
            }
        }).repeat(Duration.ofSeconds(1)).schedule();
    }

    public void resendBoard(UUID uuid) {
        VeloBoard board = boards.get(uuid);
        if (board != null) {
            board.resend();
            //logger.info("Board resent for player UUID: " + uuid);
        }
    }

    public void addBoard(Player player) {
        VeloBoard board = new VeloBoard(player);
        board.updateTitle(Component.text("FMC Server").color(NamedTextColor.GOLD));
        getBoards().put(player.getUniqueId(), board);
        //logger.info("Board added for player: " + player.getUsername());
    }

    public void removeBoard(UUID uuid) {
        VeloBoard board = getBoards().remove(uuid);
        if (board != null) {
            board.delete();
        }
    }

    private void updateBoard(VeloBoard board) {
        board.updateLines(
            Component.empty(),
            Component.text("Players: " + server.getPlayerCount()),
            Component.empty(),
            Component.text("Ping: " + board.getPlayer().getPing()),
            Component.empty()
        );
    }

    private Map<UUID, VeloBoard> getBoards() {
        return this.boards;
    }
}
