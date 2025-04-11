package f5.si.kishax.mc.forge.server;

import com.google.inject.Inject;

import f5.si.kishax.mc.forge.util.config.ForgeConfig;
import net.minecraft.server.MinecraftServer;

import java.lang.reflect.Method;

import org.slf4j.Logger;

public class CountdownTask {
  private final MinecraftServer server;
  private final Logger logger;
  private final long delayTicks;
  private long remainingTicks;

  @Inject
  public CountdownTask(MinecraftServer server, ForgeConfig config, Logger logger) {
    this.server = server;
    this.logger = logger;
    this.delayTicks = config.getLong("AutoStop.Interval", 3) * 60 * 20; // 分 -> チック
    this.remainingTicks = -1; // 未開始状態
  }

  public void tick() {
    try {
      Method getPlayerCountMethod = MinecraftServer.class.getDeclaredMethod("getPlayerCount");
      getPlayerCountMethod.setAccessible(true);
      int playerCount = (int) getPlayerCountMethod.invoke(server);
      if (playerCount == 0) {
        if (remainingTicks == -1) {
          remainingTicks = delayTicks;
          logger.info("プレイヤーがいないため、サーバー停止カウントダウンを開始します。");
        } else if (remainingTicks > 0) {
          remainingTicks--;
          if (remainingTicks % 20 == 0) {
            // 1秒毎にログ出力
            //logger.info("サーバー停止まで {} 秒", remainingTicks / 20);
          }
        } else {
          shutdownServer();
        }
      } else {
        if (remainingTicks != -1) {
          logger.info("プレイヤーが戻ったため、カウントダウンをリセットします。");
          remainingTicks = -1;
        }
      }
    } catch (Exception e) {
      logger.error("An error occurred at CountdownTask#tick: ", e);
    }
  }

  private void shutdownServer() {
    logger.info("プレイヤーがいないため、サーバーを停止します。");
    AutoShutdown.safeStopServer2(server);
  }
}
