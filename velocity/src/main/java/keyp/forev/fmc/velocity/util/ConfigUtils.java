package keyp.forev.fmc.velocity.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.inject.Inject;

public class ConfigUtils {
    private final Config config;
    @Inject
    public ConfigUtils(Config config) {
        this.config = config;
    }

    public Set<String> getKeySet(Map<String, Map<String, Object>> configMap) {
        Set<String> keySet = new HashSet<>();
        for (Map<String, Object> innerMap : configMap.values()) {
            keySet.addAll(innerMap.keySet());
        }
        return keySet;
    }
    
    @SuppressWarnings("unchecked")
    public Map<String, Map<String, Object>> getConfigMap(String configKey) {
        if (configKey == null) {
            return null;
        }
        Object configKeyObj = config.getConfig().get(configKey);
        if (configKeyObj instanceof Map) {
            Map<String, Object> configMap = (Map<String, Object>) configKeyObj;
            Map<String, Map<String, Object>> result = new HashMap<>();
            processConfig(configMap, "", result);
            Map<String, Map<String, Object>> updatedResult = new HashMap<>();
            result.forEach((key, value) -> {
                Map<String, Object> updatedValue = new HashMap<>();
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

    public void configOutputWithType(Map<String, Map<String, Object>> configMap) {
        if (configMap != null) {
            configMap.forEach((key, value) -> {
                System.out.println("Server: " + key);
                value.forEach((k, v) -> {
                    if (v == null) {
                        System.out.println("  " + k + ": " + "(Null) " + v);
                    } else if (v instanceof Integer) {
                        System.out.println("  " + k + ": " + "(Integer) " + v);
                    } else if (v instanceof Boolean) {
                        System.out.println("  " + k + ": " + "(Boolean) " + v);
                    } else if (v instanceof String) {
                        System.out.println("  " + k + ": " + "(String) " + v);
                    } else if (v instanceof List) {
                        System.out.println("  " + k + ": " + "(List) " + v);
                    } else {
                        System.out.println("  " + k + ": " + "(Unknown type) " + v);
                    }
                });
            }); 
        }
    }

    @SuppressWarnings("unchecked")
    public void configOutput() {
        Map<String, Object> configMap = (Map<String, Object>) config.getConfig().get("Servers");
        Map<String, Map<String, Object>> result = new HashMap<>();
        processConfig(configMap, "", result);
        result.forEach((key, value) -> {
            System.out.println("Server: " + key);
            value.forEach((k, v) -> {
                System.out.println("  " + k.replace(key + "_", "") + ": " + v);
            });
        });
    }

    @SuppressWarnings("unchecked")
    private void processConfig(Map<String, Object> configMap, String parentKey, Map<String, Map<String, Object>> result) {
        for (Map.Entry<String, Object> entry : configMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String newKey = parentKey.isEmpty() ? key : parentKey + "_" + key;
            if (value instanceof Map) {
                processConfig((Map<String, Object>) value, newKey, result);
            } else {
                String serverName = parentKey.split("_")[0];
                result.putIfAbsent(serverName, new HashMap<>());
                result.get(serverName).put(newKey, value != null ? value : null);
            }
        }
    }
}
