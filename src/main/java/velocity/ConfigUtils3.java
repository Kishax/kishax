package velocity;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.inject.Inject;

public class ConfigUtils3 {
    private final Config config;
    private Set<String> serversKeySet = null;
    @Inject
    public ConfigUtils3(Config config) {
        this.config = config;
    }

    @SuppressWarnings("unchecked")
    public void configOutput() {
        Map<String, Object> configMap = (Map<String, Object>) config.getConfig().get("Servers");
        configMap.forEach((key, value) -> {
            System.out.println("Server: " + key);
            if (value instanceof Map) {
                Map<String, Object> serverMap = (Map<String, Object>) value;
                this.serversKeySet = serverMap.keySet();
            }
        });
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
            String newKey = null;
            if (serversKeySet != null) {
                if (serversKeySet.contains(parentKey)) {
                    newKey =  key;
                } else {
                    newKey =  parentKey.isEmpty() ? key : parentKey + "_" + key;
                }
            }

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
}
