package velocity;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ConfigUtils {
    private final Logger logger;
    private final Config config;
    private Map<String, Object> savedConfig = new HashMap<>();
    private int floor = 0;
    // ○階層めにおいて、○個めのMapに入ったときのKeySetを保存するMap
    private Map<Integer, Map<Integer, Set<String>>> floorKeySets = new HashMap<>();
    @Inject
    public ConfigUtils(Logger logger, Config config) {
        this.logger = logger;
        this.config = config;
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
    // ConfigUtils1-3を作成して、ConfigUtils2を採用することにした
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
                //logger.info(value + " is Map!!");
                flattenMap(newKey, (Map<?, ?>) value, result);
            } else {
                //logger.info(value + " is not Map!!");
                result.computeIfAbsent(parentKey, k -> new HashMap<>()).put(newKey, value != null ? value.toString() : null);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void output() {
        logger.info("output");
        Object servers = config.getConfig().get("Servers");
        if (servers instanceof Map) {
            Map<String, Object> serversMap = (Map<String, Object>) servers;
            floor++; // 1
            floorKeySets.put(floor, Map.of(floor, serversMap.keySet()));
            int index = 0;
            for (Map.Entry<String, Object> entry : serversMap.entrySet()) {
                String key = entry.getKey(); // サーバー名
                logger.info("key: " + key);
                Object value = entry.getValue();
                if (value instanceof Map) {
                    Map<String, Object> server = (Map<String, Object>) value;
                    if (index == 0) {
                        floor++; // 2
                        floorKeySets.put(floor, Map.of(2, server.keySet()));
                    }
                    for (Map.Entry<String, Object> entry2 : server.entrySet()) {
                        String indent = " ".repeat(floor + 1);
                        logger.info(indent + "key: " + entry2.getKey());
                        Object isIfUnderMap = entry2.getValue();
                        output2(isIfUnderMap); // ここで再帰呼び出し
                        //logger.info("key: " + e.getKey() + ", value: " + e.getValue());
                    }
                }
                index++;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void output2(Object isIfUnderMaps) {
        if (isIfUnderMaps instanceof Map) {
            floor++; // mapをみつけたら問答無用で階層をプラスする
            Map<String, Object> isIfUnderMap = (Map<String, Object>) isIfUnderMaps;

            isIfUnderMap.entrySet().stream().forEach(e -> {
                String key = e.getKey();
                // 1つ前の階層のMapに、今の階層のkeyが含まれているかどうか
                floorKeySets.keySet().stream().filter(key2 -> key2 == floor - 1).forEach(key2 -> {
                    Map<Integer, Set<String>> isIfUnderMap2 = floorKeySets.get(key2);
                    Set<String> beforeFloorKeySets = isIfUnderMap2.get(key2);
                    if (beforeFloorKeySets != null) {
                        if (beforeFloorKeySets.contains(key)) {
                            floor--; // 1つ前の階層のMapのkeySetに、今の階層のkeyが含まれている場合、階層を戻す
                        } else {
                            // 1つ前の階層のMapのkeySetに、今の階層のkeyが含まれていない場合、
                            floorKeySets.keySet().stream().filter(key3 -> key3 == floor).forEach(key3 -> {
                                Map<Integer, Set<String>> isIfUnderMap3 = floorKeySets.get(key3);
                                Integer maxMapNums = isIfUnderMap3.entrySet().stream()
                                    .max(Map.Entry.comparingByKey())
                                    .map(Map.Entry::getKey)
                                    .orElse(0);
                                if (maxMapNums != null) {
                                    floorKeySets.put(floor, Map.of(maxMapNums + 1, isIfUnderMap.keySet()));
                                }
                            });
                        }
                    }
                });
                String indent = " ".repeat(floor + 1);
                logger.info(indent + "key: " + key);
                Object isIfUnderMap2 = e.getValue();
                output2(isIfUnderMap2); // ここで再帰呼び出し
            });
        } else {
            String last = (String) isIfUnderMaps;
            String indent = " ".repeat(floor + 1);
            logger.info(indent + "last: " + (String) isIfUnderMaps);
        }
    }
}