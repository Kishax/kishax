package spigot;

import java.net.ServerSocket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.inject.Inject;

public class PortFinder {
    public static int foundPort;
    private final common.Main plugin;
    private final int startPort;
    private final int endPort;
    @Inject
    public PortFinder(common.Main plugin) {
        this.plugin = plugin;
        this.startPort = 6000;
        this.endPort = 6999;
    }

    public CompletableFuture<Integer> findAvailablePortAsync(Map<String, Map<String, Map<String, Object>>> statusMap) {
        return CompletableFuture.supplyAsync(() -> {
            // startPortからendPortまでの範囲のポートリストを作成
            List<Integer> portRange = IntStream.rangeClosed(startPort, endPort).boxed().collect(Collectors.toList());
            statusMap.values().stream()
                .flatMap(serverMap -> serverMap.values().stream())
                .map(serverInfo -> serverInfo.get("socketport"))
                .filter(port -> port instanceof Integer)
                .forEach(port -> portRange.remove((Integer) port));
            // 使用可能なポートを検索
            for (int port : portRange) {
                try (ServerSocket serverSocket = new ServerSocket(port)) {
                    serverSocket.close();
                    return port; // 使用可能なポートを見つけたら返す
                } catch (Exception e) {
                    // ポートが使用中の場合は次のポートを試す
                    plugin.getLogger().log(Level.INFO, "Port {0} is already in use, so skip and tried next one", port);
                }
            }
            throw new RuntimeException("No available port found in the range " + startPort + " to " + endPort);
        });
    }
}