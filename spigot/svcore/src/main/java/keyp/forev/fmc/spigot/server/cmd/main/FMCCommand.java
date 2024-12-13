package keyp.forev.fmc.spigot.server.cmd.main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.StringUtil;

import com.google.inject.Inject;
import keyp.forev.fmc.spigot.Main;
import keyp.forev.fmc.spigot.server.ImageMap;
import keyp.forev.fmc.spigot.server.cmd.sub.Check;
import keyp.forev.fmc.spigot.server.cmd.sub.CommandForward;
import keyp.forev.fmc.spigot.server.cmd.sub.Confirm;
import keyp.forev.fmc.spigot.server.cmd.sub.HidePlayer;
import keyp.forev.fmc.spigot.server.cmd.sub.MCVC;
import keyp.forev.fmc.spigot.server.cmd.sub.ReloadConfig;
import keyp.forev.fmc.spigot.server.cmd.sub.SetPoint;
import keyp.forev.fmc.spigot.server.cmd.sub.portal.PortalsDelete;
import keyp.forev.fmc.spigot.server.cmd.sub.portal.PortalsNether;
import keyp.forev.fmc.spigot.server.cmd.sub.portal.PortalsRename;
import keyp.forev.fmc.spigot.server.cmd.sub.portal.PortalsWand;
import keyp.forev.fmc.spigot.server.cmd.sub.teleport.TeleportBack;
import keyp.forev.fmc.spigot.server.cmd.sub.MenuExecutor;
import keyp.forev.fmc.spigot.util.config.PortalsConfig;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;

import org.bukkit.plugin.java.JavaPlugin;

public class FMCCommand implements TabExecutor {
	private final JavaPlugin plugin;
	private final PortalsConfig psConfig;
	private final List<String> subcommands = new ArrayList<>(Arrays.asList("reload","fv","mcvc","portal","hideplayer", "im", "image", "menu", "button", "check", "setpoint", "confirm", "back"));
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
						.append(ChatColor.AQUA+"\n\n/fmc back")
							.event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/fmc back"))
							.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("前回のテレポート先に戻る！(クリックしてコピー)")))
    			        .create();
    		sender.spigot().sendMessage(component);
    		return true;
    	}
    	if (!sender.hasPermission("fmc." + args[0])) {
    		sender.sendMessage(ChatColor.RED + "権限がありません。");
    		return true;
    	}
		switch (args[0].toLowerCase()) {
			case "back" -> Main.getInjector().getInstance(TeleportBack.class).onCommand(sender, cmd, label, args);
			case "confirm" -> Main.getInjector().getInstance(Confirm.class).execute(sender, cmd, label, args);
			case "fv" -> Main.getInjector().getInstance(CommandForward.class).execute(sender, cmd, label, args);
			case "reload" -> Main.getInjector().getInstance(ReloadConfig.class).execute(sender, cmd, label, args);
			case "mcvc" -> Main.getInjector().getInstance(MCVC.class).execute(sender, cmd, label, args);
			case "hideplayer" -> Main.getInjector().getInstance(HidePlayer.class).execute(sender, cmd, label, args);
			case "menu" -> Main.getInjector().getInstance(MenuExecutor.class).execute(sender, cmd, label, args);
			case "check" -> Main.getInjector().getInstance(Check.class).execute(sender, cmd, label, args);
			case "setpoint" -> Main.getInjector().getInstance(SetPoint.class).execute(sender, cmd, label, args);
			case "image","im" -> {
				if (args.length > 1) {
					if (!sender.hasPermission("fmc." + args[0] + "." + args[1])) {
						sender.sendMessage(ChatColor.RED + "権限がありません。");
						return true;
					}
					switch (args[1].toLowerCase()) {
						case "create" -> Main.getInjector().getInstance(ImageMap.class).executeImageMapLeading(sender, args);
						case "q" -> Main.getInjector().getInstance(ImageMap.class).executeQ(sender, args, false);
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
						case "wand" -> Main.getInjector().getInstance(PortalsWand.class).execute(sender, cmd, label, args);
						case "delete" -> Main.getInjector().getInstance(PortalsDelete.class).execute(sender, cmd, label, args);
						case "rename" -> Main.getInjector().getInstance(PortalsRename.class).execute(sender, cmd, label, args);
						case "nether" -> Main.getInjector().getInstance(PortalsNether.class).execute(sender, cmd, label, args);
					}
				} else {
					sender.sendMessage("Usage: /fmc portal <rename|delete|wand>");
					return true;
				}
			}
		}
		return true;
	}

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
						for (String args2 : MenuExecutor.args1) {
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
								for (String portalMenuServerCmd : MenuExecutor.args2) {
									ret.add(portalMenuServerCmd);
								}
								return StringUtil.copyPartialMatches(args[2].toLowerCase(), ret, new ArrayList<>());
							}
							case "tp" -> {
								for (String portalMenuTpCmd : MenuExecutor.args2tp) {
									ret.add(portalMenuTpCmd);
								}
								return StringUtil.copyPartialMatches(args[2].toLowerCase(), ret, new ArrayList<>());
							}
						}
					}
					case "portal" -> {
						switch (args[1].toLowerCase()) {
							case "delete","rename" -> {
								List<Map<?, ?>> portals = psConfig.getListMap("portals");
								if (portals != null) {
									for (Map<?, ?> portal : portals) {
										String portalName = (String) portal.get("name");
										if (portalName != null && sender.hasPermission("fmc.portal.delete." + portalName)) {
											ret.add(portalName);
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
					case "menu" -> {
						switch (args[1].toLowerCase()) {
							case "tp" -> {
								switch (args[2].toLowerCase()) {
									case "point" -> {
										for (String portalMenuTpCmd : MenuExecutor.args3tpsp) {
											ret.add(portalMenuTpCmd);
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