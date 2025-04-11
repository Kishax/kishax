package f5.si.kishax.mc.forge;

import java.util.Objects;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Injector;

import f5.si.kishax.mc.forge.server.events.EventListener;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

@Mod(Main.MODID)
public class Main {
  public static final String MODID = "kishax";
  public static Injector injector = null;
  public static final Logger logger = LoggerFactory.getLogger("kishax");

  public Main() {
    logger.info("detected forge platform.");
    TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));
    MinecraftForge.EVENT_BUS.register(EventListener.class);
  }

  //public Main() {
  //	IEventBus forgeEventBus = MinecraftForge.EVENT_BUS;
  //	forgeEventBus.addListener(this::commonSetup);
  //}

  //private void commonSetup(final FMLCommonSetupEvent e) {
  //	logger.info("detected forge platform.");
  //	TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));
  //	MinecraftForge.EVENT_BUS.register(this);
  //}

  public static synchronized Injector getInjector() {
    if (Objects.isNull(injector)) {
      throw new IllegalStateException("Injector has not been initialized yet.");
    }
    return injector;
  }
}
