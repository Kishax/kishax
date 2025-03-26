package keyp.forev.fmc.spigot.server;

import java.io.File;
import java.util.Objects;

import com.google.inject.Inject;

import keyp.forev.fmc.common.server.interfaces.ServerHomeDir;

import org.bukkit.plugin.java.JavaPlugin;

public class SpigotServerHomeDir implements ServerHomeDir {
  private final File dataFolder;

  @Inject
  public SpigotServerHomeDir(JavaPlugin plugin) {
    this.dataFolder = plugin.getDataFolder();
  }

  @Override
  public String getServerName() {
    return getParentDir(dataFolder);
  }

  private String getParentDir(File dataFolder) {
    File serverHomeDirectory = dataFolder.getParentFile();
    String homeDirectoryPath = serverHomeDirectory.getAbsolutePath();
    File file = new File(homeDirectoryPath);
    File parentDir = file.getParentFile();
    if (Objects.nonNull(parentDir)) {
      return parentDir.getName();
    }
    return null;
  }
}
