package keyp.forev.fmc.cmd;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.google.inject.Inject;

public class HidePlayer {
	private final keyp.forev.fmc.spigot.Main plugin;
	@Inject
	public HidePlayer(common.Main plugin) {
		this.plugin = plugin;
	}
	
	public void execute(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
		if (args.length < 3) {
            sender.sendMessage("使用法: /fmc hideplayer <プレイヤー名> <hide|show>");
			return;
        }
		if (sender instanceof Player player) {
            String targetPlayerName = args[1];
            String action = args[2];
			Player targetPlayer = plugin.getServer().getPlayer(targetPlayerName);
            if (targetPlayer == null) {
                sender.sendMessage("プレイヤーが見つかりません。");
				return;
            }
            if (action.equalsIgnoreCase("hide")) {
                player.hidePlayer(plugin, targetPlayer);
                sender.sendMessage(targetPlayerName + "を隠しました。");
            } else if (action.equalsIgnoreCase("show")) {
                player.showPlayer(plugin, targetPlayer);
                sender.sendMessage(targetPlayerName + "を表示しました。");
            } else {
                sender.sendMessage("無効なアクションです。使用法: /fmc hideplayer <プレイヤー名> <hide|show>");
            }
        } else {
			if (sender != null) {
				sender.sendMessage("このコマンドはプレイヤーのみが実行できます。");
			}
        }
	}
}
