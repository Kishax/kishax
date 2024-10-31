package spigot;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapView;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.logging.Level;

import com.google.inject.Inject;

public class ImageMap {
    private final common.Main plugin;
    private final Database db;
    @Inject
    public ImageMap(common.Main plugin, Database db) {
        this.plugin = plugin;
        this.db = db;
    }

    public void executeImageMap(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            if (args.length < 1) {
                player.sendMessage("使用法: /givemap <画像ファイル名>");
                return;
            }
            String fileName = args[0];
            File imageFile = new File(plugin.getDataFolder(), fileName);
            if (!imageFile.exists()) {
                player.sendMessage("画像ファイルが見つかりません: " + fileName);
                return;
            }
            try {
                BufferedImage image = ImageIO.read(imageFile);
                ItemStack mapItem = createMapWithImage(player, image);
                player.getInventory().addItem(mapItem);
                player.sendMessage("地図を渡しました: " + fileName);
            } catch (IOException e) {
                player.sendMessage("画像の読み込みに失敗しました: " + fileName);
                plugin.getLogger().log(Level.SEVERE, "An IOException error occurred: {0}", e.getMessage());
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
            if (args.length < 1) {
                player.sendMessage("使用法: /giveqrmap <URL>");
                return;
            }
            String url = args[0];
            try {
                BufferedImage qrImage = generateQRCodeImage(url);
                String filePath = saveImageToFileSystem(qrImage, player.getUniqueId().toString());
                saveMetadataToDatabase(player.getUniqueId().toString(), filePath, url);
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

    private BufferedImage generateQRCodeImage(String text) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        int width = 128;
        int height = 128;
        var bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }

    private String saveImageToFileSystem(BufferedImage image, String playerId) throws IOException {
        LocalDate today = LocalDate.now();
        Path dirPath = Paths.get(plugin.getDataFolder().getAbsolutePath(), today.toString());
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }

        String fileName = playerId + ".png";
        Path filePath = dirPath.resolve(fileName);
        ImageIO.write(image, "png", filePath.toFile());
        return filePath.toString();
    }

    private void saveMetadataToDatabase(String playerId, String filePath, String url) throws SQLException, ClassNotFoundException {
        try (Connection conn = db.getConnection()) {
            String query = "INSERT INTO player_images (player_id, file_path, url, date) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(query)) {
                ps.setString(1, playerId);
                ps.setString(2, filePath);
                ps.setString(3, url);
                ps.setDate(4, java.sql.Date.valueOf(LocalDate.now()));
                ps.executeUpdate();
            }
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