package net.kishax.mc.spigot.server.imagemap;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

import javax.imageio.ImageIO;

import org.slf4j.Logger;

/**
 * ローカルファイルシステムへの画像保存・取得実装
 */
public class LocalImageStorage implements ImageStorage {
    
    private final Logger logger;
    private final String baseDirectory;
    
    public LocalImageStorage(Logger logger, String baseDirectory) {
        this.logger = logger;
        this.baseDirectory = baseDirectory;
    }
    
    @Override
    public CompletableFuture<Void> saveImage(BufferedImage image, String imageUUID, String ext, LocalDate date) {
        return CompletableFuture.runAsync(() -> {
            try {
                String dateStr = date.toString().replace("-", "");
                Path dirPath = Paths.get(baseDirectory, dateStr);
                
                if (!Files.exists(dirPath)) {
                    Files.createDirectories(dirPath);
                }
                
                String fileName = imageUUID + "." + ext;
                Path filePath = dirPath.resolve(fileName);
                
                ImageIO.write(image, ext, filePath.toFile());
                logger.info("Saved image to local storage: {}", filePath);
                
            } catch (IOException e) {
                logger.error("Failed to save image to local storage: {}", e.getMessage());
                throw new RuntimeException("Failed to save image locally", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<BufferedImage> loadImage(String imageUUID, String ext, LocalDate date) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String dateStr = date.toString().replace("-", "");
                Path filePath = Paths.get(baseDirectory, dateStr, imageUUID + "." + ext);
                
                if (!Files.exists(filePath)) {
                    logger.warn("Image not found in local storage: {}", filePath);
                    return null;
                }
                
                BufferedImage image = ImageIO.read(filePath.toFile());
                logger.info("Loaded image from local storage: {}", filePath);
                return image;
                
            } catch (IOException e) {
                logger.error("Failed to load image from local storage: {}", e.getMessage());
                return null;
            }
        });
    }
    
    @Override
    public CompletableFuture<Boolean> exists(String imageUUID, String ext, LocalDate date) {
        return CompletableFuture.supplyAsync(() -> {
            String dateStr = date.toString().replace("-", "");
            Path filePath = Paths.get(baseDirectory, dateStr, imageUUID + "." + ext);
            return Files.exists(filePath);
        });
    }
    
    @Override
    public CompletableFuture<Boolean> deleteImage(String imageUUID, String ext, LocalDate date) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String dateStr = date.toString().replace("-", "");
                Path filePath = Paths.get(baseDirectory, dateStr, imageUUID + "." + ext);
                
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    logger.info("Deleted image from local storage: {}", filePath);
                    return true;
                }
                return false;
                
            } catch (IOException e) {
                logger.error("Failed to delete image from local storage: {}", e.getMessage());
                return false;
            }
        });
    }
    
    @Override
    public String getStorageType() {
        return "local";
    }
}






