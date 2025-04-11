package f5.si.kishax.mc.fabric.util.config;

import java.nio.file.Path;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import f5.si.kishax.mc.common.module.interfaces.binding.annotation.DataDirectory;
import f5.si.kishax.mc.common.util.config.Config;

@Singleton
public class FabricConfig extends Config {
  @Inject
  public FabricConfig(Logger logger, @DataDirectory Path dataDirectory) {
    super(logger, dataDirectory, "config.yml");
    setConfig();
  }
}
