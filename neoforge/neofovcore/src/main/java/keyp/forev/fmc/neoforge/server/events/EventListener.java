package keyp.forev.fmc.neoforge.server.events;

import org.slf4j.Logger;

import com.google.inject.Guice;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;

import keyp.forev.fmc.common.server.DoServerOffline;
import keyp.forev.fmc.common.server.ServerStatusCache;
import keyp.forev.fmc.common.util.PlayerUtils;
import keyp.forev.fmc.neoforge.Main;
import keyp.forev.fmc.neoforge.module.Module;
import keyp.forev.fmc.neoforge.server.AutoShutdown;
import keyp.forev.fmc.neoforge.server.NeoForgeLuckperms;
import keyp.forev.fmc.neoforge.server.cmd.main.FMCCommand;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.bus.api.SubscribeEvent;

public class EventListener {
	private static boolean isStop = false;
	private static final Logger logger = Main.logger;
	@SubscribeEvent
	public static void onServerStarting(ServerStartingEvent event) {
		Main.injector = Guice.createInjector(new Module(logger, event.getServer()));
		Main.getInjector().getInstance(AutoShutdown.class).start();
		try {
			LuckPerms luckperms = LuckPermsProvider.get();
			Main.getInjector().getInstance(NeoForgeLuckperms.class).triggerNetworkSync();
			logger.info("linking with LuckPerms...");
			logger.info(luckperms.getPlatform().toString());
		} catch (IllegalStateException e1) {
			logger.error("LuckPermsが見つかりませんでした。");
			return;
		}
		Main.getInjector().getInstance(PlayerUtils.class).loadPlayers();
		Main.getInjector().getInstance(ServerStatusCache.class).serverStatusCache();
		logger.info("fmc neoforge mod has been enabled.");
	}

	@SubscribeEvent
	public static void onServerStopping(ServerStoppingEvent event) {
		if (!isStop) {
			isStop = true;
			Main.getInjector().getInstance(DoServerOffline.class).updateDatabase();
			Main.getInjector().getInstance(AutoShutdown.class).stop();
		}
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent event) {
		CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
		logger.info("Registering fmc commands...");
		LiteralArgumentBuilder<CommandSourceStack> command = LiteralArgumentBuilder.<CommandSourceStack>literal("fmc")
			.then(LiteralArgumentBuilder.<CommandSourceStack>literal("fv")
				.then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("player", StringArgumentType.string())
					.suggests((context, builder) ->
						SharedSuggestionProvider.suggest(
							context.getSource().getServer().getPlayerList().getPlayers().stream()
							.map(player -> player.getGameProfile().getName()),
						builder))
					.then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("proxy_cmds", StringArgumentType.greedyString())
						.executes(context -> FMCCommand.execute(context, "fv")))))
			.then(LiteralArgumentBuilder.<CommandSourceStack>literal("reload")
				.executes(context -> FMCCommand.execute(context, "reload")))
			.then(LiteralArgumentBuilder.<CommandSourceStack>literal("test")
				.then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("arg", StringArgumentType.string())
					.suggests((context, builder) ->
						SharedSuggestionProvider.suggest(
							context.getSource().getServer().getPlayerList().getPlayers().stream()
							.map(player -> player.getGameProfile().getName()),
						builder))
					.then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("option", StringArgumentType.string())
						.suggests((context, builder) ->
							SharedSuggestionProvider.suggest(FMCCommand.customList, builder))
						.executes(context -> FMCCommand.execute(context, "test")))));
		dispatcher.register(command);
		logger.info("FMC command registered.");
	}
}
