package velocity_command;

import org.jetbrains.annotations.NotNull;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class Cancel
{
	
	@Inject
	public Cancel()
	{
		//
	}
	
    public void execute(@NotNull CommandSource source,String[] args)
    {
        source.sendMessage(Component.text("キャンセルしました。").color(NamedTextColor.WHITE));
    }
}
