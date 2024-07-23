package velocity_command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import velocity.Config;
import velocity.PlayerList;

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FMCCommand implements SimpleCommand
{
    private final ProxyServer server;
    private final Logger logger;
    public List<String> subcommands = new ArrayList<>(Arrays.asList("debug", "hub", "reload", "ss", "req", "start", "stp", "retry", "debug", "cancel", "perm"));
    public List<String> anylists = new ArrayList<>(Arrays.asList("true", "false"));

    public FMCCommand(ProxyServer server, Logger logger)
    {
        this.server = server;
        this.logger = logger;
    }

    @Override
    public void execute(Invocation invocation)
    {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length == 0 || !subcommands.contains(args[0].toLowerCase()))
        {
            if (source.hasPermission("fmc.velocity.commandslist"))
            {
                TextComponent component = Component.text()
                        .append(Component.text("FMC COMMANDS LIST").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD, TextDecoration.UNDERLINED))
                        .append(Component.text("\n/hub").color(NamedTextColor.AQUA))
                        .append(Component.text("\n/fmcb hub").color(NamedTextColor.AQUA)
                                .clickEvent(ClickEvent.suggestCommand("/fmcb hub"))
                                .hoverEvent(HoverEvent.showText(Component.text("ホームサーバーに帰還"))))
                        .append(Component.text("\n\n/fmcb ss <server>").color(NamedTextColor.AQUA)
                                .clickEvent(ClickEvent.suggestCommand("/fmcb ss "))
                                .hoverEvent(HoverEvent.showText(Component.text("サーバーステータス取得"))))
                        .append(Component.text("\n\n/fmcb start <server>").color(NamedTextColor.AQUA)
                                .clickEvent(ClickEvent.suggestCommand("/fmcb start "))
                                .hoverEvent(HoverEvent.showText(Component.text("サーバーを起動"))))
                        .append(Component.text("\n\n/fmcb req <server>").color(NamedTextColor.AQUA)
                                .clickEvent(ClickEvent.suggestCommand("/fmcb req"))
                                .hoverEvent(HoverEvent.showText(Component.text("サーバー起動リクエスト"))))
                        .append(Component.text("\n\n/fmcb cancel").color(NamedTextColor.AQUA)
                                .clickEvent(ClickEvent.suggestCommand("/fmcb cancel"))
                                .hoverEvent(HoverEvent.showText(Component.text("「キャンセルしました」メッセージ"))))
                        .append(Component.text("\n\n/fmcb debug").color(NamedTextColor.AQUA)
                                .clickEvent(ClickEvent.suggestCommand("/fmcb debug"))
                                .hoverEvent(HoverEvent.showText(Component.text("管理者専用デバッグモード"))))
                        .append(Component.text("\n\n/fmcb reload").color(NamedTextColor.AQUA)
                                .clickEvent(ClickEvent.suggestCommand("/fmcb reload"))
                                .hoverEvent(HoverEvent.showText(Component.text("コンフィグ、リロード"))))
                        .append(Component.text("\n\n/fmcb perm <add|remove|list> [Short:permission] <player>").color(NamedTextColor.AQUA)
                                .clickEvent(ClickEvent.suggestCommand("/fmcb perm "))
                                .hoverEvent(HoverEvent.showText(Component.text("ユーザーに対して権限の追加と除去"))))
                        .build();
                source.sendMessage(component);
            }
            else
            {
                source.sendMessage(Component.text("You do not have permission for fmc commands.").color(NamedTextColor.RED));
            }
            return;
        }

        String subCommand = args[0];

        switch (subCommand.toLowerCase())
        {
            case "debug":
                new Debug(source, args);
                break;

            case "start":
                new StartServer(source, args);
                break;

            case "ss":
                new SetServer(source, args);
                break;

            case "hub":
                new Hub().execute(invocation);
                break;

            case "retry":
                new Retry(source, args);
                break;

            case "reload":
                new ReloadConfig(source, args);
                break;

            case "stp":
                new ServerTeleport(source, args);
                break;

            case "req":
                new Request(source, args);
                break;

            case "cancel":
                new Cancel(source, args);
                break;

            case "perm":
                new Perm(source, args);
                break;

            default:
                source.sendMessage(Component.text("Unknown subcommand: " + subCommand));
        }
    }

    @Override
    public List<String> suggest(Invocation invocation)
    {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();
        List<String> ret = new ArrayList<>();

        switch (args.length)
        {
            case 1:
                for (String subcmd : subcommands)
                {
                    if (!source.hasPermission("fmc.velocity." + subcmd)) continue;
                    ret.add(subcmd);
                }
                return ret;

            case 2:
                if (!source.hasPermission("fmc.velocity." + args[0].toLowerCase())) return Collections.emptyList();

                switch (args[0].toLowerCase())
                {
                    case "something":
                        for (String any : anylists)
                        {
                            ret.add(any);
                        }
                        return ret;

                    case "start":
                    case "ss":
                    case "req":
                    case "stp":
                        for (RegisteredServer server : server.getAllServers())
                        {
                            ret.add(server.getServerInfo().getName());
                        }
                        return ret;
                    case "perm":
                        for (String args1 : Perm.args1)
                        {
                            ret.add(args1);
                        }
                        return ret;
                    default:
                        return Collections.emptyList();
                }
            case 3:
                if (!source.hasPermission("fmc.velocity." + args[0].toLowerCase())) return Collections.emptyList();

                switch (args[1].toLowerCase())
                {
                    case "add":
                    case "remove":
                        List<String> permS = Config.getInstance().getStringList("Permission.Short_Name");
                        for (String permS1 : permS)
                        {
                            ret.add(permS1);
                        }
                        return ret;
                }
            case 4:
                if (!source.hasPermission("fmc.velocity." + args[0].toLowerCase())) return Collections.emptyList();

                PlayerList.loadPlayers(); // プレイヤーリストをロード
                List<String> permS = Config.getInstance().getStringList("Permission.Short_Name");
                
                if (permS.contains(args[2].toLowerCase()))
                {
                    for (String player : PlayerList.getPlayerList())
                    {
                        ret.add(player);
                    }
                }
                return ret;
        }
        return Collections.emptyList();
    }
}
