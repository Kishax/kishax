package keyp.forev.fmc.fabric;

import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;

import keyp.forev.fmc.common.server.DoServerOffline;
import keyp.forev.fmc.common.server.ServerStatusCache;
import keyp.forev.fmc.common.util.PlayerUtils;
import keyp.forev.fmc.fabric.cmd.main.FMCCommand;
import keyp.forev.fmc.fabric.module.Module;
import keyp.forev.fmc.fabric.server.AutoShutdown;
import keyp.forev.fmc.fabric.server.FabricLuckperms;
import keyp.forev.fmc.fabric.server.Rcon;
import keyp.forev.fmc.fabric.util.config.FabricConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
	
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
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
		    new FMCCommand(logger).registerCommand(dispatcher, registryAccess, environment);
		});
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
		    injector = Guice.createInjector(new Module(fabric, logger, server));
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
			getInjector().getInstance(ServerStatusCache.class).serverStatusCache();
			logger.info("fmc fabric mod has been enabled.");
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			getInjector().getInstance(DoServerOffline.class).updateDatabase();
			getInjector().getInstance(AutoShutdown.class).stop();
		    if (getInjector().getInstance(FabricConfig.class).getBoolean("MCVC.Mode", false)) {
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
}
