package spigot;

import java.util.logging.Level;

import com.google.inject.Inject;

public class SocketResponse {
    private final common.Main plugin;
    @Inject
    public SocketResponse(common.Main plugin) {
        this.plugin = plugin;
    }

    public void resaction(String res) {
        plugin.getLogger().log(Level.INFO, "Received Message: {0}", res);
    }
}