package common;

import java.util.Objects;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    private spigot.Main main;
	private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("FMC-Plugin");
    @Override
    public void onEnable() {
        try {
            if (!isVelocity()) {
                main = new spigot.Main(this, logger);
                main.onEnable();
            }
        } catch (Exception e) {
            logger.error("An Exception error occurred: {}", e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                logger.error(element.toString());
            }
        }
    }

    @Override
    public void onDisable() {
    	try {
            if (!isVelocity()) {
            	main.onDisable();
            }
        } catch (Exception e) {
            logger.error("An Exception error occurred: {}", e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                logger.error(element.toString());
            }
        }
    }
    
    private boolean isVelocity() {
        return Objects.nonNull(getClass().getClassLoader().getResource("com/velocitypowered/api/plugin/Plugin.class"));
    }
}
