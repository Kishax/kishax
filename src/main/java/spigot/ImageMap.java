package spigot;

import java.awt.image.BufferedImage;
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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapView;

import com.google.inject.Inject;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;

public class ImageMap {
    public static List<String> args2 = new ArrayList<>(Arrays.asList("create", "createqr"));
    private final common.Main plugin;
    private final Database db;
    @Inject
    public ImageMap(common.Main plugin, Database db) {
        this.plugin = plugin;
        this.db = db;
    }

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
                saveImageToFileSystem(conn, image, imageUUID, ext);
                db.insertLog(conn, "INSERT INTO images (name, uuid, title, imuuid, ext, url, comment, isqr, date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", new Object[] {playerName, playerUUID, title, imageUUID, ext, url, comment, false, java.sql.Date.valueOf(LocalDate.now())});
                ItemStack mapItem = createMapWithImage(player, image);
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
                BufferedImage qrImage = generateQRCodeImage(url);
                saveImageToFileSystem(conn, qrImage, imageUUID, ext);
                db.insertLog(conn, "INSERT INTO images (name, uuid, title, imuuid, ext, url, comment, isqr, date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", new Object[] {playerName, playerUUID, title, imageUUID, ext, url, comment, true, java.sql.Date.valueOf(LocalDate.now())});
                ItemStack mapItem = createMapWithImage(player, qrImage);
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

    public static boolean isValidURL(String urlString) {
        try {
            URI uri = new URI(urlString);
            uri.toURL();
            return true;
        } catch (MalformedURLException | URISyntaxException e) {
            return false;
        }
    }
    
    private String getImageSaveFolder(Connection conn) throws SQLException, ClassNotFoundException {
        try (Connection connDefault = conn != null ? conn : db.getConnection();) {
            String query = "SELECT value FROM settings WHERE name=?;";
            try (PreparedStatement ps = connDefault.prepareStatement(query)) {
                ps.setString(1, "image_folder");
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("image_folder");
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
            LocalDate today = LocalDate.now();
            Path dirPath = Paths.get(folder, today.toString().replace("-", ""));
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

    private ItemStack createMapWithImage(Player player, BufferedImage image) {
        ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
        MapView mapView = Bukkit.createMap(player.getWorld());
        mapView.getRenderers().clear();
        mapView.addRenderer(new ImageMapRenderer(image));
        var meta = (org.bukkit.inventory.meta.MapMeta) mapItem.getItemMeta();
        if (meta != null) {
            meta.setMapView(mapView);
            mapItem.setItemMeta(meta);
        }
        return mapItem;
    }
}