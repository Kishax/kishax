package velocity_command;

import org.jetbrains.annotations.NotNull;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;

public class Test {
	@Inject
	public Test() {
		//
	}
	
	public void execute(@NotNull CommandSource source, String[] args) {
		//Main.getInjector().getInstance(EmojiManager.class).checkAndAddEmojis();
		switch(args.length) {
			case 1-> {
				// args[0]
			}
			case 2-> {
				// args[1]
			}
			default-> {
				//
			}
		}
	}
}
