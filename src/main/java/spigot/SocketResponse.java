package spigot;

import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.inject.Inject;

public class SocketResponse {
    private final common.Main plugin;
    private final ServerStatusCache serverStatusCache;
    @Inject
    public SocketResponse(common.Main plugin, ServerStatusCache serverStatusCache) {
        this.plugin = plugin;
        this.serverStatusCache = serverStatusCache;
    }

    public void resaction(String res) {
    	if (res != null) {
            res = res.replace("\n", "").replace("\r", "");
            plugin.getLogger().log(Level.INFO, "{0}", res);
            if (res.contains("起動")) {
                // /stpで用いるセッションタイム(現在時刻)(sst)をデータベースに
                String pattern = "(.*?)サーバーが起動しました。";
    
                // パターンをコンパイル
                Pattern compiledPattern = Pattern.compile(pattern);
                Matcher matcher = compiledPattern.matcher(res);
    
                // パターンにマッチする部分を抽出
                if (matcher.find()) {
                    String extracted = matcher.group(1);
                    Map<String, Map<String, Map<String, String>>> statusMap = serverStatusCache.getStatusMap();
                    for (Map<String, Map<String, String>> serverMap : statusMap.values()) {
                        for (Map.Entry<String, Map<String, String>> entry : serverMap.entrySet()) {
                            Map<String, String> serverInfo = entry.getValue();
                            if (serverInfo.get("name").equals(extracted)) {
                                serverInfo.put("online", "1");
                                serverStatusCache.setStatusMap(statusMap);
                                //plugin.getLogger().log(Level.INFO, "Server {0} is now online", extracted);
                            }
                        }
                    }
                }
            }
        }
    }
}