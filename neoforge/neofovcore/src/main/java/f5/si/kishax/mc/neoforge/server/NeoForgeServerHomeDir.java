package f5.si.kishax.mc.neoforge.server;

import java.nio.file.Path;

import com.google.inject.Inject;

import f5.si.kishax.mc.common.server.interfaces.ServerHomeDir;

public class NeoForgeServerHomeDir implements ServerHomeDir {
  private final Path configPath;

  @Inject
  public NeoForgeServerHomeDir(Path configPath) {
    this.configPath = configPath;
  }

  @Override
  public String getServerName() {
    return configPath.getParent().getFileName().toString();
  }
}
