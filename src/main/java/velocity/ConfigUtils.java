package velocity;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

import com.google.inject.Inject;

public class ConfigUtils {
    private final Logger logger;
    private final Config config;
    @Inject
    public ConfigUtils(Logger logger, Config config) {
        this.logger = logger;
        this.config = config;
    }

    public void testOutput() {
        String configKey = config.getString("Test.config.key", null);
        if (configKey != null) {
            logger.info("Test.config.key: " + configKey);
            getConfigMap(configKey).forEach((key, value) -> {
                System.out.println("Server: " + key);
                value.forEach((k, v) -> {
                    System.out.println("  " + k + ": " + v);
                });
            }); 
        } else {
            logger.error("Test.config.key is not found.");
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String,Map<String,String>> getConfigMap(String configKey) {
        if (configKey == null) {
            return null;
        }
        Object configKeyObj = config.getConfig().get(configKey);
        if (configKeyObj instanceof Map) {
            Map<String, Object> configMap = (Map<String, Object>) configKeyObj;
            Map<String, Map<String, String>> result = new HashMap<>();
            processConfig(configMap, "", result);
            Map<String, Map<String, String>> updatedResult = new HashMap<>();
            result.forEach((key, value) -> {
                Map<String, String> updatedValue = new HashMap<>();
                value.forEach((k, v) -> {
                    //value.put(k.replace(key + "_", ""), v);
                    updatedValue.put(k.replace(key + "_", ""), v);
                });
                updatedResult.put(key, updatedValue);
            });
            return updatedResult;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public void configOutput() {
        Map<String, Object> configMap = (Map<String, Object>) config.getConfig().get("Servers");
        Map<String, Map<String, String>> result = new HashMap<>();
        processConfig(configMap, "", result);
        result.forEach((key, value) -> {
            System.out.println("Server: " + key);
            value.forEach((k, v) -> {
                System.out.println("  " + k.replace(key + "_", "") + ": " + v);
            });
        });
    }

    @SuppressWarnings("unchecked")
    private void processConfig(Map<String, Object> configMap, String parentKey, Map<String, Map<String, String>> result) {
        for (Map.Entry<String, Object> entry : configMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String newKey = parentKey.isEmpty() ? key : parentKey + "_" + key;
            if (value instanceof Map) {
                processConfig((Map<String, Object>) value, newKey, result);
            } else {
                String serverName = parentKey.split("_")[0];
                result.putIfAbsent(serverName, new HashMap<>());
                result.get(serverName).put(newKey, value != null ? value.toString() : null);
            }
        }
    }
}
