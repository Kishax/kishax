package spigot_command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.StringUtil;

import com.google.inject.Inject;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import spigot.Main;

public class FMCCommand implements TabExecutor {

	private final List<String> subcommands = new ArrayList<>(Arrays.asList("reload","test","fv","mcvc","portal"));
	
	@Inject
	public FMCCommand() {
		//
	}
	
	@Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
		if(sender == null) return true;

    	if (args.length == 0 || !subcommands.contains(args[0].toLowerCase())) {
    		BaseComponent[] component =
    			    new ComponentBuilder(ChatColor.YELLOW+"FMC COMMANDS LIST").bold(true).underlined(true)
    			        .append(ChatColor.AQUA+"\n\n/fmc reload")
    			        .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,"/fmc reload"))
    			        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("コンフィグ、リロードします！(クリックしてコピー)")))
    			        .append(ChatColor.AQUA+"\n\n/fmc test <arg-1>")
    			        .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,"/fmc test "))
    			        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("第一引数を返します！(クリックしてコピー)")))
    			        .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,"/fmc fv "))
    			        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("プロキシコマンドをフォワードします！(クリックしてコピー)")))
    			        .append(ChatColor.AQUA+"\n\n/fmcb mcvc")
                        .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/fmcp mcvc"))
                        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("MCVCモードの切り替えを行います！(クリックしてコピー)")))
						.append(ChatColor.AQUA+"\n\n/fmcb portal")
                        .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/fmcp portals"))
                        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("ポータルに関して！(クリックしてコピー)")))
    			        .create();
    		sender.spigot().sendMessage(component);
    		return true;
    	}

    	if (!sender.hasPermission("fmc." + args[0])) {
    		sender.sendMessage("access-denied");
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
				
			case "test" -> {
				Main.getInjector().getInstance(Test.class).execute(sender, cmd, label, args);
				return true;
			}
				
			case "mcvc" -> {
				Main.getInjector().getInstance(MCVC.class).execute(sender, cmd, label, args);
				return true; 
			}

			case "portal" -> {
				if (args.length > 1 && args[1].equalsIgnoreCase("menu")) {
					if (args.length > 2 && args[2].equalsIgnoreCase("server")) {
						if (args.length > 3) {
							switch (args[3].toLowerCase()) {
								case "life" -> {
									Main.getInjector().getInstance(PortalsMenu.class).openLifeServerInventory((Player) sender);
									return true;
								}
								case "distribution" -> {
									Main.getInjector().getInstance(PortalsMenu.class).openDistributionServerInventory((Player) sender);
									return true;
								}
								case "mod" -> {
									Main.getInjector().getInstance(PortalsMenu.class).openModServerInventory((Player) sender);
									return true;
								}
								default -> {
									sender.sendMessage("Unknown server type. Usage: /fmc portal menu server <life | distribution | mod>");
									return false;
								}
							}
						} else {
							// /fmc portal menu serverと打った場合
							//sender.sendMessage("Usage: /fmc portal menu server <life|distribution|mod>");
							Main.getInjector().getInstance(PortalsMenu.class).OpenServerTypeInventory((Player) sender);
							return false;
						}
					} else {
						sender.sendMessage("Usage: /fmc portal menu server");
						return false;
					}
				} else {
					sender.sendMessage("Usage: /fmc portal menu");
					return false;
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

					case "test" -> {
						return StringUtil.copyPartialMatches(args[1].toLowerCase(), ret, new ArrayList<>());
					}

					case "portal" -> {
						List<String> portalCmds = new ArrayList<>(Arrays.asList("menu","wand"));
						for (String portalcmd : portalCmds) {
							if (!sender.hasPermission("fmc.portal." + portalcmd)) continue;
							ret.add(portalcmd);
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
						}
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
										List<String> portalMenuServerCmds = new ArrayList<>(Arrays.asList("life","distribution","mod"));
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