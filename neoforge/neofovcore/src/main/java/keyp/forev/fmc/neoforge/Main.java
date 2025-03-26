package keyp.forev.fmc.neoforge;

import java.util.Objects;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Injector;

import keyp.forev.fmc.neoforge.server.events.EventListener;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Main.MODID)
public class Main {
  public static final String MODID = "fmc";
  public static Injector injector = null;
  public static final Logger logger = LoggerFactory.getLogger("fmc");

  public Main(IEventBus modBus) {
    logger.info("detected neoforge platform.");
    TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));
    NeoForge.EVENT_BUS.register(EventListener.class);
  }

  public static synchronized Injector getInjector() {
    if (Objects.isNull(injector)) {
      throw new IllegalStateException("Injector has not been initialized yet.");
    }
    return injector;
  }
}
