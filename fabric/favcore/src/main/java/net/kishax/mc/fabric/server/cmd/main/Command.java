package net.kishax.mc.fabric.server.cmd.main;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.kishax.mc.fabric.Main;
import net.kishax.mc.fabric.server.FabricLuckperms;
import net.kishax.mc.fabric.server.cmd.sub.CommandForward;
import net.kishax.mc.fabric.server.cmd.sub.ReloadConfig;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.CommandManager.RegistrationEnvironment;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class Command {
  private static final List<String> customList = Arrays.asList("option1", "option2", "option3");
  private final Logger logger;

  public Command(Logger logger) {
    this.logger = logger;
  }

  // CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment)ブロックで読み込む
  public void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, RegistrationEnvironment environment) {
    logger.info("Registering commands...");
    dispatcher.register(LiteralArgumentBuilder.<ServerCommandSource>literal("kishax")
      .then(LiteralArgumentBuilder.<ServerCommandSource>literal("fv")
        .then(CommandManager.argument("player", StringArgumentType.string())
          .suggests((context, builder) -> CommandSource.suggestMatching(
            context.getSource().getServer().getPlayerManager().getPlayerNames(), builder))
          .then(CommandManager.argument("proxy_cmds", StringArgumentType.greedyString())
            .executes(context -> execute(context, "fv")))))
      .then(LiteralArgumentBuilder.<ServerCommandSource>literal("reload")
        .executes(context -> execute(context, "reload")))
      .then(LiteralArgumentBuilder.<ServerCommandSource>literal("test")
        .then(CommandManager.argument("arg", StringArgumentType.string())
          .suggests((context, builder) -> {
            return CommandSource.suggestMatching(
              context.getSource().getServer().getPlayerManager().getPlayerNames(), builder);
            })
          .then(CommandManager.argument("option", StringArgumentType.string())
            .suggests((_p, builder) -> {
              return CommandSource.suggestMatching(customList, builder);
            }))
            .executes(context -> execute(context, "test"))))
      );
  }

  public int execute(CommandContext<ServerCommandSource> context, String subcommand) throws CommandSyntaxException {
    ServerCommandSource source = context.getSource();
    try {
      if (!Main.getInjector().getInstance(FabricLuckperms.class).hasPermission(source, "kishax." + subcommand)) {
        source.sendMessage(Text.literal("権限がありません。").formatted(Formatting.RED));
        return 1;
      }
      switch (subcommand) {
        case "reload" -> Main.getInjector().getInstance(ReloadConfig.class).execute(context);
        case "fv" -> Main.getInjector().getInstance(CommandForward.class).execute(context);
        default -> {
          source.sendMessage(Text.literal("Unknown command"));
          return 1;
        }
      }
      return 0;
    } catch (Exception e) {
      logger.error("An Exception error occurred: " + e.getMessage());
      for (StackTraceElement element : e.getStackTrace()) {
        logger.error(element.toString());
      }
      source.sendMessage(Text.literal("An error occurred while executing the command"));
      return 1;
    }
  }
}
