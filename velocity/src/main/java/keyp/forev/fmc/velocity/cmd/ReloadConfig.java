package keyp.forev.fmc.velocity.cmd;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;

import common.src.main.java.keyp.forev.fmc.main.Database;
import net.kyori.adventure.text.Component;
import velocity.src.main.java.keyp.forev.fmc.core.main.Config;
import velocity.src.main.java.keyp.forev.fmc.core.main.DoServerOnline;

public class ReloadConfig {
	private final Database db;
	private final Config config;
	private final Logger logger;
	private final DoServerOnline dso;
	@Inject
	public ReloadConfig(Database db, Config config, Logger logger, DoServerOnline dso) {
		this.db = db;
		this.config = config;
		this.logger = logger;
		this.dso = dso;
	}
	
    public void execute(@NotNull CommandSource source, String[] args) {
    	try (Connection conn = db.getConnection()) {
    		config.loadConfig();
			dso.updateDatabaseFromCmd(conn);
		} catch (IOException | SQLException | ClassNotFoundException e1) {
			logger.error("An IOException | SQLException | ClassNotFoundException error occurred: " + e1.getMessage());
			for (StackTraceElement element : e1.getStackTrace()) {
				logger.error(element.toString());
			}
		}
        source.sendMessage(Component.text("コンフィグをリロードしました。"));
    }
}
