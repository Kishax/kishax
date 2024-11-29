package keyp.forev.fmc.fabric.database;

import com.google.inject.Inject;

import keyp.forev.fmc.common.database.interfaces.DatabaseInfo;
import keyp.forev.fmc.fabric.util.config.FabricConfig;

public class FabricDatabaseInfo implements DatabaseInfo {
    private final String host;
    private final String user;
    private final String password;
    private final String defaultDatabase;
    private final int port;
    @Inject
    public FabricDatabaseInfo(FabricConfig config) {
        this.host = config.getString("MySQL.Host", "");
        this.user = config.getString("MySQL.User", "");
        this.password = config.getString("MySQL.Password", "");
		this.defaultDatabase = config.getString("MySQL.Database", "");
		this.port = config.getInt("MySQL.Port", 0);
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
