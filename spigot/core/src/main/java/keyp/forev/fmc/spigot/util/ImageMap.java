package keyp.forev.fmc.spigot.util;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
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
import org.bukkit.scheduler.BukkitTask;
import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;

import keyp.forev.fmc.common.CalcUtil;
import keyp.forev.fmc.common.Database;
import keyp.forev.fmc.common.FMCSettings;
import keyp.forev.fmc.common.SocketSwitch;
import keyp.forev.fmc.spigot.events.EventListener;
import net.coobird.thumbnailator.Thumbnails;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import keyp.forev.fmc.spigot.cmd.Menu;
import org.bukkit.plugin.java.JavaPlugin;

public class ImageMap {
    public static final String PERSISTANT_KEY = "custom_image";
    public static final String LARGE_PERSISTANT_KEY = "custom_large_image";
    public static final String ACTIONS_KEY = "largeImageMap";
    public static final String ACTIONS_KEY2 = "createImageMapFromQ";
    public static final String ACTIONS_KEY3 = "createImageMapFromCommandLine";
    public static List<String> imagesColumnsList = new ArrayList<>();
    public static List<Integer> thisServerMapIds = new ArrayList<>();
    public static List<String> args2 = new ArrayList<>(Arrays.asList("create", "q"));
    private final JavaPlugin plugin;
    private final Logger logger;
    private final Database db;
    private final Provider<SocketSwitch> sswProvider;
    private final String serverName;
    private final int inputPeriod = 60;
    @Inject
    public ImageMap(JavaPlugin plugin, Logger logger, Database db, SpigotServerHomeDir shd, Provider<SocketSwitch> sswProvider) {
        this.plugin = plugin;
        this.logger = logger;
        this.db = db;
        this.serverName = shd.getServerName();
        this.sswProvider = sswProvider;
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
                leadAction(conn, player, ACTIONS_KEY3, new String[] {url, title, comment}, null);
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

    @SuppressWarnings("null")
	private void executeLargeImageMap(CommandSender sender, String[] args, Object[] dArgs, Object[] inputs, Object[] inputs2, Object[] inputs3) {
        if (sender instanceof Player player) {
            if (checkIsOtherInputMode(player)) {
                player.sendMessage(ChatColor.RED + "他でインプットモード中です。");
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
                    player.sendMessage(ChatColor.RED + "1日の登録回数は"+limitUploadTimes+"回までです。");
                    return;
                }
                String now;
                final BufferedImage image;
                if (inputs == null) {
                    if (checkIsInputMode(player)) {
                        player.sendMessage(ChatColor.RED + "インプットモード中に他のラージマップを作成することはできません。");
                        return;
                    }
                    if (!isValidURL(url)) {
                        player.sendMessage("無効なURLです。");
                        return;
                    }
                    URL getUrl = new URI(url).toURL();
                    HttpURLConnection connection = (HttpURLConnection) getUrl.openConnection();
                    connection.setRequestMethod("GET");
                    connection.connect();
                    String contentType = connection.getContentType();
                    switch (contentType) {
                        case "image/png" -> ext = "png";
                        case "image/jpeg" -> ext = "jpeg";
                        case "image/jpg" -> ext = "jpg";
                        default -> {
                            player.sendMessage("指定のURLは規定の拡張子を持ちません。");
                            return;
                        }
                    }
                    LocalDate localDate = LocalDate.now();
                    now = fromDiscord ? (String) dArgs[1] : localDate.toString();
                    image =  ImageIO.read(getUrl);
                    if (image == null) {
                        player.sendMessage(ChatColor.RED + "指定のURLは規定の拡張子を持ちません。");
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
                    TextComponent ratio = new TextComponent(x + ":" + y);
                    ratio.setUnderlined(true);
                    ratio.setBold(true);
                    ratio.setColor(ChatColor.GOLD);
                    player.spigot().sendMessage(
                        new TextComponent("現在、適切な縦横比は、"),
                        ratio,
                        new TextComponent("です。\n"),
                        new TextComponent("元の画像のアスペクト比を維持するため、縦か横のもう一方は自動的に決定されます。"),
                        new TextComponent("縦か横のサイズを整数値で指定してください。\n(例) x=5 or y=4\n"),
                        TCUtils.INPUT_MODE.get()
                    );
                    final Object[] inputs_1 = new Object[] {now, ext, image};
                    Map<String, MessageRunnable> playerActions = new HashMap<>();
                    playerActions.put(ImageMap.ACTIONS_KEY, (input) -> {
                        // x=? or y=?
                        if (input.contains("=")) {
                            String[] xy = input.split("=");
                            // 空白を削除
                            xy[0] = xy[0].trim();
                            xy[1] = xy[1].trim();
                            if (!xy[0].equals("x") && !xy[0].equals("y")) {
                                player.sendMessage(ChatColor.RED + "無効な入力です。\n(例) x=5 or y=4のように入力してください。");
                                return;
                            }
                            try {
                                int xOry = Integer.parseInt(xy[1].trim());
                                if (xOry == 1) {
                                    player.sendMessage(ChatColor.RED + "ラージマップでないため、x=1, y=1を選択することはできません。\n(例) x=5 or y=4のように入力してください。");
                                    extendTask(player, ImageMap.ACTIONS_KEY);
                                    return;
                                } else if (xOry < 1) {
                                    player.sendMessage(ChatColor.RED + "無効な入力です。\n(例) x=5 or y=4のように入力してください。");
                                    extendTask(player, ImageMap.ACTIONS_KEY);
                                    return;
                                }
                                player.spigot().sendMessage(new TCUtils2(input).getResponseComponent());
                                Object[] inputs_2 = new Object[] {xy[0], xOry};
                                executeLargeImageMap(sender, args, dArgs, inputs_1, inputs_2, null);
                            } catch (NumberFormatException e) {
                                player.sendMessage(ChatColor.RED + "無効な入力です。\n(例) x=5 or y=4のように入力してください。");
                                extendTask(player, ImageMap.ACTIONS_KEY);
                            }
                        } else {
                            player.sendMessage(ChatColor.RED + "無効な入力です。\n(例) x=5 or y=4のように入力してください。");
                            extendTask(player, ImageMap.ACTIONS_KEY);
                        }
                    });
                    addTaskRunnable(player, playerActions, ImageMap.ACTIONS_KEY);
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
                            player.sendMessage(ChatColor.RED + "総タイル数("+maxTiles+")を超過しています。\nもう一度、入力してください。");
                            extendTask(player, ImageMap.ACTIONS_KEY);
                            return;
                        }
                        removeCancelTaskRunnable(player, ImageMap.ACTIONS_KEY);
                        Object[] inputs_3 = new Object[] {x, y, null};
                        // ここでプレイヤーに色を決めさせる
                        player.sendMessage("次に、背景色を選択します。");
                        TextComponent note = new TextComponent("jpegかjpgの画像の場合、背景色に透明を選ぶことはできません。\n");
                        note.setUnderlined(true);
                        note.setItalic(true);
                        note.setColor(ChatColor.GRAY);
                        player.spigot().sendMessage(note, TCUtils.INPUT_MODE.get());
                        Map<Integer, Runnable> playerMenuActions = new HashMap<>();
                        Inventory inv = Bukkit.createInventory(null, 27, Menu.chooseColorInventoryName);
                        Map<ItemStack, java.awt.Color> colorItems = ColorItems.getColorItems();
                        //logger.info("colorItems: {}", colorItems);
                        int index = 0;
                        for (Map.Entry<ItemStack, java.awt.Color> entry : colorItems.entrySet()) {
                            ItemStack item = entry.getKey();
                            java.awt.Color color = entry.getValue();
                            inv.setItem(index, item);
                            playerMenuActions.put(index, () -> {
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
                        playerMenuActions.put(index, () -> {
                            removeCancelTaskRunnable(player, ImageMap.ACTIONS_KEY);
                            player.closeInventory();
                            TextComponent link1 = new TextComponent("ココ");
                            link1.setBold(true);
                            link1.setUnderlined(true);
                            link1.setColor(ChatColor.GOLD);
                            link1.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://note.cman.jp/color/base_color.cgi"));
                            TextComponent link2 = new TextComponent("ココ");
                            link2.setBold(true);
                            link2.setUnderlined(true);
                            link2.setColor(ChatColor.GOLD);
                            link2.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://www.cc.kyoto-su.ac.jp/~shimizu/MAKE_HTML/rgb2.html"));
                            player.spigot().sendMessage(
                                new TextComponent("カスタム色を選択してください。\n"),
                                new TextComponent("色のRGB値を入力してください。\n(例) 255, 255, 255"),
                                new TextComponent("それぞれ、0~255の範囲で入力してください。\n"),
                                new TextComponent("外部サイトのRGBカラーリストを見るには、"), 
                                link1,
                                new TextComponent("をクリックしてください。\n"),
                                new TextComponent("外部サイトのRGBカラーシミュレーターツールを使うには、"),
                                link2,
                                new TextComponent("をクリックしてください。\n"),
                                new TextComponent("色選択メニューに戻る場合は、0と入力してください。\n"),
                                TCUtils.INPUT_MODE.get()
                            );
                            Map<String, MessageRunnable> playerActions = new HashMap<>();
                            playerActions.put(ImageMap.ACTIONS_KEY, (input) -> {
                                if (input.equals("0")) {
                                    TextComponent message = new TextComponent("5秒後にインベントリを開きます。");
                                    message.setBold(true);
                                    message.setUnderlined(true);
                                    message.setColor(ChatColor.GOLD);
                                    player.spigot().sendMessage(
                                        new TCUtils2(input).getResponseComponent(),
                                        new TextComponent("\n色選択メニューに戻ります。"),
                                        message
                                    );
                                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                                        player.openInventory(inv);
                                    }, 100L);
                                    return;
                                }
                                String[] rgb = input.split(",");
                                if (rgb.length != 3) {
                                    player.sendMessage(ChatColor.RED + "無効な入力です。\n(例) 255, 255, 255のように入力してください。");
                                    extendTask(player, ImageMap.ACTIONS_KEY);
                                    return;
                                }
                                try {
                                    int r = Integer.parseInt(rgb[0].trim());
                                    int g = Integer.parseInt(rgb[1].trim());
                                    int b = Integer.parseInt(rgb[2].trim());
                                    if (r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255) {
                                        player.sendMessage(ChatColor.RED + "無効な入力です。\n(例) 255, 255, 255のように入力してください。");
                                        extendTask(player, ImageMap.ACTIONS_KEY);
                                        return;
                                    }
                                    java.awt.Color customColor = new java.awt.Color(r, g, b);
                                    inputs_3[2] = customColor;
                                    player.spigot().sendMessage(new TCUtils2("(R, G, B) = (" + r + ", " + g + ", " + b + ")").getResponseComponent());
                                    executeLargeImageMap(sender, args, dArgs, inputs, inputs2, inputs_3);
                                } catch (NumberFormatException e) {
                                    player.sendMessage(ChatColor.RED + "無効な入力です。\n(例) 255, 255, 255のように入力してください。");
                                    extendTask(player, ImageMap.ACTIONS_KEY);
                                }
                            });
                            addTaskRunnable(player, playerActions, ImageMap.ACTIONS_KEY);
                        });
                        Menu.menuActions.computeIfAbsent(player, _p -> new HashMap<>()).put(Menu.chooseColorInventoryName, playerMenuActions);
                        player.spigot().sendMessage(TCUtils.LATER_OPEN_INV_5.get());
                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                            player.openInventory(inv);
                            player.spigot().sendMessage(
                                new TextComponent("手動で背景色選択メニューを開くには、"),
                                TCUtils.ONE.get(),
                                new TextComponent("と入力してください。\n"),
                                new TextComponent("生成を中止する場合は、"),
                                TCUtils.TWO.get(),
                                new TextComponent("と入力してください。\n"),
                                TCUtils.INPUT_MODE.get()
                            );
                            Map<String, MessageRunnable> playerActions = new HashMap<>();
                            playerActions.put(ImageMap.ACTIONS_KEY, (input) -> {
                                switch (input) {
                                    case "1" -> {
                                        player.spigot().sendMessage(
                                            new TCUtils2(input).getResponseComponent(),
                                            new TextComponent("\n"),
                                            TCUtils.LATER_OPEN_INV_3.get()
                                        );
                                        player.spigot().sendMessage();
                                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                                            player.openInventory(inv);
                                        }, 60L);
                                    }
                                    case "2" -> {
                                        player.spigot().sendMessage(
                                            new TCUtils2(input).getResponseComponent(),
                                            new TextComponent("\n生成を中止しました。")
                                        );
                                        removeCancelTaskRunnable(player, ImageMap.ACTIONS_KEY);
                                    }
                                    default -> {
                                        TextComponent alert = new TextComponent("無効な入力です。\n");
                                        alert.setColor(ChatColor.RED);
                                        player.spigot().sendMessage(
                                            alert,
                                            new TextComponent("手動で背景色選択メニューを開くには、"),
                                            TCUtils.ONE.get(),
                                            new TextComponent("と入力してください。\n"),
                                            new TextComponent("生成を中止する場合は、"),
                                            TCUtils.TWO.get(),
                                            new TextComponent("と入力してください。\n")
                                        );
                                        extendTask(player, ImageMap.ACTIONS_KEY);
                                    }
                                }
                            });
                            addTaskRunnable(player, playerActions, ImageMap.ACTIONS_KEY);
                        }, 100L);
                    } else {
                        java.awt.Color color = (java.awt.Color) inputs3[2];
                        String colorName = ColorItems.getColorName(color);
                        if (ext.equals("jpg") || ext.equals("jpeg")) {
                            if (ColorItems.isTransparent(color)) {
                                TextComponent alert = new TextComponent("jpg, jpeg形式の画像は透明度を持たないため、透明色を選択することはできません。\n背景色を選択し直してください。\n");
                                alert.setColor(ChatColor.RED);
                                player.spigot().sendMessage(
                                    alert,
                                    new TextComponent("手動で背景色選択メニューを開くには、"),
                                    TCUtils.ONE.get(),
                                    new TextComponent("と入力してください。\n"),
                                    new TextComponent("生成を中止する場合は、"),
                                    TCUtils.TWO.get(),
                                    new TextComponent("と入力してください。\n"),
                                    TCUtils.INPUT_MODE.get()
                                );
                                extendTask(player, ImageMap.ACTIONS_KEY);
                                return;
                            }
                        }
                        removeCancelTaskRunnable(player, ImageMap.ACTIONS_KEY);
                        // ここで一度確認を挟む
                        int x = (int) inputs3[0];
                        int y = (int) inputs3[1];
                        player.spigot().sendMessage(
                            new TextComponent("以下の内容で画像マップを生成します。\n"),
                            new TextComponent("タイトル: " + title + "\n"),
                            new TextComponent("コメント: " + comment + "\n"),
                            new TextComponent("サイズ: " + x + "x" + y + "(" + x * y + ")\n"),
                            new TextComponent("背景色: " + colorName + "\n"),
                            new TextComponent("生成する場合は、"),
                            TCUtils.ONE.get(),
                            new TextComponent("と入力してください。\n"),
                            new TextComponent("生成を中止する場合は、"),
                            TCUtils.TWO.get(),
                            new TextComponent("と入力してください。\n"),
                            TCUtils.INPUT_MODE.get()
                        );
                        Map<String, MessageRunnable> playerActions = new HashMap<>();
                        playerActions.put(ImageMap.ACTIONS_KEY, (input) -> {
                            switch (input) {
                                case "1" -> {
                                    player.spigot().sendMessage(new TCUtils2(input).getResponseComponent());
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
                                                lores.add("at " + now.replace("-", "/"));
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
                                                player.spigot().sendMessage(
                                                    new TextComponent("すべての画像マップを渡しました。"),
                                                    getPlayerTimesComponent(conn3, playerName)[1]
                                                );
                                            } catch (SQLException | ClassNotFoundException e) {
                                                player.sendMessage("すべての画像マップを渡しました。(" + thisTimes + "/" + limitUploadTimes + ")");
                                                logger.error("An SQLException | ClassNotFoundException error occurred: {}", e.getMessage());
                                                for (StackTraceElement element : e.getStackTrace()) {
                                                    logger.error(element.toString());
                                                }
                                            }
                                            removeCancelTaskRunnable(player, ImageMap.ACTIONS_KEY);
                                        } else {
                                            // プレイヤーが浮いているかどうかを確認する
                                            // プレイヤーがブロックの上にいる場合、地面にドロップする
                                            if (block.getType() != Material.AIR) {
                                                Bukkit.getScheduler().runTask(plugin, () -> {
                                                    player.sendMessage("インベントリに入り切らないマップは、ドロップしました。");
                                                    for (ItemStack remainingItem : remainingItems) {
                                                        world.dropItemNaturally(playerLocation, remainingItem);
                                                    }
                                                    try (Connection conn3 = db.getConnection()) {
                                                        player.spigot().sendMessage(
                                                            new TextComponent("すべての画像マップを渡しました。"),
                                                            getPlayerTimesComponent(conn3, playerName)[1]
                                                        );
                                                    } catch (SQLException | ClassNotFoundException e) {
                                                        player.sendMessage("すべての画像マップを渡しました。(" + thisTimes + "/" + limitUploadTimes + ")");
                                                        logger.error("An SQLException | ClassNotFoundException error occurred: {}", e.getMessage());
                                                        for (StackTraceElement element : e.getStackTrace()) {
                                                            logger.error(element.toString());
                                                        }
                                                    }
                                                    removeCancelTaskRunnable(player, ImageMap.ACTIONS_KEY);
                                                });
                                            } else {
                                                player.spigot().sendMessage(
                                                    new TextComponent("インベントリに入り切らないマップをドロップするために、ブロックの上に移動し、"),
                                                    TCUtils.ONE.get(),
                                                    new TextComponent("と入力してください。\n"),
                                                    TCUtils.INPUT_MODE.get()
                                                );
                                                Map<String, MessageRunnable> playerActions2 = new HashMap<>();
                                                playerActions2.put(ImageMap.ACTIONS_KEY, (input2) -> {
                                                    if (input2.equals("1")) {
                                                        Location playerLocation_ = player.getLocation();
                                                        Block block_ = playerLocation_.getBlock();
                                                        if (block_.getType() != Material.AIR) {
                                                            TextComponent alert = new TextComponent("ブロックの上に移動してください。");
                                                            alert.setColor(ChatColor.RED);
                                                            TextComponent alert2 = new TextComponent("と入力してください。\n");
                                                            alert2.setColor(ChatColor.RED);
                                                            player.spigot().sendMessage(
                                                                alert,
                                                                TCUtils.ONE.get(),
                                                                alert2,
                                                                TCUtils.INPUT_MODE.get()
                                                            );
                                                            extendTask(player, ImageMap.ACTIONS_KEY);
                                                            return;
                                                        }
                                                        Bukkit.getScheduler().runTask(plugin, () -> {
                                                            for (ItemStack remainingItem : remainingItems) {
                                                                world.dropItemNaturally(playerLocation, remainingItem);
                                                            }
                                                            player.sendMessage("インベントリに入り切らないマップをドロップしました。");
                                                            try (Connection conn3 = db.getConnection()) {
                                                                player.spigot().sendMessage(
                                                                    new TextComponent("すべての画像マップを渡しました。"),
                                                                    getPlayerTimesComponent(conn3, playerName)[1]
                                                                );
                                                            } catch (SQLException | ClassNotFoundException e) {
                                                                player.sendMessage("すべての画像マップを渡しました。(" + thisTimes + "/" + limitUploadTimes + ")");
                                                                logger.error("An SQLException | ClassNotFoundException error occurred: {}", e.getMessage());
                                                                for (StackTraceElement element : e.getStackTrace()) {
                                                                    logger.error(element.toString());
                                                                }
                                                            }
                                                            removeCancelTaskRunnable(player, ImageMap.ACTIONS_KEY);
                                                        });
                                                    } else {
                                                        player.sendMessage(ChatColor.RED + "無効な入力です。");
                                                    }
                                                });
                                                addTaskRunnable(player, playerActions2, ImageMap.ACTIONS_KEY);
                                            }
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
                                    player.spigot().sendMessage(
                                        new TCUtils2(input).getResponseComponent(),
                                        new TextComponent("\n生成を中止しました。")
                                    );
                                    removeCancelTaskRunnable(player, ImageMap.ACTIONS_KEY);
                                }
                                default -> {
                                    player.sendMessage(ChatColor.RED + "無効な入力です。\n生成する場合は、1と入力してください。\n生成を中止する場合は、2と入力してください。");
                                    extendTask(player, ImageMap.ACTIONS_KEY);
                                }
                            }
                        });
                        addTaskRunnable(player, playerActions, ImageMap.ACTIONS_KEY);
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
                    player.sendMessage(ChatColor.RED + "1日の登録回数は"+limitUploadTimes+"回までです。");
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
                    HttpURLConnection connection = (HttpURLConnection) getUrl.openConnection();
                    connection.setRequestMethod("GET");
                    connection.connect();
                    String contentType = connection.getContentType();
                    switch (contentType) {
                        case "image/png" -> ext = "png";
                        case "image/jpeg" -> ext = "jpeg";
                        case "image/jpg" -> ext = "jpg";
                        default -> {
                            player.sendMessage("指定のURLは規定の拡張子を持ちません。");
                            return;
                        }
                    }
                    BufferedImage imageDefault =  ImageIO.read(getUrl);
                    if (imageDefault == null) {
                        player.sendMessage(ChatColor.RED + "指定のURLは規定の拡張子を持ちません。");
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
                lores.add("at " + now.replace("-", "/"));
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
                player.getInventory().addItem(mapItem);
                if (!confirm) {
                    try (Connection conn3 = db.getConnection()) {
                        player.spigot().sendMessage(
                            new TextComponent("画像マップを渡しました。"),
                            getPlayerTimesComponent(conn3, playerName)[0]
                        );
                    } catch (SQLException | ClassNotFoundException e) {
                        player.sendMessage("すべての画像マップを渡しました。(" + thisTimes + "/" + limitUploadTimes + ")");
                        logger.error("An SQLException | ClassNotFoundException error occurred: {}", e.getMessage());
                        for (StackTraceElement element : e.getStackTrace()) {
                            logger.error(element.toString());
                        }
                    }
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
                        player.sendMessage(ChatColor.GREEN + "認証に成功しました。");
                        Map<String, Object> imageInfo = getImageInfoByOTP(conn, otp);
                        title = (String) imageInfo.get("title");
                        comment = (String) imageInfo.get("comment");
                        url = (String) imageInfo.get("url");
                        date = Date.valueOf(((Date) imageInfo.get("date")).toLocalDate()).toString();
                        // locked->false, nameをプレイヤーネームに更新する
                        db.updateLog(conn, "UPDATE images SET name=?, uuid=?, server=?, locked=? WHERE otp=?;", new Object[] {player.getName(), player.getUniqueId().toString(), serverName, false, otp});
                    } else {
                        player.sendMessage(ChatColor.RED + "ワンタイムパスワードが間違っています。");
                        return;
                    }
                } else {
                    otp = (String) Args[0];
                    title = (String) Args[1];
                    comment = (String) Args[2];
                    url = (String) Args[3];
                    date = (String) Args[4];
                }
                leadAction(conn, player, ACTIONS_KEY2, new String[] {url, title, comment}, new Object[] {otp, date});
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
    
    private BaseComponent[] getPlayerTimesComponent(Connection conn, String playerName) throws SQLException, ClassNotFoundException {
        int limitUploadSmallTimes = FMCSettings.IMAGE_LIMIT_TIMES.getIntValue(),
            limitUploadLargeTimes = FMCSettings.LARGE_IMAGE_LIMIT_TIMES.getIntValue(),
            playerUploadSmallTimes = getPlayerTodayCreateImageTimes(conn, playerName),
            playerUploadLargeTimes = getPlayerTodayCreateLargeImageTimes(conn, playerName),
            thisSmallTimes = playerUploadSmallTimes + 1,
            thisLargeTimes = playerUploadLargeTimes + 1;
        TextComponent smallTimes = new TextComponent(playerUploadSmallTimes + "/" + limitUploadSmallTimes),
            largeTimes = new TextComponent(playerUploadLargeTimes + "/" + limitUploadLargeTimes);
        smallTimes.setBold(true);
        smallTimes.setUnderlined(true);
        largeTimes.setBold(true);
        largeTimes.setUnderlined(true);
        if (playerUploadSmallTimes >= limitUploadSmallTimes) {
            smallTimes.setColor(ChatColor.RED);
        } else if (thisSmallTimes >= limitUploadSmallTimes) {
            smallTimes.setColor(ChatColor.YELLOW);
        } else {
            smallTimes.setColor(ChatColor.LIGHT_PURPLE);
        }
        if (playerUploadLargeTimes >= limitUploadLargeTimes) {
            largeTimes.setColor(ChatColor.RED);
        } else if (thisLargeTimes >= limitUploadLargeTimes) {
            largeTimes.setColor(ChatColor.YELLOW);
        } else {
            largeTimes.setColor(ChatColor.GOLD);
        }
        return new BaseComponent[] {smallTimes, largeTimes};
    }

    private void leadAction(Connection conn, Player player, String key, String[] usingArgs, Object[] qArgs) throws SQLException, ClassNotFoundException {
        String playerName = player.getName();
        boolean fromQ = (qArgs != null);
        String url = usingArgs[0],
            title = usingArgs[1],
            comment = usingArgs[2];
        // ラージか1✕1かをプレイヤーに問う
        TextComponent message;
        if (fromQ) {
            message = new TextComponent("操作が途中で中断された場合、メニュー->画像マップより再開できます。\n");
            message.setColor(ChatColor.GRAY);
        } else {
            message = new TextComponent();
        }
        BaseComponent[] times = getPlayerTimesComponent(conn, playerName);
        player.spigot().sendMessage(
            new TextComponent("1✕1の画像マップを作成する場合は、"),
            TCUtils.ZERO.get(),
            new TextComponent("と入力してください。"),
            times[0],
            new TextComponent("\n"),
            new TextComponent("1✕1のQRコードコードを作成する場合は、"),
            TCUtils.ONE.get(),
            new TextComponent("と入力してください。"),
            times[0],
            new TextComponent("\n"),
            new TextComponent("ラージマップを作成する場合は、"),
            TCUtils.TWO.get(),
            new TextComponent("と入力してください。"),
            times[1],
            new TextComponent("\n"),
            message,
            TCUtils.INPUT_MODE.get()
        );
        Map<String, MessageRunnable> playerActions = new HashMap<>();
        playerActions.put(key, (input) -> {
            switch (input) {
                case "0", "1" -> {
                    removeCancelTaskRunnable(player, key);
                    String cmd;
                    if (input.equals("0")) {
                        cmd = "create";
                    } else {
                        cmd = "createqr";
                    }
                    player.spigot().sendMessage(new TCUtils2(input).getResponseComponent());
                    String[] imageArgs = new String[] {"im", cmd, url, title, comment};
                    executeImageMap(player, imageArgs, qArgs);
                }
                case "2" -> {
                    removeCancelTaskRunnable(player, key);
                    player.spigot().sendMessage(new TCUtils2(input).getResponseComponent());
                    String[] imageArgs = new String[] {"im", "largecreate", url, title, comment};
                    executeLargeImageMap(player, imageArgs, qArgs, null, null, null);
                }
                default -> {
                    player.sendMessage(ChatColor.RED + "無効な入力です。\n1または2を入力してください。");
                    extendTask(player, key);
                }
            }
        });
        addTaskRunnable(player, playerActions, key);
    }

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
            lores.add("at " + date.replace("-", "/"));
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

    private void addTaskRunnable(Player player, Map<String, MessageRunnable> playerActions, String key) {
        EventListener.playerInputerMap.put(player, playerActions);
        scheduleNewTask(player, inputPeriod, key);
        try (Connection connection3 = db.getConnection()) {
            SocketSwitch ssw = sswProvider.get();
            ssw.sendVelocityServer(connection3, "inputMode->on->name->" + player.getName() + "->");
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("An SQLException | ClassNotFoundException error occurred: {}", e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                logger.error(element.toString());
            }
        }
    }

    private void extendTask(Player player, String key) {
        cancelTask(player);
        scheduleNewTask(player, inputPeriod, key);
    }

    private void scheduleNewTask(Player player, int delaySeconds, String key) {
        BukkitTask newTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.sendMessage(ChatColor.RED + "入力がタイムアウトしました。");
            removeCancelTaskRunnable(player, key);
        }, 20 * delaySeconds);
        if (EventListener.playerTaskMap.containsKey(player)) {
            Map<String, BukkitTask> playerTasks = EventListener.playerTaskMap.get(player);
            playerTasks.put(key, newTask);
            EventListener.playerTaskMap.put(player, playerTasks);
        } else {
            Map<String, BukkitTask> playerTasks = new HashMap<>();
            playerTasks.put(key, newTask);
            EventListener.playerTaskMap.put(player, playerTasks);
        }
    }

    private boolean checkIsOtherInputMode(Player player)  {
        if (EventListener.playerInputerMap.containsKey(player) && EventListener.playerTaskMap.containsKey(player)) {
            Map<String, MessageRunnable> playerActions = EventListener.playerInputerMap.get(player);
            Map<String, BukkitTask> playerTasks = EventListener.playerTaskMap.get(player);
            // playerActions, playerTasksにACTIONS_KEY以外が含まれているとき
            boolean isInputMode = playerActions.entrySet().stream().anyMatch(entry -> !entry.getKey().equals(ImageMap.ACTIONS_KEY)),
                isTaskMode = playerTasks.entrySet().stream().anyMatch(entry -> !entry.getKey().equals(ImageMap.ACTIONS_KEY));
            return isInputMode || isTaskMode;
        }
        return false;
    }

    private boolean checkIsInputMode(Player player) {
        if (EventListener.playerInputerMap.containsKey(player) && EventListener.playerTaskMap.containsKey(player)) {
            Map<String, MessageRunnable> playerActions = EventListener.playerInputerMap.get(player);
            Map<String, BukkitTask> playerTasks = EventListener.playerTaskMap.get(player);
            // playerActions, playerTasksにACTIONS_KEYが含まれているとき
            boolean isInputMode = playerActions.containsKey(ImageMap.ACTIONS_KEY),
                isTaskMode = playerTasks.containsKey(ImageMap.ACTIONS_KEY);
            return isInputMode && isTaskMode;
        }
        return false;
    }

    private void cancelTask(Player player) {
        if (EventListener.playerTaskMap.containsKey(player)) {
            Map<String, BukkitTask> playerTasks = EventListener.playerTaskMap.get(player);
            if (playerTasks.containsKey(ImageMap.ACTIONS_KEY)) {
                BukkitTask task = playerTasks.get(ImageMap.ACTIONS_KEY);
                task.cancel();
            }
        }
    }

    private void removeCancelTaskRunnable(Player player, String key) {
        String playerName = player.getName();
        EventListener.playerInputerMap.entrySet().removeIf(entry -> entry.getKey().equals(player) && entry.getValue().containsKey(key));
        // タスクをキャンセルしてから、playerTaskMapから削除する
        EventListener.playerTaskMap.entrySet().stream()
            .filter(entry -> entry.getKey().equals(player) && entry.getValue().containsKey(key))
            .forEach(entry -> {
                BukkitTask task = entry.getValue().get(key);
                task.cancel();
                entry.getValue().remove(key);
            });
        try (Connection connection2 = db.getConnection()) {
            SocketSwitch ssw = sswProvider.get();
            ssw.sendVelocityServer(connection2, "inputMode->off->name->" + playerName + "->");
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("An SQLException | ClassNotFoundException error occurred: {}", e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                logger.error(element.toString());
            }
        }
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