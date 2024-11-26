package keyp.forev.fmc.fabric.util;

import com.google.inject.Inject;

import keyp.forev.fmc.common.ServerHomeDir;
import net.fabricmc.loader.api.FabricLoader;

public class FabricServerHomeDir implements ServerHomeDir {
	private final FabricLoader fabric;
	@Inject
	public FabricServerHomeDir(FabricLoader fabric) {
	    this.fabric = fabric;
	}
	
	@Override
	public String getServerName() {
	    return fabric.getGameDir().toAbsolutePath().getParent().getFileName().toString();
	}
}
