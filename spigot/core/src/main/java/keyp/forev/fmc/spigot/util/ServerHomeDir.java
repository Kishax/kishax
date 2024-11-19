package keyp.forev.fmc.spigot.util;

import java.io.File;
import java.util.Objects;

import com.google.inject.Inject;
import org.bukkit.plugin.java.JavaPlugin;

public class ServerHomeDir {

	private final File dataFolder;
	
	@Inject
	public ServerHomeDir(JavaPlugin plugin) {
		this.dataFolder = plugin.getDataFolder();
	}
	
	public String getServerName() {
		return getParentDir(dataFolder);
	}
	
	private String getParentDir(File dataFolder) {
		// サーバーのホームディレクトリを取得
        File serverHomeDirectory = dataFolder.getParentFile();
        
        // ホームディレクトリ名を取得
        String homeDirectoryPath = serverHomeDirectory.getAbsolutePath();
        
        // Fileオブジェクトを作成
        File file = new File(homeDirectoryPath);
        
        // "plugins"ディレクトリの親ディレクトリを取得
        File parentDir = file.getParentFile();
        
        // 親ディレクトリが存在するか確認し、名前を取得
        if (Objects.nonNull(parentDir)) {
        	return parentDir.getName();
        }
		
        return null;
	}
}
