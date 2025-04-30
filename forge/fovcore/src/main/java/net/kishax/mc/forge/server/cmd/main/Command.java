package net.kishax.mc.forge.server.cmd.main;

import java.util.Arrays;
import java.util.List;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.kishax.mc.forge.Main;
import net.kishax.mc.forge.server.ForgeLuckperms;
import net.kishax.mc.forge.server.cmd.sub.CommandForward;
import net.kishax.mc.forge.server.cmd.sub.ReloadConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public class Command {
  public static final List<String> customList = Arrays.asList("option1", "option2", "option3");

  public static int execute(CommandContext<CommandSourceStack> context, String subcommand) throws CommandSyntaxException {
    CommandSourceStack source = context.getSource();
    try {
      if (!Main.getInjector().getInstance(ForgeLuckperms.class).hasPermission(source, "kishax." + subcommand)) {
        source.sendFailure(Component.literal("Access denied"));
        return 1;
      }
      switch (subcommand) {
        case "reload" -> Main.getInjector().getInstance(ReloadConfig.class).execute(context);
        case "test" -> source.sendSuccess(() -> Component.literal("TestCommandExecuted"), false);
        case "fv" -> Main.getInjector().getInstance(CommandForward.class).execute(context);
        default -> {
          source.sendFailure(Component.literal("Unknown command"));
          return 1;
        }
      }
      return 0;
    } catch (Exception e) {
      Main.logger.error("An Exception error occurred: " + e.getMessage());
      for (StackTraceElement element : e.getStackTrace()) {
        Main.logger.error(element.toString());
      }
      source.sendFailure(Component.literal("An error occurred while executing the command"));
      return 1;
    }
  }
}
