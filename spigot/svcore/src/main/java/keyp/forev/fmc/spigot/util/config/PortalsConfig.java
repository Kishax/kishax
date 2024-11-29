package keyp.forev.fmc.spigot.util.config;

import java.nio.file.Path;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import keyp.forev.fmc.common.module.interfaces.binding.annotation.DataDirectory;
import keyp.forev.fmc.common.util.config.Config;

@Singleton
public final class PortalsConfig extends Config {
    @Inject
    public PortalsConfig(Logger logger, @DataDirectory Path dataDirectory) {
        super(logger, dataDirectory, "portals.yml");
        setConfig();
    }
}
