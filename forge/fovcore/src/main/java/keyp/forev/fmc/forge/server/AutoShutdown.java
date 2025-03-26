package keyp.forev.fmc.forge.server;

import java.lang.reflect.Method;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import keyp.forev.fmc.forge.Main;
import keyp.forev.fmc.forge.util.config.ForgeConfig;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;

public class AutoShutdown {
  private static final Logger logger = Main.logger;
  private final ForgeConfig config;
  private final CountdownTask countdownTask;
  private boolean isActive;

  @Inject
  public AutoShutdown(ForgeConfig config, CountdownTask countdownTask) {
    this.config = config;
    this.countdownTask = countdownTask;
    this.isActive = false;
  }

  public void start() {
    if (config.getBoolean("AutoStop.Mode", false)) {
      isActive = true;
      MinecraftForge.EVENT_BUS.register(this);
    }
  }

  public void stop() {
    if (isActive) {
      isActive = false;
      MinecraftForge.EVENT_BUS.unregister(this);
    }
  }

  @SubscribeEvent
  public void onServerTick(TickEvent.ServerTickEvent event) {
    if (event.phase == TickEvent.Phase.END) {
      countdownTask.tick();
    }
  }

  public static void safeStopServer(MinecraftServer server) {
    try {
      Method isRunningMethod = MinecraftServer.class.getDeclaredMethod("isRunning");
      isRunningMethod.setAccessible(true);
      boolean isRunning = (boolean) isRunningMethod.invoke(server);
      if (isRunning) {
        logger.info("Triggering server stopping...");
        Method haltMethod = MinecraftServer.class.getDeclaredMethod("halt", boolean.class);
        haltMethod.setAccessible(true);
        haltMethod.invoke(server, true);
      }
    } catch (Exception e) {
      logger.error("An error occurred at AutoShutdown#safeStopServer: ", e);
    }
  }

  public static void safeStopServer2(MinecraftServer server) {
    logger.info("safestopServer method!!");
    try {
      Method isRunningMethod = MinecraftServer.class.getDeclaredMethod("isRunning");
      isRunningMethod.setAccessible(true);
      boolean isRunning = (boolean) isRunningMethod.invoke(server);
      if (isRunning) {
        logger.info("Triggering server stopping...");
        ServerLifecycleHooks.handleServerStopping(server);
        try {
          Method getCommandsMethod = MinecraftServer.class.getDeclaredMethod("getCommands");
          getCommandsMethod.setAccessible(true);
          Object commands = getCommandsMethod.invoke(server);

          Method getDispatcherMethod = commands.getClass().getDeclaredMethod("getDispatcher");
          getDispatcherMethod.setAccessible(true);
          Object dispatcherObject = getDispatcherMethod.invoke(commands);

          if (dispatcherObject instanceof CommandDispatcher) {
            @SuppressWarnings("unchecked")
            CommandDispatcher<CommandSourceStack> dispatcher = (CommandDispatcher<CommandSourceStack>) dispatcherObject;

            Method createCommandSourceStackMethod = MinecraftServer.class.getDeclaredMethod("createCommandSourceStack");
            createCommandSourceStackMethod.setAccessible(true);
            CommandSourceStack source = (CommandSourceStack) createCommandSourceStackMethod.invoke(server);

            dispatcher.execute("stop", source);
            logger.info("Stop command executed successfully.");
          } else {
            logger.error("Failed to cast dispatcherObject to CommandDispatcher<CommandSourceStack>.");
          }
        } catch (CommandSyntaxException e) {
          logger.error("A CommandSyntaxException error occurred: " + e.getMessage());
          for (StackTraceElement element : e.getStackTrace()) {
            logger.error(element.toString());
          }
        } catch (Exception e) {
          logger.error("An error occurred at AutoShutdown#safeStopServer: ", e);
        }
      }
    } catch (Exception e) {
      logger.error("An error occurred at AutoShutdown#safeStopServer: ", e);
    }
  }

  public static void safeStopServer3(MinecraftServer server) {
    logger.info("safestopServer method!!");
    if (server.isRunning()) {
      logger.info("safestopServer method in running blocks!!!");
      logger.info("Triggering server stopping...");
      ServerLifecycleHooks.handleServerStopping(server);
      try {
        Method haltMethod = MinecraftServer.class.getDeclaredMethod("halt", boolean.class);
        haltMethod.setAccessible(true);
        haltMethod.invoke(server, true);
      } catch (Exception e) {
        logger.error("An error occurred at AutoShutdown#safeStopServer: ", e);
      }
    }
  }
}
