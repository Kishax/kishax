package velocity_command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import velocity.Main;

public class Cancel
{
    public Cancel(CommandSource source,String[] args)
    {
        source.sendMessage(Component.text("キャンセルしました。").color(NamedTextColor.WHITE));
    }
}
