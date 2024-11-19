package keyp.forev.fmc.spigot.util;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.bukkit.plugin.java.JavaPlugin;

@Singleton
public final class PortalsConfig {
    private final JavaPlugin plugin;
    private final Logger logger;
    private File portalsFile;
    private YamlConfiguration portalsConfig;
    private List<Map<?, ?>> portals;

    @Inject
    public PortalsConfig(JavaPlugin plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
        if (Objects.isNull(portalsConfig)) {
            createPortalsConfig();
        }
    }
    
    @SuppressWarnings("unchecked")
    public void createPortalsConfig() {
        this.portalsFile = new File(plugin.getDataFolder(), "portals.yml");
        if (!portalsFile.exists()) {
            logger.info("portals.yml not found, creating!");
            portalsFile.getParentFile().mkdirs();
            plugin.saveResource("portals.yml", false);
        }
    
        this.portalsConfig = YamlConfiguration.loadConfiguration(portalsFile);
        this.portals = (List<Map<?, ?>>) portalsConfig.getList("portals");
    }
    
    public FileConfiguration getPortalsConfig() {
        return this.portalsConfig;
    }
    
    public List<Map<?, ?>> getPortals() {
        return this.portals;
    }

    public void savePortalsConfig() {
        try {
            portalsConfig.save(portalsFile);
            reloadPortalsConfig();
        } catch (IOException e) {
            logger.error("An IOException error occurred: {}", e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                logger.error(element.toString());
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void reloadPortalsConfig() {
        //this.portalsFile = new File(plugin.getDataFolder(), "portals.yml");
    
        this.portalsConfig = YamlConfiguration.loadConfiguration(portalsFile);
        // portalsConfigの内容をログに出力
        //logger.info("portals.yml contents: {}", portalsConfig.saveToString());
        
        this.portals = (List<Map<?, ?>>) portalsConfig.getList("portals");
    }
}
