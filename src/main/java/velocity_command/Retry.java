package velocity_command;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Random;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;

import common.Database;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

public class Retry {

	private final Logger logger;
	private final Database db;
	
	@Inject
	public Retry(Logger logger, Database db) {
		this.logger = logger;
		this.db = db;
	}
	
	public void execute(@NotNull CommandSource source, String[] args) {
		if (!(source instanceof Player)) {
			source.sendMessage(Component.text("このコマンドはプレイヤーのみが実行できます。").color(NamedTextColor.RED));	
			return;
		}
		
		Player player = (Player) source;
		String query = "UPDATE members SET secret2=? WHERE uuid=?;";
		try (Connection conn = db.getConnection();
			PreparedStatement ps = conn.prepareStatement(query)) {
			// 6桁の乱数を生成
			Random rnd = new Random();
			int ranum = 100000 + rnd.nextInt(900000);
			String ranumstr = Integer.toString(ranum);
			
			ps.setInt(1, ranum);
			ps.setString(2, player.getUniqueId().toString());
			int rsAffected = ps.executeUpdate();
			if (rsAffected > 0) {
				TextComponent component = Component.text()
						.append(Component.text("認証コードを再生成しました。").color(NamedTextColor.GREEN))
						.append(Component.text("\n認証コードは ").color(NamedTextColor.WHITE))
						.append(Component.text(ranumstr).color(NamedTextColor.BLUE)
							.clickEvent(ClickEvent.copyToClipboard(ranumstr))
							.hoverEvent(HoverEvent.showText(Component.text("(クリックして)コピー"))))
						.append(Component.text(" です。").color(NamedTextColor.WHITE))
						.build();
				player.sendMessage(component);
			} else {
				player.sendMessage(Component.text("認証コードの再生成に失敗しました。").color(NamedTextColor.RED));
			}
		} catch (SQLException | ClassNotFoundException e) {
			logger.error("A SQLException | ClassNotFoundException error occurred: " + e.getMessage());
			for (StackTraceElement element : e.getStackTrace()) {
				logger.error(element.toString());
			}
		}
	}
}