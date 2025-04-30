package net.kishax.mc.spigot.socket.message.handlers.minecraft;

import com.google.inject.Inject;

import net.kishax.mc.common.server.ServerStatusCache;
import net.kishax.mc.common.socket.message.Message;
import net.kishax.mc.common.socket.message.handlers.interfaces.minecraft.SyncContentHandler;

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

