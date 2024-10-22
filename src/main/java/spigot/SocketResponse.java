package spigot;

import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.inject.Inject;

public class SocketResponse {
    private final common.Main plugin;
    private final ServerStatusCache ssc;
    @Inject
    public SocketResponse(common.Main plugin, ServerStatusCache ssc) {
        this.plugin = plugin;
        this.ssc = ssc;
    }

    public void resaction(String res) {
    	if (res != null) {
            res = res.replace("\n", "").replace("\r", "");
            plugin.getLogger().log(Level.INFO, "{0}", res);
            if (res.contains("起動")) {
                String pattern = "(.*?)サーバーが起動しました。";
                Pattern compiledPattern = Pattern.compile(pattern);
                Matcher matcher = compiledPattern.matcher(res);
                if (matcher.find()) {
                    String extracted = matcher.group(1);
                    Map<String, Map<String, Map<String, String>>> statusMap = ssc.getStatusMap();
                    for (Map<String, Map<String, String>> serverMap : statusMap.values()) {
                        for (Map.Entry<String, Map<String, String>> entry : serverMap.entrySet()) {
                            Map<String, String> serverInfo = entry.getValue();
                            if (serverInfo.get("name").equals(extracted)) {
                                serverInfo.put("online", "1");
                                ssc.setStatusMap(statusMap);
                                //plugin.getLogger().log(Level.INFO, "Server {0} is now online", extracted);
                            }
                        }
                    }
                }
            } else if (res.contains("PHP")) {
                if (res.contains("uuid")) {
                    ssc.refreshMemberInfo();
                }
            } else if (res.contains("MineStatusSync")) {
                ssc.refreshCache();
            }
        }
    }
}