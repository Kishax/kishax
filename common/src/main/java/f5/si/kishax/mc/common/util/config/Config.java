package f5.si.kishax.mc.common.util.config;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public class Config {
  protected Map<String, Object> config = null;
  protected final String configName;
  protected final Path dataDirectory;
  private final Logger logger;

  public Config(Logger logger, Path dataDirectory, String configName) {
    this.logger = logger;
    this.dataDirectory = dataDirectory;
    this.configName = configName;
  }

  public Map<String, Object> getConfig() {
    return config;
  }

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
        String addContents = "";
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

  public void setConfig() {
    if (config == null) {
      try {
        loadConfig();
      } catch (IOException e) {
        logger.error("Error loading {}.", configName, e);
      }
    }
  }

  @SuppressWarnings("unchecked")
  public List<String> getStringList(String key) {
    if (Objects.isNull(config)) {
      logger.error("Config has not been initialized.");
      return Collections.emptyList();
    }
    Object value = config.get(key);
    if (value instanceof List) {
      return (List<String>) value;
    } else if (Objects.isNull(value)) {
      logger.error("The key '" + key + "' does not exist in the configuration.");
      return Collections.emptyList();
    } else {
      logger.error("The value for the key '" + key + "' is not a list.");
      return Collections.emptyList();
    }
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> getMap(String key) {
    if (Objects.isNull(config)) {
      logger.error("Config has not been initialized.");
      return null;
    }
    Object value = config.get(key);
    if (value instanceof Map) {
      return (Map<String, Object>) value;
    } else if (Objects.isNull(value)) {
      logger.error("The key '" + key + "' does not exist in the configuration.");
      return null;
    } else {
      logger.error("The value for the key '" + key + "' is not a map.");
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  public Object getNestedValue(String path) {
    if (Objects.isNull(config))	return null;
    String[] keys = path.split("\\.");
    Map<String, Object> currentMap = config;
    for (int i = 0; i < keys.length; i++) {
      Object value = currentMap.get(keys[i]);
      if (Objects.isNull(value))	return null;
      if (i == keys.length - 1)	return value;
      if (value instanceof Map) {
        currentMap = (Map<String, Object>) value;
      } else {
        return null; // キーがマップではない場合
      }
    }
    return null;
  }

  public String getString(String path, String defaultValue) {
    Object value = getNestedValue(path);
    return value instanceof String ? (String) value : defaultValue;
  }

  public String getString(String path) {
    return getString(path, null);
  }

  public boolean getBoolean(String path, boolean defaultValue) {
    Object value = getNestedValue(path);
    return value instanceof Boolean ? (Boolean) value : defaultValue;
  }

  public boolean getBoolean(String path) {
    return getBoolean(path, false);
  }

  public int getInt(String path, int defaultValue) {
    Object value = getNestedValue(path);
    if (value instanceof Number number) {
      return number.intValue();
    }
    return defaultValue;
  }

  public int getInt(String path) {
    return getInt(path, 0);
  }

  public long getLong(String path, long defaultValue) {
    Object value = getNestedValue(path);
    if (value instanceof Number number) {
      return number.longValue();
    }
    return defaultValue;
  }

  public long getLong(String path) {
    return getLong(path, 0L);
  }

  @SuppressWarnings("unchecked")
  public List<String> getList(String path, List<String> defaultValue) {
    Object value = getNestedValue(path);
    return value instanceof List ? (List<String>) value : defaultValue;
  }

  public List<String> getList(String path) {
    return getList(path, Collections.emptyList());
  }

  @SuppressWarnings("unchecked")
  public List<Map<?, ?>> getListMap(String path) {
    Object value = getNestedValue(path);
    return value instanceof List ? (List<Map<?, ?>>) value : Collections.emptyList();
  }

  public Map<String, Object> getStringObjectMap(String key) {
    Object mapObject = getConfig().get(key);
    if (mapObject instanceof Map<?, ?> tempMap) {
      tempMap = (Map<?, ?>) mapObject;
      boolean isStringObjectMap = true;
      for (Map.Entry<?, ?> entry : tempMap.entrySet()) {
        if (!(entry.getKey() instanceof String) || !(entry.getValue() instanceof Object)) {
          isStringObjectMap = false;
          break;
        }
      }
      if (isStringObjectMap) {
        @SuppressWarnings("unchecked") // checked by above, So this annotation doen not need
        Map<String, Object> mapConfig = (Map<String, Object>) mapObject;
        return mapConfig;
      }
    }
    return null;
  }

  public Map<String, Object> getStringObjectMap(Object mapObject) {
    if (mapObject instanceof Map<?, ?> tempMap) {
      tempMap = (Map<?, ?>) mapObject;
      boolean isStringObjectMap = true;
      for (Map.Entry<?, ?> entry : tempMap.entrySet()) {
        if (!(entry.getKey() instanceof String) || !(entry.getValue() instanceof Object)) {
          isStringObjectMap = false;
          break;
        }
      }
      if (isStringObjectMap) {
        @SuppressWarnings("unchecked")
        Map<String, Object> mapConfig = (Map<String, Object>) mapObject;
        return mapConfig;
      }
    }

    return null;
  }

  public void replaceValue(String key, Object newValue) throws IOException {
    if (config != null) {
      config.put(key, newValue);
      saveConfig();
      loadConfig();
    }
  }
}
