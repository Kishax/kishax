package keyp.forev.fmc.fabric.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;

import com.google.inject.Inject;

import net.minecraft.server.MinecraftServer;

public class CountdownTask implements Runnable {

    private final MinecraftServer server;
    private final Logger logger;
    private final AtomicBoolean isShutdown;
    private final long delayMillis; // タイマーの遅延時間
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> shutdownTask;

    @Inject
    public CountdownTask(MinecraftServer server, Logger logger, Config config) {
        this.server = server;
        this.logger = logger;
        this.isShutdown = new AtomicBoolean(false);
        this.delayMillis = config.getLong("AutoStop.Interval", 3) * 60 * 1000;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void run() {
        if (isShutdown.get()) return;

        // プレイヤーがいない場合にのみシャットダウンタスクをスケジュール
        if (server.getCurrentPlayerCount() == 0) {
            if (shutdownTask == null || shutdownTask.isCancelled()) {
                shutdownTask = scheduler.schedule(this::shutdownServer, delayMillis, TimeUnit.MILLISECONDS);
                logger.info("プレイヤー不在のため、サーバーを停止するタスクがスケジュールされました。");
            }
        } else {
            // プレイヤーがいる場合はシャットダウンタスクをキャンセル
            if (shutdownTask != null && !shutdownTask.isCancelled()) {
                shutdownTask.cancel(false);
                logger.info("プレイヤーがいるため、サーバーの停止タスクをキャンセルしました。");
            }
        }

        // 定期的にチェックを続けるために、次のチェックをスケジュール
        scheduler.schedule(this, 1, TimeUnit.SECONDS);
    }

    private void shutdownServer() {
        if (isShutdown.get()) return;
        logger.info("サーバーを停止します。");
        isShutdown.set(true);
        server.stop(false);
        scheduler.shutdown();
    }
}
