package f5.si.kishax.mc.fabric.server;

import com.google.inject.Inject;

import f5.si.kishax.mc.common.server.interfaces.ServerHomeDir;
import net.fabricmc.loader.api.FabricLoader;

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
