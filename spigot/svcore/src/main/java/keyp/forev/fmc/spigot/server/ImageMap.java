package keyp.forev.fmc.spigot.server;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.persistence.PersistentDataType;
import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import keyp.forev.fmc.common.server.interfaces.ServerHomeDir;
import keyp.forev.fmc.common.database.Database;
import keyp.forev.fmc.common.settings.FMCSettings;
import keyp.forev.fmc.common.socket.message.Message;
import keyp.forev.fmc.common.socket.SocketSwitch;
import keyp.forev.fmc.common.util.CalcUtil;
import keyp.forev.fmc.common.util.ExtUtil;
import keyp.forev.fmc.spigot.server.menu.Menu;
import keyp.forev.fmc.spigot.server.menu.Type;
import keyp.forev.fmc.spigot.server.menu.interfaces.MenuEventRunnable;
import keyp.forev.fmc.spigot.server.render.ImageMapRenderer;
import keyp.forev.fmc.spigot.server.textcomponent.TCUtils;
import keyp.forev.fmc.spigot.server.textcomponent.TCUtils2;
import keyp.forev.fmc.spigot.util.RunnableTaskUtil;
import keyp.forev.fmc.spigot.util.interfaces.MessageRunnable;
import net.coobird.thumbnailator.Thumbnails;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.plugin.java.JavaPlugin;

public class ImageMap {
    public static final String PERSISTANT_KEY = "custom_image";
    public static final String LARGE_PERSISTANT_KEY = "custom_large_image";
    public static List<String> imagesColumnsList = new ArrayList<>();
    public static List<Integer> thisServerMapIds = new ArrayList<>();
    public static List<String> args2 = new ArrayList<>(Arrays.asList("create", "q"));
    private final JavaPlugin plugin;
    private final BukkitAudiences audiences;
    private final Logger logger;
    private final Database db;
    private final RunnableTaskUtil rt;
    private final String serverName;
    private final Provider<SocketSwitch> sswProvider;
    
    @Inject
    public ImageMap(JavaPlugin plugin, BukkitAudiences audiences, Logger logger, Database db, ServerHomeDir shd, RunnableTaskUtil rt, Provider<SocketSwitch> sswProvider) {
        this.plugin = plugin;
        this.audiences = audiences;
        this.logger = logger;
        this.db = db;
        this.serverName = shd.getServerName();
        this.rt = rt;
        this.sswProvider = sswProvider;
    }

    public void leadAction(Connection conn, Player player, RunnableTaskUtil.Key key, String[] usingArgs, Object[] qArgs) throws SQLException, ClassNotFoundException {
        String playerName = player.getName();
        boolean fromQ = (qArgs != null);
        String url = usingArgs[0],
            title = usingArgs[1],
            comment = usingArgs[2];
        // ラージか1✕1かをプレイヤーに問う
        Component message = Component.empty();
        if (fromQ) {
            message = message.append(
                Component.text("操作が途中で中断された場合、メニュー->画像マップより再開できます。")
                    .appendNewline()
                    .color(NamedTextColor.GRAY)
            );
        }

        Component[] times = getPlayerTimesComponent(conn, playerName);

        TextComponent messages = Component.text()
            .append(Component.text("1✕1の画像マップを作成する場合は、"))
            .append(TCUtils.ZERO.get())
            .append(Component.text("と入力してください。"))
            .append(times[0])
            .appendNewline()
            .append(Component.text("1✕1のQRコードを作成する場合は、"))
            .append(TCUtils.ONE.get())
            .append(Component.text("と入力してください。"))
            .append(times[0])
            .appendNewline()
            .append(Component.text("ラージマップを作成する場合は、"))
            .append(TCUtils.TWO.get())
            .append(Component.text("と入力してください。"))
            .append(times[1])
            .appendNewline()
            .append(message)
            .append(TCUtils.INPUT_MODE.get())
            .build();
        
        audiences.player(player).sendMessage(messages);

        Map<RunnableTaskUtil.Key, MessageRunnable> playerActions = new HashMap<>();
        playerActions.put(key, (input) -> {
            audiences.player(player).sendMessage(TCUtils2.getResponseComponent(input));
            switch (input) {
                case "0" -> {
                    rt.removeCancelTaskRunnable(player, key);
                    String[] imageArgs = new String[] {"im", "create", url, title, comment};
                    executeImageMap(player, imageArgs, qArgs);
                }
                case "1" -> {
                    rt.removeCancelTaskRunnable(player, key);
                    String[] imageArgs = new String[] {"im", "createqr", url, title, comment};
                    executeImageMap(player, imageArgs, qArgs);
                }
                case "2" -> {
                    rt.removeCancelTaskRunnable(player, key);
                    String[] imageArgs = new String[] {"im", "largecreate", url, title, comment};
                    executeLargeImageMap(player, imageArgs, qArgs, null, null, null);
                }
                default -> {
                    Component errorMessage = Component.text("無効な入力です。")
                        .appendNewline()
                        .append(Component.text("0, 1, 2のいずれかを入力してください。"))
                        .color(NamedTextColor.RED);
                    audiences.player(player).sendMessage(errorMessage);
                    rt.extendTask(player, key);
                }
            }
        });
        rt.addTaskRunnable(player, playerActions, key);
    }

    public void executeQFromMenu(Player player, Object[] Args) {
        executeQ(player, null, false, Args);
    }

    public void executeQ(CommandSender sender, String[] args, boolean q) {
        executeQ(sender, args, q, null);
    }

    public void executeImageMapFromGivingMap(Player player, Object[] mArgs) {
        executeImageMapFromMenu(player, mArgs, true);
    }

    public void executeImageMapFromMenu(Player player, Object[] mArgs) {
        executeImageMapFromMenu(player, mArgs, false);
    }

    public void executeImageMapLeading(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            if (args.length < 3) {
                player.sendMessage("使用法: /fmc im <create> <url> [Optional: <title> <comment>]");
                return;
            }
            String url = args[2],
                title = (args.length > 3 && !args[3].isEmpty()) ? args[3]: "無名のタイトル",
                comment = (args.length > 4 && !args[4].isEmpty()) ? args[4]: "コメントなし";
            try (Connection conn = db.getConnection()) {
                leadAction(conn, player, RunnableTaskUtil.Key.IMAGEMAP_CREATE_IMAGE_MAP_FROM_CMD, new String[] {url, title, comment}, null);
            } catch (SQLException | ClassNotFoundException e) {
                player.sendMessage("データベースとの通信に問題が発生しました。");
                logger.error("An SQLException | ClassNotFoundException error occurred: {}", e.getMessage());
                for (StackTraceElement element : e.getStackTrace()) {
                    logger.error(element.toString());
                }
            }
        } else {
            if (sender != null) {
                sender.sendMessage("このコマンドはプレイヤーのみが実行できます。");
            }
        }
    }

    public void executeImageMapForConfirm(CommandSender sender, String[] args) {
        executeImageMap(sender, args, null, true);
    }

    public Map<Integer, Map<String, Object>> getImageMap(Connection conn) throws SQLException, ClassNotFoundException {
        Map<Integer, Map<String, Object>> imageInfo = new HashMap<>();
        String query = "SELECT * FROM images WHERE menu!=? AND confirm!=?;";
        PreparedStatement ps = conn.prepareStatement(query);
        ps.setBoolean(1, true);
        ps.setBoolean(2, true);
        ResultSet rs = ps.executeQuery();
        int index = 0;
        while (rs.next()) {
            Map<String, Object> rowMap = new HashMap<>();
            int columnCount = rs.getMetaData().getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = rs.getMetaData().getColumnName(i);
                rowMap.put(columnName, rs.getObject(columnName));
            }
            imageInfo.computeIfAbsent(index, _p -> rowMap);
            index++;
        }
        return imageInfo;
    }

    public Map<Integer, Map<String, Object>> getThisServerImages(Connection conn) throws SQLException, ClassNotFoundException {
        Map<Integer, Map<String, Object>> serverImageInfo = new HashMap<>();
        String query = "SELECT * FROM images WHERE server=?;";
        PreparedStatement ps = conn.prepareStatement(query);
        ps.setString(1, serverName);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            Map<String, Object> rowMap = new HashMap<>();
            int mapId = rs.getInt("mapid");
            int columnCount = rs.getMetaData().getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = rs.getMetaData().getColumnName(i);
                if (!columnName.equals("mapid")) {
                    rowMap.put(columnName, rs.getObject(columnName));
                }
            }
            serverImageInfo.computeIfAbsent(mapId, _p -> rowMap);
        }
        return serverImageInfo;
    }

    public void giveMapToPlayer(Player player, int mapId) {
        AtomicBoolean found = new AtomicBoolean(false);
        for (World world : Bukkit.getWorlds()) {
            for (ItemFrame itemFrame : world.getEntitiesByClass(ItemFrame.class)) {
                ItemStack item = itemFrame.getItem();
                if (item.getType() == Material.FILLED_MAP) {
                    MapMeta mapMeta = (MapMeta) item.getItemMeta();
                    if (mapMeta != null && mapMeta.hasMapView()) {
                        MapView mapView = mapMeta.getMapView();
                        if (mapView != null && mapView.getId() == mapId) {
                            found.set(true);
                            ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
                            mapMeta.setMapView(mapView);
                            mapItem.setItemMeta(mapMeta);
                            player.getInventory().addItem(mapItem);
                            player.sendMessage("画像マップを渡しました。");
                            return;
                        }
                    }
                }
            }
        }
        if (!found.get()) {
            // このサーバーに過去あった、もしくは現在あることは確定
            // ロードして、プレイヤーに上げて、もとのデータベースのmapIDを更新すればいい
            // このサーバーとmapIdで一致するデータを取得
            // そのデータを使って、マップを生成
            try (Connection conn = db.getConnection()) {
                Map<String, Object> mapInfo = getMapInfoForThisServerByMapId(conn, mapId);
                if (!mapInfo.isEmpty()) {
                    int id = (Integer) mapInfo.get("id");
                    boolean isQr = (boolean) mapInfo.get("isqr");
                    String authorName = (String) mapInfo.get("name"),
                        imageUUID = (String) mapInfo.get("imuuid"),
                        title = (String) mapInfo.get("title"),
                        comment = (String) mapInfo.get("comment"),
                        ext = (String) mapInfo.get("ext"),
                        date = ((Date) mapInfo.get("date")).toString();
                    Object[] mArgs = new Object[] {id, isQr, authorName, imageUUID, title, comment, ext, date, mapId};
                    executeImageMapFromGivingMap(player, mArgs);
                }
            } catch (SQLException | ClassNotFoundException e) {
                player.sendMessage("画像の読み取りに失敗しました。");
                logger.error("An SQLException | ClassNotFoundException error occurred: {}", e.getMessage());
                for (StackTraceElement element : e.getStackTrace()) {
                    logger.error(element.toString());
                }
            }
        }
    }

    public void loadAndSetImageTile(Connection conn, int mapId, ItemStack item, MapMeta mapMeta, MapView mapView) throws SQLException, ClassNotFoundException {
        //logger.info("Replacing tile image to the map(No.{})...", mapId);
        try {
            BufferedImage tileImage = loadTileImage(conn, mapId);
            if (tileImage != null) {
                mapView.getRenderers().clear();
                mapView.addRenderer(new ImageMapRenderer(tileImage));
                item.setItemMeta(mapMeta);
                ImageMap.thisServerMapIds.add(mapId);
            } else {
                throw new IOException("Failed to load tile image.");
            }
        } catch (IOException e) {
            logger.error("Failed to load tile image: {}", e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                logger.error(element.toString());
            }
        }
    }

    public void loadAndSetImage(Connection conn, int mapId, ItemStack item, MapMeta mapMeta, MapView mapView) throws SQLException, ClassNotFoundException {
        //logger.info("Replacing image to the map(No.{})...", mapId);
        try {
            BufferedImage image = loadImage(conn, mapId);
            if (image != null) {
                image = resizeImage(image, 128, 128);
                mapView.getRenderers().clear();
                mapView.addRenderer(new ImageMapRenderer(image));
                item.setItemMeta(mapMeta);
                ImageMap.thisServerMapIds.add(mapId);
            } else {
                throw new IOException("Failed to load image.");
            }
        } catch (IOException e) {
            logger.error("マップId {} の画像の読み込みに失敗しました: {}", mapId);
            logger.error("An IOException error occurred: {}", e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                logger.error(element.toString());
            }
        }
    }

    @Deprecated
    @SuppressWarnings("null")
	private void executeLargeImageMap(CommandSender sender, String[] args, Object[] dArgs, Object[] inputs, Object[] inputs2, Object[] inputs3) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (rt.checkIsOtherInputMode(player, RunnableTaskUtil.Key.IMAGEMAP_CREATE_LARGE_IMAGE)) {
                Component errorMessage = Component.text("他でインプットモード中です。")
                    .color(NamedTextColor.RED);
                audiences.player(player).sendMessage(errorMessage);
                return;
            }
            if (args.length < 3) {
                player.sendMessage("使用法: /fmc im <largecreate> <url> [Optional: <title> <comment>]");
                return;
            }
            boolean fromDiscord = (dArgs != null);
            String playerName = player.getName(),
                playerUUID = player.getUniqueId().toString(),
                imageUUID = UUID.randomUUID().toString(),
                url = args[2],
                title = (args.length > 3 && !args[3].isEmpty()) ? args[3]: "無名のタイトル",
                comment = (args.length > 4 && !args[4].isEmpty()) ? args[4]: "コメントなし",
                ext;
            final int maxTiles = FMCSettings.MAX_IMAGE_TILES.getIntValue();
            try (Connection conn = db.getConnection()) {
                // 一日のアップロード回数は制限する
                // ラージマップは要考
                int limitUploadTimes = FMCSettings.LARGE_IMAGE_LIMIT_TIMES.getIntValue(),
                    playerUploadTimes = getPlayerTodayCreateLargeImageTimes(conn, playerName),
                    thisTimes = playerUploadTimes + 1;
                if (thisTimes > limitUploadTimes) {
                    Component errorMessage = Component.text("1日の登録回数は"+limitUploadTimes+"回までです。")
                        .color(NamedTextColor.RED);
                    audiences.player(player).sendMessage(errorMessage);
                    return;
                }
                String now;
                final BufferedImage image;
                if (inputs == null) {
                    if (rt.checkIsInputMode(player, RunnableTaskUtil.Key.IMAGEMAP_CREATE_LARGE_IMAGE)) {
                        Component errorMessage = Component.text("インプットモード中に他のラージマップを作成することはできません。")
                            .color(NamedTextColor.RED);
                        audiences.player(player).sendMessage(errorMessage);
                        return;
                    }
                    if (!isValidURL(url)) {
                        player.sendMessage("無効なURLです。");
                        return;
                    }

                    URL getUrl = new URI(url).toURL();

                    ext = ExtUtil.getExtension(getUrl);
                    if (ext == null) {
                        Component errorMessage = Component.text("指定のURLは規定の拡張子を持ちません。")
                        .color(NamedTextColor.RED)
                        .decorate(TextDecoration.BOLD);
                        audiences.player(player).sendMessage(errorMessage);
                        return;
                    }
                    
                    LocalDate localDate = LocalDate.now();
                    now = fromDiscord ? (String) dArgs[1] : localDate.toString();
                    image =  ImageIO.read(getUrl);
                    if (image == null) {
                        Component errorMessage = Component.text("URLより画像を取得できませんでした。")
                            .color(NamedTextColor.RED)
                            .decorate(TextDecoration.BOLD);

                        audiences.player(player).sendMessage(errorMessage);
                        return;
                    }
                } else {
                    now = (String) inputs[0];
                    ext = (String) inputs[1];
                    image = (BufferedImage) inputs[2];
                }
                // x, y, now, ext, fullPath, null
                if (inputs2 == null) {
                    // 縦か横のサイズを入力させるフェーズに入る
                    // その前に、適切な縦横のタイル数(x, y)を計算する
                    // それを提示し、プレイヤーに入力させる
                    int imageWidth = image.getWidth();
                    int imageHeight = image.getHeight();
                    int x = (int) Math.ceil((double) imageWidth / 128);
                    int y = (int) Math.ceil((double) imageHeight / 128);
                    int gcd = CalcUtil.gcd(x, y);
                    x /= gcd;
                    y /= gcd;
                    //int mapsX = (image.getWidth() + 127) / 128;
                    //int mapsY = (image.getHeight() + 127) / 128;
                    
                    Component ratio = Component.text(x + ":" + y)
                        .color(NamedTextColor.GOLD)
                        .decorate(
                            TextDecoration.BOLD,
                            TextDecoration.UNDERLINED);
                            
                    Component message = Component.text("現在、適切な縦横比は、")
                        .append(ratio)
                        .append(Component.text("です。"))
                        .appendNewline()
                        .append(Component.text("元の画像のアスペクト比を維持するため、縦か横のもう一方は自動的に決定されます。"))
                        .append(Component.text("縦か横のサイズを整数値で指定してください。"))
                        .appendNewline()
                        .append(Component.text("(例) x=5 or y=4"))
                        .appendNewline()
                        .append(TCUtils.INPUT_MODE.get());

                    audiences.player(player).sendMessage(message);

                    final Object[] inputs_1 = new Object[] {now, ext, image};
                    Map<RunnableTaskUtil.Key, MessageRunnable> playerActions = new HashMap<>();
                    playerActions.put(RunnableTaskUtil.Key.IMAGEMAP_CREATE_LARGE_IMAGE, (input) -> {
                        // x=? or y=?
                        if (input.contains("=")) {
                            String[] xy = input.split("=");
                            // 空白を削除
                            xy[0] = xy[0].trim();
                            xy[1] = xy[1].trim();
                            if (!xy[0].equals("x") && !xy[0].equals("y")) {
                                Component errorMessage = Component.text("無効な入力です。")
                                    .appendNewline()
                                    .append(Component.text("(例) x=5 or y=4のように入力してください。"))
                                    .color(NamedTextColor.RED);
                                audiences.player(player).sendMessage(errorMessage);
                                return;
                            }
                            try {
                                int xOry = Integer.parseInt(xy[1].trim());
                                if (xOry == 1) {
                                    Component errorMessage = Component.text("ラージマップでないため、x=1, y=1を選択することはできません。")
                                        .appendNewline()
                                        .append(Component.text("(例) x=5 or y=4のように入力してください。"))
                                        .color(NamedTextColor.RED);
                                    audiences.player(player).sendMessage(errorMessage);
                                    rt.extendTask(player, RunnableTaskUtil.Key.IMAGEMAP_CREATE_LARGE_IMAGE);
                                    return;
                                } else if (xOry < 1) {
                                    Component errorMessage = Component.text("無効な入力です。")
                                        .appendNewline()
                                        .append(Component.text("(例) x=5 or y=4のように入力してください。"))
                                        .color(NamedTextColor.RED);
                                    audiences.player(player).sendMessage(errorMessage);
                                    rt.extendTask(player, RunnableTaskUtil.Key.IMAGEMAP_CREATE_LARGE_IMAGE);
                                    return;
                                }
                                audiences.player(player).sendMessage(TCUtils2.getResponseComponent(input));
                                Object[] inputs_2 = new Object[] {xy[0], xOry};
                                executeLargeImageMap(sender, args, dArgs, inputs_1, inputs_2, null);
                            } catch (NumberFormatException e) {
                                Component errorMessage = Component.text("無効な入力です。")
                                    .appendNewline()
                                    .append(Component.text("(例) x=5 or y=4のように入力してください。"))
                                    .color(NamedTextColor.RED);
                                audiences.player(player).sendMessage(errorMessage);
                                rt.extendTask(player, RunnableTaskUtil.Key.IMAGEMAP_CREATE_LARGE_IMAGE);
                            }
                        } else {
                            Component errorMessage = Component.text("無効な入力です。")
                                .appendNewline()
                                .append(Component.text("(例) x=5 or y=4のように入力してください。"))
                                .color(NamedTextColor.RED);
                            audiences.player(player).sendMessage(errorMessage);
                            rt.extendTask(player, RunnableTaskUtil.Key.IMAGEMAP_CREATE_LARGE_IMAGE);
                        }
                    });
                    rt.addTaskRunnable(player, playerActions, RunnableTaskUtil.Key.IMAGEMAP_CREATE_LARGE_IMAGE);
                } else {
                    if (inputs3 == null) {
                        //saveImageToFileSystem(image, imageUUID, ext); // リサイズ前の画像を保存
                        // x=? or y=?
                        String xOry = (String) inputs2[0];
                        int xOryValue = (Integer) inputs2[1];
                        int x, y;
                        if (xOry.equals("x")) {
                            x = xOryValue;
                            y = (int) Math.ceil((double) image.getHeight() / (image.getWidth() / x));
                        } else {
                            y = xOryValue;
                            x = (int) Math.ceil((double) image.getWidth() / (image.getHeight() / y));
                        }
                        // 総タイル数がmaxTilesを超過しているかを確認する
                        if (x * y > maxTiles) {
                            Component errorMessage = Component.text("総タイル数("+maxTiles+")を超過しています。")
                                .appendNewline()
                                .append(Component.text("もう一度、入力してください。"))
                                .color(NamedTextColor.RED);
                            audiences.player(player).sendMessage(errorMessage);
                            rt.extendTask(player, RunnableTaskUtil.Key.IMAGEMAP_CREATE_LARGE_IMAGE);
                            return;
                        }
                        rt.removeCancelTaskRunnable(player, RunnableTaskUtil.Key.IMAGEMAP_CREATE_LARGE_IMAGE);
                        Object[] inputs_3 = new Object[] {x, y, null};
                        // ここでプレイヤーに色を決めさせる
                        player.sendMessage("次に、背景色を選択します。");
                        Component note = Component.text("jpegかjpgの画像の場合、背景色に透明を選ぶことはできません。")
                            .appendNewline()
                            .color(NamedTextColor.GRAY)
                            .decorate(
                                TextDecoration.UNDERLINED,
                                TextDecoration.ITALIC);

                        audiences.player(player).sendMessage(note.append(TCUtils.INPUT_MODE.get()));

                        Map<Integer, MenuEventRunnable> playerMenuActions = new HashMap<>();
                        Inventory inv = Bukkit.createInventory(null, 27, Type.CHOOSE_COLOR.get());
                        Map<ItemStack, java.awt.Color> colorItems = ColorItems.getColorItems();
                        //logger.info("colorItems: {}", colorItems);
                        int index = 0;
                        for (Map.Entry<ItemStack, java.awt.Color> entry : colorItems.entrySet()) {
                            ItemStack item = entry.getKey();
                            java.awt.Color color = entry.getValue();
                            inv.setItem(index, item);
                            playerMenuActions.put(index, (clickType) -> {
                                player.closeInventory();
                                inputs_3[2] = color;
                                executeLargeImageMap(sender, args, dArgs, inputs, inputs2, inputs_3);
                            });
                            index++;
                        }
                        ItemStack custom = new ItemStack(Material.WHITE_GLAZED_TERRACOTTA);
                        ItemMeta customMeta = custom.getItemMeta();
                        customMeta.setDisplayName("カスタム");
                        custom.setItemMeta(customMeta);
                        inv.setItem(index, custom);
                        index++;
                        playerMenuActions.put(index, (clickType) -> {
                            rt.removeCancelTaskRunnable(player, RunnableTaskUtil.Key.IMAGEMAP_CREATE_LARGE_IMAGE);
                            player.closeInventory();

                            Component link1 = Component.text("ココ")
                                .color(NamedTextColor.GOLD)
                                .decorate(
                                    TextDecoration.BOLD,
                                    TextDecoration.UNDERLINED)
                                .clickEvent(ClickEvent.openUrl("https://note.cman.jp/color/base_color.cgi"));

                            Component link2 = Component.text("ココ")
                                .color(NamedTextColor.GOLD)
                                .decorate(
                                    TextDecoration.BOLD,
                                    TextDecoration.UNDERLINED)
                                .clickEvent(ClickEvent.openUrl("https://www.cc.kyoto-su.ac.jp/~shimizu/MAKE_HTML/rgb2.html"));

                            TextComponent messages = Component.text()
                                .append(Component.text("カスタム色を選択してください。"))
                                .appendNewline()
                                .append(Component.text("色のRGB値を入力してください。"))
                                .appendNewline()
                                .append(Component.text("(例) 255, 255, 255"))
                                .appendNewline()
                                .append(Component.text("それぞれ、0~255の範囲で入力してください。"))
                                .appendNewline()
                                .append(Component.text("外部サイトのRGBカラーリストを見るには、"))
                                .append(link1)
                                .append(Component.text("をクリックしてください。"))
                                .appendNewline()
                                .append(Component.text("外部サイトのRGBカラーシミュレーターツールを使うには、"))
                                .append(link2)
                                .append(Component.text("をクリックしてください。"))
                                .appendNewline()
                                .append(Component.text("色選択メニューに戻る場合は、0と入力してください。"))
                                .appendNewline()
                                .append(TCUtils.INPUT_MODE.get())
                                .build();
                            audiences.player(player).sendMessage(messages);

                            Map<RunnableTaskUtil.Key, MessageRunnable> playerActions = new HashMap<>();
                            playerActions.put(RunnableTaskUtil.Key.IMAGEMAP_CREATE_LARGE_IMAGE, (input) -> {
                                if (input.equals("0")) {
                                    TextComponent message = Component.text()
                                        .append(TCUtils2.getResponseComponent(input))
                                        .appendNewline()
                                        .append(Component.text("色選択メニューに戻ります。"))
                                        .append(TCUtils.LATER_OPEN_INV_5.get())
                                        .build();
                                    audiences.player(player).sendMessage(message);
                                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                                        player.openInventory(inv);
                                    }, 100L);
                                    return;
                                }
                                String[] rgb = input.split(",");
                                if (rgb.length != 3) {
                                    Component errorMessage = Component.text("無効な入力です。")
                                        .appendNewline()
                                        .append(Component.text("(例) 255, 255, 255のように入力してください。"))
                                        .color(NamedTextColor.RED);
                                    audiences.player(player).sendMessage(errorMessage);
                                    rt.extendTask(player, RunnableTaskUtil.Key.IMAGEMAP_CREATE_LARGE_IMAGE);
                                    return;
                                }
                                try {
                                    int r = Integer.parseInt(rgb[0].trim());
                                    int g = Integer.parseInt(rgb[1].trim());
                                    int b = Integer.parseInt(rgb[2].trim());
                                    if (r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255) {
                                        Component errorMessage = Component.text("無効な入力です。")
                                            .appendNewline()
                                            .append(Component.text("(例) 255, 255, 255のように入力してください。"))
                                            .color(NamedTextColor.RED);
                                        audiences.player(player).sendMessage(errorMessage);
                                        rt.extendTask(player, RunnableTaskUtil.Key.IMAGEMAP_CREATE_LARGE_IMAGE);
                                        return;
                                    }
                                    java.awt.Color customColor = new java.awt.Color(r, g, b);
                                    inputs_3[2] = customColor;
                                    audiences.player(player).sendMessage(TCUtils2.getResponseComponent("(R, G, B) = (" + r + ", " + g + ", " + b + ")"));
                                    executeLargeImageMap(sender, args, dArgs, inputs, inputs2, inputs_3);
                                } catch (NumberFormatException e) {
                                    Component errorMessage = Component.text("無効な入力です。")
                                        .appendNewline()
                                        .append(Component.text("(例) 255, 255, 255のように入力してください。"))
                                        .color(NamedTextColor.RED);
                                    audiences.player(player).sendMessage(errorMessage);
                                    rt.extendTask(player, RunnableTaskUtil.Key.IMAGEMAP_CREATE_LARGE_IMAGE);
                                }
                            });
                            rt.addTaskRunnable(player, playerActions, RunnableTaskUtil.Key.IMAGEMAP_CREATE_LARGE_IMAGE);
                        });
                        Menu.menuEventActions.computeIfAbsent(player, _p -> new HashMap<>()).put(Type.CHOOSE_COLOR, playerMenuActions);
                        audiences.player(player).sendMessage(TCUtils.LATER_OPEN_INV_5.get());
                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                            player.openInventory(inv);

                            TextComponent messages = Component.text()
                                .append(Component.text("手動で背景色選択メニューを開くには、"))
                                .append(TCUtils.ONE.get())
                                .append(Component.text("と入力してください。"))
                                .appendNewline()
                                .append(Component.text("生成を中止する場合は、"))
                                .append(TCUtils.TWO.get())
                                .append(Component.text("と入力してください。"))
                                .appendNewline()
                                .append(TCUtils.INPUT_MODE.get())
                                .build();
                                
                            audiences.player(player).sendMessage(messages);

                            Map<RunnableTaskUtil.Key, MessageRunnable> playerActions = new HashMap<>();
                            playerActions.put(RunnableTaskUtil.Key.IMAGEMAP_CREATE_LARGE_IMAGE, (input) -> {
                                audiences.player(player).sendMessage(TCUtils2.getResponseComponent(input));
                                switch (input) {
                                    case "1" -> {
                                        TextComponent messages2 = Component.text()
                                            .appendNewline()
                                            .append(TCUtils.LATER_OPEN_INV_3.get())
                                            .build();
                                        audiences.player(player).sendMessage(messages2);

                                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                                            player.openInventory(inv);
                                        }, 60L);
                                    }
                                    case "2" -> {
                                        Component cancelMessage = Component.text("生成を中止しました。")
                                            .color(NamedTextColor.RED);
                                        audiences.player(player).sendMessage(cancelMessage);
                                        rt.removeCancelTaskRunnable(player, RunnableTaskUtil.Key.IMAGEMAP_CREATE_LARGE_IMAGE);
                                    }
                                    default -> {
                                        Component alertMessage = Component.text("無効な入力です。")
                                            .appendNewline()
                                            .color(NamedTextColor.RED);

                                        TextComponent errorMessages = Component.text()
                                            .append(alertMessage)
                                            .append(Component.text("手動で背景色選択メニューを開くには、"))
                                            .append(TCUtils.ONE.get())
                                            .append(Component.text("と入力してください。"))
                                            .appendNewline()
                                            .append(Component.text("生成を中止する場合は、"))
                                            .append(TCUtils.TWO.get())
                                            .append(Component.text("と入力してください。"))
                                            .appendNewline()
                                            .append(TCUtils.INPUT_MODE.get())
                                            .build();
                                        audiences.player(player).sendMessage(errorMessages);

                                        rt.extendTask(player, RunnableTaskUtil.Key.IMAGEMAP_CREATE_LARGE_IMAGE);
                                    }
                                }
                            });
                            rt.addTaskRunnable(player, playerActions, RunnableTaskUtil.Key.IMAGEMAP_CREATE_LARGE_IMAGE);
                        }, 100L);
                    } else {
                        java.awt.Color color = (java.awt.Color) inputs3[2];
                        String colorName = ColorItems.getColorName(color);
                        if (ext.equals("jpg") || ext.equals("jpeg")) {
                            if (ColorItems.isTransparent(color)) {
                                Component alertMessage = Component.text("jpg, jpeg形式の画像は透明度を持たないため、透明色を選択することはできません。")
                                    .appendNewline()
                                    .append(Component.text("背景色を選択し直してください。"))
                                    .appendNewline()
                                    .color(NamedTextColor.RED);

                                TextComponent errorMessages = Component.text()
                                    .append(alertMessage)
                                    .append(Component.text("手動で背景色選択メニューを開くには、"))
                                    .append(TCUtils.ONE.get())
                                    .append(Component.text("と入力してください。"))
                                    .appendNewline()
                                    .append(Component.text("生成を中止する場合は、"))
                                    .append(TCUtils.TWO.get())
                                    .append(Component.text("と入力してください。"))
                                    .appendNewline()
                                    .append(TCUtils.INPUT_MODE.get())
                                    .build();
                                audiences.player(player).sendMessage(errorMessages);

                                rt.extendTask(player, RunnableTaskUtil.Key.IMAGEMAP_CREATE_LARGE_IMAGE);
                                return;
                            }
                        }
                        rt.removeCancelTaskRunnable(player, RunnableTaskUtil.Key.IMAGEMAP_CREATE_LARGE_IMAGE);
                        // ここで一度確認を挟む
                        int x = (int) inputs3[0];
                        int y = (int) inputs3[1];

                        TextComponent messages = Component.text()
                            .append(Component.text("以下の内容で画像マップを生成します。"))
                            .appendNewline()
                            .append(Component.text("タイトル: " + title))
                            .appendNewline()
                            .append(Component.text("コメント: " + comment))
                            .appendNewline()
                            .append(Component.text("サイズ: " + x + "x" + y + "(" + x * y + ")"))
                            .appendNewline()
                            .append(Component.text("背景色: " + colorName))
                            .appendNewline()
                            .append(Component.text("生成する場合は、"))
                            .append(TCUtils.ONE.get())
                            .append(Component.text("と入力してください。"))
                            .appendNewline()
                            .append(Component.text("生成を中止する場合は、"))
                            .append(TCUtils.TWO.get())
                            .append(Component.text("と入力してください。"))
                            .appendNewline()
                            .append(TCUtils.INPUT_MODE.get())
                            .build();
                        
                        audiences.player(player).sendMessage(messages);

                        Map<RunnableTaskUtil.Key, MessageRunnable> playerActions = new HashMap<>();
                        playerActions.put(RunnableTaskUtil.Key.IMAGEMAP_CREATE_LARGE_IMAGE, (input) -> {
                            audiences.player(player).sendMessage(TCUtils2.getResponseComponent(input));
                            switch (input) {
                                case "1" -> {
                                    try (Connection conn2 = db.getConnection()) {
                                        // x, y に応じて、画像をリサイズする
                                        int canvasWidth = x * 128;
                                        int canvasHeight = y * 128;
                                        BufferedImage resizedImage = resizeImage(image, x * 128, y * 128);
                                        saveImageToFileSystem(resizedImage, imageUUID, ext);
                                        BufferedImage canvas = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_ARGB);
                                        Graphics2D g2d = canvas.createGraphics();
                                        g2d.setColor(color);
                                        g2d.fillRect(0, 0, canvasWidth, canvasHeight);
                                        int xOffset = (canvasWidth - resizedImage.getWidth()) / 2;
                                        int yOffset = (canvasHeight - resizedImage.getHeight()) / 2;
                                        g2d.drawImage(resizedImage, xOffset, yOffset, null);
                                        g2d.dispose();
                                        List<ItemStack> mapItems = new ArrayList<>();
                                        for (int y_ = 0; y_ < y; y_++) {
                                            for (int x_ = 0; x_ < x; x_++) {
                                                int tileX = x_ * 128;
                                                int tileY = y_ * 128;
                                                String in = "(" + x_ + ", " + y_ + ")";
                                                BufferedImage tile = canvas.getSubimage(tileX, tileY, 128, 128);
                                                MapView mapView = Bukkit.createMap(player.getWorld());
                                                mapView.getRenderers().clear();
                                                mapView.addRenderer(new ImageMapRenderer(tile));
                                                ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
                                                MapMeta mapMeta = (MapMeta) mapItem.getItemMeta();
                                                List<String> lores = new ArrayList<>();
                                                lores.add("<ラージイメージマップ>");
                                                List<String> commentLines = Arrays.stream(comment.split("\n"))
                                                    .map(String::trim)
                                                    .collect(Collectors.toList());
                                                lores.addAll(commentLines);
                                                lores.add("created by " + playerName);
                                                lores.add("created at " + now.replace("-", "/"));
                                                lores.add("size " + x + "x" + y + "(" + x * y + ")");
                                                lores.add("in " + in);
                                                int mapId = mapView.getId();
                                                if (mapMeta != null) {
                                                    mapMeta.setDisplayName(title + " " + in);
                                                    mapMeta.setLore(lores);
                                                    mapMeta.setMapView(mapView);
                                                    mapMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, ImageMap.LARGE_PERSISTANT_KEY), PersistentDataType.STRING, "true");
                                                    mapItem.setItemMeta(mapMeta);
                                                }
                                                mapItems.add(mapItem);
                                                saveImageToDatabase(conn2, mapId, x_, y_, tile, ext);
                                            }
                                        }
                                        if (fromDiscord) {
                                            db.updateLog(conn2, "UPDATE images SET name=?, uuid=?, server=?, mapid=?, title=?, imuuid=?, ext=?, url=?, comment=?, isqr=?, otp=?, date=?, large=?, locked_action=?  WHERE otp=?;", new Object[] {playerName, playerUUID, serverName, -1, title, imageUUID, ext, url, comment, false, null, now, true, true, (String) dArgs[0]});
                                        } else {
                                            db.insertLog(conn2, "INSERT INTO images (name, uuid, server, mapid, title, imuuid, ext, url, comment, isqr, confirm, date, large) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);", new Object[] {playerName, playerUUID, serverName, -1, title, imageUUID, ext, url, comment, false, false, Date.valueOf(LocalDate.now()), true});
                                        }
                                        Location playerLocation = player.getLocation();
                                        World world = player.getWorld();
                                        Block block = playerLocation.getBlock();
                                        List<ItemStack> remainingItems = new ArrayList<>();
                                        for (ItemStack mapItem : mapItems) {
                                            HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(mapItem);
                                            remainingItems.addAll(remaining.values());
                                        }
                                        if (remainingItems.isEmpty()) {
                                            try (Connection conn3 = db.getConnection()) {
                                                TextComponent messages2 = Component.text()
                                                    .append(Component.text("すべての画像マップを渡しました。"))
                                                    .append(getPlayerTimesComponent(conn3, playerName)[1])
                                                    .build();

                                                audiences.player(player).sendMessage(messages2);
                                            } catch (SQLException | ClassNotFoundException e) {
                                                player.sendMessage("すべての画像マップを渡しました。(" + thisTimes + "/" + limitUploadTimes + ")");
                                                logger.error("An SQLException | ClassNotFoundException error occurred: {}", e.getMessage());
                                                for (StackTraceElement element : e.getStackTrace()) {
                                                    logger.error(element.toString());
                                                }
                                            }
                                            rt.removeCancelTaskRunnable(player, RunnableTaskUtil.Key.IMAGEMAP_CREATE_LARGE_IMAGE);
                                        } else {
                                            // プレイヤーが浮いているかどうかを確認する
                                            // プレイヤーがブロックの上にいる場合、地面にドロップする
                                            if (block.getType() != Material.AIR) {
                                                Bukkit.getScheduler().runTask(plugin, () -> {
                                                    Component message = Component.text("インベントリに入り切らないマップは、ドロップしました。")
                                                        .color(NamedTextColor.GREEN)
                                                        .decorate(TextDecoration.BOLD);

                                                    audiences.player(player).sendMessage(message);
                                                    
                                                    for (ItemStack remainingItem : remainingItems) {
                                                        world.dropItemNaturally(playerLocation, remainingItem);
                                                    }
                                                    try (Connection conn3 = db.getConnection()) {
                                                        TextComponent messages2 = Component.text("すべての画像マップを渡しました。")
                                                            .append(getPlayerTimesComponent(conn3, playerName)[1]);
                                                        audiences.player(player).sendMessage(messages2);
                                                    } catch (SQLException | ClassNotFoundException e) {
                                                        player.sendMessage("すべての画像マップを渡しました。(" + thisTimes + "/" + limitUploadTimes + ")");
                                                        logger.error("An SQLException | ClassNotFoundException error occurred: {}", e.getMessage());
                                                        for (StackTraceElement element : e.getStackTrace()) {
                                                            logger.error(element.toString());
                                                        }
                                                    }
                                                    rt.removeCancelTaskRunnable(player, RunnableTaskUtil.Key.IMAGEMAP_CREATE_LARGE_IMAGE);
                                                });
                                            } else {
                                                TextComponent messages2 = Component.text()
                                                    .append(Component.text("インベントリに入り切らないマップをドロップするために、ブロックの上に移動し、"))
                                                    .append(TCUtils.ONE.get())
                                                    .append(Component.text("と入力してください。"))
                                                    .appendNewline()
                                                    .append(TCUtils.INPUT_MODE.get())
                                                    .build();

                                                audiences.player(player).sendMessage(messages2);

                                                Map<RunnableTaskUtil.Key, MessageRunnable> playerActions2 = new HashMap<>();
                                                playerActions2.put(RunnableTaskUtil.Key.IMAGEMAP_CREATE_LARGE_IMAGE, (input2) -> {
                                                    if (input2.equals("1")) {
                                                        Location playerLocation_ = player.getLocation();
                                                        Block block_ = playerLocation_.getBlock();
                                                        if (block_.getType() != Material.AIR) {
                                                            Component alert = Component.text("ブロックの上に移動してください。")
                                                                .color(NamedTextColor.RED);

                                                            Component alert2 = Component.text("と入力してください。")
                                                                .appendNewline()
                                                                .color(NamedTextColor.RED);
                                                            
                                                            TextComponent alertMessage = Component.text()
                                                                .append(alert)
                                                                .append(TCUtils.ONE.get())
                                                                .append(alert2)
                                                                .append(TCUtils.INPUT_MODE.get())
                                                                .build();

                                                            audiences.player(player).sendMessage(alertMessage);

                                                            rt.extendTask(player, RunnableTaskUtil.Key.IMAGEMAP_CREATE_LARGE_IMAGE);
                                                            return;
                                                        }
                                                        Bukkit.getScheduler().runTask(plugin, () -> {
                                                            for (ItemStack remainingItem : remainingItems) {
                                                                world.dropItemNaturally(playerLocation, remainingItem);
                                                            }
                                                            try (Connection conn3 = db.getConnection()) {
                                                                StringBuilder messageBuilder3 = new StringBuilder();
                                                                messageBuilder3.append("インベントリに入り切らないマップをドロップしました。")
                                                                    .append("すべての画像マップを渡しました。");
                                                                
                                                                TextComponent messages3 = Component.text()
                                                                    .append(Component.text(messageBuilder3.toString()))
                                                                    .append(getPlayerTimesComponent(conn3, playerName)[1])
                                                                    .build();

                                                                audiences.player(player).sendMessage(messages3);
                                                            } catch (SQLException | ClassNotFoundException e) {
                                                                Component errorMessage = Component.text("データベースエラーが発生しました。")
                                                                    .color(NamedTextColor.RED);
                                                                audiences.player(player).sendMessage(errorMessage);
                                                                logger.error("An SQLException | ClassNotFoundException error occurred: {}", e.getMessage());
                                                                for (StackTraceElement element : e.getStackTrace()) {
                                                                    logger.error(element.toString());
                                                                }
                                                            }
                                                            rt.removeCancelTaskRunnable(player, RunnableTaskUtil.Key.IMAGEMAP_CREATE_LARGE_IMAGE);
                                                        });
                                                    } else {
                                                        Component errorMessage = Component.text("無効な入力です。")
                                                            .appendNewline()
                                                            .append(Component.text("もう一度入力し直してください。")) 
                                                            .color(NamedTextColor.RED);
                                                        audiences.player(player).sendMessage(errorMessage);
                                                    }
                                                });
                                                rt.addTaskRunnable(player, playerActions2, RunnableTaskUtil.Key.IMAGEMAP_CREATE_LARGE_IMAGE);
                                            }
                                        }

                                        Message msg = new Message();
                                        msg.mc = new Message.Minecraft();
                                        msg.mc.cmd = new Message.Minecraft.Command();
                                        msg.mc.cmd.imagemap = new Message.Minecraft.Command.ImageMap();
                                        msg.mc.cmd.imagemap.who = new Message.Minecraft.Who();
                                        msg.mc.cmd.imagemap.who.name = playerName;
                                        msg.mc.cmd.imagemap.type = "LARGE";

                                        SocketSwitch ssw = sswProvider.get();
                                        try (Connection connection = db.getConnection()) {
                                            ssw.sendVelocityServer(connection, msg);
                                        } catch (SQLException | ClassNotFoundException e) {
                                            logger.info("An error occurred at Menu#teleportPointMenu: {}", e);
                                        }
                                    } catch (IOException | SQLException | ClassNotFoundException e) {
                                        player.sendMessage("画像のダウンロードまたは保存に失敗しました: " + url);
                                        logger.error("An IOException | SQLException | URISyntaxException | ClassNotFoundException error occurred: {}", e.getMessage());
                                        for (StackTraceElement element : e.getStackTrace()) {
                                            logger.error(element.toString());
                                        }
                                    }
                                }
                                case "2" -> {
                                    TextComponent message = Component.text()
                                        .append(TCUtils2.getResponseComponent(input))
                                        .appendNewline()
                                        .append(Component.text("生成を中止しました。"))
                                        .build();

                                    audiences.player(player).sendMessage(message);

                                    rt.removeCancelTaskRunnable(player, RunnableTaskUtil.Key.IMAGEMAP_CREATE_LARGE_IMAGE);
                                }
                                default -> {
                                    Component errorMessage = Component.text("無効な入力です。")
                                        .appendNewline()
                                        .append(Component.text("もう一度入力し直してください。"))
                                        .color(NamedTextColor.RED);

                                    audiences.player(player).sendMessage(errorMessage);
                                    
                                    rt.extendTask(player, RunnableTaskUtil.Key.IMAGEMAP_CREATE_LARGE_IMAGE);
                                }
                            }
                        });
                        rt.addTaskRunnable(player, playerActions, RunnableTaskUtil.Key.IMAGEMAP_CREATE_LARGE_IMAGE);
                    }
                }
            } catch (IOException | SQLException | URISyntaxException | ClassNotFoundException e) {
                player.sendMessage("画像のダウンロードまたは保存に失敗しました: " + url);
                logger.error("An IOException | SQLException | URISyntaxException | ClassNotFoundException error occurred: {}", e.getMessage());
                for (StackTraceElement element : e.getStackTrace()) {
                    logger.error(element.toString());
                }
            }
        } else {
            if (sender != null) {
                sender.sendMessage("このコマンドはプレイヤーのみが実行できます。");
            }
        }
    }
    
    private void executeImageMap(CommandSender sender, String[] args, Object[] dArgs) {
        executeImageMap(sender, args, dArgs, false);
    }
    
    @Deprecated
    @SuppressWarnings("null")
	private void executeImageMap(CommandSender sender, String[] args, Object[] dArgs, boolean confirm) {
        if (sender instanceof Player player) {
            if (args.length < 3) {
                player.sendMessage("使用法: /fmc im <create|createqr> <url> [Optional: <title> <comment>]");
                return;
            }
            boolean isQr = args[1].equalsIgnoreCase("createqr"),
                fromDiscord = (dArgs != null);
            String playerName = player.getName(),
                playerUUID = player.getUniqueId().toString(),
                imageUUID = UUID.randomUUID().toString(),
                url = args[2],
                title = (args.length > 3 && !args[3].isEmpty()) ? args[3]: "無名のタイトル",
                comment = (args.length > 4 && !args[4].isEmpty()) ? args[4]: "コメントなし",
                ext;
            try (Connection conn = db.getConnection()) {
                int limitUploadTimes = FMCSettings.IMAGE_LIMIT_TIMES.getIntValue(),
                    playerUploadTimes = getPlayerTodayCreateImageTimes(conn, playerName),
                    thisTimes = playerUploadTimes + 1;
                if (thisTimes > limitUploadTimes) {
                    StringBuilder messageBuilder = new StringBuilder();
                    messageBuilder.append("1日の登録回数は")
                        .append(limitUploadTimes)
                        .append("回までです。");

                    Component errorMessage = Component.text(messageBuilder.toString())
                        .color(NamedTextColor.RED);

                    audiences.player(player).sendMessage(errorMessage);
                    return;
                }
                if (!isQr && !isValidURL(url)) {
                    player.sendMessage("無効なURLです。");
                    return;
                }
                LocalDate localDate = LocalDate.now();
				String now = fromDiscord ? (String) dArgs[1] : localDate.toString();
                BufferedImage image;
                if (isQr) {
                    ext = "png";
                    image = generateQRCodeImage(url);
                    saveImageToFileSystem(image, imageUUID, ext);
                } else {
                    URL getUrl = new URI(url).toURL();

                    ext = ExtUtil.getExtension(getUrl);
                    if (ext == null) {
                        Component errorMessage = Component.text("指定のURLは規定の拡張子を持ちません。")
                        .color(NamedTextColor.RED)
                        .decorate(TextDecoration.BOLD);
                        audiences.player(player).sendMessage(errorMessage);
                        return;
                    }

                    BufferedImage imageDefault =  ImageIO.read(getUrl);
                    if (imageDefault == null) {
                        Component errorMessage = Component.text("URLより画像を取得できませんでした。")
                            .color(NamedTextColor.RED);

                        audiences.player(player).sendMessage(errorMessage);
                        return;
                    }
                    saveImageToFileSystem(imageDefault, imageUUID, ext); // リサイズ前の画像を保存
                    image = resizeImage(imageDefault, 128, 128);
                }
                List<String> lores = new ArrayList<>();
                lores.add(isQr ? "<QRコード>" : "<イメージマップ>");
                List<String> commentLines = Arrays.stream(comment.split("\n"))
                    .map(String::trim)
                    .collect(Collectors.toList());
                lores.addAll(commentLines);
                lores.add("created by " + playerName);
                lores.add("created at " + now.replace("-", "/"));
                lores.add("size 1x1(1)");
                ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
                MapView mapView = Bukkit.createMap(player.getWorld());
                int mapId = mapView.getId(); // 一意のmapIdを取得
                mapView.getRenderers().clear();
                mapView.addRenderer(new ImageMapRenderer(image));
                var meta = (org.bukkit.inventory.meta.MapMeta) mapItem.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(title);
                    meta.setLore(lores);
                    meta.setMapView(mapView);
                    meta.getPersistentDataContainer().set(new NamespacedKey(plugin, ImageMap.PERSISTANT_KEY), PersistentDataType.STRING, "true");
                    mapItem.setItemMeta(meta);
                }
                if (fromDiscord) {
                    db.updateLog(conn, "UPDATE images SET name=?, uuid=?, server=?, mapid=?, title=?, imuuid=?, ext=?, url=?, comment=?, isqr=?, otp=?, date=?, locked_action=? WHERE otp=?;", new Object[] {playerName, playerUUID, serverName, mapId, title, imageUUID, ext, url, comment, isQr, null, now, true, (String) dArgs[0]});
                } else {
                    db.insertLog(conn, "INSERT INTO images (name, uuid, server, mapid, title, imuuid, ext, url, comment, isqr, confirm, date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);", new Object[] {playerName, playerUUID, serverName, mapId, title, imageUUID, ext, url, comment, isQr, confirm, Date.valueOf(LocalDate.now())});
                }

                HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(mapItem);

                Location playerLocation = player.getLocation();
                World world = player.getWorld();
                Block block = playerLocation.getBlock();

                if (remaining.isEmpty()) {
                    if (!confirm) {
                        try (Connection conn3 = db.getConnection()) {
                            TextComponent messages = Component.text()
                                .append(Component.text("画像マップを渡しました。"))
                                .append(getPlayerTimesComponent(conn3, playerName)[0])
                                .build();
    
                            audiences.player(player).sendMessage(messages);
                        } catch (SQLException | ClassNotFoundException e) {
                            player.sendMessage("すべての画像マップを渡しました。(" + thisTimes + "/" + limitUploadTimes + ")");
                            logger.error("An SQLException | ClassNotFoundException error occurred: {}", e.getMessage());
                            for (StackTraceElement element : e.getStackTrace()) {
                                logger.error(element.toString());
                            }
                        }
                    }
                } else {
                    // プレイヤーが浮いているかどうかを確認する
                    // プレイヤーがブロックの上にいる場合、地面にドロップする
                    if (block.getType() != Material.AIR) {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            player.sendMessage("インベントリに入り切らないマップは、ドロップしました。");
                            remaining.values().forEach(item -> {
                                world.dropItemNaturally(playerLocation, item);
                            });
                        });
                    } else {
                        Component message = Component.text("空中で実行しないでください！")
                            .color(NamedTextColor.RED)
                            .decorate(TextDecoration.BOLD);

                        audiences.player(player).sendMessage(message);
                    }
                }

                Message msg = new Message();
                msg.mc = new Message.Minecraft();
                msg.mc.cmd = new Message.Minecraft.Command();
                msg.mc.cmd.imagemap = new Message.Minecraft.Command.ImageMap();
                msg.mc.cmd.imagemap.who = new Message.Minecraft.Who();
                msg.mc.cmd.imagemap.who.name = playerName;
                msg.mc.cmd.imagemap.type = isQr ? "QR" : "SINGLE";

                SocketSwitch ssw = sswProvider.get();
                try (Connection connection = db.getConnection()) {
                    ssw.sendVelocityServer(connection, msg);
                } catch (SQLException | ClassNotFoundException e) {
                    logger.info("An error occurred at Menu#teleportPointMenu: {}", e);
                }
            } catch (IOException | SQLException | URISyntaxException | ClassNotFoundException | WriterException e) {
                player.sendMessage("画像のダウンロードまたは保存に失敗しました: " + url);
                logger.error("An IOException | SQLException | URISyntaxException | ClassNotFoundException error occurred: {}", e.getMessage());
                for (StackTraceElement element : e.getStackTrace()) {
                    logger.error(element.toString());
                }
            }
        } else {
            if (sender != null) {
                sender.sendMessage("このコマンドはプレイヤーのみが実行できます。");
            }
        }
    }

    @SuppressWarnings("null")
	private void executeQ(CommandSender sender, String[] args, boolean q, Object[] Args) {
        if (sender instanceof Player player) {
            String otp, title, comment, url, date;
            boolean fromMenu = (Args != null);
            try (Connection conn = db.getConnection()) {
                if (!fromMenu) {
                    if (q) {
                        otp = args[0];
                        if (args.length < 1) {
                            player.sendMessage("使用法: /q <code>");
                            return;
                        }
                    } else {
                        otp = args[1];
                        if (args.length < 2) {
                            player.sendMessage("使用法: /fmc q <code>");
                            return;
                        }
                    }
                    if (checkOTPIsCorrect(conn, otp)) {
                        Component message = Component.text("認証に成功しました。")
                            .color(NamedTextColor.GREEN)
                            .decorate(TextDecoration.BOLD);

                        audiences.player(player).sendMessage(message);

                        Map<String, Object> imageInfo = getImageInfoByOTP(conn, otp);
                        title = (String) imageInfo.get("title");
                        comment = (String) imageInfo.get("comment");
                        url = (String) imageInfo.get("url");
                        date = Date.valueOf(((Date) imageInfo.get("date")).toLocalDate()).toString();
                        // locked->false, nameをプレイヤーネームに更新する
                        db.updateLog(conn, "UPDATE images SET name=?, uuid=?, server=?, locked=? WHERE otp=?;", new Object[] {player.getName(), player.getUniqueId().toString(), serverName, false, otp});
                    } else {
                        Component errorMessage = Component.text("ワンタイムパスワードが間違っています。")
                            .color(NamedTextColor.RED);

                        audiences.player(player).sendMessage(errorMessage);
                        return;
                    }
                } else {
                    otp = (String) Args[0];
                    title = (String) Args[1];
                    comment = (String) Args[2];
                    url = (String) Args[3];
                    date = (String) Args[4];
                }
                leadAction(conn, player, RunnableTaskUtil.Key.IMAGEMAP_CREATE_IMAGE_MAP_FROM_Q, new String[] {url, title, comment}, new Object[] {otp, date});
            } catch (SQLException | ClassNotFoundException e) {
                player.sendMessage("データベースとの通信に問題が発生しました。");
                logger.error("An SQLException | ClassNotFoundException error occurred: {}", e.getMessage());
                for (StackTraceElement element : e.getStackTrace()) {
                    logger.error(element.toString());
                }
            }
        } else {
            if (sender != null) {
                sender.sendMessage("このコマンドはプレイヤーのみが実行できます。");
            }
        }
    }
    
    private Component[] getPlayerTimesComponent(Connection conn, String playerName) throws SQLException, ClassNotFoundException {
        int limitUploadSmallTimes = FMCSettings.IMAGE_LIMIT_TIMES.getIntValue(),
            limitUploadLargeTimes = FMCSettings.LARGE_IMAGE_LIMIT_TIMES.getIntValue(),
            playerUploadSmallTimes = getPlayerTodayCreateImageTimes(conn, playerName),
            playerUploadLargeTimes = getPlayerTodayCreateLargeImageTimes(conn, playerName),
            thisSmallTimes = playerUploadSmallTimes + 1,
            thisLargeTimes = playerUploadLargeTimes + 1;

        Component smallTimes = Component.text(playerUploadSmallTimes + "/" + limitUploadSmallTimes)
            .decorate(
                TextDecoration.BOLD,
                TextDecoration.UNDERLINED);

        Component largeTimes = Component.text(playerUploadLargeTimes + "/" + limitUploadLargeTimes)
            .decorate(
                TextDecoration.BOLD,
                TextDecoration.UNDERLINED);

        if (playerUploadSmallTimes >= limitUploadSmallTimes) {
            smallTimes.color(NamedTextColor.RED);
        } else if (thisSmallTimes >= limitUploadSmallTimes) {
            smallTimes.color(NamedTextColor.YELLOW);
        } else {
            smallTimes.color(NamedTextColor.LIGHT_PURPLE);
        }

        if (playerUploadLargeTimes >= limitUploadLargeTimes) {
            largeTimes.color(NamedTextColor.RED);
        } else if (thisLargeTimes >= limitUploadLargeTimes) {
            largeTimes.color(NamedTextColor.YELLOW);
        } else {
            largeTimes.color(NamedTextColor.GOLD);
        }

        return new Component[] {smallTimes, largeTimes};
    }

    @Deprecated
    private void executeImageMapFromMenu(Player player, Object[] mArgs, boolean isGiveMap) {
        // このサーバーにはないマップを生成する
        try (Connection conn = db.getConnection()) {
            int id = (int) mArgs[0];
            boolean isQr = (boolean) mArgs[1];
            String authorName = (String) mArgs[2],
                imageUUID = (String) mArgs[3],
                title = (String) mArgs[4],
                comment = (String) mArgs[5],
                ext = (String) mArgs[6],
                date = (String) mArgs[7],
                fullPath = FMCSettings.IMAGE_FOLDER.getValue() + "/" + date.replace("-", "") + "/" + imageUUID + "." + ext,
                playerName = player.getName();
            BufferedImage image = ImageIO.read(new File(fullPath));
            image = !isQr ? resizeImage(image, 128, 128) : image;
            List<String> lores = new ArrayList<>();
            lores.add(isQr ? "<QRコード>" : "<イメージマップ>");
            List<String> commentLines = Arrays.stream(comment.split("\n"))
                                .map(String::trim)
                                .collect(Collectors.toList());
            lores.addAll(commentLines);
            lores.add("created by " + authorName);
            lores.add("created at " + date.replace("-", "/"));
            ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
            MapView mapView = Bukkit.createMap(player.getWorld());
            int mapId = mapView.getId(); // 一意のmapIdを取得
            mapView.getRenderers().clear();
            mapView.addRenderer(new ImageMapRenderer(image));
            var meta = (org.bukkit.inventory.meta.MapMeta) mapItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(title);
                meta.setLore(lores);
                meta.setMapView(mapView);
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, ImageMap.PERSISTANT_KEY), PersistentDataType.STRING, "true");
                mapItem.setItemMeta(meta);
            }
            Map<String, String> modifyMap = new HashMap<>();
            modifyMap.put("menu", "true");
            modifyMap.put("menuer", "\"" + playerName + "\"");
            modifyMap.put("server", "\"" + serverName + "\"");
            modifyMap.put("mapid", String.valueOf(mapId));
            copyLineFromMenu(conn, id, modifyMap);
            if (isGiveMap) {
                // mapIdを更新する必要がある
                // なぜなら、このサーバーにはないマップを生成するため
                // giveMapメソッドで、そのワールド内にないことは確認済みなので
                db.updateLog(conn, "UPDATE images SET mapid=? WHERE id=?;", new Object[] {mapId, id});
            }
            player.getInventory().addItem(mapItem);
            if (!isGiveMap) {
                player.sendMessage("画像マップを渡しました。");
            }
        } catch (IOException | SQLException | ClassNotFoundException e) {
            player.sendMessage("画像の読み取りに失敗しました。");
            logger.error("An IOException | SQLException | URISyntaxException | ClassNotFoundException error occurred: {}", e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                logger.error(element.toString());
            }
        }
    }

    private void saveImageToDatabase(Connection conn, int mapId, int x, int y, BufferedImage image, String ext) throws SQLException, IOException {
        byte[] imageBytes = getImageBytes(image, ext);
        String sql = "INSERT INTO image_tiles (server, mapid, x, y, image) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE image = VALUES(image)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, serverName);
            stmt.setInt(2, mapId);
            stmt.setInt(3, x);
            stmt.setInt(4, y);
            stmt.setBytes(5, imageBytes);
            stmt.executeUpdate();
        }
    }

    private byte[] getImageBytes(BufferedImage image, String ext) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Thumbnails.of(image)
            .scale(1)
            .outputFormat(ext)
            .toOutputStream(baos);
        return baos.toByteArray();
    }

    private BufferedImage loadImage(Connection conn, int mapId) throws SQLException, IOException {
        String sql = "SELECT isqr, date, imuuid, ext FROM images WHERE mapid = ? AND server = ?;";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, mapId);
            stmt.setString(2, serverName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    boolean isQr = rs.getBoolean("isqr");
                    Date date = rs.getDate("date");
                    String imageUUID = rs.getString("imuuid"),
                        ext = rs.getString("ext"),
                        fullPath = getFullPath(date, imageUUID, ext);
                    BufferedImage image = ImageIO.read(new File(fullPath));
                    if (image != null) {
                        return isQr ? image : resizeImage(image, 128, 128);
                    }
                }
            }
        }
        return null;
    }

    private BufferedImage loadTileImage(Connection conn, int mapId) throws SQLException, IOException {
        String sql = "SELECT image FROM image_tiles WHERE mapid = ? AND server = ?;";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, mapId);
            stmt.setString(2, serverName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    byte[] imageBytes = rs.getBytes("image");
                    ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
                    return ImageIO.read(bais);
                }
            }
        }
        return null;
    }

    private Map<String, Object> getMapInfoForThisServerByMapId(Connection conn, int mapId) throws SQLException, ClassNotFoundException {
        Map<String, Object> mapInfo = new HashMap<>();
        String query = "SELECT * FROM images WHERE mapid=? AND server=?;";
        PreparedStatement ps = conn.prepareStatement(query);
        ps.setInt(1, mapId);
        ps.setString(2, serverName);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            int columnCount = rs.getMetaData().getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = rs.getMetaData().getColumnName(i);
                mapInfo.put(columnName, rs.getObject(columnName));
            }
        }
        return mapInfo;
    }

    private void copyLineFromMenu(Connection conn, int id, Map<String, String> modifyMap) throws SQLException, ClassNotFoundException {
        List<String> modifiedImageColumnsList = new ArrayList<>(ImageMap.imagesColumnsList),
            imagesColumnsListCopied = new ArrayList<>(ImageMap.imagesColumnsList);
        for (Map.Entry<String, String> entry : modifyMap.entrySet()) {
            String key = entry.getKey(),
                newValue = entry.getValue();
            modifiedImageColumnsList = modifiedImageColumnsList.stream()
                .map(s -> s.equals(key) ? newValue : s)
                .collect(Collectors.toList());
        }
        // idカラムを除外
        imagesColumnsListCopied.remove("id");
        modifiedImageColumnsList.remove("id");
        String query = "INSERT INTO images (" + String.join(", ", imagesColumnsListCopied) + ") " +
                       "SELECT " + String.join(", ", modifiedImageColumnsList) + " " +
                       "FROM images " +
                       "WHERE id = ?;";
        //logger.info(query);
        PreparedStatement ps = conn.prepareStatement(query);
        ps.setInt(1, id);
        ps.executeUpdate();
    }
    
    private Map<String, Object> getImageInfoByOTP(Connection conn, String otp) throws SQLException, ClassNotFoundException {
        Map<String, Object> imageInfo = new HashMap<>();
        String query = "SELECT * FROM images WHERE otp=?;";
        PreparedStatement ps = conn.prepareStatement(query);
        ps.setString(1, otp);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            int columnCount = rs.getMetaData().getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = rs.getMetaData().getColumnName(i);
                imageInfo.put(columnName, rs.getObject(columnName));
            }
        }
        return imageInfo;
    }

    private int getPlayerTodayCreateImageTimes(Connection conn, String playerName) throws SQLException, ClassNotFoundException {
        String query = "SELECT COUNT(*) FROM images WHERE menu != ? AND name = ? AND DATE(date) = ?";
		PreparedStatement ps = conn.prepareStatement(query);
        ps.setBoolean(1, true);
		ps.setString(2, playerName);
		ps.setDate(3, java.sql.Date.valueOf(LocalDate.now()));
		ResultSet rs = ps.executeQuery();
		if (rs.next()) {
			return rs.getInt(1);
		}
        return 0;
    }

    private int getPlayerTodayCreateLargeImageTimes(Connection conn, String playerName) throws SQLException, ClassNotFoundException {
        String query = "SELECT COUNT(*) FROM images WHERE menu != ? AND name = ? AND DATE(date) = ? AND large = ?;";
		PreparedStatement ps = conn.prepareStatement(query);
        ps.setBoolean(1, true);
		ps.setString(2, playerName);
		ps.setDate(3, java.sql.Date.valueOf(LocalDate.now()));
        ps.setBoolean(4, true);
		ResultSet rs = ps.executeQuery();
		if (rs.next()) {
			return rs.getInt(1);
		}
        return 0;
    }

    private String getFullPath(Date date, String imageUUID, String ext) {
        return FMCSettings.IMAGE_FOLDER.getValue() + "/" + date.toString().replace("-", "") + "/" + imageUUID + "." + ext;
    }

    private BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) throws IOException {
        return Thumbnails.of(originalImage)
            .size(targetWidth, targetHeight)
            .asBufferedImage();
    }

    private boolean checkOTPIsCorrect(Connection conn, String code) throws SQLException, ClassNotFoundException {
        String query = "SELECT * FROM images WHERE otp=? AND locked=? LIMIT 1;";
        PreparedStatement ps = conn.prepareStatement(query);
        // otpがあって、lockされているものに限る
        ps.setString(1, code);
        ps.setBoolean(2, true);
        ResultSet rs = ps.executeQuery();
        return rs.next();
    }

    private boolean isValidURL(String urlString) {
        try {
            URI uri = new URI(urlString);
            uri.toURL();
            return true;
        } catch (MalformedURLException | URISyntaxException e) {
            return false;
        }
    }
    
    private BufferedImage generateQRCodeImage(String text) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        int width = 128;
        int height = 128;
        var bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }

    private void saveImageToFileSystem(BufferedImage image, String imageUUID, String ext) throws IOException, SQLException, ClassNotFoundException {
        Path dirPath = Paths.get(FMCSettings.IMAGE_FOLDER.getValue(), LocalDate.now().toString().replace("-", ""));
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }
        String fileName = imageUUID + "." + ext;
        Path filePath = dirPath.resolve(fileName);
        ImageIO.write(image, ext, filePath.toFile());
    }

    @SuppressWarnings("unused")
    private BufferedImage rotateImage(BufferedImage image, double angle) throws IOException {
        return Thumbnails.of(image)
            .scale(1)
            .rotate(angle)
            .asBufferedImage();
    }
}
