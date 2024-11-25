package keyp.forev.fmc.forge.util;

import java.nio.file.Path;

import com.google.inject.Inject;

import keyp.forev.fmc.common.ServerHomeDir;

public class ForgeServerHomeDir implements ServerHomeDir {
	private final Path configPath;
	@Inject
	public ForgeServerHomeDir(Path configPath) {
		this.configPath = configPath;
	}
	
	@Override
	public String getServerName() {
		return configPath.getParent().getFileName().toString();
	}
}
