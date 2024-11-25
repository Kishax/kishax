package keyp.forev.fmc.forge;

import java.util.Objects;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Injector;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Main.MODID)
public class Main {
	public static final String MODID = "fmc";
	public static Injector injector = null;
	public static final Logger logger = LoggerFactory.getLogger("fmc");
	
	public Main() {
	    IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
	    modEventBus.addListener(this::commonSetup);
	}
	
	private void commonSetup(final FMLCommonSetupEvent e) {
		logger.info("detected forge platform.");
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));
		MinecraftForge.EVENT_BUS.register(this);
	}
	
	public static synchronized Injector getInjector() {
		if (Objects.isNull(injector)) {
			throw new IllegalStateException("Injector has not been initialized yet.");
		}
		return injector;
	}
}
