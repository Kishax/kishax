package velocity;

import java.util.HashMap;
import java.util.Map;

import com.google.inject.Inject;

public class ConfigUtils {
    private final Config config;
    //private Map<String, Object> savedConfig = new HashMap<>();
    @Inject
    public ConfigUtils(Config config) {
        this.config = config;
    }

    @SuppressWarnings("unchecked")
    public void configOutput() {
        Map<String, Object> configMap = (Map<String, Object>) config.getConfig().get("Servers");
        //this.savedConfig = configMap;
        // 変換結果を格納するMap
        Map<String, Map<String, String>> result = new HashMap<>();

        // 再帰的に処理を行う
        processConfig(configMap, "", result);

        // 結果を表示
        result.forEach((key, value) -> {
            System.out.println("Server: " + key);
            value.forEach((k, v) -> System.out.println("  " + k + ": " + v));
        });
    }

    @SuppressWarnings("unchecked")
    private void processConfig(Map<String, Object> configMap, String parentKey, Map<String, Map<String, String>> result) {
        for (Map.Entry<String, Object> entry : configMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // 新しいキーを作成
            String newKey = parentKey.isEmpty() ? key : parentKey + "_" + key;

            if (value instanceof Map) {
                // 再帰的に処理
                processConfig((Map<String, Object>) value, newKey, result);
            } else {
                // サーバー名を抽出
                String serverName = parentKey.split("_")[0];
                // サーバー名が存在しない場合、新しいMapを作成
                result.putIfAbsent(serverName, new HashMap<>());
                // 値を設定
                result.get(serverName).put(newKey, value != null ? value.toString() : null);
            }
        }
    }

    /*
    Object configServersObj = config.getConfig().get("Servers");
    if (configServersObj != null) {
        if (configServersObj instanceof Map) {
            Map<String, Object> configServers = (Map<String, Object>) config.getConfig().get("Servers");
            configServers.entrySet().stream().forEach(entry -> {
                String serverName = (String) entry.getKey();
                Object entryObj = entry.getValue();
                if (entryObj instanceof Map) {
                    Map<String, Object> entry2 = (Map<String, Object>) entry.getValue();
                    entry2.entrySet().stream().forEach(entry3 -> {
                        String key = (String) entry3.getKey();
                        Object value = entry3.getValue();
                        if (key.equalsIgnoreCase("hub")) {
                            if (value instanceof Boolean hubfound) {
                                if (hubfound && serverName.equalsIgnoreCase(currentServerName)) {
                                }
                            }
                        }
                    });
                }
            });
        }
    }
    */
}
