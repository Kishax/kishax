package keyp.forev.fmc.velocity.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;

import keyp.forev.fmc.velocity.server.BroadCast;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

//ローマ字変換を行う
public class RomajiConversion {

	public static Map<String, String> csvSets = new HashMap<>(); 
	private final Path filePath;
	private final Logger logger;
	private final BroadCast bc;
	// 定義を保存する変数
	private static ArrayList<RomajiDefinition> RomajiDefinitionData;

	// コンストラクタ
	@Inject
	public RomajiConversion(@DataDirectory Path dataDirectory, Logger logger, BroadCast bc) {
		this.filePath = dataDirectory.resolve("romaji.csv");
		this.logger = logger;
		this.bc = bc;
		RomajiDefinitionData = new ArrayList<>();
				
		try {
            List<String> lines = Files.readAllLines(filePath);
            
            // データの読み込み処理
            for (String s : lines) {
                String[] t = s.split(",");
                if (t.length == 2) {
					RomajiDefinitionData.add(new RomajiDefinition(t[0], t[1]));
					RomajiConversion.csvSets.put(t[0], t[1]);
				}
            }
        } catch (IOException e) {
            logger.error("An IOException error occurred: " + e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                logger.error(element.toString());
            }
        }
	}
	
	public void addEntry(@NotNull CommandSource source, String key, String value, boolean force) {
		try {
			boolean invalid = false;
			if (key.contains(",") && value.contains(",")) {
				invalid = true;
				source.sendMessage(Component.text("Not Allowed to contain ',' in key/value.").color(NamedTextColor.RED));
			}

			if (
				(key.contains("\n") || key.contains("\r") || key.contains("\t") || key.contains("\b") || key.contains("\f")) &&
				(value.contains("\n") || value.contains("\r") || value.contains("\t") || value.contains("\b") || value.contains("\f"))
			) {
				invalid = true;
				source.sendMessage(Component.text("Key/Value cannot contain escape sequences such as newline, tab, etc.").color(NamedTextColor.RED));
			}

			if (invalid) return;

			String who = (source instanceof Player player) ? player.getUsername() : "Server";

			// ファイルの内容を読み込む
			List<String> lines = Files.readAllLines(filePath);
			List<String> updatedLines = new ArrayList<>();
			boolean entryFound = false;

			
			for (String line : lines) {
				String[] parts = line.split(",");
				if (parts.length == 2 && parts[0].equals(key)) {
					entryFound = true;
					if (force) {
						// エントリーを上書き
						updatedLines.add(key + "," + value);
                        source.sendMessage(Component.text("Forced updated entry by " + who + ":\n" + key + " -> " + value).color(NamedTextColor.YELLOW));
					}
				} else {
					updatedLines.add(line);
				}
			}
	
			if (force && entryFound) {
				// 上書きされたListを書き込む
				Files.write(filePath, updatedLines);
				reloadCsv();
				return;
			}

			if (entryFound) {
				if (!force) {
					Component component = Component.text("そのキーはすでに存在しています。\n上書きしますか？\n").color(NamedTextColor.RED)
											.append(Component.text("YES")
        			    			    			.color(NamedTextColor.GOLD)
        			    			    			.clickEvent(ClickEvent.runCommand("/fmcp conv add "+key+" "+value+" true"))
        			                                .hoverEvent(HoverEvent.showText(Component.text("(クリックして)("+key+","+value+")を追加します。"))))
        			    			    	.append(Component.text(" or ").color(NamedTextColor.GOLD))
        			    			    	.append(Component.text("NO").color(NamedTextColor.GOLD)
        			    			    			.clickEvent(ClickEvent.runCommand("/fmcp cancel"))
        			                                .hoverEvent(HoverEvent.showText(Component.text("(クリックして)キャンセルします。"))));
					source.sendMessage(component);
					return;
				}
			}

			// 新しいエントリを先頭に追加する
			lines.add(0, key + "," + value);
	
			// ファイルに書き込む
			Files.write(filePath, lines);

			Component addComponent = Component.text("Added new entry by "+who+":\n" + key + " -> " + value).color(NamedTextColor.YELLOW);
			bc.broadCastMessage(addComponent);

			// データの再読み込み
			reloadCsv();
		} catch (IOException e) {
			logger.error("An IOException occurred while adding entry to CSV: " + e.getMessage());
			for (StackTraceElement element : e.getStackTrace()) {
				logger.error(element.toString());
			}
		}
	}

	public void removeEntry(CommandSource source, String key) {
		try {
			boolean invalid = false;
			if (key.contains(",")) {
				invalid = true;
				source.sendMessage(Component.text("Not Allowed to contain ',' in key.").color(NamedTextColor.RED));
			}

			if (key.contains("\n") || key.contains("\r") || key.contains("\t") || key.contains("\b") || key.contains("\f")) {
				invalid = true;
				source.sendMessage(Component.text("Key cannot contain escape sequences such as newline, tab, etc.").color(NamedTextColor.RED));
			}

			if(invalid) return;

			String who;
			if(source instanceof Player player) {
				who = player.getUsername();
			} else {
				who = "Server";
			}

			// ファイルの内容を読み込む
			List<String> lines = Files.readAllLines(filePath);
			Component removeComponent = null;
			// 削除対象のエントリを探し、削除する
			List<String> updatedLines = new ArrayList<>();
			boolean entryFound = false;
			for (String line : lines) {
				String[] parts = line.split(",");
				if (parts.length == 2 && !parts[0].equals(key)) {
					updatedLines.add(line);
				} else if (parts.length == 2 && parts[0].equals(key)) {
					entryFound = true;
					removeComponent = Component.text("Removed key: "+key+" by "+who).color(NamedTextColor.RED);
				}
			}
	
			if (!entryFound) {
				removeComponent = Component.text("key: "+key+" does not exist.").color(NamedTextColor.RED);
				bc.sendSpecificPlayerMessage(removeComponent, who);
				return;
			}
	
			// ファイルに書き込む
			Files.write(filePath, updatedLines);
			
			bc.broadCastMessage(removeComponent);
			// データの再読み込み
			reloadCsv();
		} catch (IOException e) {
			logger.error("An IOException occurred while removing entry from CSV: " + e.getMessage());
			for (StackTraceElement element : e.getStackTrace()) {
				logger.error(element.toString());
			}
		}
	}

	public void reloadCsv() {
		// 初期化
		RomajiDefinitionData.clear();
		RomajiConversion.csvSets.clear();

		try {
            List<String> lines = Files.readAllLines(filePath);
            
            // データの読み込み処理
            for (String s : lines) {
                String[] t = s.split(",");
                if (t.length == 2) {
					RomajiDefinitionData.add(new RomajiDefinition(t[0], t[1]));
					RomajiConversion.csvSets.put(t[0], t[1]);
				}
            }
        } catch (IOException e) {
            logger.error("An IOException error occurred: " + e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                logger.error(element.toString());
            }
        }
	}

	// 変換データを保存するクラス
	private class RomajiDefinition {
		private String beforeConversion = "";
		private String afterConversion = "";
	
		public RomajiDefinition(String beforeConversion, String afterConversion) {
			this.beforeConversion = beforeConversion;
			this.afterConversion = afterConversion;
		}
		
		// 変換前を取得
		private String getBeforeConversion() {
			return beforeConversion;
		}
		
		// 変換後を取得
		private String getAfterConversion() {
			return afterConversion;
		}
	}
	
	// 変換を行う関数
	public String Romaji(String data) {
		// すべて小文字に
		//data = data.toLowerCase();
	
		// 変換する
		int i = 0;
		do {
			// 変換できるかチェック
			for (RomajiDefinition r : RomajiConversion.RomajiDefinitionData) {
				if (data.length()-i >= r.getBeforeConversion().length()) {
					if (data.substring(i, i+r.getBeforeConversion().length()).equals(r.getBeforeConversion())) {
						data = data.substring(0, i) + r.getAfterConversion() + data.substring(i+r.getBeforeConversion().length(), data.length());  // 変換する
						break;
					}
				}
			}
		}

		while (++i < data.length());  // 文字列の最後まで変換（変換があっても前の文字を調べる必要は無いはず）
		return data;
	}
}
