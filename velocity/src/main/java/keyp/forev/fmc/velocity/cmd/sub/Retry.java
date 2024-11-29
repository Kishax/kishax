package keyp.forev.fmc.velocity.cmd.sub;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;

import keyp.forev.fmc.common.database.Database;
import keyp.forev.fmc.common.server.DefaultLuckperms;
import keyp.forev.fmc.common.settings.PermSettings;
import keyp.forev.fmc.common.util.OTPGenerator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

public class Retry implements SimpleCommand {

	private final Logger logger;
	private final Database db;
	private final DefaultLuckperms lp;
	private final String[] subcommands = {"retry"};
	@Inject
	public Retry(Logger logger, Database db, DefaultLuckperms lp) {
		this.logger = logger;
		this.db = db;
		this.lp = lp;
	}
	
	@Override
	public void execute(Invocation invocation) {
		CommandSource source = invocation.source();
		if (!(source instanceof Player)) {
			if (source != null) {
				source.sendMessage(Component.text("このコマンドはプレイヤーのみが実行できます。").color(NamedTextColor.RED));	
			}
			return;
		}
		Player player = (Player) source;
		if (!lp.hasPermission(player.getUsername(), PermSettings.RETRY.get())) {
			source.sendMessage(Component.text("権限がありません。").color(NamedTextColor.RED));
			return;
		}
		String query = "UPDATE members SET secret2=? WHERE uuid=?;";
		try (Connection conn = db.getConnection();
			PreparedStatement ps = conn.prepareStatement(query)) {
			int ranum = OTPGenerator.generateOTPbyInt();
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

	@Override
    public List<String> suggest(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();
        List<String> ret = new ArrayList<>();
        switch (args.length) {
        	case 0, 1 -> {
                for (String subcmd : subcommands) {
                    if (!source.hasPermission("fmc.proxy." + subcmd)) continue;
                    ret.add(subcmd);
                }
                return ret;
            }
		}
		return Collections.emptyList();
	}
}