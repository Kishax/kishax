package spigot;

import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class SocketResponse {
    private final common.Main plugin;
    private final ServerStatusCache ssc;
    private final ServerHomeDir shd;
    private final Provider<SocketSwitch> sswProvider;
    private final AutoShutdown asd;
    @Inject
    public SocketResponse(common.Main plugin, ServerStatusCache ssc, ServerHomeDir shd, Provider<SocketSwitch> sswProvider, AutoShutdown asd) {
        this.plugin = plugin;
        this.ssc = ssc;
        this.shd = shd;
        this.sswProvider = sswProvider;
        this.asd = asd;
    }

    public void resaction(String res) {
    	if (res != null) {
            res = res.replace("\n", "").replace("\r", "");
            if (res.startsWith("proxy->")) {
                if (res.contains("stop")) {
                    SocketSwitch ssw = sswProvider.get();
                    String serverName = shd.getServerName();
	            	ssw.sendVelocityServer("管理者の命令より、"+serverName+"サーバーを停止させます。");
	                plugin.getServer().broadcastMessage(ChatColor.RED+"管理者の命令より、"+serverName+"サーバーを5秒後に停止します。");
	                asd.countdownAndShutdown(5);
                }
            } else if (res.contains("起動")) {
                plugin.getLogger().log(Level.INFO, "{0}", res);
                String pattern = "(.*?)サーバーが起動しました。";
                Pattern compiledPattern = Pattern.compile(pattern);
                Matcher matcher = compiledPattern.matcher(res);
                if (matcher.find()) {
                    String extracted = matcher.group(1);
                    ssc.refreshManualOnlineServer(extracted);
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