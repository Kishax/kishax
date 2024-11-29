package keyp.forev.fmc.spigot.database;

import org.bukkit.plugin.java.JavaPlugin;

import com.google.inject.Inject;

import keyp.forev.fmc.common.database.interfaces.DatabaseInfo;

public class SpigotDatabaseInfo implements DatabaseInfo {
    private final String host;
    private final String user;
    private final String password;
    private final String defaultDatabase;
    private final int port;
    @Inject
    public SpigotDatabaseInfo(JavaPlugin plugin) {
        this.host = plugin.getConfig().getString("MySQL.Host", "");
        this.user = plugin.getConfig().getString("MySQL.User", "");
        this.password = plugin.getConfig().getString("MySQL.Password", "");
		this.defaultDatabase = plugin.getConfig().getString("MySQL.Database", "");
		this.port = plugin.getConfig().getInt("MySQL.Port", 0);
    }

    @Override
    public boolean check() {
        return !host.isEmpty() && port != 0 && !defaultDatabase.isEmpty() && !user.isEmpty() && !password.isEmpty();
    }
    
    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getUser() {
        return user;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getDefaultDatabase() {
        return defaultDatabase;
    }

    @Override
    public int getPort() {
        return port;
    }
}
