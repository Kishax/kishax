package spigot;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

public class ImageMapRenderer extends MapRenderer {
    private final common.Main plugin;
    private BufferedImage image;
    private final String pathToImageFile;
    private boolean rendered = false;

    public ImageMapRenderer(common.Main plugin, BufferedImage image, String pathToImageFile) {
        this.plugin = plugin;
        this.image = image;
        this.pathToImageFile = pathToImageFile;
    }

    @Override
    public void render(MapView mapView, MapCanvas mapCanvas, Player player) {
        if (rendered) return;
        // 画像がnullの場合、ファイルから読み込む
        if (image == null) {
            try {
                image = loadImage(pathToImageFile);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "An IOException error occurred: {0}", e.getMessage());
                for (StackTraceElement element : e.getStackTrace()) {
                    plugin.getLogger().severe(element.toString());
                }
                return;
            }
        }
        mapCanvas.drawImage(0, 0, image);
        rendered = true;
    }

    public static BufferedImage loadImage(String pathToImageFile) throws IOException {
        File imageFile = new File(pathToImageFile);
        return ImageIO.read(imageFile);
    }
}