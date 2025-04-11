package f5.si.kishax.mc.neoforge.server;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

import com.google.inject.Inject;

import f5.si.kishax.mc.neoforge.Main;
import f5.si.kishax.mc.neoforge.util.config.NeoForgeConfig;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class AutoShutdown {
  private static final Logger logger = Main.logger;
  private final NeoForgeConfig config;
  private final CountdownTask countdown;
  private final ScheduledExecutorService scheduler;

  @Inject
  public AutoShutdown(NeoForgeConfig config, CountdownTask countdown) {
    this.config = config;
    this.countdown = countdown;
    this.scheduler = Executors.newScheduledThreadPool(1);
  }

  public void start() {
    if (config.getBoolean("AutoStop.Mode", false)) {
      scheduler.scheduleAtFixedRate(countdown, 0, 10, TimeUnit.SECONDS);
    }
  }

  public void stop() {
    if (config.getBoolean("AutoStop.Mode", false)) {
      scheduler.shutdownNow();
    }
  }

  public static void safeStopServer(MinecraftServer server) {
    if (server.isRunning()) {
      logger.info("Triggering server stopping...");
      ServerLifecycleHooks.handleServerStopping(server);
      server.halt(true);
    }
  }
}
