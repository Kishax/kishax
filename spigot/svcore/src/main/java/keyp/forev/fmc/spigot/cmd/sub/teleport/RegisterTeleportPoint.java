package keyp.forev.fmc.spigot.cmd.sub.teleport;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.google.inject.Inject;

import keyp.forev.fmc.spigot.server.textcomponent.TCUtils;
import keyp.forev.fmc.spigot.server.textcomponent.TCUtils2;
import keyp.forev.fmc.spigot.util.RunnableTaskUtil;
import keyp.forev.fmc.spigot.util.interfaces.MessageRunnable;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentBuilder;

public class RegisterTeleportPoint implements TabExecutor {
    private final JavaPlugin plugin;
    private final RunnableTaskUtil rt;
    @Inject
    public RegisterTeleportPoint(JavaPlugin plugin, RunnableTaskUtil rt) {
        this.plugin = plugin;
        this.rt = rt;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            String playerName = player.getName();
            // ラージか1✕1かをプレイヤーに問う
            //player.sendMessage();
            Component t = Component.text("");
            Component t1 = Component.text("");
            ComponentBuilder cb = Component.text();
            player.sendMessage(t);
            
            player.spigot().sendMessage(
                new TextComponent("1✕1の画像マップを作成する場合は、"),
                TCUtils.ZERO.get(),
                new TextComponent("と入力してください。"),
                new TextComponent("\n"),
                new TextComponent("1✕1のQRコードを作成する場合は、"),
                TCUtils.ONE.get(),
                new TextComponent("と入力してください。"),
                new TextComponent("\n"),
                new TextComponent("ラージマップを作成する場合は、"),
                TCUtils.TWO.get(),
                new TextComponent("と入力してください。"),
                new TextComponent("\n"),
                TCUtils.INPUT_MODE.get()
            );
            Map<String, MessageRunnable> playerActions = new HashMap<>();
            playerActions.put(RunnableTaskUtil.Key.TELEPORT_REGISTER_POINT.get(), (input) -> {
                player.spigot().sendMessage(new TCUtils2(input).getResponseComponent());
                switch (input) {
                    case "0" -> {
                        rt.removeCancelTaskRunnable(player, RunnableTaskUtil.Key.TELEPORT_REGISTER_POINT.get());
                    }
                    case "1" -> {
                        rt.removeCancelTaskRunnable(player, RunnableTaskUtil.Key.TELEPORT_REGISTER_POINT.get());
                    }
                    case "2" -> {
                        rt.removeCancelTaskRunnable(player, RunnableTaskUtil.Key.TELEPORT_REGISTER_POINT.get());
                    }
                    default -> {
                        player.sendMessage(ChatColor.RED + "無効な入力です。\n1または2を入力してください。");
                        rt.extendTask(player, RunnableTaskUtil.Key.TELEPORT_REGISTER_POINT.get());
                    }
                }
            });
            rt.addTaskRunnable(player, playerActions, RunnableTaskUtil.Key.TELEPORT_REGISTER_POINT.get());
        } else {
            sender.sendMessage(ChatColor.RED + "プレイヤーのみが実行できます。");
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
