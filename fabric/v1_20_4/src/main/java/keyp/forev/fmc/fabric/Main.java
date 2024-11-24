package keyp.forev.fmc.fabric;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;

import keyp.forev.fmc.common.Database;
import keyp.forev.fmc.common.PlayerUtils;
import keyp.forev.fmc.fabric.cmd.FMCCommand;
import keyp.forev.fmc.fabric.util.AutoShutdown;
import keyp.forev.fmc.fabric.util.Config;
import keyp.forev.fmc.fabric.util.DoServerOffline;
import keyp.forev.fmc.fabric.util.FabricLuckperms;
import keyp.forev.fmc.fabric.util.FabricServerHomeDir;
import keyp.forev.fmc.fabric.util.Module;
import keyp.forev.fmc.fabric.util.Rcon;
import keyp.forev.fmc.fabric.util.ServerStatusCache;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class Main implements ModInitializer {
    public static AtomicBoolean isHub = new AtomicBoolean(false);
	private static Injector injector = null;
	private final FabricLoader fabric;
	private final Logger logger = LoggerFactory.getLogger("FMC");
	
	public Main() {
		this.fabric = FabricLoader.getInstance();
	}
	
    @Override
    public void onInitialize() {
        logger.info("detected fabric platform.");
    	CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            new FMCCommand(logger).registerCommand(dispatcher, registryAccess, environment);
        });
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            injector = Guice.createInjector(new Module(fabric, logger, server));
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));
            Database db = getInjector().getInstance(Database.class);
            try (Connection conn = db.getConnection()) {
                ifHubThenUpdate(conn);
            } catch (SQLException | ClassNotFoundException e) {
                logger.error("An error occurred while updating the database: {}", e.getMessage());
            }
            getInjector().getInstance(AutoShutdown.class).start();
            getInjector().getInstance(Rcon.class).startMCVC();
            try {
                LuckPerms luckperms = LuckPermsProvider.get();
                getInjector().getInstance(FabricLuckperms.class).triggerNetworkSync();
                logger.info("linking with LuckPerms...");
                logger.info(luckperms.getPlatform().toString());
            } catch (Exception e) {
                logger.error("Error linking with LuckPerms", e);
            }
            getInjector().getInstance(PlayerUtils.class).loadPlayers();
            // DoServerOnlineとPortFinderとSocketの処理を統合
            getInjector().getInstance(ServerStatusCache.class).serverStatusCache();
            logger.info("fmc plugin has been enabled.");
        });
        
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
        	server.sendMessage(Text.literal("サーバーが停止中です...").formatted(Formatting.RED));
        	getInjector().getInstance(DoServerOffline.class).UpdateDatabase();
        	getInjector().getInstance(AutoShutdown.class).stop();
            if (getInjector().getInstance(Config.class).getBoolean("MCVC.Mode", false)) {
                getInjector().getInstance(Rcon.class).stopMCVC();
            }
        });
    }
    
    public static synchronized Injector getInjector() {
    	if (Objects.isNull(injector)) {
    		throw new IllegalStateException("Injector has not been initialized yet.");
    	}
        return injector;
    }

    private void ifHubThenUpdate(Connection conn) throws SQLException, ClassNotFoundException {
		String thisServerName = getInjector().getInstance(FabricServerHomeDir.class).getServerName();
        String query = "SELECT hub FROM status WHERE name=? LIMIT 1;";
        PreparedStatement ps = conn.prepareStatement(query);
        ps.setString(1, thisServerName);
		ResultSet rs = ps.executeQuery();
        if (rs.next()) {
			if (rs.getBoolean("hub")) {
				Main.isHub.set(true);
			}
        }
    }
}
