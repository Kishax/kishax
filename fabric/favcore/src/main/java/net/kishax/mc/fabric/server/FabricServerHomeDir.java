package net.kishax.mc.fabric.server;

import com.google.inject.Inject;

import net.fabricmc.loader.api.FabricLoader;
import net.kishax.mc.common.server.interfaces.ServerHomeDir;

public class FabricServerHomeDir implements ServerHomeDir {
  private final FabricLoader fabric;

  @Inject
  public FabricServerHomeDir(FabricLoader fabric) {
    this.fabric = fabric;
  }

  @Override
  public String getServerName() {
    return fabric.getGameDir().toAbsolutePath().getParent().getFileName().toString();
  }
}
