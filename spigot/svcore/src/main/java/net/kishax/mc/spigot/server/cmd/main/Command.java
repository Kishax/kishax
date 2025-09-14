package net.kishax.mc.spigot.server.cmd.main;

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

import net.kishax.mc.spigot.Main;
import net.kishax.mc.spigot.server.ImageMap;
import net.kishax.mc.spigot.server.cmd.sub.Check;
import net.kishax.mc.spigot.server.cmd.sub.CommandForward;
import net.kishax.mc.spigot.server.cmd.sub.Confirm;
import net.kishax.mc.spigot.server.cmd.sub.HidePlayer;
import net.kishax.mc.spigot.server.cmd.sub.MCVC;
import net.kishax.mc.spigot.server.cmd.sub.MenuExecutor;
import net.kishax.mc.spigot.server.cmd.sub.ReloadConfig;
import net.kishax.mc.spigot.server.cmd.sub.SetPoint;
import net.kishax.mc.spigot.server.cmd.sub.portal.PortalsDelete;
import net.kishax.mc.spigot.server.cmd.sub.portal.PortalsNether;
import net.kishax.mc.spigot.server.cmd.sub.portal.PortalsRename;
import net.kishax.mc.spigot.server.cmd.sub.portal.PortalsWand;
import net.kishax.mc.spigot.server.cmd.sub.teleport.TeleportBack;
import net.kishax.mc.spigot.util.config.PortalsConfig;
import net.md_5.bungee.api.ChatColor;

import org.bukkit.plugin.java.JavaPlugin;

public class Command implements TabExecutor {
  private final JavaPlugin plugin;
  private final PortalsConfig psConfig;
  private final List<String> subcommands = new ArrayList<>(Arrays.asList("reload", "fv", "mcvc", "portal", "hideplayer",
      "im", "image", "menu", "button", "check", "setpoint", "confirm", "back", "test"));

  @Inject
  public Command(JavaPlugin plugin, PortalsConfig psConfig) {
    this.plugin = plugin;
    this.psConfig = psConfig;
  }

  @Deprecated
  @Override
  public boolean onCommand(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
    if (sender == null) {
      return true;
    }
    if (args.length == 0 || !subcommands.contains(args[0].toLowerCase())) {
      return true;
    }
    if (!sender.hasPermission("kishax." + args[0])) {
      sender.sendMessage(ChatColor.RED + "権限がありません。");
      return true;
    }
    switch (args[0].toLowerCase()) {
      case "back" -> Main.getInjector().getInstance(TeleportBack.class).onCommand(sender, cmd, label, args);
      case "confirm" -> Main.getInjector().getInstance(Confirm.class).execute(sender, cmd, label, args);
      case "test" -> {
        if (args.length > 1 && args[1].equalsIgnoreCase("kishax") && args.length > 2
            && args[2].equalsIgnoreCase("confirm")) {
          handleTestKishaxConfirm(sender, cmd, label, args);
        } else {
          sender.sendMessage(ChatColor.RED + "Usage: /kishax test kishax confirm [player_name] [player_uuid]");
        }
      }
      case "fv" -> Main.getInjector().getInstance(CommandForward.class).execute(sender, cmd, label, args);
      case "reload" -> Main.getInjector().getInstance(ReloadConfig.class).execute(sender, cmd, label, args);
      case "mcvc" -> Main.getInjector().getInstance(MCVC.class).execute(sender, cmd, label, args);
      case "hideplayer" -> Main.getInjector().getInstance(HidePlayer.class).execute(sender, cmd, label, args);
      case "menu" -> Main.getInjector().getInstance(MenuExecutor.class).execute(sender, cmd, label, args);
      case "check" -> Main.getInjector().getInstance(Check.class).execute(sender, cmd, label, args);
      case "setpoint" -> Main.getInjector().getInstance(SetPoint.class).execute(sender, cmd, label, args);
      case "image", "im" -> {
        if (args.length > 1) {
          if (!sender.hasPermission("kishax." + args[0] + "." + args[1])) {
            sender.sendMessage(ChatColor.RED + "権限がありません。");
            return true;
          }
          switch (args[1].toLowerCase()) {
            case "create" -> Main.getInjector().getInstance(ImageMap.class).executeImageMapLeading(sender, args);
            case "q" -> Main.getInjector().getInstance(ImageMap.class).executeQ(sender, args, false);
          }
        } else {
          sender.sendMessage("Usage: /kishax im <create|createqr> <title> <comment> <url>");
          return true;
        }
      }
      case "portal" -> {
        if (args.length > 1) {
          if (!sender.hasPermission("kishax." + args[0] + "." + args[1])) {
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
          sender.sendMessage("Usage: /kishax portal <rename|delete|wand>");
          return true;
        }
      }
    }
    return true;
  }

  @Deprecated
  @Override
  public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
    List<String> ret = new ArrayList<>();
    switch (args.length) {
      case 1 -> {
        Collections.sort(subcommands);
        for (String subcmd : subcommands) {
          if (!sender.hasPermission("kishax." + subcmd))
            continue;
          ret.add(subcmd);
        }
        return StringUtil.copyPartialMatches(args[0].toLowerCase(), ret, new ArrayList<>());
      }
      case 2 -> {
        if (!sender.hasPermission("kishax." + args[0].toLowerCase()))
          return Collections.emptyList();
        switch (args[0].toLowerCase()) {
          case "setpoint" -> {
            List<String> types = new ArrayList<>(Arrays.asList("load", "room", "hub"));
            return StringUtil.copyPartialMatches(args[1].toLowerCase(), types, new ArrayList<>());
          }
          case "potion" -> {
            for (PotionEffectType potion : PotionEffectType.values()) {
              if (!sender.hasPermission("kishax.potion." + potion.getName().toLowerCase()))
                continue;
              ret.add(potion.getName());
            }
            return StringUtil.copyPartialMatches(args[1].toLowerCase(), ret, new ArrayList<>());
          }
          case "portal" -> {
            List<String> portalCmds = new ArrayList<>(Arrays.asList("nether", "wand", "delete", "rename"));
            for (String portalcmd : portalCmds) {
              if (!sender.hasPermission("kishax.portal." + portalcmd))
                continue;
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
          case "image", "im" -> {
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
          case "test" -> {
            List<String> testArgs = new ArrayList<>(Arrays.asList("kishax"));
            return StringUtil.copyPartialMatches(args[1].toLowerCase(), testArgs, new ArrayList<>());
          }
        }
      }
      case 3 -> {
        if (!sender.hasPermission("kishax." + args[0].toLowerCase()))
          return Collections.emptyList();
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
              case "image" -> {
                for (String imageArg : MenuExecutor.args2image) {
                  ret.add(imageArg);
                }
                return StringUtil.copyPartialMatches(args[2].toLowerCase(), ret, new ArrayList<>());
              }
            }
          }
          case "portal" -> {
            switch (args[1].toLowerCase()) {
              case "delete", "rename" -> {
                List<Map<?, ?>> portals = psConfig.getListMap("portals");
                if (portals != null) {
                  for (Map<?, ?> portal : portals) {
                    String portalName = (String) portal.get("name");
                    if (portalName != null && sender.hasPermission("kishax.portal.delete." + portalName)) {
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
          case "test" -> {
            if (args[1].equalsIgnoreCase("kishax")) {
              List<String> testKishaxArgs = new ArrayList<>(Arrays.asList("confirm"));
              return StringUtil.copyPartialMatches(args[2].toLowerCase(), testKishaxArgs, new ArrayList<>());
            }
          }
        }
      }
      case 4 -> {
        if (!sender.hasPermission("kishax." + args[0].toLowerCase()))
          return Collections.emptyList();
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

  /**
   * テスト用 kishax confirm コマンドを処理
   * コンソールから実行可能で、テストプレイヤー情報を使用して認証フローをテストします
   */
  private void handleTestKishaxConfirm(CommandSender sender, org.bukkit.command.Command cmd, String label,
      String[] args) {
    // コンソールからのみ実行可能
    if (sender instanceof Player) {
      sender.sendMessage(ChatColor.RED + "このコマンドはコンソールからのみ実行可能です。");
      return;
    }

    String testPlayerName;
    String testPlayerUuid;

    if (args.length >= 5) {
      // 引数で指定された場合
      testPlayerName = args[3];
      testPlayerUuid = args[4];
    } else {
      // デフォルトのテストプレイヤー情報を使用
      testPlayerName = "TestPlayer";
      testPlayerUuid = "00000000-0000-0000-0000-000000000000";
    }

    sender.sendMessage(ChatColor.GREEN + "テスト用認証フローを開始します:");
    sender.sendMessage(ChatColor.YELLOW + "プレイヤー名: " + testPlayerName);
    sender.sendMessage(ChatColor.YELLOW + "プレイヤーUUID: " + testPlayerUuid);

    try {
      // Confirmクラスのテスト用メソッドを呼び出し
      Confirm confirmHandler = Main.getInjector().getInstance(Confirm.class);
      confirmHandler.executeTestFlow(testPlayerName, testPlayerUuid);

      sender.sendMessage(ChatColor.GREEN + "✅ テスト認証フローが正常に実行されました。");
      sender.sendMessage("詳細はサーバーログを確認してください。");
      sender.sendMessage(ChatColor.YELLOW + "生成された認証URLを使用してWebページからの認証をテストできます。");

    } catch (Exception e) {
      sender.sendMessage(ChatColor.RED + "テスト実行中にエラーが発生しました: " + e.getMessage());
      plugin.getLogger().severe("Test kishax confirm command failed: " + e.getMessage());
    }
  }
}
