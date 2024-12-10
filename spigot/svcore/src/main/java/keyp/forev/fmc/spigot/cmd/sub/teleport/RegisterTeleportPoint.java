package keyp.forev.fmc.spigot.cmd.sub.teleport;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.google.inject.Inject;

import keyp.forev.fmc.spigot.server.textcomponent.TCUtils;
import keyp.forev.fmc.spigot.server.textcomponent.TCUtils2;
import keyp.forev.fmc.spigot.util.RunnableTaskUtil;
import keyp.forev.fmc.spigot.util.interfaces.MessageRunnable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

public class RegisterTeleportPoint implements TabExecutor {
    private final RunnableTaskUtil rt;
    @Inject
    public RegisterTeleportPoint(RunnableTaskUtil rt) {
        this.rt = rt;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            String playerName = player.getName();
            // ラージか1✕1かをプレイヤーに問う
            TextComponent messages = Component.text()
                .append(Component.text("1✕1の画像マップを作成する場合は、"))
                .append(TCUtils.ONE.get())
                .append(Component.text("と入力してください。"))
                .appendNewline()
                .append(Component.text("ラージマップを作成する場合は、"))
                .append(TCUtils.TWO.get())
                .append(Component.text("と入力してください。"))
                .appendNewline()
                .append(TCUtils.INPUT_MODE.get())
                .build();

            player.sendMessage(messages);
            
            Map<String, MessageRunnable> playerActions = new HashMap<>();
            playerActions.put(RunnableTaskUtil.Key.TELEPORT_REGISTER_POINT.get(), (input) -> {
                player.sendMessage(TCUtils2.getResponseComponent(input));
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
                        Component errorMessage = Component.text("無効な入力です。")
                            .appendNewline()
                            .append(Component.text("1または2を入力してください。"))
                            .color(NamedTextColor.RED);
                        rt.extendTask(player, RunnableTaskUtil.Key.TELEPORT_REGISTER_POINT.get());
                    }
                }
            });
            rt.addTaskRunnable(player, playerActions, RunnableTaskUtil.Key.TELEPORT_REGISTER_POINT.get());
        } else {
            Component errorMessage = Component.text("プレイヤーのみが実行できます。")
                .color(NamedTextColor.RED);
            sender.sendMessage(errorMessage);
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
