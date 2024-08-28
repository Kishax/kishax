package velocity_command;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import velocity.Config;

public class Debug {
	private final Config config;
	
	private String value1 = null, value2 = null;
	private long value3 = 0, value4 = 0, value5 = 0, value6 = 0, value7 = 0, value8 = 0;
	private final Logger logger;

	@Inject
	public Debug(Config config, Logger logger) {
		this.config = config;
		this.logger = logger;
	}
	
	public void execute(@NotNull CommandSource source,String[] args) {
		Map<String, Object> DebugConfig = config.getStringObjectMap("Debug");
		Map<String, Object> DiscordConfig = config.getStringObjectMap("Discord");
		if (Objects.isNull(DebugConfig) || Objects.isNull(DiscordConfig)) {
			source.sendMessage(Component.text("コンフィグの設定が正しくありません。").color(NamedTextColor.RED));
			return;
		}
		
		if (
			!(config.getString("Debug.Webhook_URL","").isEmpty()) && 
			!(config.getString("Discord.Webhook_URL","").isEmpty())
		) {
			value1 = config.getString("Debug.Webhook_URL");
			value2 = config.getString("Discord.Webhook_URL");
			DiscordConfig.put("Webhook_URL", value1);
			DebugConfig.put("Webhook_URL", value2);
		} else {
			source.sendMessage(Component.text("コンフィグの設定が不十分です。").color(NamedTextColor.RED));
		}
		
		if (config.getLong("Debug.ChannelId", 0) != 0 && config.getLong("Discord.ChannelId", 0) != 0) {
			// Long.valueOf(value3).toString()
			// 置換する前にDiscord-Botをログアウトさせておく
			// 現在、Discord-Tokenは一つしか扱っていないため、コメントアウトにしておく
			// discord.logoutDiscordBot();
			
			value3 = config.getLong("Debug.ChannelId");
			value4 = config.getLong("Discord.ChannelId");
			DiscordConfig.put("ChannelId", value3);
			DebugConfig.put("ChannelId", value4);
		} else {
			source.sendMessage(Component.text("コンフィグの設定が不十分です。(ChannelId)").color(NamedTextColor.RED));
		}
		
		if (config.getLong("Debug.ChatChannelId", 0) != 0 && config.getLong("Discord.ChatChannelId", 0) != 0) {
			value5 = config.getLong("Debug.ChatChannelId");
			value6 = config.getLong("Discord.ChatChannelId");
			DiscordConfig.put("ChatChannelId", value5);
			DebugConfig.put("ChatChannelId", value6);
		} else {
			source.sendMessage(Component.text("コンフィグの設定が不十分です。(ChatChannelId)").color(NamedTextColor.RED));
		}
		
		if (config.getLong("Debug.AdminChannelId", 0) != 0 && config.getLong("Discord.AdminChannelId", 0) != 0) {
			value7 = config.getLong("Debug.AdminChannelId");
			value8 = config.getLong("Discord.AdminChannelId");
			DiscordConfig.put("AdminChannelId", value7);
			DebugConfig.put("AdminChannelId", value8);
		} else {
			source.sendMessage(Component.text("コンフィグの設定が不十分です。(AdminChannelId)").color(NamedTextColor.RED));
		}
		
		if (config.getBoolean("Debug.Mode", false)) {
			source.sendMessage(Component.text("デバッグモードがOFFになりました。").color(NamedTextColor.GREEN));
			DebugConfig.put("Mode", false);
		} else {
			source.sendMessage(Component.text("デバッグモードがONになりました。").color(NamedTextColor.GREEN));
			DebugConfig.put("Mode", true);
		}
		
		try {
			config.saveConfig();
		} catch (IOException e) {
			source.sendMessage(Component.text("コマンド実行中にエラーが発生しました。").color(NamedTextColor.RED));
			logger.error("An IOException error occurred: " + e.getMessage());
			for (StackTraceElement element : e.getStackTrace()) {
				logger.error(element.toString());
			}
		}
	}
}
