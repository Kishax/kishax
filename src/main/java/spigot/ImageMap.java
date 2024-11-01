package spigot;

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

public class ImageMap {
    public static List<String> args2 = new ArrayList<>(Arrays.asList("create", "createqr"));
    private final common.Main plugin;
    private final Database db;
    private final String serverName;
    @Inject
    public ImageMap(common.Main plugin, Database db, ServerHomeDir shd) {
        this.plugin = plugin;
        this.db = db;
        this.serverName = shd.getServerName();
    }

    // 別にimageUUIDをキーにしなくてもいいかもしれない(使わないし)
    // date -> {playerName -> {imageUUID -> {ext -> {title -> {comment -> {filePath -> image}}}}}
    public Map<String, Map<String, Map<String, Map<String, String>>>> getImageInfo(Connection conn) {
        Map<String, Map<String, Map<String, Map<String, String>>>> imageInfo = new HashMap<>();
        String query = "SELECT * FROM images;";
        try (Connection connection = (conn != null && !conn.isClosed()) ? conn : db.getConnection();
            PreparedStatement ps = connection.prepareStatement(query);
            ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, String> rowMap = new HashMap<>();
                String playerName = rs.getString("name");
                Date date = rs.getDate("date");
                String dateString = formatDate(date.toLocalDate());
                int columnCount = rs.getMetaData().getColumnCount();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = rs.getMetaData().getColumnName(i);
                    if (!columnName.equals("name") || !columnName.equals("date")) {
                        rowMap.put(columnName, rs.getString(columnName));
                    }
                }
                imageInfo.computeIfAbsent(dateString, _ -> new HashMap<>())
                         .computeIfAbsent(playerName, _ -> new HashMap<>())
                        .put(rs.getString("imuuid"), rowMap);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "An error occurred while loading images: {0}", e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                plugin.getLogger().severe(element.toString());
            }
        }
        return imageInfo;
    }
    /*public void loadAllImages(Connection conn) {
        String query = "SELECT * FROM images;";
        try (Connection connection = (conn != null && !conn.isClosed()) ? conn : db.getConnection();
            PreparedStatement ps = connection.prepareStatement(query);
            ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Date date = rs.getDate("date");
                String formattedDate = formatDate(date.toLocalDate());
                String imageUUID = rs.getString("imuuid");
                String ext = rs.getString("ext");
                String title = rs.getString("title");
                String comment = rs.getString("comment");
                // getImageSaveFolder(conn)
                String filePath = "/opt/mc/images" + "/" + formattedDate + "/" + imageUUID + "." + ext;
                BufferedImage image = loadImage(filePath);
                if (image != null) {
                    List<String> lores = Arrays.stream(comment.split("\n"))
                                                .map(String::trim)
                                                .collect(Collectors.toList());
                    createMapWithImage(image, title, lores, filePath);
                }
            }
        } catch (SQLException | IOException | ClassNotFoundException e) {
            plugin.getLogger().log(Level.SEVERE, "An error occurred while loading images: {0}", e.getMessage());
            for (StackTraceElement element : e.getStackTrace()) {
                plugin.getLogger().severe(element.toString());
            }
        }
    }*/

    public void executeImageMap(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            if (args.length < 5) {
                player.sendMessage("使用法: /fmc im create <title> <comment> <url>");
                return;
            }
            String playerName = player.getName(),
                playerUUID = player.getUniqueId().toString();
            String imageUUID = UUID.randomUUID().toString(),
                url = args[4],
                comment = args[3],
                title = args[2];
            if (!isValidURL(url)) {
                player.sendMessage("無効なURLです。");
                return;
            }
            try (Connection conn = db.getConnection()) {
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
                String fullPath = getFullPath(conn, imageUUID, ext);
                saveImageToFileSystem(conn, image, imageUUID, ext);
                db.insertLog(conn, "INSERT INTO images (name, uuid, server, title, imuuid, ext, url, comment, isqr, date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", new Object[] {playerName, playerUUID, serverName, title, imageUUID, ext, url, comment, false, java.sql.Date.valueOf(LocalDate.now())});
                List<String> lores = new ArrayList<>();
                lores.add("QRコード");
                List<String> commentLines = Arrays.stream(comment.split("\n"))
                                  .map(String::trim)
                                  .collect(Collectors.toList());
                lores.addAll(commentLines);
                ItemStack mapItem = createMapWithImage(player, image, title, lores, fullPath);
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
            if (args.length < 5) {
                player.sendMessage("使用法: /fmc im createqr <title> <comment> <url>");
                return;
            }
            String playerName = player.getName(),
                playerUUID = player.getUniqueId().toString();
            String imageUUID = UUID.randomUUID().toString(),
                url = args[4],
                comment = args[3],
                title = args[2],
                ext = "png";
            try (Connection conn = db.getConnection()) {
                String fullPath = getFullPath(conn, imageUUID, ext);
                BufferedImage qrImage = generateQRCodeImage(url);
                saveImageToFileSystem(conn, qrImage, imageUUID, ext);
                db.insertLog(conn, "INSERT INTO images (name, uuid, server, title, imuuid, ext, url, comment, isqr, date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", new Object[] {playerName, playerUUID, serverName, title, imageUUID, ext, url, comment, true, java.sql.Date.valueOf(LocalDate.now())});
                List<String> lores = new ArrayList<>();
                lores.add("QRコード");
                List<String> commentLines = Arrays.stream(comment.split("\n"))
                                  .map(String::trim)
                                  .collect(Collectors.toList());
                lores.addAll(commentLines);
                ItemStack mapItem = createMapWithImage(player, qrImage, title, lores, fullPath);
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

    private Map<String, String> getThisServerImages(Connection conn) throws SQLException, ClassNotFoundException {
        Map<String, String> images = new HashMap<>();
        String query = "SELECT date ,imuuid, ext FROM images WHERE server=?;";
        try (Connection connection = (conn != null && !conn.isClosed()) ? conn : db.getConnection();
            PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, serverName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    images.put(rs.getString("imuuid"), rs.getString("ext"));
                }
            }
        }
        return images;
    }

    private void loadAllItemFrames() {
        for (World world : Bukkit.getWorlds()) {
            for (ItemFrame itemFrame : world.getEntitiesByClass(ItemFrame.class)) {
                ItemStack item = itemFrame.getItem();
                if (item.getType() == Material.FILLED_MAP) {
                    MapMeta mapMeta = (MapMeta) item.getItemMeta();
                    if (mapMeta != null && mapMeta.hasMapView()) {
                        if (mapMeta.getPersistentDataContainer().has(new NamespacedKey(plugin, "custom_image"), PersistentDataType.STRING)) {
                            MapView mapView = mapMeta.getMapView();
                            if (mapView != null) {
                                mapView.getRenderers().clear();
                                mapView.addRenderer(new ImageMapRenderer(plugin, null, getPathFromMapView(mapView)));
                            }
                        }
                    }
                }
            }
        }
    }

    private String formatDate(LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
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

    private String getPathFromMapView(MapView mapView) {
        // MapViewのIDを使用してファイルパスを生成
        plugin.getLogger().log(Level.INFO, "MapView ID: {0}", mapView.getId());
        return "/opt/mc/images/" + mapView.getId() + ".png";
    }

    private String getFullPath(Connection conn, String imageUUID, String ext) throws SQLException, ClassNotFoundException {
        return getImageSaveFolder(conn) + "/" + LocalDate.now().toString().replace("-", "") + "/" + imageUUID + "." + ext;
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

    private void createMapWithImage(BufferedImage image, String title, List<String> lores, String fullPath) {
        MapView mapView = Bukkit.createMap(Bukkit.getWorlds().get(0));
        mapView.getRenderers().clear();
        mapView.addRenderer(new ImageMapRenderer(plugin, image, fullPath));
    }

    private ItemStack createMapWithImage(Player player, BufferedImage image, String title, List<String> lores, String fullPath) {
        ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
        MapView mapView = Bukkit.createMap(player.getWorld());
        mapView.getRenderers().clear();
        mapView.addRenderer(new ImageMapRenderer(plugin, image, fullPath));
        var meta = (org.bukkit.inventory.meta.MapMeta) mapItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(title);
            meta.setLore(lores);
            meta.setMapView(mapView);
            mapItem.setItemMeta(meta);
        }
        return mapItem;
    }
}