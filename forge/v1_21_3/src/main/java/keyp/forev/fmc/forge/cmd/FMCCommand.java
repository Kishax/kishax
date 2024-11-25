package keyp.forev.fmc.forge.cmd;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import keyp.forev.fmc.forge.Main;
import keyp.forev.fmc.forge.util.ForgeLuckperms;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

public class FMCCommand {

    private static final List<String> customList = Arrays.asList("option1", "option2", "option3");
    private final Logger logger;

    public FMCCommand(Logger logger) {
        this.logger = logger;
    }

    public void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        logger.info("Registering fmc commands...");

        dispatcher.register(Commands.literal("fmc")
            .then(Commands.literal("fv")
                .then(Commands.argument("player", StringArgumentType.string())
                    .suggests((context, builder) -> 
                        SharedSuggestionProvider.suggest(
                            context.getSource().getServer().getPlayerList().getPlayers().stream()
                                .map(player -> player.getGameProfile().getName()),
                            builder))
                    .then(Commands.argument("proxy_cmds", StringArgumentType.greedyString())
                        .executes(context -> execute(context, "fv")))))
            .then(Commands.literal("reload")
                .executes(context -> execute(context, "reload")))
            .then(Commands.literal("test")
                .then(Commands.argument("arg", StringArgumentType.string())
                    .suggests((context, builder) -> 
                        SharedSuggestionProvider.suggest(
                            context.getSource().getServer().getPlayerList().getPlayers().stream()
                                .map(player -> player.getGameProfile().getName()),
                            builder))
                    .then(Commands.argument("option", StringArgumentType.string())
                        .suggests((context, builder) -> 
                            SharedSuggestionProvider.suggest(customList, builder))
                        .executes(context -> execute(context, "test"))))
        ));
    }

    public int execute(CommandContext<CommandSourceStack> context, String subcommand) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();

        try {
            if (!Main.getInjector().getInstance(ForgeLuckperms.class).hasPermission(source, "fmc." + subcommand)) {
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

            return 0; // 正常に実行された場合は 0 を返す
        } catch (Exception e) {
            logger.error("An Exception error occurred: " + e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                logger.error(element.toString());
            }

            source.sendFailure(Component.literal("An error occurred while executing the command"));
            return 1; // 例外が発生した場合は 1 を返す
        }
    }
}
