package spigot;

import org.bukkit.Bukkit;
import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import common.FMCSettings2;
import redis.clients.jedis.Jedis;

@Singleton
public class TPSUtils {
    private final common.Main plugin;
    private final Logger logger;
    private final Provider<Jedis> jedisProvider;
    private final String serverName;
    private long lastTickTime = System.currentTimeMillis();
    private int tickCount = 0;
    private double currentTPS = 20.0; // 初期値として20.0を設定

    @Inject
    public TPSUtils(common.Main plugin, Logger logger, Provider<Jedis> jedisProvider, ServerHomeDir shd) {
        this.plugin = plugin;
        this.logger = logger;
        this.jedisProvider = jedisProvider;
        this.serverName = shd.getServerName();
    }

    public void log() {
        logger.info(serverName + "\'s TPS: " + getTPS());
    }

    public void sendToVelocity() {
        String message = serverName + ": " + this.currentTPS;
        try (Jedis jedis = jedisProvider.get()) {
            jedis.publish(FMCSettings2.JEDIS_TPS_CHANNEL, message);
        }
    }

    public void startTickMonitor() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long currentTime = System.currentTimeMillis();
            tickCount++;
            if (currentTime - lastTickTime >= 1000) {
                this.currentTPS = (tickCount / ((currentTime - lastTickTime) / 1000.0));
                logger.info("Calculated TPS: " + currentTPS); // デバッグログを追加
                this.lastTickTime = currentTime;
                tickCount = 0;
            }
        }, 1L, 1L);
    }

    private double getTPS() {
        return this.currentTPS;
    }
}