package keyp.forev.fmc.velocity.cmd;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import keyp.forev.fmc.velocity.util.Config;
import keyp.forev.fmc.velocity.util.RomajiConversion;

public class SwitchRomajiConvType {

	public static List<String> args1 = new ArrayList<>(Arrays.asList("add","remove"));
	public static List<String> args1_1 = new ArrayList<>(Arrays.asList("switch","reload"));
	private final Config config;
	private final Logger logger;
	private final RomajiConversion rc;
	private String convserverName = null;
	
	@Inject
	public SwitchRomajiConvType(Config config, Logger logger, RomajiConversion rc) {
		this.config = config;
		this.logger = logger;
		this.rc = rc;
	}
	
	private void sendDefaultMessage(@NotNull CommandSource source) {
		if (source.hasPermission("fmc.proxy.conv.*")) {
			source.sendMessage(Component.text("usage: /fmcp conv <add|remove|switch|reload> [<add|remove>:key] [<add>:value]\n(例) /fmcp conv add bakumoriraisu 爆盛りライス\n(例) /fmcp conv remove bakumoriraisu").color(NamedTextColor.GREEN));
		} else {
			source.sendMessage(Component.text("usage: /fmcp conv <add|remove> [<add|remove>:key] [<add>:value]\n(例) /fmcp conv add bakumoriraisu 爆盛りライス\n(例) /fmcp conv remove bakumoriraisu").color(NamedTextColor.GREEN));
		}
	}

	public void execute(@NotNull CommandSource source, String[] args) {
		switch(args.length) {
			case 0, 1 -> sendDefaultMessage(source);
			case 2 -> {
				switch(args[1].toLowerCase()) {
					case "reload" -> {
						try {
							rc.reloadCsv();
							source.sendMessage(Component.text("CSVファイルをリロードしました。").color(NamedTextColor.GREEN));
						} catch(Exception e) {
							source.sendMessage(Component.text("CSVファイルの読み込みに失敗しました。").color(NamedTextColor.RED));
						}
					}

					case "switch"-> {
						Map<String, Object> ConvConfig = config.getStringObjectMap("Conv");
						if(Objects.isNull(ConvConfig)) {
							source.sendMessage(Component.text("コンフィグの設定が不十分です。").color(NamedTextColor.RED));
							return;
						}
						
						if (!(source instanceof Player)) {
							//source.sendMessage(Component.text("このコマンドはプレイヤーのみが実行できます。").color(NamedTextColor.RED));
							if (Objects.isNull(convserverName)) {
								source.sendMessage(Component.text("現在のサーバーの値がNullです。").color(NamedTextColor.RED));
								return;
							}
							
							if (config.getBoolean("Conv.Mode")) {
								source.sendMessage(Component.text("ローマ字変換がpde方式になりました。").color(NamedTextColor.GREEN));
								ConvConfig.put("Mode", false);
								try {
									config.saveConfig();
								} catch (IOException e) {
									logger.error("An IOException error occurred: " + e.getMessage());
									for (StackTraceElement element : e.getStackTrace()) {
										logger.error(element.toString());
									}
								}
							} else {
								source.sendMessage(Component.text("ローマ字変換がMap方式になりました。").color(NamedTextColor.GREEN));
								ConvConfig.put("Mode", true);
								try {
									config.saveConfig();
								} catch (IOException e) {
									logger.error("An IOException error occurred: " + e.getMessage());
									for (StackTraceElement element : e.getStackTrace()) {
										logger.error(element.toString());
									}
								}
							}
						} else {
							Player player = (Player) source;
							
							// プレイヤーの現在のサーバーを取得
							player.getCurrentServer().ifPresent(serverConnection -> {
								RegisteredServer server = serverConnection.getServer();
								convserverName = server.getServerInfo().getName();
							});
							
							if (Objects.isNull(convserverName)) {
								source.sendMessage(Component.text("現在のサーバーの値がNullです。").color(NamedTextColor.RED));
								return;
							}
							
							if (config.getBoolean("Conv.Mode")) {
								source.sendMessage(Component.text("ローマ字変換がpde方式になりました。").color(NamedTextColor.GREEN));
								ConvConfig.put("Mode", false);
								try {
									config.saveConfig();
								} catch (IOException e) {
									logger.error("An IOException error occurred: " + e.getMessage());
									for (StackTraceElement element : e.getStackTrace()) {
										logger.error(element.toString());
									}
								}
							} else {
								source.sendMessage(Component.text("ローマ字変換がMap方式になりました。").color(NamedTextColor.GREEN));
								ConvConfig.put("Mode", true);
								try {
									config.saveConfig();
								} catch (IOException e) {
									logger.error("An IOException e error occurred: " + e.getMessage());
									for (StackTraceElement element : e.getStackTrace()) {
										logger.error(element.toString());
									}
								}
							}
						}
					}
					case "add" -> source.sendMessage(Component.text("usage: /fmcp conv <add|remove> <key> <value>\n(例) /fmcp conv add bakumoriraisu 爆盛りライス").color(NamedTextColor.GREEN));
					case "remove" -> source.sendMessage(Component.text("usage: /fmcp conv remove <key>\n(例) /fmcp conv remove bakumoriraisu").color(NamedTextColor.GREEN));
					default -> sendDefaultMessage(source);
				}
			}
			case 3 -> {
				switch(args[1].toLowerCase()) {
					case "add" -> {
						source.sendMessage(Component.text("変換後の日本語／カタカナ／漢字を入力してください。\n(例) /fmcp conv add bakumoriraisu 爆盛りライス\n(例) /fmcp conv remove bakumoriraisu").color(NamedTextColor.RED));
					}
					case "remove" -> {
						// Listから削除&romaji.csv編集処理
						String key = args[2].toLowerCase();
						rc.removeEntry(source, key);
					}
				}
			}
			case 4 -> {
				switch(args[1].toLowerCase()) {
					case "add" -> {
						String key = args[2].toLowerCase();
						String value = args[3];
						// Listに追加&romaji.csv編集処理
						rc.addEntry(source, key, value, false);
					}
				}
			}
			case 5 -> {
				switch(args[1].toLowerCase()) {
					case "add" -> {
						switch(args[4].toLowerCase()) {
							case "true"-> {
								String key = args[2].toLowerCase();
								String value = args[3];
								rc.addEntry(source, key, value, true);
							}
							case "false"-> {
								String key = args[2].toLowerCase();
								String value = args[3];
								rc.addEntry(source, key, value, false);
							}
							default -> {
								source.sendMessage(Component.text("第6引数にtrueかfalseを入力してください。").color(NamedTextColor.RED));
							}
						}
					}
				}
			}
			default -> sendDefaultMessage(source);
		}
	}
}