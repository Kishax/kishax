package spigot;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.google.inject.Inject;

public final class PortalsConfig {
    private final common.Main plugin;
    private File portalsFile;
    private YamlConfiguration portalsConfig;
    private List<Map<?, ?>> portals;

    @Inject
    public PortalsConfig(common.Main plugin) {
        this.plugin = plugin;
        if (Objects.isNull(portalsConfig)) {
            createPortalsConfig();
        }
    }
    
    public void createPortalsConfig() {
        this.portalsFile = new File(plugin.getDataFolder(), "portals.yml");
        if (!portalsFile.exists()) {
            plugin.getLogger().info("portals.yml not found, creating!");
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
            plugin.getLogger().log(Level.SEVERE, "An IOException error occurred: {0}", e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                plugin.getLogger().severe(element.toString());
            }
        }
    }

    public void reloadPortalsConfig() {
        //this.portalsFile = new File(plugin.getDataFolder(), "portals.yml");
    
        this.portalsConfig = YamlConfiguration.loadConfiguration(portalsFile);
        // portalsConfigの内容をログに出力
        //plugin.getLogger().log(Level.INFO, "portals.yml contents: {0}", portalsConfig.saveToString());
        
        this.portals = (List<Map<?, ?>>) portalsConfig.getList("portals");
    }
}
