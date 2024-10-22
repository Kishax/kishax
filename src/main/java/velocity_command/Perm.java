package velocity_command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import velocity.Config;
import velocity.Luckperms;

public class Perm {

	private final Config config;
	private final Luckperms lp;
	public static List<String> args1 = new ArrayList<>(Arrays.asList("add","remove","list"));
	public static List<String> permS = null;
	public static List<String> permD = null;
	
	@Inject
	public Perm (Config config, Luckperms lp) {
		this.config = config;
		this.lp = lp;
	}
	
	public void execute(@NotNull CommandSource source, String[] args) {
		permS = config.getList("Permission.Short_Name");
		permD = config.getList("Permission.Detail_Name");
		
		if (!(permS.size() == permD.size())) {
			source.sendMessage(Component.text("コンフィグのDetail_NameとShort_Nameの要素の数を同じにしてください。").color(NamedTextColor.RED));
			return;
		}
		String permD1;
		switch (args.length) {
			case 0, 1 -> {
				source.sendMessage(Component.text("usage: /fmcp perm <add|remove|list> [Short:permission] <player>").color(NamedTextColor.GREEN));
			}
			case 2 -> {
				switch(args[1].toLowerCase()) {
					case "list"-> {
						TextComponent componentBuilder = Component.text()
							.append(Component.text("FMC Specific Permission List")
								.color(NamedTextColor.GOLD)
								.decorate(TextDecoration.BOLD, TextDecoration.UNDERLINED))
							.build();
						
						// アドミンリスト表示処理
						List<String> nonFoundPermList = new ArrayList<>();
						for (int i = 0; i < permD.size(); i++) {
							String permission = permD.get(i);
							String shortPermission = permS.get(i);
							List<String> playersPermList = lp.getPlayersWithPermission(permission);
							if (playersPermList.isEmpty()) {
								nonFoundPermList.add(shortPermission);
								continue;
							}
							for (String playerName : playersPermList) {
								TextComponent additionalComponent;
								additionalComponent = Component.text()
										.append(Component.text("\n"+playerName)
											.color(NamedTextColor.WHITE))
										.append(Component.text("  -"+shortPermission)
											.color(NamedTextColor.GOLD))
										.build();
								componentBuilder = componentBuilder.append(additionalComponent);
							}
						}
						if (!nonFoundPermList.isEmpty()) {
							for (String nonFoundPerm : nonFoundPermList) {
								TextComponent additionalComponent;
								additionalComponent = Component.text()
										.append(Component.text("\n"+nonFoundPerm)
											.color(NamedTextColor.GOLD))
										.append(Component.text("  -No player has this permission.")
											.color(NamedTextColor.RED))
										.build();
								componentBuilder = componentBuilder.append(additionalComponent);
							}
						}
						source.sendMessage(componentBuilder);
					}
					default-> {
						source.sendMessage(Component.text("usage: /fmcp perm <add|remove|list> [Short:permission] <player>").color(NamedTextColor.GREEN));
					}
				}
			}
			case 3 -> {
				// 以下はパーミッションが所持していることが確認されている上で、permというコマンドを使っているので、確認の必要なし
				//if(args[0].toLowerCase().equalsIgnoreCase("perm"))
				if (!(args1.contains(args[1].toLowerCase()))) {
					source.sendMessage(Component.text("第2引数が不正です。\n").color(NamedTextColor.RED).append(Component.text("usage: /fmcp perm <add|remove|list> [Short:permission] <player>").color(NamedTextColor.GREEN)));
					break;
				}
				
				if (!(permS.contains(args[2].toLowerCase()))) {
					source.sendMessage(Component.text("第3引数が不正です。\n").color(NamedTextColor.RED).append(Component.text("usage: /fmcp perm <add|remove|list> [Short:permission] <player>").color(NamedTextColor.GREEN)));
					break;
				}
				
				source.sendMessage(Component.text("対象のプレイヤー名を入力してください。\n").color(NamedTextColor.RED).append(Component.text("usage: /fmcp perm <add|remove|list> [Short:permission] <player>").color(NamedTextColor.GREEN)));
			}
			case 4 -> {
				if (!(args1.contains(args[1].toLowerCase()))) {
					source.sendMessage(Component.text("第2引数が不正です。\n").color(NamedTextColor.RED).append(Component.text("usage: /fmcp perm <add|remove|list> [Short:permission] <player>").color(NamedTextColor.GREEN)));
					break;
				}
				
				if (!(permS.contains(args[2].toLowerCase()))) {
					source.sendMessage(Component.text("第3引数が不正です。\n").color(NamedTextColor.RED).append(Component.text("usage: /fmcp perm <add|remove|list> [Short:permission] <player>").color(NamedTextColor.GREEN)));
					break;
				}
				
				// permSのindex値をもって、permDからDetail_Nameを取得(1:1対応)
				String detailPermissionName = args[2],
					playerName = args[3];
				
				permD1 = permD.get(permS.indexOf(detailPermissionName));
				switch (args[1].toLowerCase()) {
					case "add"-> {
						if (lp.hasPermission(playerName, permD1)) {
							source.sendMessage(Component.text(playerName+"はすでにpermission: "+permD1+"を持っているため、追加できません。").color(NamedTextColor.RED));
							break;
						}
						lp.addPermission(playerName, permD1);
						source.sendMessage(Component.text(playerName+"にpermission: "+permD1+"を追加しました。").color(NamedTextColor.GREEN));
						break;
					}
					case "remove"-> {
						if (!lp.hasPermission(playerName, permD1)) {
							source.sendMessage(Component.text(playerName+"はpermission: "+permD1+"を持っていないため、除去できません。").color(NamedTextColor.RED));
							break;
						}
						lp.removePermission(playerName, permD1);
						source.sendMessage(Component.text(playerName+"からpermission: "+permD1+"を除去しました。").color(NamedTextColor.GREEN));
						break;
					}
				}
			}
			default-> {
				source.sendMessage(Component.text("usage: /fmcp perm <add|remove|list> [Short:permission] <player>").color(NamedTextColor.GREEN));
			}
        }
	}
}