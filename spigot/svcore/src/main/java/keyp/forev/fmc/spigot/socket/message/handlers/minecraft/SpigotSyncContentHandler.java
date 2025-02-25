package keyp.forev.fmc.spigot.socket.message.handlers.minecraft;

import com.google.inject.Inject;

import keyp.forev.fmc.common.server.ServerStatusCache;
import keyp.forev.fmc.common.socket.message.Message;
import keyp.forev.fmc.common.socket.message.handlers.interfaces.minecraft.SyncContentHandler;

public class SpigotSyncContentHandler implements SyncContentHandler {
    private final ServerStatusCache ssc;

    @Inject
    public SpigotSyncContentHandler(ServerStatusCache ssc) {
        this.ssc = ssc;
    }

    @Override
    public void handle(Message.Minecraft.Sync sync) {
        switch (sync.content) {
            case "STATUS" -> {
                ssc.refreshCache();
            }
            case "MEMBER" -> {
                ssc.refreshMemberInfo();
            }
        }
    }
}

