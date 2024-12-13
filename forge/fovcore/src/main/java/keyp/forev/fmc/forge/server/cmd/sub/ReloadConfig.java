package keyp.forev.fmc.forge.server.cmd.sub;

import java.io.IOException;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.mojang.brigadier.context.CommandContext;

import keyp.forev.fmc.forge.util.config.ForgeConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public class ReloadConfig {
	private final ForgeConfig config;
	private final Logger logger;
	@Inject
	public ReloadConfig(ForgeConfig config, Logger logger) {
		this.config = config;
		this.logger = logger;
	}
	
	public int execute(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		try {
            config.loadConfig();
            source.sendSuccess(() -> Component.literal("コンフィグをリロードしました。").withStyle(style -> style.withColor(ChatFormatting.GREEN)), false);
        } catch (IOException e1) {
            logger.error("Error loading config", e1);
        }
		return 0;
	}
}
