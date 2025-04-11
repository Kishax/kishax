package f5.si.kishax.mc.velocity.util.config;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import com.google.inject.Inject;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import f5.si.kishax.mc.common.util.config.Config;

import com.google.inject.Singleton;

@Singleton
public class VelocityConfig extends Config {
  private final Logger logger;
  private final ProxyServer server;

  @Inject
  public VelocityConfig(Logger logger, @DataDirectory Path dataDirectory, ProxyServer server) {
    super(logger, dataDirectory, "config.yml");
    this.logger = logger;
    this.server = server;
    setConfig();
  }

  @Override
  public void loadConfig() throws IOException {
    Path configPath = dataDirectory.resolve(configName);
    if (Files.notExists(dataDirectory)) {
      Files.createDirectories(dataDirectory);
    }
    if (!Files.exists(configPath)) {
      try (InputStream in = getClass().getResourceAsStream("/" + configName)) {
        if (in == null) {
          logger.error("{} not found in resources.", configName);
          return;
        }
        Files.copy(in, configPath);
        String existingContent = Files.readString(configPath);
        String addContents = "\n\nServers:\n";
        addContents += "\n\n    proxy:";
        addContents += "\n        entry: false";
        addContents += "\n        platform: \"\"";
        addContents += "\n        memory: ";
        for (RegisteredServer registeredServer : server.getAllServers()) {
          addContents += "\n    " + registeredServer.getServerInfo().getName() + ":";
          addContents += "\n        entry: false";
          addContents += "\n        hub: false";
          addContents += "\n        platform: \"\"";
          addContents += "\n        type: \"\"";
          addContents += "\n        modded:";
          addContents += "\n          mode: false";
          addContents += "\n          listUrl: \"\"";
          addContents += "\n          loaderType: \"\"";
          addContents += "\n          loaderUrl: \"\"";
          addContents += "\n        distributed:";
          addContents += "\n          mode: false";
          addContents += "\n          url: \"\"";
          addContents += "\n        memory: ";
          addContents += "\n        exec: \"\"";
        }
        Files.writeString(configPath, existingContent + addContents);
      }
    }
    Yaml yaml = new Yaml();
    try (InputStream inputStream = Files.newInputStream(configPath)) {
      config = yaml.load(inputStream);
      if (config == null) {
        logger.error("Failed to load {}: null.", configName);
      } else {
        logger.info("{} loaded successfully.", configName);
      }
    } catch (IOException e) {
      logger.error("Error reading {}.", configName, e);
    }
  }

  public void saveConfig() throws IOException {
    Path configPath = dataDirectory.resolve(configName);
    DumperOptions options = new DumperOptions();
    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    Yaml yaml = new Yaml(options);
    try (FileWriter writer = new FileWriter(configPath.toFile())) {
      yaml.dump(config, writer);
    }
  }
}
