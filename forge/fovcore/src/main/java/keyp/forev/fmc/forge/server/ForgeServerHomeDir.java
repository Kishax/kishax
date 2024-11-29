package keyp.forev.fmc.forge.server;

import java.nio.file.Path;

import com.google.inject.Inject;

import keyp.forev.fmc.common.server.interfaces.DefaultServerHomeDir;

public class ForgeServerHomeDir implements DefaultServerHomeDir {
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
