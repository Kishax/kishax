package keyp.forev.fmc.forge.server;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import keyp.forev.fmc.forge.util.config.ForgeConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;

public class CountdownTask implements Runnable {

    private final MinecraftServer server;
    private final Logger logger;
    private final AtomicBoolean isShutdown;
    private final long delayMillis; // タイマーの遅延時間
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> shutdownTask;

    @Inject
    public CountdownTask(MinecraftServer server, ForgeConfig config, Logger logger) {
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
        if (server.getPlayerCount() == 0) {
            if (shutdownTask == null || shutdownTask.isCancelled()) {
                shutdownTask = scheduler.schedule(this::shutdownServer, delayMillis, TimeUnit.MILLISECONDS);
                //logger.info("プレイヤー不在のため、サーバーを停止するタスクがスケジュールされました。");
            }
        } else {
            // プレイヤーがいる場合はシャットダウンタスクをキャンセル
            if (shutdownTask != null && !shutdownTask.isCancelled()) {
                shutdownTask.cancel(false);
                //logger.info("プレイヤーがいるため、サーバーの停止タスクをキャンセルしました。");
            }
        }

        // 定期的にチェックを続けるために、次のチェックをスケジュール
        scheduler.schedule(this, 1, TimeUnit.SECONDS);
    }

    private void shutdownServer() {
        if (isShutdown.get()) return;

        logger.info("サーバーを停止します。");
        isShutdown.set(true);

        // コマンドを実行するためのコマンドディスパッチャーを取得
        CommandDispatcher<CommandSourceStack> dispatcher = server.getCommands().getDispatcher();
        CommandSourceStack source = server.createCommandSourceStack();
        try {
            dispatcher.execute("stop", source);
        } catch (CommandSyntaxException e) {
            logger.error("An IOException error occurred: " + e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                logger.error(element.toString());
            }
        }

        scheduler.shutdown();
    }
}
