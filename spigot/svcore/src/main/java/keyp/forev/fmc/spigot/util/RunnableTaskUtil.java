package keyp.forev.fmc.spigot.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import keyp.forev.fmc.common.database.Database;
import keyp.forev.fmc.common.socket.SocketSwitch;
import keyp.forev.fmc.spigot.events.EventListener;
import keyp.forev.fmc.spigot.util.interfaces.MessageRunnable;
import net.md_5.bungee.api.ChatColor;

import com.google.inject.Provider;
import com.google.inject.Inject;
import org.slf4j.Logger;
import java.util.Arrays;

public class RunnableTaskUtil {
    private final JavaPlugin plugin;
    private final Logger logger;
    private final Database db;
    private final Provider<SocketSwitch> sswProvider;
    private final int inputPeriod = 60;

    @Inject
    public RunnableTaskUtil(JavaPlugin plugin, Logger logger, Database db, Provider<SocketSwitch> sswProvider) {
        this.plugin = plugin;
        this.logger = logger;
        this.db = db;
        this.sswProvider = sswProvider;
    }

    public enum Key {
        IMAGEMAP_CREATE_LARGE_IMAGE("createLargeImageMap"),
        IMAGEMAP_CREATE_IMAGE_MAP_FROM_Q("createImageMapFromQ"),
        IMAGEMAP_CREATE_IMAGE_MAP_FROM_CMD("createImageMapFromCommandLine"),
        TELEPORT_REGISTER_POINT("registerTeleportPoint"),
        ;
        private final String key;
        Key(String key) {
            this.key = key;
        }

        public String get() {
            return key;
        }

        public static List<String> getKeys() {
        return Arrays.stream(values())
            .map(Key::get)
            .collect(Collectors.toList());
        }
    }

    public void addTaskRunnable(Player player, Map<String, MessageRunnable> playerActions, String key) {
        EventListener.playerInputerMap.put(player, playerActions);
        scheduleNewTask(player, inputPeriod, key);
        try (Connection connection3 = db.getConnection()) {
            SocketSwitch ssw = sswProvider.get();
            ssw.sendVelocityServer(connection3, "inputMode->on->name->" + player.getName() + "->");
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("An SQLException | ClassNotFoundException error occurred: {}", e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                logger.error(element.toString());
            }
        }
    }

    public void extendTask(Player player, String key) {
        cancelTask(player, key);
        scheduleNewTask(player, inputPeriod, key);
    }

    private void scheduleNewTask(Player player, int delaySeconds, String key) {
        BukkitTask newTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.sendMessage(ChatColor.RED + "入力がタイムアウトしました。");
            removeCancelTaskRunnable(player, key);
        }, 20 * delaySeconds);
        if (EventListener.playerTaskMap.containsKey(player)) {
            Map<String, BukkitTask> playerTasks = EventListener.playerTaskMap.get(player);
            playerTasks.put(key, newTask);
            EventListener.playerTaskMap.put(player, playerTasks);
        } else {
            Map<String, BukkitTask> playerTasks = new HashMap<>();
            playerTasks.put(key, newTask);
            EventListener.playerTaskMap.put(player, playerTasks);
        }
    }

    public boolean checkIsOtherInputMode(Player player, String exceptedKey)  {
        if (EventListener.playerInputerMap.containsKey(player) && EventListener.playerTaskMap.containsKey(player)) {
            Map<String, MessageRunnable> playerActions = EventListener.playerInputerMap.get(player);
            Map<String, BukkitTask> playerTasks = EventListener.playerTaskMap.get(player);
            // playerActions, playerTasksにACTIONS_KEY以外が含まれているとき
            boolean isInputMode = playerActions.entrySet().stream().anyMatch(entry -> !entry.getKey().equals(exceptedKey)),
                isTaskMode = playerTasks.entrySet().stream().anyMatch(entry -> !entry.getKey().equals(exceptedKey));
            return isInputMode || isTaskMode;
        }
        return false;
    }

    public boolean checkIsInputMode(Player player, String key) {
        if (EventListener.playerInputerMap.containsKey(player) && EventListener.playerTaskMap.containsKey(player)) {
            Map<String, MessageRunnable> playerActions = EventListener.playerInputerMap.get(player);
            Map<String, BukkitTask> playerTasks = EventListener.playerTaskMap.get(player);
            // playerActions, playerTasksにACTIONS_KEYが含まれているとき
            boolean isInputMode = playerActions.containsKey(key),
                isTaskMode = playerTasks.containsKey(key);
            return isInputMode && isTaskMode;
        }
        return false;
    }

    public void removeCancelTaskRunnable(Player player, String key) {
        String playerName = player.getName();
        EventListener.playerInputerMap.entrySet().removeIf(entry -> entry.getKey().equals(player) && entry.getValue().containsKey(key));
        // タスクをキャンセルしてから、playerTaskMapから削除する
        EventListener.playerTaskMap.entrySet().stream()
            .filter(entry -> entry.getKey().equals(player) && entry.getValue().containsKey(key))
            .forEach(entry -> {
                BukkitTask task = entry.getValue().get(key);
                task.cancel();
                entry.getValue().remove(key);
            });
        try (Connection connection2 = db.getConnection()) {
            SocketSwitch ssw = sswProvider.get();
            ssw.sendVelocityServer(connection2, "inputMode->off->name->" + playerName + "->");
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("An SQLException | ClassNotFoundException error occurred: {}", e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                logger.error(element.toString());
            }
        }
    }

    private void cancelTask(Player player, String key) {
        if (EventListener.playerTaskMap.containsKey(player)) {
            Map<String, BukkitTask> playerTasks = EventListener.playerTaskMap.get(player);
            if (playerTasks.containsKey(key)) {
                BukkitTask task = playerTasks.get(key);
                task.cancel();
            }
        }
    }
}
