package keyp.forev.fmc.fabric.cmd;

import java.io.IOException;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.mojang.brigadier.context.CommandContext;

import keyp.forev.fmc.fabric.util.Config;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ReloadConfig {
	private final Config config;
	private final Logger logger;
	@Inject
	public ReloadConfig(Config config, Logger logger) {
		this.config = config;
		this.logger = logger;
	}
	
	public int execute(CommandContext<ServerCommandSource> context) {
		ServerCommandSource source = context.getSource();
		try {
            config.loadConfig(); // 一度だけロードする
            source.sendMessage(Text.literal("コンフィグをリロードしました。").formatted(Formatting.GREEN));
        } catch (IOException e1) {
            logger.error("Error loading config", e1);
        }
		
		return 0;
	}
}
