package spigot_command;

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

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import spigot.ImageMap;
import spigot.Main;
import spigot.PortalsConfig;

public class FMCCommand implements TabExecutor {
	private final common.Main plugin;
	private final PortalsConfig psConfig;
	private final PortalsMenu pm;
	private final List<String> subcommands = new ArrayList<>(Arrays.asList("reload","fv","mcvc","portal","hideplayer", "im", "image"));
	@Inject
	public FMCCommand(common.Main plugin, PortalsConfig psConfig, PortalsMenu pm) {
		this.plugin = plugin;
		this.psConfig = psConfig;
		this.pm = pm;
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
			case "image","im" -> {
				if (args.length > 1) {
					if (!sender.hasPermission("fmc." + args[0] + "." + args[1])) {
						sender.sendMessage(ChatColor.RED + "権限がありません。");
						return true;
					}
					if (args[1].equalsIgnoreCase("create")) {
						Main.getInjector().getInstance(ImageMap.class).executeImageMap(sender, cmd, label, args);
						return true;
					} else if (args[1].equalsIgnoreCase("createqr")) {
						Main.getInjector().getInstance(ImageMap.class).executeQRMap(sender, cmd, label, args);
						return true;
					} else if (args[1].equalsIgnoreCase("q")) {
						Main.getInjector().getInstance(ImageMap.class).executeQ(sender, cmd, label, args, false);
						return true;
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
					if (args[1].equalsIgnoreCase("wand")) {
						Main.getInjector().getInstance(PortalsWand.class).execute(sender, cmd, label, args);
						return true;
					} else if (args.length > 1 && args[1].equalsIgnoreCase("delete")) {
						if (args.length > 2) {
							String portalName = args[2];
							Main.getInjector().getInstance(PortalsDelete.class).execute(sender,portalName);
							return true;
						} else {
							sender.sendMessage("Usage: /fmc portal delete <portalUUID>");
							return true;
						}
					} else if (args[1].equalsIgnoreCase("rename")) {
						if (args.length > 2) {
							if (args.length > 3) {
								String portalUUID = args[2];
								String portalName = args[3];
								Main.getInjector().getInstance(PortalsRename.class).execute(sender,portalUUID,portalName);
								return true;
							}
						} else {
							sender.sendMessage("Usage: /fmc portal rename <portalUUID> <newName>");
							return true;
						}
					} else if (args[1].equalsIgnoreCase("menu")) {
						if (plugin.getConfig().getBoolean("Portals.Menu", false)) {
							if (sender instanceof Player player) {
								if (args.length > 2 && args[2].equalsIgnoreCase("server")) {
									if (!(player.hasPermission("group.new-fmc-user") || player.hasPermission("group.sub-admin") || player.hasPermission("group.super-admin"))) {
										player.sendMessage("先にUUID認証を完了させてください。");
										return true;
									}
									if (args.length > 3) {
										String serverType = args[3].toLowerCase();
										switch (serverType) {
											case "life","distributed","mod" -> {
												int page = pm.getPage(player, serverType);
												pm.openServerEachInventory((Player) sender, serverType, page);
												return true;
											}
											default -> {
												sender.sendMessage("Unknown server type. Usage: /fmc portal menu server <life | distribution | mod>");
												return true;
											}
										}
									} else {
										// /fmc portal menu serverと打った場合(タイプ指定していない場合)
										Main.getInjector().getInstance(PortalsMenu.class).openServerTypeInventory((Player) sender);
										return true;
									}
								} else {
									sender.sendMessage("Usage: /fmc portal menu server");
									return true;
								}
							} else {
								sender.sendMessage("You must be a player to use this command.");
								return true;
							}
						} else {
							sender.sendMessage(ChatColor.RED + "このサーバーでは、この機能は無効になっています。");
							return true;
						}
					}
				} else {
					sender.sendMessage("Usage: /fmc portal menu");
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
					case "potion" -> {
						for (PotionEffectType potion : PotionEffectType.values()) {
							if (!sender.hasPermission("fmc.potion." + potion.getName().toLowerCase())) continue;
							ret.add(potion.getName());
						}
						return StringUtil.copyPartialMatches(args[1].toLowerCase(), ret, new ArrayList<>());
					}
					case "portal" -> {
						List<String> portalCmds = new ArrayList<>(Arrays.asList("menu","wand","delete","rename"));
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
				}
            }
			case 3 -> {
				if (!sender.hasPermission("fmc." + args[0].toLowerCase())) return Collections.emptyList();
				switch (args[0].toLowerCase()) {
					case "portal" -> {
						switch (args[1].toLowerCase()) {
							case "menu" -> {
								List<String> portalMenuCmds = new ArrayList<>(Arrays.asList("server"));
								for (String portalMenuCmd : portalMenuCmds) {
									if (!sender.hasPermission("fmc.portal.menu.*")) {
										if (!sender.hasPermission("fmc.portal.menu." + portalMenuCmd)) continue;
									}
									ret.add(portalMenuCmd);
								}
								return StringUtil.copyPartialMatches(args[2].toLowerCase(), ret, new ArrayList<>());
							}
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
			case 4 -> {
				if (!sender.hasPermission("fmc." + args[0].toLowerCase())) return Collections.emptyList();
				switch (args[0].toLowerCase()) {
					case "portal" -> {
						switch (args[1].toLowerCase()) {
							case "menu" -> {
								switch (args[2].toLowerCase()) {
									case "server" -> {
										List<String> portalMenuServerCmds = new ArrayList<>(Arrays.asList("life","distibuted","mod"));
										for (String portalMenuServerCmd : portalMenuServerCmds) {
											if (!sender.hasPermission("fmc.portal.menu.server." + portalMenuServerCmd)) continue;
											ret.add(portalMenuServerCmd);
										}
										return StringUtil.copyPartialMatches(args[3].toLowerCase(), ret, new ArrayList<>());
									}
								}
							}
						}
					}
				}
			}
    	}
    	return Collections.emptyList();
    }
}