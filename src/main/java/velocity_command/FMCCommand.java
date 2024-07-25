package velocity_command;

import com.google.inject.Inject;
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
import velocity.Main;
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
    private final Config config;
    private final PlayerList pl;
    
    public List<String> subcommands = new ArrayList<>(Arrays.asList("debug", "hub", "reload", "ss", "req", "start", "stp", "retry", "debug", "cancel", "perm","test"));
    public List<String> anylists = new ArrayList<>(Arrays.asList("true", "false"));

    @Inject
    public FMCCommand(ProxyServer server, Logger logger, Config config, PlayerList pl)
    {
        this.server = server;
        this.logger = logger;
        this.config = config;
        this.pl = pl;
    }

    @Override
    public void execute(Invocation invocation)
    {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length == 0 || !subcommands.contains(args[0].toLowerCase()))
        {
            if (source.hasPermission("fmc.proxi.commandslist"))
            {
                TextComponent component = Component.text()
                        .append(Component.text("FMC COMMANDS LIST").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD, TextDecoration.UNDERLINED))
                        .append(Component.text("\n/hub").color(NamedTextColor.AQUA))
                        .append(Component.text("\n/fmcp hub").color(NamedTextColor.AQUA)
                                .clickEvent(ClickEvent.suggestCommand("/fmcp hub"))
                                .hoverEvent(HoverEvent.showText(Component.text("ホームサーバーに帰還"))))
                        .append(Component.text("\n\n/fmcp ss <server>").color(NamedTextColor.AQUA)
                                .clickEvent(ClickEvent.suggestCommand("/fmcp ss "))
                                .hoverEvent(HoverEvent.showText(Component.text("サーバーステータス取得"))))
                        .append(Component.text("\n\n/fmcp start <server>").color(NamedTextColor.AQUA)
                                .clickEvent(ClickEvent.suggestCommand("/fmcp start "))
                                .hoverEvent(HoverEvent.showText(Component.text("サーバーを起動"))))
                        .append(Component.text("\n\n/fmcp req <server>").color(NamedTextColor.AQUA)
                                .clickEvent(ClickEvent.suggestCommand("/fmcp req"))
                                .hoverEvent(HoverEvent.showText(Component.text("サーバー起動リクエスト"))))
                        .append(Component.text("\n\n/fmcp cancel").color(NamedTextColor.AQUA)
                                .clickEvent(ClickEvent.suggestCommand("/fmcp cancel"))
                                .hoverEvent(HoverEvent.showText(Component.text("「キャンセルしました」メッセージ"))))
                        .append(Component.text("\n\n/fmcp debug").color(NamedTextColor.AQUA)
                                .clickEvent(ClickEvent.suggestCommand("/fmcp debug"))
                                .hoverEvent(HoverEvent.showText(Component.text("管理者専用デバッグモード"))))
                        .append(Component.text("\n\n/fmcp reload").color(NamedTextColor.AQUA)
                                .clickEvent(ClickEvent.suggestCommand("/fmcp reload"))
                                .hoverEvent(HoverEvent.showText(Component.text("コンフィグ、リロード"))))
                        .append(Component.text("\n\n/fmcp perm <add|remove|list> [Short:permission] <player>").color(NamedTextColor.AQUA)
                                .clickEvent(ClickEvent.suggestCommand("/fmcp perm "))
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
            	Main.getInjector().getInstance(Debug.class).execute(source, args);
                break;

            case "start":
            	Main.getInjector().getInstance(StartServer.class).execute(source, args);
                break;

            case "ss":
            	Main.getInjector().getInstance(SetServer.class).execute(source, args);
                break;

            case "hub":
            	Main.getInjector().getInstance(Hub.class).execute(invocation);
                break;

            case "retry":
            	Main.getInjector().getInstance(Retry.class).execute(source, args);
                break;

            case "reload":
            	Main.getInjector().getInstance(ReloadConfig.class).execute(source, args);
                break;

            case "stp":
            	Main.getInjector().getInstance(ServerTeleport.class).execute(source, args);
                break;

            case "req":
            	Main.getInjector().getInstance(Request.class).execute(source, args);
                break;

            case "cancel":
            	Main.getInjector().getInstance(Cancel.class).execute(source, args);
                break;

            case "perm":
            	Main.getInjector().getInstance(Perm.class).execute(source, args);
                break;
            case "test":
            	Main.getInjector().getInstance(Test.class).execute(source, args);
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
        	case 0:
            case 1:
                for (String subcmd : subcommands)
                {
                    if (!source.hasPermission("fmc.proxi." + subcmd)) continue;
                    ret.add(subcmd);
                }
                return ret;

            case 2:
                if (!source.hasPermission("fmc.proxi." + args[0].toLowerCase())) return Collections.emptyList();

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
                if (!source.hasPermission("fmc.proxi." + args[0].toLowerCase())) return Collections.emptyList();

                switch (args[1].toLowerCase())
                {
                    case "add":
                    case "remove":
                        List<String> permS = config.getList("Permission.Short_Name");
                        for (String permS1 : permS)
                        {
                            ret.add(permS1);
                        }
                        return ret;
                }
            case 4:
                if (!source.hasPermission("fmc.proxi." + args[0].toLowerCase())) return Collections.emptyList();

                pl.loadPlayers(); // プレイヤーリストをロード
                List<String> permS = config.getList("Permission.Short_Name");
                
                if (permS.contains(args[2].toLowerCase()))
                {
                    for (String player : pl.getPlayerList())
                    {
                        ret.add(player);
                    }
                }
                return ret;
        }
        return Collections.emptyList();
    }
}
