package spigot;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.persistence.PersistentDataType;

import com.google.inject.Inject;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;

import net.md_5.bungee.api.ChatColor;

public class ImageMap {
    public static List<String> args2 = new ArrayList<>(Arrays.asList("create", "createqr", "q"));
    private final common.Main plugin;
    private final Database db;
    private final String serverName;
    @Inject
    public ImageMap(common.Main plugin, Database db, ServerHomeDir shd) {
        this.plugin = plugin;
        this.db = db;
        this.serverName = shd.getServerName();
    }

    public void executeQ(CommandSender sender, Command command, String label, String[] args, boolean q) {
        if (sender instanceof Player player) {
            int argsLength = args.length;
            String otp;
            if (q) {
                otp = args[0];
                if (argsLength < 1) {
                    player.sendMessage("使用法: /q <code>");
                    return;
                }
            } else {
                otp = args[1];
                if (argsLength < 2) {
                    player.sendMessage("使用法: /fmc q <code>");
                    return;
                }
            }
            try (Connection conn = db.getConnection()) {
                if (checkOTPIsCorrect(conn, otp)) {
                    Map<String, Object> imageInfo = getImageInfoByOTP(conn, otp);
                    //plugin.getLogger().log(Level.INFO, "Image info: {0}", imageInfo);
                    String title = (String) imageInfo.get("title"),
                        comment = (String) imageInfo.get("comment"),
                        url = (String) imageInfo.get("url"),
                        date = formatDate2(((Date) imageInfo.get("date")).toLocalDate());
                    boolean isQr = (boolean) imageInfo.get("isqr");
                    String[] imageArgs = new String[] {title, comment, url, date};
                    
                } else {
                    player.sendMessage(ChatColor.RED + "ワンタイムパスワードが間違っています。");
                }

            } catch (SQLException | ClassNotFoundException e) {
                player.sendMessage("画像の読み込みに失敗しました。");
                plugin.getLogger().log(Level.SEVERE, "An SQLException | ClassNotFoundException error occurred: {0}", e.getMessage());
                for (StackTraceElement element : e.getStackTrace()) {
                    plugin.getLogger().severe(element.toString());
                }
            }
        } else {
            if (sender != null) {
                sender.sendMessage("このコマンドはプレイヤーのみが実行できます。");
            }
        }
    }
    
    /*public void executeImageMap(CommandSender sender, Command command, String label, String[] args) {
        executeImageMap(sender, command, label, args, eArgs);
    }*/

    public void executeImageMap(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            if (args.length < 3) {
                player.sendMessage("使用法: /fmc im create <url> [Optional: <title> <comment>]");
                return;
            }
            String playerName = player.getName(),
                playerUUID = player.getUniqueId().toString();
            String imageUUID = UUID.randomUUID().toString(),
                url = args[2],
                title = (args.length > 4 && !args[3].isEmpty()) ? args[3]: "無名のタイトル",
                comment = (args.length > 5 && !args[4].isEmpty()) ? args[4]: "コメントなし";
            // 
            if (!isValidURL(url)) {
                player.sendMessage("無効なURLです。");
                return;
            }
            //
            try (Connection conn = db.getConnection()) {
                LocalDate now = LocalDate.now();
                URL getUrl = new URI(url).toURL();
                HttpURLConnection connection = (HttpURLConnection) getUrl.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();
                String ext;
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
                BufferedImage image =  ImageIO.read(getUrl);
                if (image == null) {
                    player.sendMessage("指定のURLは規定の拡張子を持ちません。");
                    return;
                }
                String fullPath = getImageSaveFolder(conn) + "/" + now.toString().replace("-", "") + "/" + imageUUID + "." + ext;
                // リサイズ前の画像を保存
                saveImageToFileSystem(conn, image, imageUUID, ext);
                BufferedImage resizedImage = resizeImage(image, 128, 128);
                List<String> lores = new ArrayList<>();
                lores.add("<イメージマップ>");
                List<String> commentLines = Arrays.stream(comment.split("\n"))
                                  .map(String::trim)
                                  .collect(Collectors.toList());
                lores.addAll(commentLines);
                lores.add("created by " + playerName);
                lores.add("at " + now.toString().replace("-", "/"));
                ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
                MapView mapView = Bukkit.createMap(player.getWorld());
                int mapId = mapView.getId();
                mapView.getRenderers().clear();
                mapView.addRenderer(new ImageMapRenderer(plugin, resizedImage, fullPath));
                var meta = (org.bukkit.inventory.meta.MapMeta) mapItem.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(title);
                    meta.setLore(lores);
                    meta.setMapView(mapView);
                    meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "custom_image"), PersistentDataType.STRING, "true");
                    mapItem.setItemMeta(meta);
                }
                db.insertLog(conn, "INSERT INTO images (name, uuid, server, mapid, title, imuuid, ext, url, comment, isqr, date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", new Object[] {playerName, playerUUID, serverName, mapId, title, imageUUID, ext, url, comment, false, java.sql.Date.valueOf(LocalDate.now())});
                player.getInventory().addItem(mapItem);
                player.sendMessage("画像地図を渡しました: " + url);
            } catch (IOException | SQLException | URISyntaxException | ClassNotFoundException e) {
                player.sendMessage("画像のダウンロードまたは保存に失敗しました: " + url);
                plugin.getLogger().log(Level.SEVERE, "An IOException | SQLException | URISyntaxException | ClassNotFoundException error occurred: {0}", e.getMessage());
                for (StackTraceElement element : e.getStackTrace()) {
                    plugin.getLogger().severe(element.toString());
                }
            }
        } else {
            if (sender != null) {
                sender.sendMessage("このコマンドはプレイヤーのみが実行できます。");
            }
        }
    }

    public void executeQRMap(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            if (args.length < 3) {
                player.sendMessage("使用法: /fmc im createqr <url> [Optional: <title> <comment>]");
                return;
            }
            String playerName = player.getName(),
                playerUUID = player.getUniqueId().toString();
            String imageUUID = UUID.randomUUID().toString(),
                url = args[2],
                title = (args.length > 4 && !args[3].isEmpty()) ? args[3]: "無名のタイトル",
                comment = (args.length > 5 && !args[4].isEmpty()) ? args[4]: "コメントなし",
                ext = "png";
            try (Connection conn = db.getConnection()) {
                LocalDate now = LocalDate.now();


                String fullPath = getImageSaveFolder(conn) + "/" + now.toString().replace("-", "") + "/" + imageUUID + "." + ext;
                BufferedImage qrImage = generateQRCodeImage(url);
                saveImageToFileSystem(conn, qrImage, imageUUID, ext);
                List<String> lores = new ArrayList<>();
                lores.add("<QRコード>");
                List<String> commentLines = Arrays.stream(comment.split("\n"))
                                  .map(String::trim)
                                  .collect(Collectors.toList());
                lores.addAll(commentLines);
                lores.add("created by " + playerName);
                lores.add("at " + now.toString().replace("-", "/"));
                ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
                MapView mapView = Bukkit.createMap(player.getWorld());
                int mapId = mapView.getId();
                mapView.getRenderers().clear();
                mapView.addRenderer(new ImageMapRenderer(plugin, qrImage, fullPath));
                var meta = (org.bukkit.inventory.meta.MapMeta) mapItem.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(title);
                    meta.setLore(lores);
                    meta.setMapView(mapView);
                    meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "custom_image"), PersistentDataType.STRING, "true");
                    mapItem.setItemMeta(meta);
                }
                db.insertLog(conn, "INSERT INTO images (name, uuid, server, mapid, title, imuuid, ext, url, comment, isqr, date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", new Object[] {playerName, playerUUID, serverName, mapId, title, imageUUID, ext, url, comment, true, java.sql.Date.valueOf(now)});
                player.getInventory().addItem(mapItem);
                player.sendMessage("QRコード地図を渡しました: " + url);
            } catch (WriterException | IOException | SQLException | ClassNotFoundException e) {
                player.sendMessage("QRコードの生成または保存に失敗しました: " + url);
                plugin.getLogger().log(Level.SEVERE, "A WriterException | IOException | SQLException | ClassNotFoundException error occurred: {0}", e.getMessage());
                for (StackTraceElement element : e.getStackTrace()) {
                    plugin.getLogger().severe(element.toString());
                }
            }
        } else {
            if (sender != null) {
                sender.sendMessage("このコマンドはプレイヤーのみが実行できます。");
            }
        }
    }

    public void loadAllItemInThisServerFrames(Connection conn) throws SQLException, ClassNotFoundException {
        Map<Integer, Map<String, Object>> serverImageInfo = getThisServerImages(conn);
        for (World world : Bukkit.getWorlds()) {
            for (ItemFrame itemFrame : world.getEntitiesByClass(ItemFrame.class)) {
                ItemStack item = itemFrame.getItem();
                if (item.getType() == Material.FILLED_MAP) {
                    MapMeta mapMeta = (MapMeta) item.getItemMeta();
                    if (mapMeta != null && mapMeta.hasMapView()) {
                        if (mapMeta.getPersistentDataContainer().has(new NamespacedKey(plugin, "custom_image"), PersistentDataType.STRING)) {
                            MapView mapView = mapMeta.getMapView();
                            if (mapView != null) {
                                int mapId = mapView.getId();
                                if (serverImageInfo.containsKey(mapId)) {
                                    Map<String, Object> imageInfo = serverImageInfo.get(mapId);
                                    boolean isQr = (boolean) imageInfo.get("isqr");
                                    Date date = (Date) imageInfo.get("date");
                                    String imageUUID = (String) imageInfo.get("imuuid");
                                    String ext = (String) imageInfo.get("ext");
                                    String fullPath = getFullPath(conn, date, imageUUID, ext);
                                    plugin.getLogger().log(Level.INFO, "Replacing image to the map(No.{0})...", new Object[] {mapId, fullPath});
                                    try {
                                        BufferedImage image = loadImage(fullPath);
                                        if (image != null) {
                                            // QRコードならリサイズしない
                                            image = isQr ? image : resizeImage(image, 128, 128);
                                            mapView.getRenderers().clear();
                                            mapView.addRenderer(new ImageMapRenderer(plugin, image, fullPath));
                                        } 
                                    } catch (IOException e) {
                                        plugin.getLogger().log(Level.SEVERE, "マップId {0} の画像の読み込みに失敗しました: {1}", new Object[] {mapId, fullPath});
                                        plugin.getLogger().log(Level.SEVERE, "An IOException error occurred: {0}", e.getMessage());
                                        for (StackTraceElement element : e.getStackTrace()) {
                                            plugin.getLogger().severe(element.toString());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        plugin.getLogger().info("Loaded all item frames.");
    }

    private void giveMapToPlayer(Player player, int mapId) {
        boolean found = false;
        ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
        for (World world : Bukkit.getWorlds()) {
            for (ItemFrame itemFrame : world.getEntitiesByClass(ItemFrame.class)) {
                ItemStack item = itemFrame.getItem();
                if (item.getType() == Material.FILLED_MAP) {
                    MapMeta mapMeta = (MapMeta) item.getItemMeta();
                    if (mapMeta != null && mapMeta.hasMapView()) {
                        MapView mapView = mapMeta.getMapView();
                        if (mapView != null && mapView.getId() == mapId) {
                            found = true;
                            mapMeta.setMapView(mapView);
                            mapItem.setItemMeta(mapMeta);
                            player.getInventory().addItem(mapItem);
                            player.sendMessage("地図ID " + mapId + " を渡しました。");
                            return;
                        }
                    }
                }
            }
        }
        if (!found) {
            player.sendMessage("地図ID " + mapId + " が見つかりません。");
        }
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

    private boolean checkOTPIsCorrect(Connection conn, String code) throws SQLException, ClassNotFoundException {
        String query = "SELECT * FROM images WHERE otp=? LIMIT 1;";
        PreparedStatement ps = conn.prepareStatement(query);
        ps.setString(1, code);
        ResultSet rs = ps.executeQuery();
        return rs.next();
    }

    private Map<String, Map<String, Object>> getImageInfo(Connection conn) {
        Map<String, Map<String, Object>> imageInfo = new HashMap<>();
        String query = "SELECT * FROM images;";
        try (Connection connection = (conn != null && !conn.isClosed()) ? conn : db.getConnection();
            PreparedStatement ps = connection.prepareStatement(query);
            ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> rowMap = new HashMap<>();
                Date date = rs.getDate("date");
                String dateString = formatDate(date.toLocalDate());
                int columnCount = rs.getMetaData().getColumnCount();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = rs.getMetaData().getColumnName(i);
                    if (!columnName.equals("date")) {
                        rowMap.put(columnName, rs.getObject(columnName));
                    }
                }
                imageInfo.computeIfAbsent(dateString, _ -> rowMap);
            }
        } catch (SQLException | ClassNotFoundException e) {
            plugin.getLogger().log(Level.SEVERE, "An error occurred while loading images: {0}", e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                plugin.getLogger().severe(element.toString());
            }
        }
        return imageInfo;
    }

    private BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        Image resultingImage = originalImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
        BufferedImage outputImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = outputImage.createGraphics();
        g2d.drawImage(resultingImage, 0, 0, null);
        g2d.dispose();
        return outputImage;
    }
    
    private Map<Integer, Map<String, Object>> getThisServerImages(Connection conn) throws SQLException, ClassNotFoundException {
        Map<Integer, Map<String, Object>> serverImageInfo = new HashMap<>();
        String query = "SELECT * FROM images WHERE server=?;";
        try (Connection connection = (conn != null && !conn.isClosed()) ? conn : db.getConnection();
            PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, serverName);
            try (ResultSet rs = ps.executeQuery()) {
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
                    serverImageInfo.computeIfAbsent(mapId, _ -> rowMap);
                }
            }
        }
        return serverImageInfo;
    }

    private String formatDate(LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        return date.format(formatter);
    }

    private String formatDate2(LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        return date.format(formatter);
    }

    private BufferedImage loadImage(String filePath) throws IOException {
        File imageFile = new File(filePath);
        if (!imageFile.exists()) {
            plugin.getLogger().log(Level.SEVERE, "Image file does not exist: {0}", filePath);
            return null;
        }
        return ImageIO.read(imageFile);
    }

    private String getFullPath(Connection conn, Date date, String imageUUID, String ext) throws SQLException, ClassNotFoundException {
        return getImageSaveFolder(conn) + "/" + date.toString().replace("-", "") + "/" + imageUUID + "." + ext;
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
    
    private String getImageSaveFolder(Connection conn) throws SQLException, ClassNotFoundException {
        try (Connection connection = (conn != null && !conn.isClosed()) ? conn : db.getConnection();) {
            String query = "SELECT value FROM settings WHERE name=?;";
            try (PreparedStatement ps = connection.prepareStatement(query)) {
                ps.setString(1, "image_folder");
                try (ResultSet rs2 = ps.executeQuery()) {
                    if (rs2.next()) {
                        return rs2.getString("value");
                    }
                }
            }
        }
        return null;
    }

    private BufferedImage generateQRCodeImage(String text) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        int width = 128;
        int height = 128;
        var bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }

    private void saveImageToFileSystem(Connection conn, BufferedImage image, String imageUUID, String ext) throws IOException, SQLException, ClassNotFoundException {
        if (getImageSaveFolder(conn) instanceof String folder) {
            Path dirPath = Paths.get(folder, LocalDate.now().toString().replace("-", ""));
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }
            String fileName = imageUUID + "." + ext;
            Path filePath = dirPath.resolve(fileName);
            ImageIO.write(image, ext, filePath.toFile());
        } else {
            plugin.getLogger().severe("画像保存フォルダが見つかりません。");
        }
    }
}