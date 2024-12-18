package keyp.forev.fmc.spigot.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import keyp.forev.fmc.common.database.Database;
import keyp.forev.fmc.common.socket.SocketSwitch;
import keyp.forev.fmc.spigot.server.events.EventListener;
import keyp.forev.fmc.spigot.util.interfaces.MessageRunnable;
import net.md_5.bungee.api.ChatColor;

import com.google.inject.Provider;
import com.google.inject.Inject;
import org.slf4j.Logger;

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
        IMAGEMAP_REGISTER_MAP("registerImageMap"),
        IMAGEMAP_CREATE_LARGE_IMAGE("createLargeImageMap"),
        IMAGEMAP_CREATE_IMAGE_MAP_FROM_Q("createImageMapFromQ"),
        IMAGEMAP_CREATE_IMAGE_MAP_FROM_CMD("createImageMapFromCommandLine"),
        IMAGEMAP_CREATE_IMAGE_MAP_FROM_MENU("createImageMapFromMenu"),
        TELEPORT_REGISTER_POINT("registerTeleportPoint"),
        ;
        private final String key;
        Key(String key) {
            this.key = key;
        }

        public String get() {
            return key;
        }

        public static Optional<Key> search(String name) {
        return Arrays.stream(Key.values())
            .filter(type -> type.get().equalsIgnoreCase(name))
            .findFirst();
        }
    }

    public void addTaskRunnable(Player player, Map<Key, MessageRunnable> playerActions, Key key) {
        //EventListener.playerInputerMap.computeIfAbsent(player, (somekey) -> playerActions);
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

    public void extendTask(Player player, Key key) {
        cancelTask(player, key);
        scheduleNewTask(player, inputPeriod, key);
    }

    private void scheduleNewTask(Player player, int delaySeconds, Key key) {
        //removeCancelTaskRunnable(player, key); // 新しいタスクをセットする前に

        BukkitTask newTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.sendMessage(ChatColor.RED + "入力がタイムアウトしました。");
            removeCancelTaskRunnable(player, key);
            //logger.info("key: {}", key);
        }, 20 * delaySeconds);

        EventListener.playerTaskMap.computeIfAbsent(player, (somekey) -> new HashMap<>()).put(key, newTask);
    }

    public boolean checkIsOtherInputMode(Player player, Key exceptedKey)  {
        if (EventListener.playerInputerMap.containsKey(player) && EventListener.playerTaskMap.containsKey(player)) {
            Map<Key, MessageRunnable> playerActions = EventListener.playerInputerMap.get(player);
            Map<Key, BukkitTask> playerTasks = EventListener.playerTaskMap.get(player);
            // playerActions, playerTasksにACTIONS_KEY以外が含まれているとき
            boolean isInputMode = playerActions.entrySet().stream().anyMatch(entry -> !entry.getKey().equals(exceptedKey)),
                isTaskMode = playerTasks.entrySet().stream().anyMatch(entry -> !entry.getKey().equals(exceptedKey));
            return isInputMode || isTaskMode;
        }
        return false;
    }

    public boolean checkIsInputMode(Player player, Key key) {
        if (EventListener.playerInputerMap.containsKey(player) && EventListener.playerTaskMap.containsKey(player)) {
            Map<Key, MessageRunnable> playerActions = EventListener.playerInputerMap.get(player);
            Map<Key, BukkitTask> playerTasks = EventListener.playerTaskMap.get(player);
            // playerActions, playerTasksにACTIONS_KEYが含まれているとき
            boolean isInputMode = playerActions.containsKey(key),
                isTaskMode = playerTasks.containsKey(key);
            return isInputMode && isTaskMode;
        }
        return false;
    }

    public void removeCancelTaskRunnable(Player player, Key key) {
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

    private void cancelTask(Player player, Key key) {
        if (EventListener.playerTaskMap.containsKey(player)) {
            Map<Key, BukkitTask> playerTasks = EventListener.playerTaskMap.get(player);
            if (playerTasks.containsKey(key)) {
                BukkitTask task = playerTasks.get(key);
                task.cancel();
            }
        }
    }
}
