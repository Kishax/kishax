package bungee_command;

import bungee.Config;
import bungee.Main;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

public class Hub extends Command implements TabExecutor
{
	public Hub()
	{
		super("hub");
	}
	
	@Override
	public void execute(CommandSender sender, String[] args)
	{
		if (Config.getConfig().getString("Servers.Hub","").isEmpty())
		{
			sender.sendMessage(new ComponentBuilder("すでに"+Config.getConfig().getString("Servers.Hub")+"サーバーにいます！").color(ChatColor.RED).create());
			return;
		}
		
        if (sender instanceof ProxiedPlayer)
        {
            ProxiedPlayer player = (ProxiedPlayer) sender;
            if (!player.getServer().getInfo().getName().equalsIgnoreCase(Config.getConfig().getString("Servers.Hub")))
            {
            	ServerInfo target = ProxyServer.getInstance().getServerInfo(Config.getConfig().getString("Servers.Hub"));
            	player.connect(target);
            }
            else
            {
            	player.sendMessage(new ComponentBuilder("すでに"+Config.getConfig().getString("Servers.Hub")+"サーバーにいます！").color(ChatColor.RED).create());
            }
        }
        else
        {
        	sender.sendMessage(new ComponentBuilder("このコマンドはプレイヤーのみが実行できます。").color(ChatColor.RED).create());
        	return;
        }
        return;
	}

	@Override
	public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
		return null;
	}
}