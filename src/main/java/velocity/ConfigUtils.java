package velocity;

import java.util.HashMap;
import java.util.Map;

import com.google.inject.Inject;

public class ConfigUtils {
    private final Config config;
    @Inject
    public ConfigUtils(Config config) {
        this.config = config;
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
