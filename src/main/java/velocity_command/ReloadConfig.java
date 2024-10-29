package velocity_command;

import java.io.IOException;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;

import net.kyori.adventure.text.Component;
import velocity.Config;
import velocity.DoServerOnline;

public class ReloadConfig {

	private final Config config;
	private final Logger logger;
	private final DoServerOnline dso;
	@Inject
	public ReloadConfig(Config config, Logger logger, DoServerOnline dso) {
		this.config = config;
		this.logger = logger;
		this.dso = dso;
	}
	
    public void execute(@NotNull CommandSource source, String[] args) {
    	try {
    		config.loadConfig();
			dso.updateDatabaseFromCmd();
		} catch (IOException e1) {
			logger.error("An IOException error occurred: " + e1.getMessage());
			for (StackTraceElement element : e1.getStackTrace()) {
				logger.error(element.toString());
			}
		}
		
        source.sendMessage(Component.text("コンフィグをリロードしました。"));
    }
}
