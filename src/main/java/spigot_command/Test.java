package spigot_command;

import java.util.Objects;

import org.bukkit.command.CommandSender;

import com.google.inject.Inject;


public class Test {
	@Inject
	public Test() {
		//
	}
	
	public void execute(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
		if (args.length == 1 || Objects.isNull(args[1]) || args[1].isEmpty()) {
			sender.sendMessage("引数を入力してください。");
			return;
  	  	}

  	  	sender.sendMessage("第1引数: "+args[1]);
	}
}
