package keyp.forev.fmc.spigot.server.render;

import java.awt.image.BufferedImage;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

public class ImageMapRenderer extends MapRenderer {
    private final BufferedImage image;
    private boolean rendered = false;

    public ImageMapRenderer(BufferedImage image) {
        this.image = image;
    }

    @Override
    public void render(MapView mapView, MapCanvas mapCanvas, Player player) {
        if (rendered) return;
        mapCanvas.drawImage(0, 0, image);
        rendered = true;
    }
}