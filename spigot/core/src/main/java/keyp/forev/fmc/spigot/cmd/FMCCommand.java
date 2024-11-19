package keyp.forev.fmc.spigot.cmd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.StringUtil;

import com.google.inject.Inject;
import keyp.forev.fmc.spigot.Main;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import keyp.forev.fmc.spigot.util.ImageMap;
import keyp.forev.fmc.spigot.util.PortalsConfig;
import org.bukkit.plugin.java.JavaPlugin;

public class FMCCommand implements TabExecutor {
	private final JavaPlugin plugin;
	private final PortalsConfig psConfig;
	private final List<String> subcommands = new ArrayList<>(Arrays.asList("reload","fv","mcvc","portal","hideplayer", "im", "image", "menu", "button", "check", "setpoint", "confirm"));
	@Inject
	public FMCCommand(JavaPlugin plugin, PortalsConfig psConfig) {
		this.plugin = plugin;
		this.psConfig = psConfig;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
		if (sender == null) {
			return true;
		}
    	if (args.length == 0 || !subcommands.contains(args[0].toLowerCase())) {
    		BaseComponent[] component =
    			    new ComponentBuilder(ChatColor.YELLOW+"FMC COMMANDS LIST").bold(true).underlined(true)
    			        .append(ChatColor.AQUA+"\n\n/fmc reload")
							.event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,"/fmc reload"))
							.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("コンフィグ、リロードします！(クリックしてコピー)")))
						.append(ChatColor.AQUA+"\n\n/fmc fv ")
							.event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,"/fmc fv "))
							.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("プロキシコマンドをフォワードします！(クリックしてコピー)")))
    			        .append(ChatColor.AQUA+"\n\n/fmc mcvc")
							.event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/fmc mcvc"))
							.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("MCVCモードの切り替えを行います！(クリックしてコピー)")))
						.append(ChatColor.AQUA+"\n\n/fmc portal ")
							.event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/fmc portal "))
							.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("ポータルに関して！(クリックしてコピー)")))
    			        .create();
    		sender.spigot().sendMessage(component);
    		return true;
    	}
    	if (!sender.hasPermission("fmc." + args[0])) {
    		sender.sendMessage(ChatColor.RED + "権限がありません。");
    		return true;
    	}
		switch (args[0].toLowerCase()) {
			case "confirm" -> {
				Main.getInjector().getInstance(Confirm.class).execute(sender, cmd, label, args);
				return true;
			}
			case "fv" -> {
				Main.getInjector().getInstance(CommandForward.class).execute(sender, cmd, label, args);
				return true;
			}
			case "reload" -> {
				Main.getInjector().getInstance(ReloadConfig.class).execute(sender, cmd, label, args);
				return true;
			}
			case "mcvc" -> {
				Main.getInjector().getInstance(MCVC.class).execute(sender, cmd, label, args);
				return true; 
			}
			case "hideplayer" -> {
				Main.getInjector().getInstance(HidePlayer.class).execute(sender, cmd, label, args);
				return true;
			}
			case "menu" -> {
				Main.getInjector().getInstance(Menu.class).execute(sender, cmd, label, args);
				return true;
			}
			case "check" -> {
				Main.getInjector().getInstance(Check.class).execute(sender, cmd, label, args);
				return true;
			}
			case "setpoint" -> {
				Main.getInjector().getInstance(SetPoint.class).execute(sender, cmd, label, args);
				return true;
			}
			case "image","im" -> {
				if (args.length > 1) {
					if (!sender.hasPermission("fmc." + args[0] + "." + args[1])) {
						sender.sendMessage(ChatColor.RED + "権限がありません。");
						return true;
					}
					switch (args[1].toLowerCase()) {
						case "create" -> {
							Main.getInjector().getInstance(ImageMap.class).executeImageMapLeading(sender, args);
							return true;
						}
						case "q" -> {
							Main.getInjector().getInstance(ImageMap.class).executeQ(sender, args, false);
							return true;
						}
					}
				} else {
					sender.sendMessage("Usage: /fmc im <create|createqr> <title> <comment> <url>");
					return true;
				}
			}
			case "portal" -> {
				if (args.length > 1) {
					if (!sender.hasPermission("fmc." + args[0] + "." + args[1])) {
						sender.sendMessage(ChatColor.RED + "権限がありません。");
						return true;
					}
					switch (args[1].toLowerCase()) {
						case "wand" -> {
							Main.getInjector().getInstance(PortalsWand.class).execute(sender, cmd, label, args);
							return true;
						}
						case "delete" -> {
							Main.getInjector().getInstance(PortalsDelete.class).execute(sender, cmd, label, args);
							return true;
						}
						case "rename" -> {
							Main.getInjector().getInstance(PortalsRename.class).execute(sender, cmd, label, args);
							return true;
						}
						case "nether" -> {
							Main.getInjector().getInstance(PortalsNether.class).execute(sender, cmd, label, args);
							return true;
						}
					}
				} else {
					sender.sendMessage("Usage: /fmc portal <rename|delete|wand>");
					return true;
				}
			}
		}
		return true;
	}

    @SuppressWarnings("deprecation")
	@Override
	public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
    	List<String> ret = new ArrayList<>();
    	switch (args.length) {
	    	case 1 -> {
				Collections.sort(subcommands);
				for (String subcmd : subcommands) {
					if (!sender.hasPermission("fmc." + subcmd)) continue;
					ret.add(subcmd);
				}
				return StringUtil.copyPartialMatches(args[0].toLowerCase(), ret, new ArrayList<>());
			}
	    	case 2 -> {
				if (!sender.hasPermission("fmc." + args[0].toLowerCase())) return Collections.emptyList();
				switch (args[0].toLowerCase()) {
					case "setpoint" -> {
						List<String> types = new ArrayList<>(Arrays.asList("load", "room", "hub"));
						return StringUtil.copyPartialMatches(args[1].toLowerCase(), types, new ArrayList<>());
					}
					case "potion" -> {
						for (PotionEffectType potion : PotionEffectType.values()) {
							if (!sender.hasPermission("fmc.potion." + potion.getName().toLowerCase())) continue;
							ret.add(potion.getName());
						}
						return StringUtil.copyPartialMatches(args[1].toLowerCase(), ret, new ArrayList<>());
					}
					case "portal" -> {
						List<String> portalCmds = new ArrayList<>(Arrays.asList("nether","wand","delete","rename"));
						for (String portalcmd : portalCmds) {
							if (!sender.hasPermission("fmc.portal." + portalcmd)) continue;
							ret.add(portalcmd);
						}
						return StringUtil.copyPartialMatches(args[1].toLowerCase(), ret, new ArrayList<>());
					}
					case "hideplayer" -> {
						for (Player player : plugin.getServer().getOnlinePlayers()) {
                            ret.add(player.getName());
                        }
                        return StringUtil.copyPartialMatches(args[1].toLowerCase(), ret, new ArrayList<>());
					}
					case "image","im" -> {
						for (String args2 : ImageMap.args2) {
							ret.add(args2);
						}
						return StringUtil.copyPartialMatches(args[1].toLowerCase(), ret, new ArrayList<>());
					}
					case "menu" -> {
						for (String args2 : Menu.args1) {
							ret.add(args2);
						}
						return StringUtil.copyPartialMatches(args[1].toLowerCase(), ret, new ArrayList<>());
					}
				}
            }
			case 3 -> {
				if (!sender.hasPermission("fmc." + args[0].toLowerCase())) return Collections.emptyList();
				switch (args[0].toLowerCase()) {
					case "menu" -> {
						switch (args[1].toLowerCase()) {
							case "server" -> {
								for (String portalMenuServerCmd : Menu.args2) {
									ret.add(portalMenuServerCmd);
								}
								return StringUtil.copyPartialMatches(args[2].toLowerCase(), ret, new ArrayList<>());
							}
						}
					}
					case "portal" -> {
						switch (args[1].toLowerCase()) {
							case "delete","rename" -> {
								// portals.ymlからポータル名を読み取る
                                FileConfiguration portalsConfig = psConfig.getPortalsConfig();
								List<?> rawPortals = portalsConfig.getList("portals");
								if (rawPortals != null) {
									for (Object obj : rawPortals) {
										if (obj instanceof Map<?, ?> portal) {
											String portalName = (String) portal.get("name");
											if (portalName != null && sender.hasPermission("fmc.portal.delete." + portalName)) {
												ret.add(portalName);
											}
										}
									}
								}
                                return StringUtil.copyPartialMatches(args[2].toLowerCase(), ret, new ArrayList<>());
							}
						}
					}
					case "hideplayer" -> {
                        List<String> actions = new ArrayList<>(Arrays.asList("hide", "show"));
                        return StringUtil.copyPartialMatches(args[2].toLowerCase(), actions, new ArrayList<>());
                    }
				}
			}
    	}
    	return Collections.emptyList();
    }
}