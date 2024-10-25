package velocity;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ConfigUtils {
    private final Logger logger;
    //private final Config config;
    private Map<String, Object> savedConfig = new HashMap<>();
    @Inject
    public ConfigUtils(Logger logger, Config config) {
        this.logger = logger;
        //this.config = config;
    }

    public void configKeySetOutPut(Map<String, Object> servers) {
        this.savedConfig = servers;
        Map<String, Map<String, String>> flattenedConfig = flattenConfig(servers);
        flattenedConfig.entrySet().forEach(entry -> {
            String key = entry.getKey();
            Map<String, String> value = entry.getValue();
            if (savedConfig.containsKey(key)) {
                logger.info("key: " + key);
                for (Map.Entry<String, String> subEntry : value.entrySet()) {
                    logger.info("  " + subEntry.getKey() + ": " + subEntry.getValue());
                }
            } else {
                logger.info("  key: " + key);
                for (Map.Entry<String, String> subEntry : value.entrySet()) {
                    logger.info("    " + subEntry.getKey() + ": " + subEntry.getValue());
                }
            }
        });
    }

    private Map<String, Map<String, String>> flattenConfig(Map<String, Object> config) {
        Map<String, Map<String, String>> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map) {
                flattenMap(key, (Map<?, ?>) value, result);
            } else {
                result.computeIfAbsent(key, k -> new HashMap<>()).put(key, value != null ? value.toString() : null);
            }
        }
        return result;
    }

    private void flattenMap(String parentKey, Map<?, ?> map, Map<String, Map<String, String>> result) {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = entry.getKey().toString();
            Object value = entry.getValue();
            String newKey;
            if (savedConfig.containsKey(parentKey)) {
                newKey = key;
            } else {
                // 特定のKeyの中で、Mapをもつ値に当たった場合、
                // もしくは、特定のKeyの中で、Mapでない値をもつ値に当たった場合
                newKey = parentKey + "_" + key;
            }
            if (value instanceof Map) {
                flattenMap(newKey, (Map<?, ?>) value, result);
            } else {
                result.computeIfAbsent(parentKey, k -> new HashMap<>()).put(newKey, value != null ? value.toString() : null);
            }
        }
    }
}