package keyp.forev.fmc.spigot.cmd.sub;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.slf4j.Logger;

import com.google.inject.Inject;

import keyp.forev.fmc.common.database.Database;
import keyp.forev.fmc.common.server.DefaultLuckperms;
import keyp.forev.fmc.common.settings.FMCSettings;
import keyp.forev.fmc.common.util.OTPGenerator;
import keyp.forev.fmc.spigot.server.ImageMap;
import keyp.forev.fmc.spigot.server.SpigotServerHomeDir;
import keyp.forev.fmc.spigot.server.textcomponent.TCUtils;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

import org.bukkit.plugin.java.JavaPlugin;

public class Confirm {
    public static final Set<Player> confirmMap = new HashSet<>();
    private final JavaPlugin plugin;
    private final Logger logger;
    private final Database db;
    private final DefaultLuckperms lp;
    private final ImageMap im;
    private final String thisServerName;
    @Inject
    public Confirm(JavaPlugin plugin, Logger logger, Database db, DefaultLuckperms lp, ImageMap im, SpigotServerHomeDir shd) {
        this.plugin = plugin;
        this.logger = logger;
        this.db = db;
        this.lp = lp;
        this.im = im;
        this.thisServerName = shd.getServerName();
    }

    public void execute(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
        if (sender instanceof Player player) {
            if (!Confirm.confirmMap.contains(player)) {
                String playerName = player.getName(),
                    playerUUID = player.getUniqueId().toString();
                int permLevel = lp.getPermLevel(playerName);
                if (permLevel < 1) {
                    try (Connection conn = db.getConnection()) {
                        int ifMapId = checkExistConfirmMap(conn, new Object[] {thisServerName, true, playerName});
                        Map<String, Object> memberMap = db.getMemberMap(conn, player.getName());
                        if (!memberMap.isEmpty()) {
                            if (memberMap.get("id") instanceof Integer id) {
                                String confirmUrl = FMCSettings.CONFIRM_URL.getValue() + "?n=" + id;
                                //player.sendMessage(ChatColor.GREEN + "WEB認証のQRコードを生成します。");
                                String[] imageArgs = {"image", "createqr", confirmUrl};
                                if (ifMapId == -1) {
                                    im.executeImageMapForConfirm(player, imageArgs);
                                } else {
                                    im.giveMapToPlayer(player, ifMapId);
                                }
                                int ranum = OTPGenerator.generateOTPbyInt();
                                int rsAffected3 = updateSecret2(conn, new Object[] {ranum, playerUUID});
                                if (rsAffected3 > 0) {
                                    sendConfirmationMessage(player, ranum, confirmUrl);
                                } else {
                                    player.sendMessage(ChatColor.RED + "エラーが発生しました。");
                                }
                            }
                        }
                    } catch (SQLException | ClassNotFoundException e2) {
                        player.sendMessage(ChatColor.RED + "WEB認証のQRコード生成に失敗しました。");
                        logger.error("A SQLException error occurred: " + e2.getMessage());
                        for (StackTraceElement element : e2.getStackTrace()) {
                            logger.error(element.toString());
                        }
                    }
                } else {
                    player.sendMessage(ChatColor.GREEN + "WEB認証済みプレイヤーが通過しました！");
                }
                Confirm.confirmMap.add(player);
            }
        } else {
            if (sender != null) {
                sender.sendMessage("プレイヤーからのみ実行可能です。");
            }
        }
    }

    private int updateSecret2(Connection conn, Object[] args) throws SQLException {
        String query = "UPDATE members SET secret2=? WHERE uuid=?;";
        PreparedStatement ps = conn.prepareStatement(query);
        for (int i = 0; i < args.length; i++) {
            ps.setObject(i + 1, args[i]);
        }
        return ps.executeUpdate();
    }

    private void sendConfirmationMessage(Player player, int ranum, String confirmUrl) {
        String ranumstr = Integer.toString(ranum);
        TextComponent webAuth = new TextComponent("WEB認証");
        webAuth.setColor(ChatColor.GOLD);
        webAuth.setBold(true);
        webAuth.setUnderlined(true);
        webAuth.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, confirmUrl));
        webAuth.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("クリックしてWEB認証ページを開く")));
        TextComponent forUser = new TextComponent("\n\n[サイトへのアクセス方法]\n");
        forUser.setColor(ChatColor.GOLD);
        forUser.setBold(true);
        forUser.setUnderlined(true);
        TextComponent javaUserAccess = new TextComponent("は、");
        javaUserAccess.setColor(ChatColor.GRAY);
        javaUserAccess.setItalic(true);
        TextComponent here = new TextComponent("ココ");
        here.setColor(ChatColor.GOLD);
        here.setUnderlined(true);
        here.setItalic(true);
        here.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, confirmUrl));
        here.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("クリックしてWEB認証ページを開く")));
        TextComponent javaUserAccess2 = new TextComponent("をクリックしてアクセスしてね！\n");
        javaUserAccess2.setColor(ChatColor.GRAY);
        javaUserAccess2.setItalic(true);
        TextComponent bedrockUserAccess = new TextComponent("は、配布されたQRコードを読み取ってアクセスしてね！");
        bedrockUserAccess.setColor(ChatColor.GRAY);
        bedrockUserAccess.setItalic(true);
        TextComponent authCode = new TextComponent("\n認証コードは ");
        authCode.setColor(ChatColor.WHITE);
        TextComponent code = new TextComponent(ranumstr);
        code.setColor(ChatColor.BLUE);
        code.setUnderlined(true);
        code.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, ranumstr));
        code.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("クリックしてコピー")));
        TextComponent endMessage = new TextComponent(" です。");
        endMessage.setColor(ChatColor.WHITE);
        TextComponent regenerate = new TextComponent("\n認証コードの再生成");
        regenerate.setColor(ChatColor.GOLD);
        regenerate.setBold(true);
        regenerate.setUnderlined(true);
        regenerate.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/fmcp retry"));
        regenerate.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("クリックして認証コードを再生成します。")));
        TextComponent bedrockUserGenerate = new TextComponent("は、 ");
        bedrockUserGenerate.setColor(ChatColor.GRAY);
        bedrockUserGenerate.setItalic(true);
        TextComponent bedrockUserGenerate2 = new TextComponent("/retry");
        bedrockUserGenerate2.setColor(ChatColor.GRAY);
        bedrockUserGenerate2.setItalic(true);
        bedrockUserGenerate2.setUnderlined(true);
        TextComponent bedrockUserGenerate3 = new TextComponent(" とコマンドを打ってね！");
        bedrockUserGenerate3.setColor(ChatColor.GRAY);
        bedrockUserGenerate3.setItalic(true);
        new BukkitRunnable() {
            int countdown = 50;
            int gCountdown = 3;
            @Override
            public void run() {
                TextComponent message = new TextComponent();
                switch (countdown) {
                    case 50 -> message.addExtra("こんにちは！");
                    case 47 -> message.addExtra("FMCサーバー代表のベラです。");
                    case 44 -> message.addExtra("サーバーに参加するには、");
                    case 41 -> message.addExtra("FMCアカウントとMinecraftアカウントをリンクさせる必要があるよ！");
                    case 39 -> {
                        message.addExtra(webAuth);
                        message.addExtra("より、手続きを進めてね！");
                    }
                    case 36 -> {
                        message.addExtra(forUser);
                    }
                    case 33 -> {
                        message.addExtra(TCUtils.JAVA_USER.get());
                        message.addExtra(javaUserAccess);
                        message.addExtra(here);
                        message.addExtra(javaUserAccess2);
                    }
                    case 30 -> {
                        message.addExtra(TCUtils.BEDROCK_USER.get());
                        message.addExtra(bedrockUserAccess);
                    }
                    case 27 -> {
                        message.addExtra("\n認証コードを生成するよ。");
                    }
                    case 24, 23, 22 -> {
                        message.addExtra(countGenerate(gCountdown));
                        gCountdown--;
                    }
                    case 21 -> {
                        message.addExtra(authCode);
                        message.addExtra(code);
                        message.addExtra(endMessage);
                    }
                    case 18 -> {
                        message.addExtra("認証コードを再生成する場合は、");
                    }
                    case 15 -> {
                        message.addExtra(regenerate);
                        message.addExtra(" をクリックしてね！\n");
                    }
                    case 13 -> {
                        message.addExtra(TCUtils.BEDROCK_USER.get());
                        message.addExtra(bedrockUserGenerate);
                        message.addExtra(bedrockUserGenerate2);
                        message.addExtra(bedrockUserGenerate3);
                    }
                    case 10 -> {
                        message.addExtra("\nそれでは、楽しいマイクラライフを！");
                    }
                    case 0 -> {
                        cancel();
                        return;
                    }
                }
                // ここで、メッセージが.addExtra()で追加されてたら、それをプレイヤーに送信する
                if (message.getExtra() != null && !message.getExtra().isEmpty()) {
                    player.spigot().sendMessage(message);
                }             
                countdown--;
            }
        }.runTaskTimer(plugin, 0, 20);
    }

    private TextComponent countGenerate(int countdown) {
        TextComponent untilGenerater = new TextComponent("生成中..." + countdown);
        untilGenerater.setColor(ChatColor.GRAY);
        untilGenerater.setItalic(true);
        return untilGenerater;
    }
    
    private int checkExistConfirmMap(Connection conn, Object[] args) throws SQLException {
        String query = "SELECT * FROM images WHERE server=? AND confirm=? AND name=?";
        PreparedStatement ps = conn.prepareStatement(query);
        for (int i = 0; i < args.length; i++) {
            ps.setObject(i + 1, args[i]);
        }
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("mapid");
            } else {
                return -1;
            }
        }
    }
}
